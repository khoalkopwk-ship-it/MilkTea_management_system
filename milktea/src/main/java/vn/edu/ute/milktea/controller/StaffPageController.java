package vn.edu.ute.milktea.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.settings.GlobalSettings;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.inventory.MaterialRepository;
import vn.edu.ute.milktea.repository.settings.GlobalSettingsRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.security.CurrentActor;

@Controller
@RequiredArgsConstructor
public class StaffPageController {

    private final GlobalSettingsRepository settingsRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final DiningTableRepository tableRepository;
    private final MaterialRepository materialRepository;

    @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "redirect", required = false) String redirect,
            @RequestParam(value = "error", required = false) String error,
            Model model) {
        model.addAttribute("redirect", redirect != null ? redirect : "/");
        model.addAttribute("error", error);
        return "auth/login";
    }

    @GetMapping("/cashier")
    public String cashierPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (actor == null || (actor.getRole() != Role.CASHIER && actor.getRole() != Role.ADMIN)) {
            return "redirect:/login?redirect=/cashier";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("categories", categoryRepository.findAllByActiveTrue());
        model.addAttribute("products", productRepository.findAllByActiveTrue());
        model.addAttribute("tables", tableRepository.findByActiveTrueOrderByIdAsc());
        return "cashier/index";
    }

    @GetMapping("/kitchen")
    public String kitchenPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (actor == null || (actor.getRole() != Role.KITCHEN && actor.getRole() != Role.ADMIN)) {
            return "redirect:/login?redirect=/kitchen";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        return "kitchen/orders";
    }

    @GetMapping({"/admin", "/admin/catalog"})
    public String adminCatalogPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (actor == null || actor.getRole() != Role.ADMIN) {
            return "redirect:/login?redirect=/admin/catalog";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute(
            "products",
            productRepository.findAllByOrderByIdAsc()
        );
        model.addAttribute("materials", materialRepository.findAllByActiveTrue());
        return "admin/catalog";
    }

    @GetMapping("/admin/tables")
    public String adminTablesPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (actor == null || actor.getRole() != Role.ADMIN) {
            return "redirect:/login?redirect=/admin/tables";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("tables", tableRepository.findAll());
        return "admin/tables";
    }

    @GetMapping("/admin/staff")
    public String adminStaffPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (actor == null || actor.getRole() != Role.ADMIN) {
            return "redirect:/login?redirect=/admin/staff";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        return "admin/staff";
    }

    @GetMapping("/admin/inventory")
    public String adminInventoryPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (actor == null || actor.getRole() != Role.ADMIN) {
            return "redirect:/login?redirect=/admin/inventory";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("materials", materialRepository.findAll());
        return "admin/inventory";
    }

    @GetMapping("/admin/reports")
    public String adminReportsPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (actor == null || actor.getRole() != Role.ADMIN) {
            return "redirect:/login?redirect=/admin/reports";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        return "admin/reports";
    }

    @GetMapping("/admin/documents")
    public String documentsPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        model.addAttribute("settings", settingsRepository.findById(1).orElse(null));
        model.addAttribute("actor", actor);
        return "admin/documents";
    }

    @GetMapping("/admin/settings")
    public String adminSettingsPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (actor == null || actor.getRole() != Role.ADMIN) {
            return "redirect:/login?redirect=/admin/settings";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        return "admin/settings";
    }
}
