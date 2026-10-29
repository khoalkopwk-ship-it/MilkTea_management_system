package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.branch.Branch;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class HomeTemplateRenderingTest {
    private String render(List<Product> products, List<Branch> branches, String qr) {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        // Render links without a servlet container or database.
        engine.setLinkBuilder(new StandardLinkBuilder() {
            @Override
            protected String computeContextPath(org.thymeleaf.context.IExpressionContext context,
                    String base, Map<String, Object> parameters) { return ""; }
        });
        var context = new Context();
        context.setVariable("featuredProducts", products);
        context.setVariable("branches", branches);
        context.setVariable("tableQr", qr);
        return engine.process(new TemplateSpec("customer/home", Set.of("content"), org.thymeleaf.templatemode.TemplateMode.HTML, null), context);
    }

    @Test
    void emptyCatalogWithoutQrRendersSafeBrowseLinks() {
        String html = render(List.of(), List.of(), null);
        assertTrue(html.contains("Thực đơn nổi bật đang được cập nhật"));
        assertTrue(html.contains("Thông tin chi nhánh đang được cập nhật"));
        assertTrue(html.contains("/images/MilkTea.png"));
        assertFalse(html.contains("30+"));
    }

    @Test
    void realProductsPreserveQrAndEscapeNamesWithMissingMediaAndPrice() {
        var product = new Product();
        product.setId(12L);
        product.setName("Trà <đặc biệt>");
        var branch = new Branch();
        branch.setId(3L);
        branch.setName("MilkTea 3");
        String html = render(List.of(product), List.of(branch), "table-token");
        assertTrue(html.contains("Trà &lt;đặc biệt&gt;"));
        assertTrue(html.contains("/products/12?qr=table-token"));
        assertTrue(html.contains("branchId=3"));
        assertTrue(html.contains("Địa chỉ đang cập nhật"));
        assertFalse(html.contains("class=\"drink-price\""));
        product.setPrice(new BigDecimal("35000"));
        assertTrue(render(List.of(product), List.of(branch), "table-token").contains("35.000 ₫"));
    }
}
