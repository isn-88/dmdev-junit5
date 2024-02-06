package com.dmdev.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PropertiesUtilTest {


  @ParameterizedTest
  @MethodSource("getPropertyArguments")
  void getProperty(String key, String value) {
    String actualValue = PropertiesUtil.get(key);

    assertEquals(value, actualValue);
  }

  static Stream<Arguments> getPropertyArguments() {
    return Stream.of(
        Arguments.of("db.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"),
        Arguments.of("db.user", "sa"),
        Arguments.of("db.password", ""),
        Arguments.of("db.driver", "org.h2.Driver")
    );
  }

}