package vn.edu.ute.milktea.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.ute.milktea.entity.catalog.Category;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.settings.GlobalSettings;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.settings.GlobalSettingsRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.security.CurrentActor;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CustomerPageController {

    private final GlobalSettingsRepository settingsRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final DiningTableRepository tableRepository;
    private final vn.edu.ute.milktea.service.catalog.CatalogService catalogService;
    private final vn.edu.ute.milktea.service.branch.BranchService branchService;
    private final vn.edu.ute.milktea.repository.account.AccountRepository accountRepository;

    private static final String COOKIE_TABLE_QR = "MILKTEA_TABLE_QR";
    private static final String COOKIE_TABLE_ID = "MILKTEA_TABLE_ID";
    private static final String SESS_TABLE_ID = "CURRENT_TABLE_ID";
    private static final String SESS_TABLE_QR = "CURRENT_TABLE_QR";

    private DiningTable resolveTable(
            String qrCode,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {

        DiningTable table = null;
        if (qrCode != null && !qrCode.isBlank()) {
            table = tableRepository.findByQrCode(qrCode.trim()).orElse(null);
        }

        if (table != null) {
            // Lưu vào Session
            if (request != null && request.getSession(false) != null) {
                request.getSession().setAttribute(SESS_TABLE_ID, table.getId());
                request.getSession().setAttribute(SESS_TABLE_QR, table.getQrCode());
            } else if (request != null) {
                request.getSession(true).setAttribute(SESS_TABLE_ID, table.getId());
                request.getSession(true).setAttribute(SESS_TABLE_QR, table.getQrCode());
            }

            // Lưu vào Cookie tạm thời (chỉ trong phiên duyệt web) để duy trì khi chuyển hướng
            if (response != null) {
                jakarta.servlet.http.Cookie qrCookie = new jakarta.servlet.http.Cookie(COOKIE_TABLE_QR, table.getQrCode());
                qrCookie.setPath("/");
                qrCookie.setHttpOnly(true);
                qrCookie.setMaxAge(-1); // Session-only cookie, xóa khi đóng trình duyệt
                response.addCookie(qrCookie);
            }
            return table;
        }

        // Nếu trên request không truyền param qr -> Thử phục hồi từ Session hiện thời nếu khách đã quét QR
        if (request != null && request.getSession(false) != null) {
            String sessQr = (String) request.getSession(false).getAttribute(SESS_TABLE_QR);
            if (sessQr != null && !sessQr.isBlank()) {
                table = tableRepository.findByQrCode(sessQr.trim()).orElse(null);
                if (table != null) return table;
            }
        }

        return null;
    }

    @GetMapping({"/", "/menu"})
    public String menuPage(
            @RequestParam(value = "qr", required = false) String qrCode,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            Model model) {

        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        List<Category> categories = categoryRepository.findAllByActiveTrue();
        List<Product> allProducts = productRepository.findAllByActiveTrue();

        // Gom nhóm theo tên món, chỉ lấy size bé nhất (giá thấp nhất) làm đại diện trên Menu
        java.util.Map<String, Product> smallestVariantsByName = new java.util.LinkedHashMap<>();
        for (Product p : allProducts) {
            String norm = p.getName().trim().toLowerCase();
            if (!smallestVariantsByName.containsKey(norm) || p.getPrice().compareTo(smallestVariantsByName.get(norm).getPrice()) < 0) {
                smallestVariantsByName.put(norm, p);
            }
        }
        List<Product> menuProducts = new java.util.ArrayList<>(smallestVariantsByName.values());

        DiningTable table = resolveTable(qrCode, request, response);

        model.addAttribute("settings", settings);
        model.addAttribute("categories", categories);
        model.addAttribute("products", menuProducts);
        model.addAttribute("allProducts", allProducts);
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : qrCode);
        Long branchId = table != null && table.getBranch() != null ? table.getBranch().getId() : null;
        model.addAttribute("branchId", branchId);

        return "customer/menu";
    }

    @GetMapping("/cart")
    public String cartPage(
            @RequestParam(value = "qr", required = false) String qrCode,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal CurrentActor actor, Model model) {

        if (actor != null && actor.hasRole(vn.edu.ute.milktea.entity.account.Role.CASHIER)) return "redirect:/cashier";
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        DiningTable table = resolveTable(qrCode, request, response);

        model.addAttribute("settings", settings);
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : qrCode);
        if (actor != null && actor.getAccountId() != null) {
            accountRepository.findById(actor.getAccountId()).ifPresent(acc -> model.addAttribute("currentUser", acc));
        }

        return "customer/cart";
    }

    @GetMapping("/orders")
    public String ordersPage(
            @RequestParam(value = "qr", required = false) String qrCode,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal CurrentActor actor,
            Model model) {

        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        DiningTable table = resolveTable(qrCode, request, response);

        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : qrCode);

        return "customer/orders";
    }

    @GetMapping("/profile")
    public String profilePage(
            @RequestParam(value = "returnUrl", required = false) String returnUrl,
            jakarta.servlet.http.HttpServletRequest request,
            @AuthenticationPrincipal CurrentActor actor,
            Model model) {
        if (actor == null || actor.getAccountId() == null) {
            return "redirect:/login?redirect=/profile";
        }

        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        model.addAttribute("settings", settings);
        model.addAttribute("actor", actor);

        String referer = request.getHeader("Referer");
        String safeBackUrl = resolveSafeBackUrl(returnUrl, referer, actor);
        model.addAttribute("backUrl", safeBackUrl);

        return "profile";
    }

    @GetMapping("/products/{id}")
    public String productDetailPage(
            @org.springframework.web.bind.annotation.PathVariable("id") Long id,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "qr", required = false) String qrCode,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal CurrentActor actor,
            Model model) {
        DiningTable table = resolveTable(qrCode, request, response);
        Long targetBranchId = branchId;
        if (targetBranchId == null && table != null && table.getBranch() != null) {
            targetBranchId = table.getBranch().getId();
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        var productDetail = catalogService.getProductDetail(id, targetBranchId, actor);

        model.addAttribute("product", productDetail);
        model.addAttribute("settings", settings);
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : qrCode);
        model.addAttribute("branchId", targetBranchId);
        return "customer/product-detail";
    }

    @GetMapping("/favorites")
    public String wishlistPage(
            @RequestParam(value = "qr", required = false) String qrCode,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal CurrentActor actor,
            Model model) {
        if (actor == null || !actor.isAuthenticated()) {
            return "redirect:/login?redirect=/favorites";
        }
        DiningTable table = resolveTable(qrCode, request, response);
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        List<Product> allProducts = productRepository.findAllByActiveTrue();

        model.addAttribute("settings", settings);
        model.addAttribute("allProducts", allProducts);
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : qrCode);
        return "customer/wishlist";
    }

    private String resolveSafeBackUrl(String returnUrl, String referer, CurrentActor actor) {
        String candidate = null;
        if (returnUrl != null && !returnUrl.isBlank()) {
            candidate = returnUrl.trim();
        } else if (referer != null && !referer.isBlank()) {
            try {
                java.net.URI uri = java.net.URI.create(referer.trim());
                String path = uri.getPath();
                String query = uri.getQuery();
                if (path != null) {
                    candidate = path + (query != null && !query.isBlank() ? "?" + query : "");
                }
            } catch (Exception ignored) {}
        }

        if (candidate != null && isValidInternalUrl(candidate, actor)) {
            return candidate;
        }

        if (actor != null && actor.getRole() != null) {
            switch (actor.getRole()) {
                case ADMIN:
                    return "/admin";
                case CASHIER:
                    return "/cashier";
                case KITCHEN:
                    return "/kitchen";
                case CUSTOMER:
                default:
                    return "/";
            }
        }
        return "/";
    }

    private boolean isValidInternalUrl(String url, CurrentActor actor) {
        if (url == null || !url.startsWith("/") || url.startsWith("//") || url.startsWith("/\\")) {
            return false;
        }
        String lower = url.toLowerCase();
        if (lower.startsWith("/login") || lower.startsWith("/logout") || lower.startsWith("/profile") || lower.startsWith("/api/")) {
            return false;
        }
        if (lower.startsWith("/admin") && (actor == null || actor.getRole() != vn.edu.ute.milktea.entity.account.Role.ADMIN)) {
            return false;
        }
        if (lower.startsWith("/cashier") && (actor == null || (actor.getRole() != vn.edu.ute.milktea.entity.account.Role.CASHIER && actor.getRole() != vn.edu.ute.milktea.entity.account.Role.ADMIN && actor.getRole() != vn.edu.ute.milktea.entity.account.Role.MANAGER))) {
            return false;
        }
        if (lower.startsWith("/kitchen") && (actor == null || (actor.getRole() != vn.edu.ute.milktea.entity.account.Role.KITCHEN && actor.getRole() != vn.edu.ute.milktea.entity.account.Role.ADMIN && actor.getRole() != vn.edu.ute.milktea.entity.account.Role.MANAGER))) {
            return false;
        }
        return true;
    }
}
