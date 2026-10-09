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

    @GetMapping({"/table-sessions/{sessionId}/cart", "/customer/cart"})
    public ResponseEntity<ApiResponse<CartDto.CartResponse>> getCart(
            @PathVariable(value = "sessionId", required = false) Long pathSessionId,
            @RequestParam(value = "sessionId", required = false) Long querySessionId,
            HttpServletRequest request) {

        CurrentActor actor = (CurrentActor) request.getAttribute(JwtAuthenticationFilter.CURRENT_ACTOR_ATTR);
        Long sessionId = pathSessionId != null ? pathSessionId : (querySessionId != null ? querySessionId : (actor != null ? actor.getSessionId() : null));

        if (sessionId == null) {
            throw BusinessException.badRequest(ErrorCode.TABLE_SESSION_CLOSED, "Chưa xác định được phiên bàn");
        }

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
        Long sessionId = pathSessionId != null ? pathSessionId : (querySessionId != null ? querySessionId : (actor != null ? actor.getSessionId() : null));

        if (sessionId == null) {
            throw BusinessException.badRequest(ErrorCode.TABLE_SESSION_CLOSED, "Chưa xác định được phiên bàn");
        }

        CartDto.CartResponse response = sessionCartService.replaceCart(sessionId, requestBody, ifMatch);
        return ResponseEntity.ok()
                .eTag("\"" + response.getVersion() + "\"")
                .body(ApiResponse.ok(response));
    }
}
