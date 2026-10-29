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
    private final vn.edu.ute.milktea.repository.review.ProductReviewRepository productReviewRepository;
    private final vn.edu.ute.milktea.repository.table.TableSessionRepository sessionRepository;

    private static final String COOKIE_TABLE_QR = "MILKTEA_TABLE_QR";
    private static final String COOKIE_TABLE_ID = "MILKTEA_TABLE_ID";
    private static final String SESS_TABLE_ID = "CURRENT_TABLE_ID";
    private static final String SESS_TABLE_QR = "CURRENT_TABLE_QR";
    private static final String SESS_TABLE_SESSION_ID = "CURRENT_TABLE_SESSION_ID";

    private DiningTable resolveTable(
            String qrCode,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {

        DiningTable table = null;
        if (qrCode != null && !qrCode.isBlank()) {
            table = tableRepository.findByQrCode(qrCode.trim()).orElse(null);
        }

        if (table != null) {
            if (!Boolean.TRUE.equals(table.getActive())) {
                return null;
            }
            // Lưu vào Session
            if (request != null) {
                var sess = request.getSession(true);
                sess.setAttribute(SESS_TABLE_ID, table.getId());
                sess.setAttribute(SESS_TABLE_QR, table.getQrCode());
                sess.setAttribute(SESS_TABLE_SESSION_ID, table.getActiveSessionId());
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
            var sess = request.getSession(false);
            String sessQr = (String) sess.getAttribute(SESS_TABLE_QR);
            Long expectedSessionId = (Long) sess.getAttribute(SESS_TABLE_SESSION_ID);
            if (sessQr != null && !sessQr.isBlank()) {
                table = tableRepository.findByQrCode(sessQr.trim()).orElse(null);
                if (table != null && Boolean.TRUE.equals(table.getActive())) {
                    // Yêu cầu 6 & 10: Nếu đã có phiên trước đó mà hiện tại phiên bàn trên DB đã kết thúc hoặc chuyển phiên mới -> Thu hồi quyền
                    boolean sessionInvalid = false;
                    if (expectedSessionId != null) {
                        if (table.getActiveSessionId() == null || !expectedSessionId.equals(table.getActiveSessionId())) {
                            sessionInvalid = true;
                        } else {
                            var activeSession = sessionRepository.findById(table.getActiveSessionId()).orElse(null);
                            if (activeSession == null || activeSession.getStatus() != vn.edu.ute.milktea.entity.table.TableSessionStatus.OPEN) {
                                sessionInvalid = true;
                            }
                        }
                    } else if (table.getActiveSessionId() != null) {
                        // Trước đó chưa có phiên, không tự gia nhập phiên mới do người khác mở
                        sessionInvalid = true;
                    }

                    if (sessionInvalid) {
                        sess.removeAttribute(SESS_TABLE_ID);
                        sess.removeAttribute(SESS_TABLE_QR);
                        sess.removeAttribute(SESS_TABLE_SESSION_ID);
                        if (response != null) {
                            jakarta.servlet.http.Cookie qrCookie = new jakarta.servlet.http.Cookie(COOKIE_TABLE_QR, "");
                            qrCookie.setPath("/");
                            qrCookie.setMaxAge(0);
                            qrCookie.setHttpOnly(true);
                            response.addCookie(qrCookie);
                        }
                        return null;
                    }
                    return table;
                }
            }
        }

        return null;
    }

    @GetMapping("/")
    public String homePage(
            @RequestParam(value = "qr", required = false) String qrCode,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            Model model) {

        // Yêu cầu 4: Nếu khách quét QR qua URL root (/?qr=...), chuyển tiếp thẳng tới menu gọi món tại bàn
        if (qrCode != null && !qrCode.isBlank()) {
            return "redirect:/menu?qr=" + qrCode.trim();
        }

        DiningTable table = resolveTable(null, request, response);
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        var branches = branchService.getActiveBranches();
        var featuredPage = catalogService.getPaginatedMenu(null, null, null, null, "default", "best_seller", null, 1, 8);
        var reviewsPage = productReviewRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 4));

        model.addAttribute("settings", settings);
        model.addAttribute("branches", branches);
        model.addAttribute("featuredProducts", featuredPage.getItems());
        model.addAttribute("reviews", reviewsPage != null ? reviewsPage.getContent() : java.util.List.of());
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : null);
        model.addAttribute("isViewOnly", table == null);
        model.addAttribute("activePage", "home");

        return "customer/home";
    }

    @GetMapping("/table/{qrCode}")
    public String tableQrRedirect(@org.springframework.web.bind.annotation.PathVariable("qrCode") String qrCode) {
        return "redirect:/menu?qr=" + (qrCode != null ? qrCode.trim() : "");
    }

    @GetMapping("/menu")
    public String menuPage(
            @RequestParam(value = "qr", required = false) String qrCode,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            @RequestParam(value = "sort", defaultValue = "default") String sort,
            @RequestParam(value = "featured", defaultValue = "all") String featuredType,
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "category", required = false) Long categoryId,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            Model model) {

        DiningTable table = resolveTable(qrCode, request, response);
        Long activeBranchId = branchId;
        boolean isViewOnly = true;

        if (table != null && table.getBranch() != null) {
            activeBranchId = table.getBranch().getId();
            isViewOnly = false;
        }

        boolean branchSelected = (activeBranchId != null);
        var activeBranches = branchService.getActiveBranches();
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        List<Category> categories = categoryRepository.findAllByActiveTrue();

        // Nạp danh sách món uống
        var initialPage = catalogService.getPaginatedMenu(keyword, categoryId, null, null, sort, featuredType, activeBranchId, page, size);

        model.addAttribute("settings", settings);
        model.addAttribute("categories", categories);
        model.addAttribute("activeBranches", activeBranches);
        model.addAttribute("branchId", activeBranchId);
        model.addAttribute("branchSelected", branchSelected);
        model.addAttribute("isViewOnly", isViewOnly);
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : qrCode);
        model.addAttribute("products", initialPage.getItems());
        model.addAttribute("currentPage", initialPage.getPage());
        model.addAttribute("pageSize", initialPage.getPageSize());
        model.addAttribute("totalItems", initialPage.getTotalItems());
        model.addAttribute("totalPages", initialPage.getTotalPages());
        model.addAttribute("activePage", "menu");

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
        model.addAttribute("isViewOnly", table == null);
        model.addAttribute("activePage", "cart");
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
        model.addAttribute("isViewOnly", table == null);
        model.addAttribute("activePage", "orders");

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
            @org.springframework.web.bind.annotation.PathVariable("id") String idParam,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "qr", required = false) String qrCode,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal CurrentActor actor,
            Model model) {

        Long productId;
        Long targetBranchId = branchId;
        if (idParam != null && idParam.contains("?")) {
            String[] parts = idParam.split("\\?", 2);
            try {
                productId = Long.parseLong(parts[0].trim());
            } catch (Exception e) {
                return "redirect:/menu";
            }
            if (targetBranchId == null && parts.length > 1 && parts[1].contains("branchId=")) {
                try {
                    for (String pair : parts[1].split("&")) {
                        String[] kv = pair.split("=", 2);
                        if ("branchId".equalsIgnoreCase(kv[0].trim()) && kv.length > 1) {
                            targetBranchId = Long.parseLong(kv[1].trim());
                        }
                    }
                } catch (Exception ignored) {}
            }
        } else {
            try {
                productId = Long.parseLong(idParam != null ? idParam.trim() : "0");
            } catch (Exception e) {
                return "redirect:/menu";
            }
        }

        DiningTable table = resolveTable(qrCode, request, response);
        if (targetBranchId == null && table != null && table.getBranch() != null) {
            targetBranchId = table.getBranch().getId();
        }
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        var productDetail = catalogService.getProductDetail(productId, targetBranchId, actor);

        model.addAttribute("product", productDetail);
        model.addAttribute("settings", settings);
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : qrCode);
        model.addAttribute("branchId", targetBranchId);
        model.addAttribute("activeBranches", branchService.getActiveBranches());
        model.addAttribute("isViewOnly", table == null);
        model.addAttribute("activePage", "menu");
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
        model.addAttribute("isViewOnly", table == null);
        model.addAttribute("activePage", "favorites");
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
