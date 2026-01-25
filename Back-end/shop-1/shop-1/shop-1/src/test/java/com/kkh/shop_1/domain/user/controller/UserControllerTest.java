package com.kkh.shop_1.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.domain.user.dto.request.UserInfoUpdateRequestDTO;
import com.kkh.shop_1.domain.user.dto.request.UserPasswordUpdateRequestDTO;
import com.kkh.shop_1.domain.user.dto.response.UserInfoResponseDTO;
import com.kkh.shop_1.domain.user.dto.response.UserInfoUpdateResponseDTO;
import com.kkh.shop_1.domain.user.entity.LoginType;
import com.kkh.shop_1.domain.user.entity.UserRole;
import com.kkh.shop_1.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("내 정보 조회 성공")
    @WithMockUser(username = "1", roles = "USER")
    void getUserInfo_Success() throws Exception {
        // given
        UserInfoResponseDTO response = UserInfoResponseDTO.builder()
                .loginType(LoginType.LOCAL)
                .email("email")
                .profileImg("img")
                .nickname("nick")
                .userRole(UserRole.CUSTOMER)
                .build();
        given(userService.findUserInfo(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/user/my")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("nick"));
    }

    @Test
    @DisplayName("프로필 수정 성공")
    @WithMockUser(username = "1", roles = "USER")
    void updateUserInfo_Success() throws Exception {
        // given
        UserInfoUpdateRequestDTO request = new UserInfoUpdateRequestDTO("newNick");
        UserInfoUpdateResponseDTO response = UserInfoUpdateResponseDTO.builder()
                .nickname("newNick")
                .build();

        given(userService.updateUserProfile(eq(1L), any(UserInfoUpdateRequestDTO.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/user/my")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("newNick"));
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    @WithMockUser(username = "1", roles = "USER")
    void updateUserPassword_Success() throws Exception {
        // given
        UserPasswordUpdateRequestDTO request = new UserPasswordUpdateRequestDTO("old", "new");

        // when & then
        mockMvc.perform(patch("/user/my/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(userService).updateUserPassword(eq(1L), any(UserPasswordUpdateRequestDTO.class));
    }
}
