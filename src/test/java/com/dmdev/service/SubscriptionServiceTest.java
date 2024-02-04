package com.dmdev.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmdev.dao.SubscriptionDao;
import com.dmdev.dto.CreateSubscriptionDto;
import com.dmdev.entity.Provider;
import com.dmdev.entity.Status;
import com.dmdev.entity.Subscription;
import com.dmdev.exception.SubscriptionException;
import com.dmdev.exception.ValidationException;
import com.dmdev.mapper.CreateSubscriptionMapper;
import com.dmdev.validator.CreateSubscriptionValidator;
import com.dmdev.validator.Error;
import com.dmdev.validator.ValidationResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class SubscriptionServiceTest {


  private SubscriptionDao subscriptionDao;
  private CreateSubscriptionMapper createSubscriptionMapper;
  private CreateSubscriptionValidator createSubscriptionValidator;
  private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
  private SubscriptionService service;


  @BeforeEach
  void init() {
    subscriptionDao = mock(SubscriptionDao.class);
    createSubscriptionMapper = mock(CreateSubscriptionMapper.class);
    createSubscriptionValidator = mock(CreateSubscriptionValidator.class);
    service = new SubscriptionService(subscriptionDao, createSubscriptionMapper, createSubscriptionValidator, clock);
  }

  @Test
  void upsertExistingSubscription() {
    Integer userId = 1;
    Instant nowPlusMonth = Instant.now().plus(30, ChronoUnit.DAYS);
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(userId)
        .name("expire")
        .provider("google")
        .expirationDate(nowPlusMonth)
        .build();
    doReturn(new ValidationResult()).when(createSubscriptionValidator).validate(createSubscriptionDto);
    doReturn(getAllUserSubscriptionsForUserId(userId)).when(subscriptionDao).findByUserId(userId);
    when(subscriptionDao.upsert(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Subscription actualResult = service.upsert(createSubscriptionDto);

    assertThat(actualResult.getStatus()).isEqualByComparingTo(Status.ACTIVE);
    assertThat(actualResult.getExpirationDate()).isEqualTo(createSubscriptionDto.getExpirationDate());
  }

  @Test
  void upsertNotExistingSubscription() {
    Integer userId = 1;
    Instant nowPlusMonth = Instant.now().plus(30, ChronoUnit.DAYS);
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(userId)
        .name("new")
        .provider("google")
        .expirationDate(nowPlusMonth)
        .build();
    Subscription newSubscription = Subscription.builder()
        .id(1)
        .userId(createSubscriptionDto.getUserId())
        .name(createSubscriptionDto.getName())
        .provider(Provider.findByNameOpt(createSubscriptionDto.getProvider()).orElseThrow())
        .expirationDate(createSubscriptionDto.getExpirationDate())
        .status(Status.ACTIVE)
        .build();
    doReturn(new ValidationResult()).when(createSubscriptionValidator).validate(createSubscriptionDto);
    doReturn(List.of(getActiveSubscriptionForUserId(userId, "new"))).when(subscriptionDao).findByUserId(userId);
    doReturn(newSubscription).when(createSubscriptionMapper).map(createSubscriptionDto);
    when(subscriptionDao.upsert(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Subscription actualResult = service.upsert(createSubscriptionDto);

    assertThat(actualResult).isEqualTo(newSubscription);
  }

  @Test
  void upsertThrowValidationException() {
    ValidationResult validationResult = new ValidationResult();
    validationResult.add(Error.of(100, "error"));

    doReturn(validationResult).when(createSubscriptionValidator).validate(any());

    assertThrows(ValidationException.class,() -> service.upsert(any()));
  }

  @Test
  void cancelSuccess() {
    Subscription subscription = getActiveSubscriptionForUserId(10, "test-cancel");
    doReturn(Optional.of(subscription)).when(subscriptionDao).findById(subscription.getId());

    service.cancel(subscription.getId());

    verify(subscriptionDao).update(subscription);
    assertThat(subscription.getStatus()).isEqualByComparingTo(Status.CANCELED);
  }

  @Test
  void cancelFailedIncorrectSubscriptionStatus() {
    Subscription subscription = getExpiredSubscriptionForUserId(10, "test-expired");
    doReturn(Optional.of(subscription)).when(subscriptionDao).findById(subscription.getId());

    assertThrows(SubscriptionException.class, () -> service.cancel(subscription.getId()));

    verify(subscriptionDao).findById(subscription.getId());
    verify(subscriptionDao, never()).update(subscription);
  }

  @Test
  void expireSuccess() {
    Subscription subscription = getActiveSubscriptionForUserId(10, "test-expire");
    doReturn(Optional.of(subscription)).when(subscriptionDao).findById(subscription.getId());

    service.expire(subscription.getId());

    verify(subscriptionDao).update(subscription);
    assertThat(subscription.getStatus()).isEqualByComparingTo(Status.EXPIRED);
    assertThat(subscription.getExpirationDate()).isEqualTo(clock.instant());
  }

  @Test
  void expireFailedIncorrectSubscriptionStatus() {
    Subscription subscription = getExpiredSubscriptionForUserId(10, "test-expired");
    doReturn(Optional.of(subscription)).when(subscriptionDao).findById(subscription.getId());

    assertThrows(SubscriptionException.class, () -> service.expire(subscription.getId()));

    verify(subscriptionDao).findById(subscription.getId());
    verify(subscriptionDao, never()).update(subscription);
  }


  private List<Subscription> getAllUserSubscriptionsForUserId(Integer userid) {
    List<Subscription> subscriptions = new ArrayList<>();
    subscriptions.add(getActiveSubscriptionForUserId(userid, "active"));
    subscriptions.add(getCanselSubscriptionForUserId(userid, "cansel"));
    subscriptions.add(getExpiredSubscriptionForUserId(userid, "expire"));
    return subscriptions;
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
        .expirationDate(Instant.now().plus(14, ChronoUnit.DAYS))
        .status(Status.CANCELED)
        .build();
  }

  private Subscription getExpiredSubscriptionForUserId(Integer userId, String name) {
    return Subscription.builder()
        .id(3)
        .userId(userId)
        .name(name)
        .provider(Provider.GOOGLE)
        .expirationDate(Instant.now().minus(7, ChronoUnit.DAYS))
        .status(Status.EXPIRED)
        .build();
  }

}