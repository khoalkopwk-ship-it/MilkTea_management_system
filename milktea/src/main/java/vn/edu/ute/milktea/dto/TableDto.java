package vn.edu.ute.milktea.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.milktea.entity.table.TableSessionStatus;
import vn.edu.ute.milktea.entity.table.TableStatus;

public class TableDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableContextResponse {
        private Long tableId;
        private String tableName;
        private String qrCode;
        private TableStatus status;
        private Long sessionId;
        private TableSessionStatus sessionStatus;
        private String tableToken; // Trả về khi phiên đang mở
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableItem {
        private Long id;
        private String name;
        private String qrCode;
        private TableStatus status;
        private Long activeSessionId;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableSessionResponse {
        private Long sessionId;
        private Long tableId;
        private String tableName;
        private TableSessionStatus status;
        private java.time.Instant openedAt;
        private java.time.Instant closedAt;
    }
}
