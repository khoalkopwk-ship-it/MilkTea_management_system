package vn.edu.ute.milktea.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.TableDto;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.table.TableContextService;
import vn.edu.ute.milktea.service.table.TableSessionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TableController {

    private final TableContextService tableContextService;
    private final TableSessionService tableSessionService;
    private final DiningTableRepository tableRepository;

    @GetMapping({"/public/table-context", "/tables/context"})
    public ResponseEntity<ApiResponse<TableDto.TableContextResponse>> getTableContext(
            @RequestParam(value = "qrCode", required = false) String qrCode,
            @RequestParam(value = "table", required = false) String tableParam) {

        if (qrCode == null || qrCode.isBlank()) {
            if (tableParam != null && !tableParam.isBlank()) {
                DiningTable t = null;
                try {
                    Long id = Long.parseLong(tableParam.trim());
                    t = tableRepository.findById(id).orElse(null);
                } catch (NumberFormatException ignored) {}
                if (t == null) {
                    t = tableRepository.findByName(tableParam.trim()).orElse(null);
                }
                if (t != null) {
                    qrCode = t.getQrCode();
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(tableContextService.readByQr(qrCode)));
    }

    @GetMapping({"/cashier/tables", "/tables"})
    public ResponseEntity<ApiResponse<List<TableDto.TableItem>>> getTables() {
        List<DiningTable> tables = tableRepository.findByActiveTrueOrderByIdAsc();
        List<TableDto.TableItem> items = tables.stream().map(t -> TableDto.TableItem.builder()
                .id(t.getId())
                .name(t.getName())
                .qrCode(t.getQrCode())
                .status(t.getStatus())
                .activeSessionId(t.getActiveSessionId())
                .active(t.getActive())
                .build()
        ).toList();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/table-sessions/{sessionId}")
    public ResponseEntity<ApiResponse<TableDto.TableSessionResponse>> getSession(@PathVariable("sessionId") Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(tableSessionService.getSessionResponse(sessionId)));
    }

    @PostMapping("/cashier/tables/{id}/open-session")
    public ResponseEntity<ApiResponse<TableDto.TableItem>> openSession(
            @PathVariable("id") Long tableId,
            @AuthenticationPrincipal CurrentActor actor) {

        Long cashierId = actor != null ? actor.getAccountId() : null;
        var session = tableSessionService.openByCashier(tableId, cashierId);
        DiningTable table = session.getTable();

        return ResponseEntity.ok(ApiResponse.ok(TableDto.TableItem.builder()
                .id(table.getId())
                .name(table.getName())
                .qrCode(table.getQrCode())
                .status(table.getStatus())
                .activeSessionId(session.getId())
                .active(table.getActive())
                .build()));
    }

    @PostMapping("/cashier/table-sessions/{sessionId}/close")
    public ResponseEntity<ApiResponse<TableDto.TableSessionResponse>> closeSessionBySessionId(
            @PathVariable("sessionId") Long sessionId,
            @AuthenticationPrincipal CurrentActor actor) {

        Long cashierId = actor != null ? actor.getAccountId() : null;
        var response = tableSessionService.closeBySessionId(sessionId, cashierId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/cashier/tables/{id}/close-session")
    public ResponseEntity<ApiResponse<Void>> closeSession(
            @PathVariable("id") Long tableId,
            @AuthenticationPrincipal CurrentActor actor) {

        Long cashierId = actor != null ? actor.getAccountId() : null;
        tableSessionService.closeByCashier(tableId, cashierId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
