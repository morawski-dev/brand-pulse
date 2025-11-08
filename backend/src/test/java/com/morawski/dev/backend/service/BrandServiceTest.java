package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.brand.*;
import com.morawski.dev.backend.entity.Brand;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.exception.*;
import com.morawski.dev.backend.mapper.BrandMapper;
import com.morawski.dev.backend.mapper.ReviewSourceMapper;
import com.morawski.dev.backend.repository.BrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BrandService Tests")
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private UserService userService;

    @Mock
    private BrandMapper brandMapper;

    @Mock
    private ReviewSourceMapper reviewSourceMapper;

    @InjectMocks
    private BrandService brandService;

    private User testUser;
    private Brand testBrand;
    private BrandResponse testBrandResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .passwordHash("hashedPassword")
            .build();

        testBrand = Brand.builder()
            .id(1L)
            .name("Test Brand")
            .user(testUser)
            .reviewSources(new ArrayList<>())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        testBrandResponse = BrandResponse.builder()
            .brandId(1L)
            .name("Test Brand")
            .sourceCount(0)
            .build();
    }

    @Nested
    @DisplayName("createBrand() Tests")
    class CreateBrandTests {

        @Test
        @DisplayName("Should create brand successfully")
        void shouldCreateBrand_Successfully() {
            // Given
            CreateBrandRequest request = new CreateBrandRequest();
            request.setName("New Brand");

            when(userService.findByIdOrThrow(1L)).thenReturn(testUser);
            when(brandRepository.existsByUserId(1L)).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenReturn(testBrand);
            when(brandMapper.toBrandResponse(testBrand)).thenReturn(testBrandResponse);

            // When
            BrandResponse response = brandService.createBrand(1L, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getBrandId()).isEqualTo(1L);

            ArgumentCaptor<Brand> brandCaptor = ArgumentCaptor.forClass(Brand.class);
            verify(brandRepository).save(brandCaptor.capture());
            Brand savedBrand = brandCaptor.getValue();
            assertThat(savedBrand.getName()).isEqualTo("New Brand");
            assertThat(savedBrand.getUser()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Should throw BrandLimitExceededException when user already has a brand")
        void shouldThrowException_WhenUserAlreadyHasBrand() {
            // Given
            CreateBrandRequest request = new CreateBrandRequest();
            request.setName("Another Brand");

            when(userService.findByIdOrThrow(1L)).thenReturn(testUser);
            when(brandRepository.existsByUserId(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> brandService.createBrand(1L, request))
                .isInstanceOf(BrandLimitExceededException.class)
                .hasMessageContaining("MVP supports one brand per user account");

            verify(brandRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ValidationException when brand name is invalid")
        void shouldThrowException_WhenBrandNameInvalid() {
            // Given
            CreateBrandRequest request = new CreateBrandRequest();
            request.setName("");

            when(userService.findByIdOrThrow(1L)).thenReturn(testUser);
            when(brandRepository.existsByUserId(1L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> brandService.createBrand(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid brand name");

            verify(brandRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUserBrands() Tests")
    class GetUserBrandsTests {

        @Test
        @DisplayName("Should return user brands")
        void shouldReturnUserBrands() {
            // Given
            List<Brand> brands = List.of(testBrand);
            when(brandRepository.findByUserId(1L)).thenReturn(brands);
            when(brandMapper.toBrandResponse(testBrand)).thenReturn(testBrandResponse);

            // When
            BrandListResponse response = brandService.getUserBrands(1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getBrands()).hasSize(1);
            assertThat(response.getBrands().get(0).getBrandId()).isEqualTo(1L);

            verify(brandRepository).findByUserId(1L);
        }

        @Test
        @DisplayName("Should return empty list when user has no brands")
        void shouldReturnEmptyList_WhenUserHasNoBrands() {
            // Given
            when(brandRepository.findByUserId(1L)).thenReturn(new ArrayList<>());

            // When
            BrandListResponse response = brandService.getUserBrands(1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getBrands()).isEmpty();

            verify(brandRepository).findByUserId(1L);
        }
    }

    @Nested
    @DisplayName("getBrandById() Tests")
    class GetBrandByIdTests {

        @Test
        @DisplayName("Should return brand by ID when user owns it")
        void shouldReturnBrandById_WhenUserOwnsIt() {
            // Given
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));

            // When
            BrandDetailResponse response = brandService.getBrandById(1L, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getBrandId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Test Brand");

            verify(brandRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when brand not found")
        void shouldThrowException_WhenBrandNotFound() {
            // Given
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> brandService.getBrandById(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Brand");

            verify(brandRepository).findById(999L);
        }

        @Test
        @DisplayName("Should throw ResourceAccessDeniedException when user doesn't own brand")
        void shouldThrowException_WhenUserDoesNotOwnBrand() {
            // Given
            User anotherUser = User.builder().id(2L).build();
            Brand anotherBrand = Brand.builder()
                .id(2L)
                .name("Another Brand")
                .user(anotherUser)
                .build();

            when(brandRepository.findById(2L)).thenReturn(Optional.of(anotherBrand));

            // When & Then
            assertThatThrownBy(() -> brandService.getBrandById(2L, 1L))
                .isInstanceOf(ResourceAccessDeniedException.class)
                .hasMessageContaining("You do not have permission to access this brand");

            verify(brandRepository).findById(2L);
        }
    }

    @Nested
    @DisplayName("updateBrand() Tests")
    class UpdateBrandTests {

        @Test
        @DisplayName("Should update brand name successfully")
        void shouldUpdateBrandName_Successfully() {
            // Given
            UpdateBrandRequest request = new UpdateBrandRequest();
            request.setName("Updated Brand Name");

            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
            when(brandRepository.save(any(Brand.class))).thenReturn(testBrand);
            when(brandMapper.toBrandResponse(testBrand)).thenReturn(testBrandResponse);

            // When
            BrandResponse response = brandService.updateBrand(1L, 1L, request);

            // Then
            assertThat(response).isNotNull();

            ArgumentCaptor<Brand> brandCaptor = ArgumentCaptor.forClass(Brand.class);
            verify(brandRepository).save(brandCaptor.capture());
            Brand savedBrand = brandCaptor.getValue();
            assertThat(savedBrand.getName()).isEqualTo("Updated Brand Name");
        }

        @Test
        @DisplayName("Should throw ValidationException when brand name is invalid")
        void shouldThrowException_WhenBrandNameInvalid() {
            // Given
            UpdateBrandRequest request = new UpdateBrandRequest();
            request.setName("");

            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));

            // When & Then
            assertThatThrownBy(() -> brandService.updateBrand(1L, 1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid brand name");

            verify(brandRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceAccessDeniedException when user doesn't own brand")
        void shouldThrowException_WhenUserDoesNotOwnBrand() {
            // Given
            UpdateBrandRequest request = new UpdateBrandRequest();
            request.setName("Updated Name");

            User anotherUser = User.builder().id(2L).build();
            Brand anotherBrand = Brand.builder()
                .id(2L)
                .name("Another Brand")
                .user(anotherUser)
                .build();

            when(brandRepository.findById(2L)).thenReturn(Optional.of(anotherBrand));

            // When & Then
            assertThatThrownBy(() -> brandService.updateBrand(2L, 1L, request))
                .isInstanceOf(ResourceAccessDeniedException.class);

            verify(brandRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteBrand() Tests")
    class DeleteBrandTests {

        @Test
        @DisplayName("Should soft delete brand successfully")
        void shouldSoftDeleteBrand_Successfully() {
            // Given
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
            when(brandRepository.save(any(Brand.class))).thenReturn(testBrand);

            // When
            brandService.deleteBrand(1L, 1L);

            // Then
            ArgumentCaptor<Brand> brandCaptor = ArgumentCaptor.forClass(Brand.class);
            verify(brandRepository).save(brandCaptor.capture());
            Brand deletedBrand = brandCaptor.getValue();
            assertThat(deletedBrand.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should cascade soft delete to review sources")
        void shouldCascadeSoftDelete_ToReviewSources() {
            // Given
            ReviewSource source1 = ReviewSource.builder().id(1L).build();
            ReviewSource source2 = ReviewSource.builder().id(2L).build();
            testBrand.setReviewSources(List.of(source1, source2));

            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
            when(brandRepository.save(any(Brand.class))).thenReturn(testBrand);

            // When
            brandService.deleteBrand(1L, 1L);

            // Then
            verify(brandRepository).save(testBrand);
            assertThat(source1.getDeletedAt()).isNotNull();
            assertThat(source2.getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Helper Methods Tests")
    class HelperMethodsTests {

        @Test
        @DisplayName("findByIdOrThrow should return brand when found")
        void findByIdOrThrow_ShouldReturnBrand_WhenFound() {
            // Given
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));

            // When
            Brand result = brandService.findByIdOrThrow(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("findByIdOrThrow should throw exception when not found")
        void findByIdOrThrow_ShouldThrowException_WhenNotFound() {
            // Given
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> brandService.findByIdOrThrow(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("userOwnsBrand should return true when user owns brand")
        void userOwnsBrand_ShouldReturnTrue_WhenUserOwnsBrand() {
            // Given
            when(brandRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testBrand));

            // When
            boolean result = brandService.userOwnsBrand(1L, 1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("userOwnsBrand should return false when user doesn't own brand")
        void userOwnsBrand_ShouldReturnFalse_WhenUserDoesNotOwnBrand() {
            // Given
            when(brandRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

            // When
            boolean result = brandService.userOwnsBrand(1L, 2L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("countUserBrands should return correct count")
        void countUserBrands_ShouldReturnCorrectCount() {
            // Given
            when(brandRepository.countByUserId(1L)).thenReturn(1L);

            // When
            long result = brandService.countUserBrands(1L);

            // Then
            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("userHasBrand should return true when user has brand")
        void userHasBrand_ShouldReturnTrue_WhenUserHasBrand() {
            // Given
            when(brandRepository.existsByUserId(1L)).thenReturn(true);

            // When
            boolean result = brandService.userHasBrand(1L);

            // Then
            assertThat(result).isTrue();
        }
    }
}
