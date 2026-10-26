package vn.edu.ute.milktea.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.milktea.entity.account.Role;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentActor {

    public enum ActorType {
        AUTHENTICATED,
        TABLE_GUEST,
        COUNTER_GUEST,
        QR_ANONYMOUS
    }

    private ActorType actorType;
    private Long accountId;
    private String email;
    private Role role;
    private Long sessionId;
    private Long tableId;
    private Long orderId;
    private Long branchId;
    private java.util.Set<Long> assignedBranchIds;

    public boolean isAuthenticated() {
        return actorType == ActorType.AUTHENTICATED;
    }

    public boolean hasRole(Role requiredRole) {
        return isAuthenticated() && this.role == requiredRole;
    }

    public boolean canAccessBranch(Long targetBranchId) {
        if (targetBranchId == null) {
            return false;
        }
        if (hasRole(Role.ADMIN)) {
            return true;
        }
        if (assignedBranchIds != null && assignedBranchIds.contains(targetBranchId)) {
            return true;
        }
        return this.branchId != null && this.branchId.equals(targetBranchId);
    }

    public static CurrentActor guest() {
        return CurrentActor.builder().actorType(ActorType.QR_ANONYMOUS).build();
    }

    public static CurrentActor customer(Long accountId, String email) {
        return CurrentActor.builder()
                .actorType(ActorType.AUTHENTICATED)
                .accountId(accountId)
                .email(email)
                .role(Role.CUSTOMER)
                .build();
    }

    public static CurrentActor tableGuest(Long sessionId, Long tableId) {
        return CurrentActor.builder().actorType(ActorType.TABLE_GUEST).sessionId(sessionId).tableId(tableId).build();
    }

    public static CurrentActor tableGuest(Long sessionId, Long tableId, Long branchId) {
        return CurrentActor.builder().actorType(ActorType.TABLE_GUEST).sessionId(sessionId).tableId(tableId).branchId(branchId).build();
    }

    public static CurrentActor authenticated(Long accountId, Role role) {
        return CurrentActor.builder().actorType(ActorType.AUTHENTICATED).accountId(accountId).role(role).build();
    }

    public static CurrentActor authenticated(Long accountId, Role role, Long branchId, java.util.List<Long> assignedBranchIds) {
        return CurrentActor.builder()
                .actorType(ActorType.AUTHENTICATED)
                .accountId(accountId)
                .role(role)
                .branchId(branchId)
                .assignedBranchIds(assignedBranchIds != null ? new java.util.HashSet<>(assignedBranchIds) : java.util.Collections.emptySet())
                .build();
    }
}
