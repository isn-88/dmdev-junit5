package com.dmdev.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dmdev.dao.SubscriptionDao;
import com.dmdev.dto.CreateSubscriptionDto;
import com.dmdev.entity.Provider;
import com.dmdev.entity.Status;
import com.dmdev.entity.Subscription;
import com.dmdev.integration.IntegrationTestBase;
import com.dmdev.mapper.CreateSubscriptionMapper;
import com.dmdev.validator.CreateSubscriptionValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionServiceIT extends IntegrationTestBase {


  private SubscriptionDao subscriptionDao;
  private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
  private SubscriptionService service;


  @BeforeEach
  void init() {
    subscriptionDao = SubscriptionDao.getInstance();

    service = new SubscriptionService(
        subscriptionDao,
        CreateSubscriptionMapper.getInstance(),
        CreateSubscriptionValidator.getInstance(),
        clock
    );
  }

  @Test
  void upsert() {
    Integer userId = 100;
    Subscription subscription = getCanselSubscriptionForUserId(userId, "prolongation");
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(userId)
        .name(subscription.getName())
        .provider(subscription.getProvider().name())
        .expirationDate(Instant.now().plus(60, ChronoUnit.DAYS))
        .build();
    subscriptionDao.insert(subscription);

    Subscription actualResult = service.upsert(createSubscriptionDto);

    assertThat(actualResult).isNotNull();
    assertThat(actualResult.getStatus()).isEqualByComparingTo(Status.ACTIVE);
    assertThat(actualResult.getExpirationDate()).isEqualTo(createSubscriptionDto.getExpirationDate());
  }

  @Test
  void cancel() {
    Subscription subscription = getActiveSubscriptionForUserId(100, "cansel");
    subscriptionDao.insert(subscription);

    service.cancel(subscription.getId());

    Optional<Subscription> actualResult = subscriptionDao.findById(subscription.getId());
    assertThat(actualResult).isPresent();
    assertThat(actualResult.get().getStatus()).isEqualByComparingTo(Status.CANCELED);
  }

  @Test
  void expire() {
    Subscription subscription = getActiveSubscriptionForUserId(100, "expired");
    subscriptionDao.insert(subscription);

    service.expire(subscription.getId());

    Optional<Subscription> actualResult = subscriptionDao.findById(subscription.getId());
    assertThat(actualResult).isPresent();
    assertThat(actualResult.get().getStatus()).isEqualByComparingTo(Status.EXPIRED);
    assertThat(actualResult.get().getExpirationDate().truncatedTo(ChronoUnit.SECONDS))
        .isEqualTo(clock.instant().truncatedTo(ChronoUnit.SECONDS));
  }


  private Subscription getActiveSubscriptionForUserId(Integer userId, String name) {
    return Subscription.builder()
        .id(1)
        .userId(userId)
        .name(name)
        .provider(Provider.APPLE)
        .expirationDate(Instant.now().plus(10, ChronoUnit.DAYS))
        .status(Status.ACTIVE)
        .build();
  }

  private Subscription getCanselSubscriptionForUserId(Integer userId, String name) {
    return Subscription.builder()
        .id(2)
        .userId(userId)
        .name(name)
        .provider(Provider.GOOGLE)
        .expirationDate(Instant.now().minus(14, ChronoUnit.DAYS))
        .status(Status.CANCELED)
        .build();
  }
}