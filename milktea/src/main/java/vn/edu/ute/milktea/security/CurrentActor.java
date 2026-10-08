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

    public boolean isAuthenticated() {
        return actorType == ActorType.AUTHENTICATED;
    }

    public boolean hasRole(Role requiredRole) {
        return isAuthenticated() && this.role == requiredRole;
    }

    public static CurrentActor guest() {
        return CurrentActor.builder().actorType(ActorType.QR_ANONYMOUS).build();
    }

    public static CurrentActor tableGuest(Long sessionId, Long tableId) {
        return CurrentActor.builder().actorType(ActorType.TABLE_GUEST).sessionId(sessionId).tableId(tableId).build();
    }

    public static CurrentActor authenticated(Long accountId, Role role) {
        return CurrentActor.builder().actorType(ActorType.AUTHENTICATED).accountId(accountId).role(role).build();
    }
}
