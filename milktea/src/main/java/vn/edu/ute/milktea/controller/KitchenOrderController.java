package vn.edu.ute.milktea.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.InventoryDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.inventory.InventoryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kitchen/orders")
@RequiredArgsConstructor
public class KitchenOrderController {

    private final InventoryService inventoryService;
    private final AccountRepository accountRepository;

    @PostMapping("/{orderId}/incidents")
    public ResponseEntity<ApiResponse<InventoryDto.IncidentResponse>> reportIncident(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody InventoryDto.IncidentRequest request,
            @AuthenticationPrincipal CurrentActor actor) {

        Account kitchen = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        var result = inventoryService.recordIncident(orderId, request, kitchen);
        return ResponseEntity.status(201).body(ApiResponse.ok(result));
    }

    @GetMapping("/{orderId}/incidents")
    public ResponseEntity<ApiResponse<List<InventoryDto.IncidentResponse>>> getIncidents(
            @PathVariable("orderId") Long orderId) {

        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getIncidents(orderId)));
    }

    @PostMapping("/{orderId}/additional-consumptions")
    public ResponseEntity<ApiResponse<Void>> recordAdditionalConsumption(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody InventoryDto.AdditionalConsumptionRequest request,
            @AuthenticationPrincipal CurrentActor actor) {

        Account kitchen = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        inventoryService.recordAdditionalConsumption(orderId, request, kitchen);
        return ResponseEntity.status(201).body(ApiResponse.ok(null));
    }

    @PostMapping("/{orderId}/incidents/{incidentId}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolveIncident(
            @PathVariable("orderId") Long orderId,
            @PathVariable("incidentId") Long incidentId,
            @AuthenticationPrincipal CurrentActor actor) {

        Account kitchen = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        inventoryService.resolveIncident(orderId, incidentId, kitchen);
        return ResponseEntity.status(201).body(ApiResponse.ok(null));
    }
}
