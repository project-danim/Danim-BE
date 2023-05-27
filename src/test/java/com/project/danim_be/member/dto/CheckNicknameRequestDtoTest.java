package com.project.danim_be.member.dto;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Nested
class CheckNicknameRequestDtoTest {

    @Test
    void checkNicknameTest() {
        // given
        String nickname = "눈부신호랑이";

        // when
        CheckNicknameRequestDto checkNicknameRequestDto = new CheckNicknameRequestDto(nickname);

        // then
        assertEquals(nickname, checkNicknameRequestDto.getNickname());
    }

}