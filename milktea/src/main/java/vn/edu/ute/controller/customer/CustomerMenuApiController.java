package vn.edu.ute.controller.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.dto.customer.MenuItemResponse;
import vn.edu.ute.service.menu.MenuService;

import java.util.List;

@RestController
@RequestMapping("/api/customer/menu")
@RequiredArgsConstructor
public class CustomerMenuApiController {

    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<List<MenuItemResponse>>
    getMenu() {

        return ResponseEntity.ok(
                menuService.getMenu()
        );
    }
}