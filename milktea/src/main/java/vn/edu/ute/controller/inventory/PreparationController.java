package vn.edu.ute.controller.inventory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.edu.ute.dto.inventory.PreparationBatchItemRequest;
import vn.edu.ute.dto.inventory.PreparationBatchRequest;
import vn.edu.ute.entity.inventory.Material;
import vn.edu.ute.entity.inventory.PreparationBatch;
import vn.edu.ute.entity.inventory.PreparationBatchItem;
import vn.edu.ute.entity.inventory.PreparationItemRole;
import vn.edu.ute.service.inventory.PreparationService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/preparations")
public class PreparationController {

    private static final Long DEFAULT_BRANCH_ID = 1L;

    private final PreparationService preparationService;

    public PreparationController(
            PreparationService preparationService
    ) {
        this.preparationService = preparationService;
    }

    @PostMapping
    public ResponseEntity<PreparationBatch> createBatch(
            @RequestBody PreparationBatchRequest request
    ) {

        List<PreparationBatchItem> items =
                new ArrayList<>();

        for (PreparationBatchItemRequest itemRequest
                : request.getItems()) {

            Material material = new Material();
            material.setId(
                    itemRequest.getMaterialId()
            );

            PreparationBatchItem item =
                    new PreparationBatchItem();

            item.setMaterial(material);

            item.setItemRole(
                    PreparationItemRole.valueOf(
                            itemRequest.getItemRole()
                                    .toUpperCase()
                    )
            );

            item.setQuantity(
                    itemRequest.getQuantity()
            );

            items.add(item);
        }

        PreparationBatch batch =
                preparationService.createBatch(
                        DEFAULT_BRANCH_ID,
                        request.getBatchCode(),
                        null,
                        request.getNote(),
                        items
                );

        return ResponseEntity.ok(batch);
    }
}