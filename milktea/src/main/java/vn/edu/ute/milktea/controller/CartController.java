package vn.edu.ute.milktea.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.CartDto;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.JwtAuthenticationFilter;
import vn.edu.ute.milktea.service.table.SessionCartService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CartController {

    private final SessionCartService sessionCartService;
    private final vn.edu.ute.milktea.repository.table.TableSessionRepository sessionRepository;

    @GetMapping({"/table-sessions/{sessionId}/cart", "/customer/cart"})
    public ResponseEntity<ApiResponse<CartDto.CartResponse>> getCart(
            @PathVariable(value = "sessionId", required = false) Long pathSessionId,
            @RequestParam(value = "sessionId", required = false) Long querySessionId,
            HttpServletRequest request) {

        CurrentActor actor = (CurrentActor) request.getAttribute(JwtAuthenticationFilter.CURRENT_ACTOR_ATTR);
        Long sessionId = resolveAndVerifySessionId(pathSessionId, querySessionId, actor);

        CartDto.CartResponse response = sessionCartService.getCart(sessionId);
        return ResponseEntity.ok()
                .eTag("\"" + response.getVersion() + "\"")
                .body(ApiResponse.ok(response));
    }

    @PutMapping({"/table-sessions/{sessionId}/cart", "/customer/cart"})
    public ResponseEntity<ApiResponse<CartDto.CartResponse>> replaceCart(
            @PathVariable(value = "sessionId", required = false) Long pathSessionId,
            @RequestParam(value = "sessionId", required = false) Long querySessionId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody CartDto.ReplaceCartRequest requestBody,
            HttpServletRequest request) {

        CurrentActor actor = (CurrentActor) request.getAttribute(JwtAuthenticationFilter.CURRENT_ACTOR_ATTR);
        Long sessionId = resolveAndVerifySessionId(pathSessionId, querySessionId, actor);

        CartDto.CartResponse response = sessionCartService.replaceCart(sessionId, requestBody, ifMatch);
        return ResponseEntity.ok()
                .eTag("\"" + response.getVersion() + "\"")
                .body(ApiResponse.ok(response));
    }

    /**
     * SEC-02.4 & F02: Xác thực quyền sở hữu giỏ hàng của phiên bàn.
     * Ngăn chặn khách bàn, customer lạ hoặc anonymous can thiệp hoặc truy cập giỏ hàng của bàn khác.
     */
    private Long resolveAndVerifySessionId(Long pathSessionId, Long querySessionId, CurrentActor actor) {
        Long sessionId = pathSessionId != null ? pathSessionId : (querySessionId != null ? querySessionId : (actor != null ? actor.getSessionId() : null));

        if (sessionId == null) {
            throw BusinessException.badRequest(ErrorCode.TABLE_SESSION_CLOSED, "Chưa xác định được phiên bàn");
        }

        if (actor == null || actor.getActorType() == CurrentActor.ActorType.QR_ANONYMOUS) {
            throw BusinessException.unauthorized(ErrorCode.ACCESS_DENIED, "Vui lòng quét QR bàn để truy cập giỏ hàng");
        }

        if (actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN)) {
            return sessionId;
        }

        if (actor.getActorType() == CurrentActor.ActorType.TABLE_GUEST) {
            if (actor.getSessionId() == null || !actor.getSessionId().equals(sessionId)) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không thể truy cập giỏ hàng của bàn khác");
            }
            return sessionId;
        }

        if (sessionRepository == null) {
            return sessionId;
        }

        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_SESSION_CLOSED, "Không tìm thấy phiên bàn"));

        if (session.getStatus() != vn.edu.ute.milktea.entity.table.TableSessionStatus.OPEN) {
            throw BusinessException.badRequest(ErrorCode.TABLE_SESSION_CLOSED, "Phiên bàn đã kết thúc");
        }

        if (actor.hasRole(vn.edu.ute.milktea.entity.account.Role.CUSTOMER)) {
            boolean hasSessionToken = actor.getSessionId() != null && actor.getSessionId().equals(sessionId);
            if (!hasSessionToken) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn không có quyền truy cập giỏ hàng của phiên bàn này");
            }
            return sessionId;
        }

        if (actor.hasRole(vn.edu.ute.milktea.entity.account.Role.CASHIER)
                || actor.hasRole(vn.edu.ute.milktea.entity.account.Role.MANAGER)
                || actor.hasRole(vn.edu.ute.milktea.entity.account.Role.KITCHEN)) {
            Long branchId = session.getTable() != null && session.getTable().getBranch() != null ? session.getTable().getBranch().getId() : null;
            if (!actor.canAccessBranch(branchId)) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không thể truy cập giỏ hàng của chi nhánh khác");
            }
            return sessionId;
        }

        throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn không có quyền truy cập giỏ hàng này");
    }
}
