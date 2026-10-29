package vn.edu.ute.milktea.service.table;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.*;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.entity.table.TableSession;
import vn.edu.ute.milktea.entity.table.TableSessionStatus;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.repository.table.TableSessionRepository;
import vn.edu.ute.milktea.security.GuestAccessService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TableContextService {

    private final DiningTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final GuestAccessService guestAccessService;

    @Transactional
    public TableDto.TableContextResponse readByQr(String qrCode) {
        if (qrCode == null || qrCode.isBlank()) {
            throw BusinessException.badRequest(ErrorCode.QR_INVALID, "Mã QR không hợp lệ");
        }

        DiningTable table = tableRepository.findByQrCode(qrCode)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));

        if (!Boolean.TRUE.equals(table.getActive())) {
            throw BusinessException.badRequest(ErrorCode.TABLE_NOT_FOUND, "Bàn đang ngừng hoạt động");
        }

        TableDto.TableContextResponse.TableContextResponseBuilder builder = TableDto.TableContextResponse.builder()
                .tableId(table.getId())
                .tableName(table.getName())
                .qrCode(table.getQrCode())
                .status(table.getStatus())
                .branchId(table.getBranch() != null ? table.getBranch().getId() : null)
                .branchName(table.getBranch() != null ? table.getBranch().getName() : null);

        if (table.getActiveSessionId() != null) {
            Optional<TableSession> sessionOpt = sessionRepository.findById(table.getActiveSessionId());
            if (sessionOpt.isPresent() && sessionOpt.get().getStatus() == TableSessionStatus.OPEN) {
                TableSession session = sessionOpt.get();
                builder.sessionId(session.getId())
                        .sessionStatus(session.getStatus());

                // Cấp token truy cập phiên scoped riêng cho tab hiện tại (lưu hash, trả raw token)
                var issued = guestAccessService.issueTableSessionToken(session, 12);
                builder.tableToken(issued.rawToken());
            }
        } else {
            // Bàn chưa có phiên: cấp Table QR capability token chứng minh đã quét QR thực tế (V4-01)
            Long bId = (table.getBranch() != null) ? table.getBranch().getId() : null;
            String qrCapabilityToken = guestAccessService.issueTableQrCapabilityToken(table.getId(), bId, 4);
            builder.tableToken(qrCapabilityToken);
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public TableDto.TableContextResponse restoreContext(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw BusinessException.unauthorized(ErrorCode.ACCESS_DENIED, "Vui lòng quét mã QR hợp lệ tại bàn để bắt đầu phiên mới.");
        }

        // 1. Kiểm tra session access token hợp lệ
        var sessionAccessOpt = guestAccessService.validateTableToken(rawToken);
        if (sessionAccessOpt.isPresent()) {
            var access = sessionAccessOpt.get();
            TableSession session = access.getSession();
            DiningTable table = session.getTable();
            if (table == null || !Boolean.TRUE.equals(table.getActive())) {
                throw BusinessException.badRequest(ErrorCode.TABLE_NOT_FOUND, "Bàn đang ngừng hoạt động");
            }
            if (session.getStatus() != TableSessionStatus.OPEN || !session.getId().equals(table.getActiveSessionId())) {
                throw BusinessException.unauthorized(ErrorCode.TABLE_SESSION_CLOSED, "Phiên bàn đã kết thúc. Vui lòng quét lại mã QR tại bàn.");
            }

            return TableDto.TableContextResponse.builder()
                    .tableId(table.getId())
                    .tableName(table.getName())
                    .qrCode(table.getQrCode())
                    .status(table.getStatus())
                    .branchId(table.getBranch() != null ? table.getBranch().getId() : null)
                    .branchName(table.getBranch() != null ? table.getBranch().getName() : null)
                    .sessionId(session.getId())
                    .sessionStatus(session.getStatus())
                    .tableToken(rawToken)
                    .build();
        }

        // 2. Nếu không phải session token, kiểm tra QR capability token (khi khách đã quét QR nhưng chưa tạo đơn đầu tiên)
        var tableIdOpt = guestAccessService.validateAndGetTableIdFromQrCapabilityToken(rawToken);
        if (tableIdOpt.isPresent()) {
            Long tableId = tableIdOpt.get();
            DiningTable table = tableRepository.findById(tableId).orElse(null);
            if (table != null && Boolean.TRUE.equals(table.getActive())) {
                if (table.getActiveSessionId() == null) {
                    return TableDto.TableContextResponse.builder()
                            .tableId(table.getId())
                            .tableName(table.getName())
                            .qrCode(table.getQrCode())
                            .status(table.getStatus())
                            .branchId(table.getBranch() != null ? table.getBranch().getId() : null)
                            .branchName(table.getBranch() != null ? table.getBranch().getName() : null)
                            .sessionId(null)
                            .sessionStatus(null)
                            .tableToken(rawToken)
                            .build();
                }
                throw BusinessException.unauthorized(ErrorCode.TABLE_SESSION_CHANGED, "Bàn đã có phiên mới phục vụ khách khác. Vui lòng quét lại mã QR tại bàn.");
            }
        }

        throw BusinessException.unauthorized(ErrorCode.ACCESS_DENIED, "Phiên bàn không hợp lệ hoặc đã kết thúc. Vui lòng quét lại mã QR tại bàn.");
    }
}
