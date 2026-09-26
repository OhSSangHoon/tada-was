package com.tada.tada.global.client;

import com.tada.tada.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/*
 * [담당: 민혁] — 본문으로 n8n diary-analysis 웹훅을 동기 호출해 인물/장소/활동 재추출 결과를 받아온다.
 *
 * 일기 수정(PUT) 시 본문이 바뀌면, 트랜잭션 밖에서 이 호출을 먼저 끝내고 결과를 받은 뒤에만
 * 짧은 트랜잭션을 열어 Diary 반영 + MentionExtractedEvent 발행을 같이 처리한다
 * (한영, 2026-09-10 확정 - DiaryContentUpdater 참고).
 */
@Slf4j
@Component
public class DiaryAnalysisClient {

	private final RestClient restClient;
	private final String webhookUrl;

	public DiaryAnalysisClient(
			RestClient.Builder restClientBuilder,
			@Value("${n8n.diary-analysis-webhook-url}") String webhookUrl
	) {
		this.restClient = restClientBuilder.build();
		this.webhookUrl = webhookUrl;
	}

	public DiaryAnalysisResponse analyze(String content, String weather) {
		try {
			DiaryAnalysisResponse response = restClient.post()
					.uri(webhookUrl)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new DiaryAnalysisRequest(content, weather))
					.retrieve()
					.body(DiaryAnalysisResponse.class);

			if (response == null) {
				throw new CustomException("AI가 응답을 제대로 주지 않았어요. 다시 시도해주세요.", 502);
			}
			return response;
		} catch (RestClientException e) {
			log.error("일기 재추출 웹훅 호출 실패", e);
			throw new CustomException("AI가 응답을 제대로 주지 않았어요. 다시 시도해주세요.", 502);
		}
	}

	private record DiaryAnalysisRequest(String content, String weather) {
	}
}
