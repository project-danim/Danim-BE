package com.project.danim_be.member.repository;


import com.project.danim_be.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
	Optional<Member> findByUserId(String userId);

	Optional<Member> findByNickname(String nickname);

    boolean existsByUserId(String userId);
}
