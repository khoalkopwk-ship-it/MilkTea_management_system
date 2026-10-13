package vn.edu.ute.milktea.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.ute.milktea.entity.settings.GlobalSettings;
import vn.edu.ute.milktea.repository.settings.GlobalSettingsRepository;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final GlobalSettingsRepository settingsRepository;

    @ModelAttribute("settings")
    public GlobalSettings globalSettings() {
        return settingsRepository.findById(1).orElse(null);
    }
}
