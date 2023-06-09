package com.project.danim_be.mypage.dto.RequestDto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class MypageRequestDto {

    private String nickname;

    private String content;

    private MultipartFile image;

}
