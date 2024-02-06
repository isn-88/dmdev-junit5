package com.dmdev.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dmdev.entity.Provider;
import com.dmdev.entity.Status;
import com.dmdev.entity.Subscription;
import com.dmdev.integration.IntegrationTestBase;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.Test;

class SubscriptionDaoIT extends IntegrationTestBase {

  private final SubscriptionDao subscriptionDao = SubscriptionDao.getInstance();

  @Test
  void upsertInsert() {
    Subscription subscription = getSubscription(1, "name");

    Subscription actualResult = subscriptionDao.upsert(subscription);

    assertThat(actualResult.getId()).isNotNull();
  }

  @Test
  void upsertUpdate() {
    Subscription subscription = subscriptionDao.insert(getSubscription(1, "name"));
    subscription.setName("updated_name");

    Subscription actualResult = subscriptionDao.upsert(subscription);

    assertThat(actualResult.getId()).isEqualTo(subscription.getId());
  }

  @Test
  void findAll() {
    Subscription subscription1 = subscriptionDao.insert(getSubscription(1, "name1"));
    Subscription subscription2 = subscriptionDao.insert(getSubscription(2, "name2"));
    Subscription subscription3 = subscriptionDao.insert(getSubscription(3, "name1"));

    List<Subscription> actualResult = subscriptionDao.findAll();

    assertThat(actualResult).hasSize(3);
    List<Integer> subscriptionIds = actualResult.stream()
        .map(Subscription::getId)
        .toList();
    assertThat(subscriptionIds).contains(subscription1.getId(), subscription2.getId(), subscription3.getId());
  }

  @Test
  void findByIdExists() {
    Subscription subscription = subscriptionDao.insert(getSubscription(1, "name"));

    Optional<Subscription> actualResult = subscriptionDao.findById(subscription.getId());

    assertThat(actualResult).isPresent();
    assertThat(actualResult.get().getId()).isEqualTo(subscription.getId());
    assertThat(actualResult.get().getUserId()).isEqualTo(subscription.getUserId());
    assertThat(actualResult.get().getName()).isEqualTo(subscription.getName());
  }

  @Test
  void findByIdNotExists() {
    Subscription subscription = subscriptionDao.insert(getSubscription(1, "name"));

    Optional<Subscription> actualResult = subscriptionDao.findById(Integer.MAX_VALUE);

    assertThat(actualResult).isEmpty();
  }

  @Test
  void deleteExistingSubscription() {
    Subscription subscription = subscriptionDao.insert(getSubscription(1, "name"));

    boolean actualResult = subscriptionDao.delete(subscription.getId());

    assertThat(actualResult).isTrue();
  }

  @Test
  void deleteNotExistingSubscription() {
    boolean actualResult = subscriptionDao.delete(Integer.MAX_VALUE);

    assertThat(actualResult).isFalse();
  }

  @Test
  void update() {
    Subscription subscription = subscriptionDao.insert(getSubscription(1, "test"));
    subscription.setStatus(Status.CANCELED);

    subscriptionDao.update(subscription);

    Optional<Subscription> actualResult = subscriptionDao.findById(subscription.getId());
    assertThat(actualResult).isPresent();
    assertThat(actualResult.get().getId()).isEqualTo(subscription.getId());
    assertThat(actualResult.get().getStatus()).isEqualByComparingTo(Status.CANCELED);
  }

  @Test
  void insert() {
    Subscription actualResult = subscriptionDao.insert(getSubscription(1, "test"));

    assertThat(actualResult.getId()).isNotNull();
  }

  @Test
  void insertDuplicate() {
    Subscription subscription1 = getSubscription(1, "test");
    Subscription subscription2 = getSubscription(1, "test");

    Subscription actualResult = subscriptionDao.insert(subscription1);
    assertThat(actualResult.getId()).isNotNull();
    assertThrows(JdbcSQLIntegrityConstraintViolationException.class,
                 () -> subscriptionDao.insert(subscription2));
  }

  @Test
  void findByUserId() {
    Subscription subscription1 = subscriptionDao.insert(getSubscription(1, "name1"));
    Subscription subscription2 = subscriptionDao.insert(getSubscription(1, "name2"));
    Subscription subscription3 = subscriptionDao.insert(getSubscription(2, "name3"));

    List<Subscription> actualResult = subscriptionDao.findByUserId(subscription1.getUserId());

    assertThat(actualResult).hasSize(2);
    List<String> subscriptionNames = actualResult.stream()
        .map(Subscription::getName)
        .toList();
    assertThat(subscriptionNames).contains(subscription1.getName(), subscription2.getName());
  }

  private Subscription getSubscription(Integer userId, String name) {
    return Subscription.builder()
        .userId(userId)
        .name(name)
        .provider(Provider.GOOGLE)
        .expirationDate(Instant.now().plusSeconds(60))
        .status(Status.ACTIVE)
        .build();
  }
}