package com.tada.tada.global.response;

import lombok.Getter;


/*
	모든 API 응답을 이 형태로 통일해서 반환한다.
	프론트는 항상 { success, data, message } 구조만 보고 처리하면 되므로
	어느 도메인의 API를 호출하든 파싱 방식이 동일해진다.

	@param <T> 실제로 담기는 데이터 타입 (예: DiaryResponse, TokenResponse 등)
 */
@Getter
public class ApiResponse<T> {

	private final boolean success; // 요청 성공 여부
	private final T data; 		   // 성공 시 실제 데이터, 실패 시 null
	private final String message;  // 실패 시 에러 메시지, 성공 시 null


	// 외부에서 new ApiResponse로 직접 생성 못하게 막고,
	// 아래 success()/error() 정적 메서드로만 만들게 강제한다.
	public ApiResponse(boolean success, T data, String message) {
		this.success = success;
		this.data = data;
		this.message = message;
	}

	/*
		성공 응답을 만들 때 사용
		예: return ApiResponse.success(diaryResponse);
	 */
	public static <T> ApiResponse<T> success(T data){
		return new ApiResponse<>(true, data, null);
	}

	/*
		실패 응답을 만들 때 사용
		예: return ApiResponse.error("일기를 찾을 수 없습니다.);
	 */
	public static <T> ApiResponse<T> error(String message){
		return new ApiResponse<>(false, null, message);
	}
}