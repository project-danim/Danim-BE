package com.project.danim_be.chat.dto;


import com.project.danim_be.chat.entity.MemberChatRoom;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomDto {
    private String member;
    private String roomName;


    public ChatRoomDto(MemberChatRoom memberChatRoom) {
        this.roomName = memberChatRoom.getChatRoom().getRoomName();
        this.member = memberChatRoom.getMember().getNickname();
    }
}
