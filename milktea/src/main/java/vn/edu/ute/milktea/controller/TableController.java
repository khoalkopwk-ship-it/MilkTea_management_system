package vn.edu.ute.milktea.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
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
    private final vn.edu.ute.milktea.repository.table.TableSessionRepository sessionRepository;

    @GetMapping({"/public/table-context", "/tables/context"})
    public ResponseEntity<ApiResponse<TableDto.TableContextResponse>> getTableContext(
            @RequestParam(value = "qrCode", required = false) String qrCode,
            @RequestParam(value = "qr", required = false) String qr) {

        String token = (qrCode != null && !qrCode.isBlank()) ? qrCode : qr;
        return getTableContext(token);
    }

    public ResponseEntity<ApiResponse<TableDto.TableContextResponse>> getTableContext(String qrCode) {
        if (qrCode == null || qrCode.isBlank()) {
            throw BusinessException.badRequest(ErrorCode.QR_INVALID, "Vui lòng quét mã QR hợp lệ trên bàn");
        }
        return ResponseEntity.ok(ApiResponse.ok(tableContextService.readByQr(qrCode)));
    }

    @PostMapping({"/public/table-context/restore", "/tables/context/restore"})
    public ResponseEntity<ApiResponse<TableDto.TableContextResponse>> restoreTableContext(
            @RequestBody(required = false) java.util.Map<String, String> body,
            @RequestHeader(value = "X-Table-Token", required = false) String headerToken) {
        String token = (headerToken != null && !headerToken.isBlank()) ? headerToken : null;
        if (token == null && body != null) {
            token = body.get("tableToken");
        }
        return ResponseEntity.ok(ApiResponse.ok(tableContextService.restoreContext(token)));
    }

    @GetMapping({"/public/table-context/restore", "/tables/context/restore"})
    public ResponseEntity<ApiResponse<TableDto.TableContextResponse>> restoreTableContextGet(
            @RequestParam(value = "tableToken", required = false) String paramToken,
            @RequestParam(value = "token", required = false) String legacyToken,
            @RequestHeader(value = "X-Table-Token", required = false) String headerToken) {
        String token = (headerToken != null && !headerToken.isBlank()) ? headerToken : paramToken;
        if (token == null || token.isBlank()) token = legacyToken;
        return ResponseEntity.ok(ApiResponse.ok(tableContextService.restoreContext(token)));
    }

    @GetMapping({"/cashier/tables", "/tables"})
    public ResponseEntity<ApiResponse<List<TableDto.TableItem>>> getTables(
            @RequestParam(value = "branchId", required = false) Long branchId,
            @AuthenticationPrincipal CurrentActor actor) {

        Long targetBranchId = branchId;
        if (targetBranchId == null && actor != null && !actor.isSystemAdmin()) {
            targetBranchId = actor.getBranchId();
        }

        List<DiningTable> tables = (targetBranchId != null)
                ? tableRepository.findByBranchIdAndActiveTrueOrderByIdAsc(targetBranchId)
                : tableRepository.findByActiveTrueOrderByIdAsc();

        boolean isStaff = actor != null && actor.isAuthenticated() && (actor.hasRole(vn.edu.ute.milktea.entity.account.Role.CASHIER)
                || actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN)
                || actor.hasRole(vn.edu.ute.milktea.entity.account.Role.MANAGER));

        List<TableDto.TableItem> items = tables.stream().map(t -> TableDto.TableItem.builder()
                .id(t.getId())
                .name(t.getName())
                .qrCode(isStaff ? t.getQrCode() : null)
                .status(t.getStatus())
                .activeSessionId(isStaff ? t.getActiveSessionId() : null)
                .active(t.getActive())
                .branchId(t.getBranch() != null ? t.getBranch().getId() : null)
                .branchName(t.getBranch() != null ? t.getBranch().getName() : null)
                .build()
        ).toList();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/table-sessions/{sessionId}")
    public ResponseEntity<ApiResponse<TableDto.TableSessionResponse>> getSession(
            @PathVariable("sessionId") Long sessionId,
            jakarta.servlet.http.HttpServletRequest request) {
        CurrentActor actor = (CurrentActor) request.getAttribute(vn.edu.ute.milktea.security.JwtAuthenticationFilter.CURRENT_ACTOR_ATTR);
        var response = tableSessionService.getSessionResponse(sessionId);
        verifySessionAccess(response, actor);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/cashier/tables/{id}/open-session")
    public ResponseEntity<ApiResponse<TableDto.TableItem>> openSession(
            @PathVariable("id") Long tableId,
            @AuthenticationPrincipal CurrentActor actor) {

        DiningTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> vn.edu.ute.milktea.common.BusinessException.notFound(vn.edu.ute.milktea.common.ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));
        if (actor != null && table.getBranch() != null && !actor.canAccessBranch(table.getBranch().getId())) {
            throw vn.edu.ute.milktea.common.BusinessException.forbidden(vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED, "Không thể mở phiên cho bàn của chi nhánh khác");
        }

        Long cashierId = actor != null ? actor.getAccountId() : null;
        var session = tableSessionService.openByCashier(tableId, cashierId);
        DiningTable updatedTable = session.getTable();

        return ResponseEntity.ok(ApiResponse.ok(TableDto.TableItem.builder()
                .id(updatedTable.getId())
                .name(updatedTable.getName())
                .qrCode(updatedTable.getQrCode())
                .status(updatedTable.getStatus())
                .activeSessionId(session.getId())
                .active(updatedTable.getActive())
                .branchId(updatedTable.getBranch() != null ? updatedTable.getBranch().getId() : null)
                .branchName(updatedTable.getBranch() != null ? updatedTable.getBranch().getName() : null)
                .build()));
    }

    @PostMapping("/cashier/table-sessions/{sessionId}/close")
    public ResponseEntity<ApiResponse<TableDto.TableSessionResponse>> closeSessionBySessionId(
            @PathVariable("sessionId") Long sessionId,
            @AuthenticationPrincipal CurrentActor actor) {

        var sessionResponse = tableSessionService.getSessionResponse(sessionId);
        if (actor != null && sessionResponse.getBranchId() != null && !actor.canAccessBranch(sessionResponse.getBranchId())) {
            throw vn.edu.ute.milktea.common.BusinessException.forbidden(vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED, "Không thể đóng phiên của chi nhánh khác");
        }

        Long cashierId = actor != null ? actor.getAccountId() : null;
        var response = tableSessionService.closeBySessionId(sessionId, cashierId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private void verifySessionAccess(TableDto.TableSessionResponse response, CurrentActor actor) {
        if (actor == null || actor.getActorType() == CurrentActor.ActorType.QR_ANONYMOUS) {
            throw vn.edu.ute.milktea.common.BusinessException.unauthorized(vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED, "Vui lòng quét QR bàn để xem phiên");
        }
        if (actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN)) {
            return;
        }
        if (actor.hasRole(vn.edu.ute.milktea.entity.account.Role.CASHIER)
                || actor.hasRole(vn.edu.ute.milktea.entity.account.Role.MANAGER)
                || actor.hasRole(vn.edu.ute.milktea.entity.account.Role.KITCHEN)) {
            if (response.getBranchId() != null && !actor.canAccessBranch(response.getBranchId())) {
                throw vn.edu.ute.milktea.common.BusinessException.forbidden(vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED, "Không thể truy cập phiên của chi nhánh khác");
            }
            return;
        }
        if (actor.getActorType() == CurrentActor.ActorType.TABLE_GUEST) {
            if (actor.getSessionId() != null && actor.getSessionId().equals(response.getSessionId())) {
                return;
            }
            throw vn.edu.ute.milktea.common.BusinessException.forbidden(vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED, "Không thể truy cập phiên của bàn khác");
        }
        if (actor.hasRole(vn.edu.ute.milktea.entity.account.Role.CUSTOMER)) {
            if (actor.getSessionId() != null && actor.getSessionId().equals(response.getSessionId())) {
                return;
            }
            if (actor.getAccountId() != null) {
                var sessionOpt = sessionRepository.findById(response.getSessionId());
                if (sessionOpt.isPresent() && sessionOpt.get().getOpenedBy() != null
                        && actor.getAccountId().equals(sessionOpt.get().getOpenedBy().getId())) {
                    return;
                }
            }
            throw vn.edu.ute.milktea.common.BusinessException.forbidden(vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem thông tin phiên bàn này");
        }
        throw vn.edu.ute.milktea.common.BusinessException.forbidden(vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem thông tin phiên bàn này");
    }
}
