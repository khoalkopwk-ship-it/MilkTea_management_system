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

    private static final String COOKIE_TABLE_QR = "MILKTEA_TABLE_QR";
    private static final String COOKIE_TABLE_ID = "MILKTEA_TABLE_ID";
    private static final String SESS_TABLE_ID = "CURRENT_TABLE_ID";
    private static final String SESS_TABLE_QR = "CURRENT_TABLE_QR";

    private DiningTable resolveTable(
            String qrCode, String tableParam,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {

        DiningTable table = null;
        if (qrCode != null && !qrCode.isBlank()) {
            table = tableRepository.findByQrCode(qrCode.trim()).orElse(null);
        } else if (tableParam != null && !tableParam.isBlank()) {
            try {
                Long id = Long.parseLong(tableParam.trim());
                table = tableRepository.findById(id).orElse(null);
            } catch (NumberFormatException ignored) {}
            if (table == null) {
                table = tableRepository.findByName(tableParam.trim()).orElse(null);
            }
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

            // Lưu vào Cookie để duy trì sau khi đăng nhập (tránh session fixation làm mất session)
            if (response != null) {
                jakarta.servlet.http.Cookie qrCookie = new jakarta.servlet.http.Cookie(COOKIE_TABLE_QR, table.getQrCode());
                qrCookie.setPath("/");
                qrCookie.setMaxAge(86400 * 3);
                response.addCookie(qrCookie);

                jakarta.servlet.http.Cookie idCookie = new jakarta.servlet.http.Cookie(COOKIE_TABLE_ID, String.valueOf(table.getId()));
                idCookie.setPath("/");
                idCookie.setMaxAge(86400 * 3);
                response.addCookie(idCookie);
            }
            return table;
        }

        // Nếu trên request không truyền param qr/table -> Thử phục hồi từ Session
        if (request != null && request.getSession(false) != null) {
            Long sessId = (Long) request.getSession(false).getAttribute(SESS_TABLE_ID);
            String sessQr = (String) request.getSession(false).getAttribute(SESS_TABLE_QR);
            if (sessId != null) {
                table = tableRepository.findById(sessId).orElse(null);
                if (table != null) return table;
            }
            if (sessQr != null && !sessQr.isBlank()) {
                table = tableRepository.findByQrCode(sessQr.trim()).orElse(null);
                if (table != null) return table;
            }
        }

        // Thử phục hồi từ Cookie
        if (request != null && request.getCookies() != null) {
            String cookieQr = null;
            Long cookieId = null;
            for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                if (COOKIE_TABLE_QR.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    cookieQr = c.getValue().trim();
                } else if (COOKIE_TABLE_ID.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    try { cookieId = Long.parseLong(c.getValue().trim()); } catch (Exception ignored) {}
                }
            }
            if (cookieId != null) {
                table = tableRepository.findById(cookieId).orElse(null);
                if (table != null) return table;
            }
            if (cookieQr != null) {
                table = tableRepository.findByQrCode(cookieQr).orElse(null);
                if (table != null) return table;
            }
        }

        return null;
    }

    @GetMapping({"/", "/menu"})
    public String menuPage(
            @RequestParam(value = "qr", required = false) String qrCode,
            @RequestParam(value = "table", required = false) String tableParam,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            Model model) {

        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        List<Category> categories = categoryRepository.findAllByActiveTrue();
        List<Product> products = productRepository.findAllByActiveTrue();

        DiningTable table = resolveTable(qrCode, tableParam, request, response);

        model.addAttribute("settings", settings);
        model.addAttribute("categories", categories);
        model.addAttribute("products", products);
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : qrCode);

        return "customer/menu";
    }

    @GetMapping("/cart")
    public String cartPage(
            @RequestParam(value = "qr", required = false) String qrCode,
            @RequestParam(value = "table", required = false) String tableParam,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal CurrentActor actor, Model model) {

        if (actor != null && actor.hasRole(vn.edu.ute.milktea.entity.account.Role.CASHIER)) return "redirect:/cashier";
        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        DiningTable table = resolveTable(qrCode, tableParam, request, response);

        model.addAttribute("settings", settings);
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : qrCode);

        return "customer/cart";
    }

    @GetMapping("/orders")
    public String ordersPage(
            @RequestParam(value = "qr", required = false) String qrCode,
            @RequestParam(value = "table", required = false) String tableParam,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal CurrentActor actor,
            Model model) {

        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        DiningTable table = resolveTable(qrCode, tableParam, request, response);

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
        if (lower.startsWith("/cashier") && (actor == null || (actor.getRole() != vn.edu.ute.milktea.entity.account.Role.CASHIER && actor.getRole() != vn.edu.ute.milktea.entity.account.Role.ADMIN))) {
            return false;
        }
        if (lower.startsWith("/kitchen") && (actor == null || (actor.getRole() != vn.edu.ute.milktea.entity.account.Role.KITCHEN && actor.getRole() != vn.edu.ute.milktea.entity.account.Role.ADMIN))) {
            return false;
        }
        return true;
    }
}
