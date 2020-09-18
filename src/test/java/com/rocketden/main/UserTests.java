package com.rocketden.main;

import com.rocketden.main.dto.user.CreateUserRequest;
import com.rocketden.main.dto.user.CreateUserResponse;
import com.rocketden.main.exception.UserError;
import com.rocketden.main.exception.api.ApiError;
import com.rocketden.main.exception.api.ApiErrorResponse;
import com.rocketden.main.util.Utility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.type=com.zaxxer.hikari.HikariDataSource")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
public class UserTests {

    @Autowired
    private MockMvc mockMvc;

    private static final String POST_USER = "/api/v1/user";

    @Test
    public void createNewUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setNickname("rocket");

        CreateUserResponse expected = new CreateUserResponse();
        expected.setNickname("rocket");

        MvcResult result = this.mockMvc.perform(post(POST_USER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Utility.convertObjectToJsonString(request)))
                .andDo(print()).andExpect(status().isCreated())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        CreateUserResponse actual = Utility.toObject(jsonResponse, CreateUserResponse.class);

        assertEquals(expected.getNickname(), actual.getNickname());
    }

    @Test
    public void createNewUserNoNickname() throws Exception {
        CreateUserRequest request = new CreateUserRequest();

        ApiError ERROR = UserError.INVALID_USER;

        MvcResult result = this.mockMvc.perform(post(POST_USER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Utility.convertObjectToJsonString(request)))
                .andDo(print()).andExpect(status().is(ERROR.getStatus().value()))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        ApiErrorResponse actual = Utility.toObject(jsonResponse, ApiErrorResponse.class);

        assertEquals(ERROR.getResponse(), actual);
    }

    @Test
    public void createNewUserEmptyNickname() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setNickname("");

        ApiError ERROR = UserError.INVALID_USER;

        MvcResult result = this.mockMvc.perform(post(POST_USER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Utility.convertObjectToJsonString(request)))
                .andDo(print()).andExpect(status().is(ERROR.getStatus().value()))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        ApiErrorResponse actual = Utility.toObject(jsonResponse, ApiErrorResponse.class);

        assertEquals(ERROR.getResponse(), actual);
    }

    @Test
    public void createNewUserTooLongNickname() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setNickname("rocketrocketrocketrocket");

        ApiError ERROR = UserError.INVALID_USER;

        MvcResult result = this.mockMvc.perform(post(POST_USER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Utility.convertObjectToJsonString(request)))
                .andDo(print()).andExpect(status().is(ERROR.getStatus().value()))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        ApiErrorResponse actual = Utility.toObject(jsonResponse, ApiErrorResponse.class);

        assertEquals(ERROR.getResponse(), actual);
    }

    @Test
    public void createNewUserNicknameContainsSpaces() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setNickname("rocket rocket");

        ApiError ERROR = UserError.INVALID_USER;

        MvcResult result = this.mockMvc.perform(post(POST_USER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Utility.convertObjectToJsonString(request)))
                .andDo(print()).andExpect(status().is(ERROR.getStatus().value()))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        ApiErrorResponse actual = Utility.toObject(jsonResponse, ApiErrorResponse.class);

        assertEquals(ERROR.getResponse(), actual);
    }
}
