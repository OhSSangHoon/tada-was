package com.tada.tada.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
		name = "users",
		/* uniqueConstraints:
		 * login_id와 provider를 묶어서 중복될 수 없도록 설정한다.
 		 * 예를 들어 같은 이메일이라도
 		 * google / kakao처럼 provider가 다르면 가입할 수 있다.
		 */
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_users_login_id_provider",
						columnNames = {"login_id", "provider"}
				)
		}
)
@Getter
@NoArgsConstructor
public class Users {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(name = "login_id", nullable = false)
	private String loginId;
	
	@Column(name = "password")
	private String password;
	
	@Column(name = "nickname", nullable = false)
	private String nickname;
	
	@Column(name = "provider", nullable = false)
	private String provider = "local";
	
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
	
	// 일반 회원가입
	public Users(String loginId, String password, String nickname) {
		this.loginId = loginId;
		this.password = password;
		this.nickname = nickname;
		this.provider = "local";
		this.createdAt = LocalDateTime.now();
	}
	
	// 소셜 회원가입
	public static Users createSocialUser(
			String loginId,
			String nickname,
			String provider
	) {
		Users users = new Users();
		users.loginId = loginId;
		users.password = null;
		users.nickname = nickname;
		users.provider = provider;
		users.createdAt = LocalDateTime.now();
		
		return users;
	}
	
}