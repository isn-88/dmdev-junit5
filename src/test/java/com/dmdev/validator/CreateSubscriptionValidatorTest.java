package com.dmdev.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dmdev.dto.CreateSubscriptionDto;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CreateSubscriptionValidatorTest {

  private final CreateSubscriptionValidator validator = CreateSubscriptionValidator.getInstance();

  @Test
  void validateSuccess() {
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(1)
        .name("test")
        .provider("google")
        .expirationDate(Instant.now().plusSeconds(60))
        .build();

    ValidationResult validationResult = validator.validate(createSubscriptionDto);

    assertFalse(validationResult.hasErrors());
  }

  @Test
  void validateInvalidUser() {
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(null)
        .name("test")
        .provider("google")
        .expirationDate(Instant.now().plusSeconds(60))
        .build();

    ValidationResult validationResult = validator.validate(createSubscriptionDto);

    assertThat(validationResult.getErrors()).hasSize(1);
    assertThat(validationResult.getErrors().get(0))
        .isEqualTo(Error.of(100, "userId is invalid"));
  }

  @Test
  void validateInvalidName() {
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(1)
        .name("   ")
        .provider("google")
        .expirationDate(Instant.now().plusSeconds(60))
        .build();

    ValidationResult validationResult = validator.validate(createSubscriptionDto);

    assertThat(validationResult.getErrors()).hasSize(1);
    assertThat(validationResult.getErrors().get(0))
        .isEqualTo(Error.of(101, "name is invalid"));
  }

  @Test
  void validateInvalidProvider() {
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(1)
        .name("test")
        .provider("undefine_provider")
        .expirationDate(Instant.now().plusSeconds(60))
        .build();

    ValidationResult validationResult = validator.validate(createSubscriptionDto);

    assertThat(validationResult.getErrors()).hasSize(1);
    assertThat(validationResult.getErrors().get(0))
        .isEqualTo(Error.of(102, "provider is invalid"));
  }

  @Test
  void validateExpiredDate() {
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(1)
        .name("test")
        .provider("google")
        .expirationDate(Instant.now().minusSeconds(60))
        .build();

    ValidationResult validationResult = validator.validate(createSubscriptionDto);

    assertThat(validationResult.getErrors()).hasSize(1);
    assertThat(validationResult.getErrors().get(0))
        .isEqualTo(Error.of(103, "expirationDate is invalid"));
  }

  @Test
  void validateExpiredDateIsNull() {
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(1)
        .name("test")
        .provider("google")
        .expirationDate(null)
        .build();

    ValidationResult validationResult = validator.validate(createSubscriptionDto);

    assertThat(validationResult.getErrors()).hasSize(1);
    assertThat(validationResult.getErrors().get(0))
        .isEqualTo(Error.of(103, "expirationDate is invalid"));
  }

  @Test
  void validateInvalidUserIdAndProviderAndExpirationDate() {
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(null)
        .name("test")
        .provider(null)
        .expirationDate(null)
        .build();

    ValidationResult validationResult = validator.validate(createSubscriptionDto);

    assertThat(validationResult.getErrors()).hasSize(3);
    assertThat(validationResult.getErrors()).contains(
        Error.of(100, "userId is invalid"),
        Error.of(102, "provider is invalid"),
        Error.of(103, "expirationDate is invalid")
    );
  }

}