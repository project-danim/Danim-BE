package com.project.danim_be.chat.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomConnectSuccessDto {

	private String roomName;

	List<String> nickName;



}
