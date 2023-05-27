package com.project.danim_be.member.dto;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Nested
class CheckIdRequestDtoTest {

    @Test
    void checkIdTest() {
        // given
        String userId = "seulki@naver.com";

        // when
        CheckIdRequestDto checkIdRequestDto = new CheckIdRequestDto(userId);

        // then
        assertEquals(userId, checkIdRequestDto.getUserId());
    }

}