package com.project.danim_be.member.dto.RequestDto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CheckNicknameRequestDto {

    @Pattern(regexp ="^(?=.*[가-힣a-zA-Z])[가-힣a-zA-Z0-9]{3,8}$", message = "닉네임은 3~8자 이내 한글or영어(대소문자),숫자(선택) 범위에서 입력해주세요. 특수문자는 포함할 수 없습니다.")
    private String nickname;

}
