package com.dinogo.member.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.member.service.AddressService;
import com.dinogo.security.AuthenticatedMember;

@SpringBootTest(properties = "jwt.secret=test-secret-for-jwt-context-only-32-bytes")
@AutoConfigureMockMvc
class AddressSecurityTest {

    // MockMvc 驗證實際 SecurityFilterChain 與 validation。
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddressService addressService;

    // 未登入與已登入授權測試。
    @Test
    void addressesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedMemberCanGetAddresses() throws Exception {
        when(addressService.getAddresses(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/addresses")
                        .with(authentication(authenticationForMember(1))))
                .andExpect(status().isOk());
    }

    // Request DTO validation 測試。
    @Test
    void createAddressValidatesRequest() throws Exception {
        mockMvc.perform(post("/api/addresses")
                        .with(authentication(authenticationForMember(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // 建立符合專案 JWT principal 型別的測試 Authentication。
    private UsernamePasswordAuthenticationToken authenticationForMember(Integer memberId) {
        AuthenticatedMember principal = new AuthenticatedMember(memberId, "user@example.com");
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }
}
