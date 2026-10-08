package vn.edu.ute.milktea.controller.page;

import lombok.RequiredArgsConstructor;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CustomerPageController {

    private final GlobalSettingsRepository settingsRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final DiningTableRepository tableRepository;

    @GetMapping({"/", "/menu"})
    public String menuPage(
            @RequestParam(value = "qr", required = false) String qrCode,
            @RequestParam(value = "table", required = false) String tableParam,
            Model model) {

        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
        List<Category> categories = categoryRepository.findAllByActiveTrue();
        List<Product> products = productRepository.findAllByActiveTrue();

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
            Model model) {

        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
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

        model.addAttribute("settings", settings);
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : qrCode);

        return "customer/cart";
    }

    @GetMapping("/orders")
    public String ordersPage(
            @RequestParam(value = "qr", required = false) String qrCode,
            @RequestParam(value = "table", required = false) String tableParam,
            Model model) {

        GlobalSettings settings = settingsRepository.findById(1).orElse(null);
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

        model.addAttribute("settings", settings);
        model.addAttribute("currentTable", table);
        model.addAttribute("tableQr", table != null ? table.getQrCode() : qrCode);

        return "customer/orders";
    }
}
