package vn.edu.ute.milktea.service.table;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.table.TableDto;
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
                .status(table.getStatus());

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
        }

        return builder.build();
    }
}
