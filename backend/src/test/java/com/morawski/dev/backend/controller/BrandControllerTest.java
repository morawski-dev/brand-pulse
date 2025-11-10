package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.brand.*;
import com.morawski.dev.backend.exception.ResourceAccessDeniedException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.exception.ValidationException;
import com.morawski.dev.backend.security.test.WithMockCustomUser;
import com.morawski.dev.backend.security.test.WithMockCustomUser;
import com.morawski.dev.backend.service.BrandService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for BrandController using @WebMvcTest.
 * Tests brand management endpoints with MockMvc.
 */
@WebMvcTest(BrandController.class)
@DisplayName("BrandController Integration Tests")
class BrandControllerTest extends ControllerTestBase {

    @MockBean
    private BrandService brandService;


    @Nested
    @DisplayName("POST /api/brands")
    class CreateBrandTests {

        @Test
        @WithMockCustomUser
        void shouldCreateBrand_Successfully() throws Exception {
            // Given
            CreateBrandRequest request = new CreateBrandRequest();
            request.setName("My Salon");

            BrandResponse response = BrandResponse.builder()
                .brandId(1L)
                .userId(1L)
                .name("My Salon")
                .sourceCount(0)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            when(brandService.createBrand(eq(1L), any(CreateBrandRequest.class)))
                .thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(post("/api/brands")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.name").value("My Salon"))
                .andExpect(jsonPath("$.sourceCount").value(0));

            verify(brandService).createBrand(eq(1L), any(CreateBrandRequest.class));
        }

        @Test
        @WithMockCustomUser
        void shouldReturnBadRequest_WhenNameMissing() throws Exception {
            // Given
            CreateBrandRequest request = new CreateBrandRequest();

            // When
            ResultActions result = mockMvc.perform(post("/api/brands")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isBadRequest());

            verify(brandService, never()).createBrand(any(), any());
        }

        @Test
        @WithMockCustomUser
        void shouldReturnConflict_WhenUserAlreadyHasBrand() throws Exception {
            // Given
            CreateBrandRequest request = new CreateBrandRequest();
            request.setName("My Salon");

            when(brandService.createBrand(eq(1L), any(CreateBrandRequest.class)))
                .thenThrow(new ValidationException("User can only have one brand in MVP"));

            // When
            ResultActions result = mockMvc.perform(post("/api/brands")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isBadRequest());

            verify(brandService).createBrand(eq(1L), any(CreateBrandRequest.class));
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when user is not authenticated")
        void shouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
            // Given
            CreateBrandRequest request = new CreateBrandRequest();
            request.setName("My Salon");

            // When
            ResultActions result = mockMvc.perform(post("/api/brands")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isUnauthorized());

            verify(brandService, never()).createBrand(any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/brands")
    class GetUserBrandsTests {

        @Test
        @WithMockCustomUser
        void shouldGetUserBrands_Successfully() throws Exception {
            // Given
            BrandResponse brand = BrandResponse.builder()
                .brandId(1L)
                .userId(1L)
                .name("My Salon")
                .sourceCount(2)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            BrandListResponse response = new BrandListResponse(List.of(brand));

            when(brandService.getUserBrands(1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.brands").isArray())
                .andExpect(jsonPath("$.brands[0].brandId").value(1))
                .andExpect(jsonPath("$.brands[0].name").value("My Salon"))
                .andExpect(jsonPath("$.brands[0].sourceCount").value(2));

            verify(brandService).getUserBrands(1L);
        }

        @Test
        @WithMockCustomUser
        void shouldReturnEmptyList_WhenUserHasNoBrands() throws Exception {
            // Given
            BrandListResponse response = new BrandListResponse(List.of());

            when(brandService.getUserBrands(1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.brands").isArray())
                .andExpect(jsonPath("$.brands").isEmpty());

            verify(brandService).getUserBrands(1L);
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when user is not authenticated")
        void shouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/brands")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isUnauthorized());

            verify(brandService, never()).getUserBrands(any());
        }
    }

    @Nested
    @DisplayName("GET /api/brands/{brandId}")
    class GetBrandByIdTests {

        @Test
        @WithMockCustomUser
        void shouldGetBrandById_Successfully() throws Exception {
            // Given
            BrandDetailResponse response = BrandDetailResponse.builder()
                .brandId(1L)
                .name("My Salon")
                .sourceCount(0)
                .sources(List.of())
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            when(brandService.getBrandById(1L, 1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.name").value("My Salon"));

            verify(brandService).getBrandById(1L, 1L);
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND when brand doesn't exist")
        void shouldReturnNotFound_WhenBrandDoesNotExist() throws Exception {
            // Given
            when(brandService.getBrandById(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Brand", "id", 999L));

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/999")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound());

            verify(brandService).getBrandById(1L, 999L);
        }

        @Test
        @DisplayName("Should return 403 FORBIDDEN when user doesn't own brand")
        void shouldReturnForbidden_WhenUserDoesNotOwnBrand() throws Exception {
            // Given
            when(brandService.getBrandById(1L, 2L))
                .thenThrow(new ResourceAccessDeniedException("Access denied"));

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/2")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isForbidden());

            verify(brandService).getBrandById(1L, 2L);
        }
    }

    @Nested
    @DisplayName("PATCH /api/brands/{brandId}")
    class UpdateBrandTests {

        @Test
        @WithMockCustomUser
        void shouldUpdateBrand_Successfully() throws Exception {
            // Given
            UpdateBrandRequest request = new UpdateBrandRequest();
            request.setName("Updated Salon Name");

            BrandResponse response = BrandResponse.builder()
                .brandId(1L)
                .userId(1L)
                .name("Updated Salon Name")
                .sourceCount(2)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            when(brandService.updateBrand(eq(1L), eq(1L), any(UpdateBrandRequest.class)))
                .thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(patch("/api/brands/1")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.name").value("Updated Salon Name"));

            verify(brandService).updateBrand(eq(1L), eq(1L), any(UpdateBrandRequest.class));
        }

        @Test
        @DisplayName("Should return 403 FORBIDDEN when user doesn't own brand")
        void shouldReturnForbidden_WhenUserDoesNotOwnBrand() throws Exception {
            // Given
            UpdateBrandRequest request = new UpdateBrandRequest();
            request.setName("Updated Name");

            when(brandService.updateBrand(eq(1L), eq(2L), any(UpdateBrandRequest.class)))
                .thenThrow(new ResourceAccessDeniedException("Access denied"));

            // When
            ResultActions result = mockMvc.perform(patch("/api/brands/2")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isForbidden());

            verify(brandService).updateBrand(eq(1L), eq(2L), any(UpdateBrandRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/brands/{brandId}")
    class DeleteBrandTests {

        @Test
        @WithMockCustomUser
        void shouldDeleteBrand_Successfully() throws Exception {
            // Given
            doNothing().when(brandService).deleteBrand(1L, 1L);

            // When
            ResultActions result = mockMvc.perform(delete("/api/brands/1")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNoContent());

            verify(brandService).deleteBrand(1L, 1L);
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND when brand doesn't exist")
        void shouldReturnNotFound_WhenBrandDoesNotExist() throws Exception {
            // Given
            doThrow(new ResourceNotFoundException("Brand", "id", 999L))
                .when(brandService).deleteBrand(1L, 999L);

            // When
            ResultActions result = mockMvc.perform(delete("/api/brands/999")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound());

            verify(brandService).deleteBrand(1L, 999L);
        }

        @Test
        @DisplayName("Should return 403 FORBIDDEN when user doesn't own brand")
        void shouldReturnForbidden_WhenUserDoesNotOwnBrand() throws Exception {
            // Given
            doThrow(new ResourceAccessDeniedException("Access denied"))
                .when(brandService).deleteBrand(1L, 2L);

            // When
            ResultActions result = mockMvc.perform(delete("/api/brands/2")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isForbidden());

            verify(brandService).deleteBrand(1L, 2L);
        }
    }
}
