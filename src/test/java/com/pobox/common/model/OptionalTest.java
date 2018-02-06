package com.pobox.common.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class OptionalTest {

    @ParameterizedTest
    @MethodSource("createInputsWithExpectedResults")
    void testOptional(Optional<String> input, String expectedResult) {
        String result = input
                .map(String::toUpperCase)
                .filter("Z"::equals)
                .orElse("DEFAULT");

        assertThat(result, is(expectedResult));
    }

    static Stream<Arguments> createInputsWithExpectedResults() {
        return Stream.of(
                Arguments.of(Optional.of("X"), "DEFAULT"),
                Arguments.of(Optional.of("Y"), "DEFAULT"),
                Arguments.of(Optional.of("Z"), "Z"),
                Arguments.of(Optional.of("z"), "Z"),
                Arguments.of(Optional.<String>empty(), "DEFAULT"));
    }

}
