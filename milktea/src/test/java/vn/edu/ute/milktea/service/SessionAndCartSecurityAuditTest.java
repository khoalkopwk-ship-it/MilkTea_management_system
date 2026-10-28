package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.controller.TableController;
import vn.edu.ute.milktea.dto.AuthDto;
import vn.edu.ute.milktea.dto.TableDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.entity.table.TableSession;
import vn.edu.ute.milktea.entity.table.TableSessionStatus;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.account.PasswordResetTokenRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.repository.table.TableSessionRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.GuestAccessService;
import vn.edu.ute.milktea.security.JwtService;
import vn.edu.ute.milktea.service.auth.AuthService;
import vn.edu.ute.milktea.service.auth.OtpService;
import vn.edu.ute.milktea.service.mail.MailService;
import vn.edu.ute.milktea.service.table.TableContextService;
import vn.edu.ute.milktea.service.table.TableSessionService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionAndCartSecurityAuditTest {

    @Mock private DiningTableRepository tableRepository;
    @Mock private TableSessionRepository sessionRepository;
    @Mock private TableSessionService tableSessionService;
    @Mock private TableContextService tableContextService;
    @Mock private AccountRepository accountRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private GuestAccessService guestAccessService;
    @Mock private OtpService otpService;
    @Mock private MailService mailService;

    private TableController tableController;
    private AuthService authService;

    private Branch branch1;
    private Branch branch2;

    @BeforeEach
    void setUp() {
        branch1 = Branch.builder().id(1L).code("CN01").name("Chi nhánh 1").build();
        branch2 = Branch.builder().id(2L).code("CN02").name("Chi nhánh 2").build();

        tableController = new TableController(tableContextService, tableSessionService, tableRepository, sessionRepository);
        authService = new AuthService(
                accountRepository, passwordResetTokenRepository, passwordEncoder,
                jwtService, guestAccessService, otpService, mailService
        );
    }

    @Test
    @DisplayName("F03: Mở phiên bàn bị từ chối nếu nhân viên thu ngân khác chi nhánh của bàn")
    void testOpenSessionRejectsCrossBranchCashier() {
        DiningTable table = DiningTable.builder().id(50L).branch(branch2).build();
        when(tableRepository.findById(50L)).thenReturn(Optional.of(table));

        CurrentActor cashierBranch1 = CurrentActor.authenticated(15L, Role.CASHIER, 1L, List.of(1L));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                tableController.openSession(50L, cashierBranch1)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Không thể mở phiên cho bàn của chi nhánh khác"));
    }

    @Test
    @DisplayName("F03: Đóng phiên bàn bị từ chối nếu nhân viên thu ngân khác chi nhánh của bàn")
    void testCloseSessionRejectsCrossBranchCashier() {
        TableDto.TableSessionResponse sessionResponse = TableDto.TableSessionResponse.builder()
                .sessionId(100L)
                .branchId(2L)
                .build();
        when(tableSessionService.getSessionResponse(100L)).thenReturn(sessionResponse);

        CurrentActor cashierBranch1 = CurrentActor.authenticated(15L, Role.CASHIER, 1L, List.of(1L));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                tableController.closeSessionBySessionId(100L, cashierBranch1)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Không thể đóng phiên của chi nhánh khác"));
    }

    @Test
    @DisplayName("F07: Đổi mật khẩu bắt buộc phải xác thực mật khẩu hiện tại")
    void testChangePasswordRequiresCurrentPassword() {
        Account account = Account.builder()
                .id(30L)
                .email("user@milktea.vn")
                .passwordHash("hashed_old_pwd")
                .build();

        when(accountRepository.findById(30L)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("WrongCurrentPwd", "hashed_old_pwd")).thenReturn(false);

        AuthDto.ChangePasswordRequest req = new AuthDto.ChangePasswordRequest("WrongCurrentPwd", "NewStrongPwd@123");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.changePassword(30L, req)
        );

        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Mật khẩu hiện tại không chính xác"));
    }
}
