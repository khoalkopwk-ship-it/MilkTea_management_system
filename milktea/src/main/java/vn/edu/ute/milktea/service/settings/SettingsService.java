package vn.edu.ute.milktea.service.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.ute.milktea.dto.SettingsDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.settings.GlobalSettings;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.settings.GlobalSettingsRepository;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final GlobalSettingsRepository settingsRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public SettingsDto.ShopInfoResponse getShopInfo() {
        GlobalSettings settings = settingsRepository.findById(1)
                .orElseGet(() -> GlobalSettings.builder()
                        .id(1)
                        .shopName("MilkTea Quán 01")
                        .address("Số 1 Võ Văn Ngân, TP. Thủ Đức")
                        .discountPercent(BigDecimal.ZERO)
                        .modifiedAt(Instant.now())
                        .build());

        return SettingsDto.ShopInfoResponse.builder()
                .shopName(settings.getShopName())
                .address(settings.getAddress())
                .bankName(settings.getBankName())
                .bankAccountNumber(settings.getBankAccountNumber())
                .bankAccountHolder(settings.getBankAccountHolder())
                .discountPercent(settings.getDiscountPercent())
                .build();
    }

    @Transactional
    public void updateSettings(SettingsDto.UpdateSettingsRequest request, Long adminId) {
        Account admin = adminId != null ? accountRepository.findById(adminId).orElse(null) : null;

        GlobalSettings settings = settingsRepository.findById(1)
                .orElseGet(() -> GlobalSettings.builder()
                        .id(1)
                        .build());

        settings.setShopName(request.getShopName());
        settings.setAddress(request.getAddress());
        settings.setBankName(request.getBankName());
        settings.setBankAccountNumber(request.getBankAccountNumber());
        settings.setBankAccountHolder(request.getBankAccountHolder());
        settings.setDiscountPercent(request.getDiscountPercent());
        settings.setModifiedBy(admin);
        settings.setModifiedAt(Instant.now());

        settingsRepository.save(settings);
    }
}
