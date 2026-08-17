package com.dinogo.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.catalog.controller.ProductController;
import com.dinogo.catalog.service.ProductService;
import com.dinogo.coupon.controller.CouponController;
import com.dinogo.coupon.service.CouponService;
import com.dinogo.sales.controller.OrderController;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.service.OrderService;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;
import com.dinogo.seller.controller.SellerProductController;
import com.dinogo.seller.service.SellerProductService;

@WebMvcTest({
        SellerProductController.class,
        CouponController.class,
        ProductController.class,
        OrderController.class
})
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class RoleAuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SellerProductService sellerProductService;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @Test
    void sellerOperationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/seller/products").param("sellerId", "1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(sellerProductService, productService, orderService);
    }

    @Test
    void buyerCannotAccessSellerEndpointsOrProductWrites() throws Exception {
        mockMvc.perform(get("/api/seller/products")
                        .param("sellerId", "1")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/seller/coupons")
                        .param("sellerId", "1")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(sellerProductService, couponService, productService);
    }

    @Test
    void buyerCannotUpdateOrderStatus() throws Exception {
        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    @Test
    void adminCannotAccessSellerEndpointsUntilAnAdminContractExists() throws Exception {
        mockMvc.perform(get("/api/seller/coupons")
                        .param("sellerId", "1")
                        .with(authentication(authenticationForRole("ADMIN"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(couponService);
    }

    @Test
    void legacyTokenWithoutRolesCannotAccessSellerEndpoints() throws Exception {
        when(jwtTokenUtil.extractSubject("legacy-token")).thenReturn("member@example.com");
        when(jwtTokenUtil.extractMemberId("legacy-token")).thenReturn(1);
        when(jwtTokenUtil.extractRoles("legacy-token")).thenReturn(List.of());

        mockMvc.perform(get("/api/seller/products")
                        .param("sellerId", "1")
                        .header("Authorization", "Bearer legacy-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(sellerProductService);
    }

    @Test
    void sellerCanReachSellerEndpointsAndOrderStatusUpdate() throws Exception {
        when(sellerProductService.getProducts(1)).thenReturn(List.of());
        when(couponService.getCoupons(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/seller/products")
                        .param("sellerId", "1")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/seller/coupons")
                        .param("sellerId", "1")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk());

        verify(sellerProductService).getProducts(1);
        verify(couponService).getCoupons(1);
        verify(productService).createProduct(any());
        verify(orderService).updateStatusBySeller(1, 1, OrderStatus.PROCESSING, null);
    }

    private UsernamePasswordAuthenticationToken authenticationForRole(String role) {
        AuthenticatedMember principal = new AuthenticatedMember(1, "member@example.com");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
