package com.dmdev.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.dmdev.dto.CreateSubscriptionDto;
import com.dmdev.entity.Provider;
import com.dmdev.entity.Status;
import com.dmdev.entity.Subscription;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CreateSubscriptionMapperTest {

  private final CreateSubscriptionMapper mapper = CreateSubscriptionMapper.getInstance();

  @Test
  void map() {
    Instant expirationDate = Instant.now();
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(1)
        .name("test")
        .provider("google")
        .expirationDate(expirationDate)
        .build();

    Subscription actualResult = mapper.map(createSubscriptionDto);

    Subscription expectedResult = Subscription.builder()
        .userId(1)
        .name("test")
        .provider(Provider.GOOGLE)
        .expirationDate(expirationDate)
        .status(Status.ACTIVE)
        .build();
    assertThat(actualResult).isEqualTo(expectedResult);
  }
}