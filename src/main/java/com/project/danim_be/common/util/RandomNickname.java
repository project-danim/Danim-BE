package com.project.danim_be.common.util;

import java.util.Random;

import lombok.RequiredArgsConstructor;



public class RandomNickname {


	static String[] adjectives = {"세련된", "사랑스러운", "귀여운", "순수한", "깜직한", "앙칼진",
			"단아한", "도도한", "청아한", "우아한", "눈부신", "럭셔리한", "멋진"};
	static String[] nouns = {"호랑이", "사자", "고양이", "강아지","멍뭉이" ,"야옹이", "새벽4시" };

	public static String getRandomNickname() {
		Random rand = new Random();
		return adjectives[rand.nextInt(adjectives.length)] +" " + nouns[rand.nextInt(nouns.length)];
	}


}
