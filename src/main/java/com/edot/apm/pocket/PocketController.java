package com.edot.apm.pocket;

import com.edot.apm.common.dto.ApiResponse;
import com.edot.apm.pocket.dto.*;
import com.edot.apm.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pocket")
public class PocketController {

    private final PocketService pocketService;

    public PocketController(PocketService pocketService) {
        this.pocketService = pocketService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PocketResponse>> getMyPocket(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PocketResponse response = pocketService.getPocketResponseByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Pocket retrieved successfully", response));
    }

    @PostMapping("/top-up")
    public ResponseEntity<ApiResponse<TransactionResponse>> topUp(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TopUpRequest request) {
        TransactionResponse response = pocketService.topUp(currentUser.getId(), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Pocket top-up completed successfully", response));
    }

    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<TransactionResponse>> pay(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentChargeRequest request) {
        TransactionResponse response = pocketService.pay(currentUser.getId(), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok("Payment processed successfully", response));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<TransactionResponse> response = pocketService.getTransactions(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Transactions retrieved successfully", response));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id) {
        TransactionResponse response = pocketService.getTransaction(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Transaction details retrieved successfully", response));
    }

    @PostMapping("/transactions/{id}/refund")
    public ResponseEntity<ApiResponse<TransactionResponse>> refund(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) RefundRequest request) {
        TransactionResponse response = pocketService.refund(currentUser.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Transaction refunded successfully", response));
    }
}
