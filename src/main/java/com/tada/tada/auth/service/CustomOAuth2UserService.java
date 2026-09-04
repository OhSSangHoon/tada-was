package com.tada.tada.auth.service;

import com.tada.tada.auth.entity.Provider;
import com.tada.tada.auth.entity.Users;
import com.tada.tada.auth.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
	
	private final UsersRepository usersRepository;
	
	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) {
		
		// 소셜 로그인 제공자에서 사용자 정보를 받아온다.
		OAuth2User oAuth2User = super.loadUser(userRequest);
		
		// 현재 로그인한 소셜 제공자를 확인한다.
		String provider = userRequest
				.getClientRegistration()
				.getRegistrationId();
		
		String loginId;
		String nickname;
		
		// Google 사용자 정보 처리
		if ("google".equals(provider)) {
			loginId = oAuth2User.getName();
			nickname = oAuth2User.getAttribute("name");
			
			// Kakao 사용자 정보 처리
		} else if ("kakao".equals(provider)) {
			
			// Kakao 계정 정보를 가져온다.
			Map<String, Object> kakaoAccount =
					oAuth2User.getAttribute("kakao_account");
			
			// Kakao의 고유 ID를 loginId로 사용한다.
			loginId = oAuth2User.getName();
			
			// Kakao에서 받은 이름을 nickname으로 사용한다.
			nickname = (String) kakaoAccount.get("name");
			
			// Naver 사용자 정보 처리
		} else if ("naver".equals(provider)) {
			
			// Naver 사용자 정보는 response 안에 들어있다.
			Map<String, Object> naverResponse =
					oAuth2User.getAttribute("response");
			
			// Naver의 고유 ID를 loginId로 사용한다.
			loginId = (String) naverResponse.get("id");
			
			// Naver에서 받은 이름을 nickname으로 사용한다.
			nickname = (String) naverResponse.get("name");
			
		} else {
			throw new IllegalArgumentException(
					"지원하지 않는 소셜 로그인입니다."
			);
		}
		
		// String으로 받은 provider를 Provider enum으로 변환한다.
		Provider providerEnum =
				Provider.valueOf(provider.toUpperCase());
		
		// 기존 소셜 회원인지 확인
		Users users = usersRepository
				.findByLoginIdAndProvider(
						loginId,
						providerEnum
				)
				.orElseGet(() -> {
					
					// 기존 회원이 없으면 새로 생성한다.
					Users newUsers = Users.createSocialUser(
							loginId,
							nickname,
							providerEnum
					);
					
					return usersRepository.save(newUsers);
				});
		
		// Spring Security에서 사용할 사용자 정보를 반환한다.
		return new CustomOAuth2User(
				users.getId(),
				oAuth2User.getAttributes()
		);
	}
}