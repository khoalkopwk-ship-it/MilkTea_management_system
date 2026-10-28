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
    private final vn.edu.ute.milktea.repository.branch.BranchRepository branchRepository;
    private final vn.edu.ute.milktea.service.branch.BranchService branchService;
    private final vn.edu.ute.milktea.service.admin.AdminService adminService;

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
        if (actor == null || actor.getRole() != Role.CASHIER) {
            return "redirect:/login?redirect=/cashier";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        if (actor.getBranchId() != null) {
            branchRepository.findById(actor.getBranchId()).ifPresent(b -> model.addAttribute("currentBranch", b));
        }
        model.addAttribute("categories", categoryRepository.findAllByActiveTrue());
        model.addAttribute("products", productRepository.findAllByActiveTrue());
        model.addAttribute("tables", tableRepository.findByActiveTrueOrderByIdAsc());
        return "cashier/index";
    }

    @GetMapping("/kitchen")
    public String kitchenPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (actor == null || (actor.getRole() != Role.KITCHEN && actor.getRole() != Role.ADMIN && actor.getRole() != Role.MANAGER)) {
            return "redirect:/login?redirect=/kitchen";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        if (actor.getBranchId() != null) {
            branchRepository.findById(actor.getBranchId()).ifPresent(b -> model.addAttribute("currentBranch", b));
        }
        return "kitchen/orders";
    }

    private boolean isStaffAdmin(CurrentActor actor) {
        return actor != null && actor.getRole() == Role.ADMIN;
    }

    private boolean isManagerOrAdmin(CurrentActor actor) {
        return actor != null && (actor.getRole() == Role.ADMIN || actor.getRole() == Role.MANAGER);
    }

    @GetMapping("/admin")
    public String adminMainPage(
            @RequestParam(value = "section", required = false) String section,
            @AuthenticationPrincipal CurrentActor actor,
            Model model) {
        if (!isManagerOrAdmin(actor)) {
            return "redirect:/login?redirect=/admin";
        }
        if ("catalog".equalsIgnoreCase(section)) {
            return adminCatalogPage(actor, model);
        } else if ("tables".equalsIgnoreCase(section)) {
            return adminTablesPage(actor, model);
        } else if ("orders".equalsIgnoreCase(section)) {
            return adminOrdersPage(actor, model);
        } else if ("staff".equalsIgnoreCase(section)) {
            return adminStaffPage(actor, model);
        } else if ("inventory".equalsIgnoreCase(section)) {
            return adminInventoryPage(actor, model);
        } else if ("documents".equalsIgnoreCase(section)) {
            return documentsPage(actor, model);
        } else if ("reports".equalsIgnoreCase(section)) {
            return adminReportsPage(actor, model);
        } else if ("settings".equalsIgnoreCase(section)) {
            return adminSettingsPage(actor, model);
        } else if ("vouchers".equalsIgnoreCase(section)) {
            return adminVouchersPage(actor, model);
        }
        return adminReportsPage(actor, model);
    }

    @GetMapping("/admin/vouchers")
    public String adminVouchersPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (!isStaffAdmin(actor)) {
            return "redirect:/admin";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("isManager", actor != null && actor.getRole() == Role.MANAGER);
        model.addAttribute("branches", branchRepository.findAll());
        return "admin/vouchers";
    }

    @GetMapping("/admin/catalog")
    public String adminCatalogPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (!isManagerOrAdmin(actor)) {
            return "redirect:/login?redirect=/admin/catalog";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("categories", categoryRepository.findAll());
        Long branchId = (actor != null && actor.getRole() == Role.MANAGER) ? actor.getBranchId() : null;
        model.addAttribute("products", adminService.listProducts(branchId));
        model.addAttribute("materials", materialRepository.findAllByActiveTrue());
        model.addAttribute("isManager", actor != null && actor.getRole() == Role.MANAGER);
        model.addAttribute("managerBranchId", actor != null ? actor.getBranchId() : null);
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("centralWarehouse", branchService.getOrCreateCentralWarehouse());
        return "admin/catalog";
    }

    @GetMapping("/admin/tables")
    public String adminTablesPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (!isManagerOrAdmin(actor)) {
            return "redirect:/login?redirect=/admin/tables";
        }
        if (actor != null && actor.getRole() == Role.ADMIN) {
            return "redirect:/admin/reports";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("baseUrl", baseUrl);
        if (actor.getRole() == Role.MANAGER && actor.getBranchId() != null) {
            model.addAttribute("tables", tableRepository.findByBranchIdOrderByIdAsc(actor.getBranchId()));
        } else {
            model.addAttribute("tables", tableRepository.findAll());
        }
        model.addAttribute("isManager", actor != null && actor.getRole() == Role.MANAGER);
        model.addAttribute("managerBranchId", actor != null ? actor.getBranchId() : null);
        model.addAttribute("branches", branchRepository.findAll());
        return "admin/tables";
    }

    @GetMapping("/admin/orders")
    public String adminOrdersPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (!isManagerOrAdmin(actor)) {
            return "redirect:/login?redirect=/admin/orders";
        }
        model.addAttribute("settings", settingsRepository.findById(1).orElse(null));
        model.addAttribute("actor", actor);
        model.addAttribute("pageTitle", "Quản lý đơn hàng");
        model.addAttribute("isManager", actor != null && actor.getRole() == Role.MANAGER);
        model.addAttribute("managerBranchId", actor != null ? actor.getBranchId() : null);
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("centralWarehouse", branchService.getOrCreateCentralWarehouse());
        return "admin/orders";
    }

    @GetMapping("/admin/staff")
    public String adminStaffPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (!isManagerOrAdmin(actor)) {
            return "redirect:/login?redirect=/admin/staff";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("isManager", actor != null && actor.getRole() == Role.MANAGER);
        model.addAttribute("managerBranchId", actor != null ? actor.getBranchId() : null);
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("centralWarehouse", branchService.getOrCreateCentralWarehouse());
        return "admin/staff";
    }

    @GetMapping("/admin/inventory")
    public String adminInventoryPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (!isManagerOrAdmin(actor)) {
            return "redirect:/login?redirect=/admin/inventory";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("materials", materialRepository.findAll());
        model.addAttribute("isManager", actor != null && actor.getRole() == Role.MANAGER);
        model.addAttribute("managerBranchId", actor != null ? actor.getBranchId() : null);
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("centralWarehouse", branchService.getOrCreateCentralWarehouse());
        return "admin/inventory";
    }

    @GetMapping("/admin/reports")
    public String adminReportsPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (!isManagerOrAdmin(actor)) {
            return "redirect:/login?redirect=/admin/reports";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("isManager", actor != null && actor.getRole() == Role.MANAGER);
        model.addAttribute("managerBranchId", actor != null ? actor.getBranchId() : null);
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("centralWarehouse", branchService.getOrCreateCentralWarehouse());
        return "admin/reports";
    }

    @GetMapping("/admin/documents")
    public String documentsPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (!isManagerOrAdmin(actor)) {
            return "redirect:/login?redirect=/admin/documents";
        }
        model.addAttribute("settings", settingsRepository.findById(1).orElse(null));
        model.addAttribute("actor", actor);
        model.addAttribute("isManager", actor != null && actor.getRole() == Role.MANAGER);
        model.addAttribute("managerBranchId", actor != null ? actor.getBranchId() : null);
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("centralWarehouse", branchService.getOrCreateCentralWarehouse());
        return "admin/documents";
    }

    @GetMapping("/admin/settings")
    public String adminSettingsPage(@AuthenticationPrincipal CurrentActor actor, Model model) {
        if (!isStaffAdmin(actor)) {
            return "redirect:/admin";
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("isManager", actor != null && actor.getRole() == Role.MANAGER);
        return "admin/settings";
    }
}
