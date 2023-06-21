package com.project.danim_be.member.dto.RequestDto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CheckIdRequestDto {

    @Pattern(regexp = "^[a-z0-9_+.-]+@[a-z0-9-]+\\.[a-z0-9]{1,20}$", message = "아이디는 올바른 이메일 형식으로 입력해주세요. (ex-danim999@naver.com)")
    private String userId;

}
