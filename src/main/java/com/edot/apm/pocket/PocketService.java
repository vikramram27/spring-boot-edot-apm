package com.edot.apm.pocket;

import com.edot.apm.common.exception.BadRequestException;
import com.edot.apm.common.exception.ResourceNotFoundException;
import com.edot.apm.pocket.dto.*;
import com.edot.apm.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PocketService {

    private static final Logger logger = LoggerFactory.getLogger(PocketService.class);
    private static final String IDEMPOTENCY_PREFIX = "idempotency:pocket:";

    private final PocketRepository pocketRepository;
    private final PocketTransactionRepository transactionRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PocketService(PocketRepository pocketRepository,
                         PocketTransactionRepository transactionRepository,
                         StringRedisTemplate redisTemplate,
                         ObjectMapper objectMapper) {
        this.pocketRepository = pocketRepository;
        this.transactionRepository = transactionRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Pocket createPocketForUser(User user) {
        logger.info("Initializing pocket for user id: {}", user.getId());
        return pocketRepository.findByUserId(user.getId())
                .orElseGet(() -> pocketRepository.save(new Pocket(user, "USD")));
    }

    @Transactional
    public PocketResponse getPocketResponseByUserId(UUID userId) {
        Pocket pocket = getOrCreatePocket(userId);
        return mapToPocketResponse(pocket);
    }

    @Transactional
    public TransactionResponse topUp(UUID userId, TopUpRequest request, String idempotencyKey) {
        logger.info("Processing top up of {} for user id: {}, idempotencyKey: {}", request.getAmount(), userId, idempotencyKey);

        // 1. Check idempotency in Redis
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            TransactionResponse cached = getCachedIdempotentResponse(idempotencyKey);
            if (cached != null) {
                logger.info("Returning cached idempotent top up response for key: {}", idempotencyKey);
                return cached;
            }
        }

        // 2. Load pocket and credit balance
        Pocket pocket = getOrCreatePocket(userId);
        pocket.credit(request.getAmount());
        pocketRepository.save(pocket);

        // 3. Create transaction audit record
        String refId = "TOP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        PaymentMethod method = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.BANK_TRANSFER;
        String desc = (request.getDescription() != null && !request.getDescription().isBlank())
                ? request.getDescription()
                : "Top up via " + method;

        PocketTransaction txn = new PocketTransaction(
                pocket,
                userId,
                TransactionType.TOP_UP,
                request.getAmount(),
                pocket.getBalance(),
                pocket.getCurrency(),
                TransactionStatus.SUCCESS,
                method,
                null,
                idempotencyKey,
                refId,
                desc
        );

        PocketTransaction savedTxn = transactionRepository.save(txn);
        TransactionResponse response = TransactionResponse.fromEntity(savedTxn);

        // 4. Cache idempotency key if provided
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            cacheIdempotentResponse(idempotencyKey, response);
        }

        logger.info("Top up successful. Ref: {}, New Balance: {} {}", refId, pocket.getBalance(), pocket.getCurrency());
        return response;
    }

    @Transactional
    public TransactionResponse pay(UUID userId, PaymentChargeRequest request, String idempotencyKey) {
        logger.info("Processing payment charge of {} for order: {} by user id: {}, idempotencyKey: {}",
                request.getAmount(), request.getOrderId(), userId, idempotencyKey);

        // 1. Check idempotency in Redis
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            TransactionResponse cached = getCachedIdempotentResponse(idempotencyKey);
            if (cached != null) {
                logger.info("Returning cached idempotent payment response for key: {}", idempotencyKey);
                return cached;
            }
        }

        // 2. Load pocket and debit balance (fails with InsufficientBalanceException if balance < amount)
        Pocket pocket = getOrCreatePocket(userId);
        pocket.debit(request.getAmount());
        pocketRepository.save(pocket);

        // 3. Create transaction audit record
        String refId = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String desc = (request.getDescription() != null && !request.getDescription().isBlank())
                ? request.getDescription()
                : "Payment for order #" + request.getOrderId();

        PocketTransaction txn = new PocketTransaction(
                pocket,
                userId,
                TransactionType.PAYMENT,
                request.getAmount(),
                pocket.getBalance(),
                pocket.getCurrency(),
                TransactionStatus.SUCCESS,
                PaymentMethod.POCKET,
                request.getOrderId(),
                idempotencyKey,
                refId,
                desc
        );

        PocketTransaction savedTxn = transactionRepository.save(txn);
        TransactionResponse response = TransactionResponse.fromEntity(savedTxn);

        // 4. Cache idempotency key if provided
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            cacheIdempotentResponse(idempotencyKey, response);
        }

        logger.info("Payment successful. Ref: {}, Remaining Balance: {} {}", refId, pocket.getBalance(), pocket.getCurrency());
        return response;
    }

    @Transactional
    public TransactionResponse refund(UUID userId, UUID transactionId, RefundRequest request) {
        logger.info("Processing refund for transaction id: {} by user id: {}", transactionId, userId);

        PocketTransaction originalTxn = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (originalTxn.getType() != TransactionType.PAYMENT) {
            throw new BadRequestException("Only PAYMENT transactions can be refunded. Current transaction type: " + originalTxn.getType());
        }

        if (originalTxn.getStatus() != TransactionStatus.SUCCESS) {
            throw new BadRequestException("Cannot refund a transaction that is not in SUCCESS status");
        }

        // Check if already refunded
        String refundRefCheck = "REF-" + originalTxn.getReferenceId();
        if (transactionRepository.findByReferenceId(refundRefCheck).isPresent()) {
            throw new BadRequestException("This transaction has already been refunded: " + originalTxn.getReferenceId());
        }

        // Credit pocket back
        Pocket pocket = getOrCreatePocket(userId);
        pocket.credit(originalTxn.getAmount());
        pocketRepository.save(pocket);

        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason()
                : "Customer requested refund";

        PocketTransaction refundTxn = new PocketTransaction(
                pocket,
                userId,
                TransactionType.REFUND,
                originalTxn.getAmount(),
                pocket.getBalance(),
                pocket.getCurrency(),
                TransactionStatus.SUCCESS,
                PaymentMethod.POCKET,
                originalTxn.getOrderId(),
                null,
                refundRefCheck,
                "Refund for " + originalTxn.getReferenceId() + " (" + reason + ")"
        );

        PocketTransaction savedRefund = transactionRepository.save(refundTxn);
        logger.info("Refund successful. Ref: {}, Restored Balance: {} {}", refundRefCheck, pocket.getBalance(), pocket.getCurrency());
        return TransactionResponse.fromEntity(savedRefund);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(UUID userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID userId, UUID transactionId) {
        PocketTransaction txn = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
        return TransactionResponse.fromEntity(txn);
    }

    private Pocket getOrCreatePocket(UUID userId) {
        return pocketRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pocket not found for user id: " + userId));
    }

    private PocketResponse mapToPocketResponse(Pocket pocket) {
        return new PocketResponse(
                pocket.getId(),
                pocket.getUser().getId(),
                pocket.getBalance(),
                pocket.getCurrency(),
                pocket.getUpdatedAt()
        );
    }

    private TransactionResponse getCachedIdempotentResponse(String idempotencyKey) {
        try {
            String json = redisTemplate.opsForValue().get(IDEMPOTENCY_PREFIX + idempotencyKey);
            if (json != null) {
                return objectMapper.readValue(json, TransactionResponse.class);
            }
        } catch (Exception e) {
            logger.warn("Failed to read idempotency key from Redis: {}", e.getMessage());
        }
        return null;
    }

    private void cacheIdempotentResponse(String idempotencyKey, TransactionResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(IDEMPOTENCY_PREFIX + idempotencyKey, json, Duration.ofHours(24));
        } catch (Exception e) {
            logger.warn("Failed to cache idempotency key in Redis: {}", e.getMessage());
        }
    }
}
