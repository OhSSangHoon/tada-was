package com.tada.tada.diary.service;

import com.tada.tada.diary.dto.CanCreateResponse;
import com.tada.tada.diary.dto.DiaryCreateForm;
import com.tada.tada.diary.dto.DiaryResponse;
import com.tada.tada.diary.dto.DiaryUpdateForm;
import com.tada.tada.diary.dto.GenerateStickerResponse;
import com.tada.tada.diary.dto.TrashedDiaryResponse;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.entity.DiaryStatus;
import com.tada.tada.diary.entity.Sticker;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.diary.repository.StickerRepository;
import com.tada.tada.curator.service.CuratorCleanupService;
import com.tada.tada.global.client.DiaryAnalysisClient;
import com.tada.tada.global.client.StickerWebhookClient;
import com.tada.tada.global.client.SupabaseStorageClient;
import com.tada.tada.global.event.DiaryCreatedEvent;
import com.tada.tada.global.event.DiaryRestoredEvent;
import com.tada.tada.global.event.DiaryTrashedEvent;
import com.tada.tada.global.event.MentionExtractedEvent;
import com.tada.tada.global.event.dto.ExtractionResult;
import com.tada.tada.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

	private final DiaryRepository diaryRepository;
	private final StickerRepository stickerRepository;
	private final CuratorCleanupService curatorCleanupService;
	private final StickerWebhookClient stickerWebhookClient;
	private final SupabaseStorageClient supabaseStorageClient;
	private final DiaryAnalysisClient diaryAnalysisClient;
	private final DiaryContentUpdater diaryContentUpdater;
	private final ApplicationEventPublisher eventPublisher;
	private static final int DAILY_CREATE_LIMIT = 5;
	private static final String STICKER_OBJECT_EXTENSION = ".jpg";
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	
	@Transactional
	public DiaryResponse createDiary(UUID userId, DiaryCreateForm form) {
		Diary diary = Diary.builder()
				.userId(userId)
				.entryDate(form.getEntryDate())
				.title(form.getTitle())
				.weather(form.getWeather())
				.content(form.getContent())
				.build();
		
		Diary savedDiary = diaryRepository.save(diary);
		
		Sticker sticker = Sticker.builder()
				.diaryId(savedDiary.getId())
				.imageUrl(form.getImageUrl())
				.keyword(form.getKeyword())
				.type(form.getType())
				.build();
				
		stickerRepository.save(sticker);
		
		eventPublisher.publishEvent(
				new MentionExtractedEvent(savedDiary.getId(), userId, form.getExtractionResult())
		);
		eventPublisher.publishEvent(
				new DiaryCreatedEvent(savedDiary.getId(), userId)
		);
		
		return DiaryResponse.from(savedDiary);
	}
	
	public DiaryResponse getDiary(UUID userId, UUID diaryId) {
		Diary diary = diaryRepository.findById(diaryId)
				.orElseThrow(() -> new CustomException("일기를 찾을 수 없습니다.", 404));
		
		if (!diary.getUserId().equals(userId)) {
			throw new CustomException("접근 권한이 없습니다.", 403);
		}
		
		if (!diary.isActive()) {
			throw new CustomException("일기를 찾을 수 없습니다.", 404);
		}
		
		return DiaryResponse.from(diary);
	}
	
	/*
	 * 본문이 바뀌면 n8n 재추출을 트랜잭션 밖에서 먼저 끝내고, 그 결과를 받은 뒤에만
	 * DiaryContentUpdater의 짧은 트랜잭션을 열어 Diary 반영 + 이벤트 발행을 같이 처리한다
	 * (한영, 2026-09-10 확정). 이 메서드 자체는 DB 쓰기가 없으니 클래스 레벨 readOnly
	 * 트랜잭션을 상속받으면 n8n 호출 동안 커넥션을 점유하게 돼서 NOT_SUPPORTED로 뺀다.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public DiaryResponse updateDiary(UUID userId, UUID diaryId, DiaryUpdateForm form) {
		Diary diary = diaryRepository.findById(diaryId)
				.orElseThrow(() -> new CustomException("일기를 찾을 수 없습니다.", 404));

		if (!diary.getUserId().equals(userId)) {
			throw new CustomException("접근 권한이 없습니다.", 403);
		}

		if (!diary.isActive()) {
			throw new CustomException("일기를 찾을 수 없습니다.", 404);
		}

		boolean contentChanged = !diary.getContent().equals(form.getContent());

		ExtractionResult extractionResult = null;
		if (contentChanged) {
			extractionResult = diaryAnalysisClient.analyze(form.getContent(), form.getWeather())
					.toExtractionResult();
		}

		return diaryContentUpdater.apply(userId, diaryId, form, extractionResult);
	}
	
	@Transactional
	public void trashDiary(UUID userId, UUID diaryId) {
		Diary diary = diaryRepository.findById(diaryId)
				.orElseThrow(() -> new CustomException("일기를 찾을 수 없습니다.", 404));
		
		if (!diary.getUserId().equals(userId)) {
			throw new CustomException("접근 권한이 없습니다.", 403);
		}
		
		if (!diary.isActive()) {
			throw new CustomException("일기를 찾을 수 없습니다.", 404);
		}
		
		diary.trash();
		
		eventPublisher.publishEvent(new DiaryTrashedEvent(diaryId, userId));
	}
	
	public List<TrashedDiaryResponse> getAllTrashedDiaries(UUID userId) {
		List<Diary> diaries = diaryRepository.findByUserIdAndStatus(userId, DiaryStatus.TRASHED);
		if (diaries.isEmpty()) {
			return List.of();
		}
		
		List<UUID> diaryIds = diaries.stream().map(Diary::getId).toList();
		Map<UUID, Sticker> stickersByDiaryId = stickerRepository.findByDiaryIdIn(diaryIds).stream()
				.collect(Collectors.toMap(Sticker::getDiaryId, Function.identity()));
		
		return diaries.stream()
				.map(diary -> TrashedDiaryResponse.of(diary, stickersByDiaryId.get(diary.getId())))
				.toList();
	}
	
	@Transactional
	public DiaryResponse restoreDiary(UUID userId, UUID diaryId, boolean replace) {
		Diary target = diaryRepository.findByIdForUpdate(diaryId)
				.orElseThrow(() -> new CustomException("일기를 찾을 수 없습니다.", 404));
		
		if (!target.getUserId().equals(userId)) {
			throw new CustomException("접근 권한이 없습니다.", 403);
		}
		
		if (target.isActive()) {
			throw new CustomException("일기를 찾을 수 없습니다.", 404);
		}
		
		Optional<Diary> existingActive = diaryRepository.findByUserIdAndEntryDateAndStatusForUpdate(
				userId, target.getEntryDate(), DiaryStatus.ACTIVE);
		
		if (existingActive.isPresent()) {
			if (!replace) {
				throw new CustomException("같은 날짜에 이미 일기가 있습니다.", 409);
			}
			Diary existing = existingActive.get();
			existing.trash();
			// 같은 날짜 ACTIVE는 1개만 허용하는 DB 유니크 제약 때문에, 기존 일기의 TRASHED 반영을 먼저 DB에 내보낸 뒤 복원해야 한다
			diaryRepository.flush();
			eventPublisher.publishEvent(new DiaryTrashedEvent(existing.getId(), userId));
		}
		
		target.restore();
		eventPublisher.publishEvent(new DiaryRestoredEvent(target.getId(), userId));
		
		return DiaryResponse.from(target);
	}
	
	public CanCreateResponse canCreate(UUID userId, LocalDate date) {
		
		Optional<Diary> existing = diaryRepository.findByUserIdAndEntryDateAndStatus(userId, date, DiaryStatus.ACTIVE);
		if (existing.isPresent()){
			return CanCreateResponse.builder()
					.canCreate(false)
					.reason("해당 날짜에 이미 일기가 있습니다.")
					.build();
		}
		
		LocalDateTime todayStart = LocalDate.now(KST).atStartOfDay();
		long todayCount = diaryRepository.countByUserIdAndCreatedAtAfter(userId, todayStart);
		if (todayCount >= DAILY_CREATE_LIMIT) {
			return CanCreateResponse.builder()
					.canCreate(false)
					.reason("하루 작성 횟수 5회 초과")
					.build();
		}
		
		return CanCreateResponse.builder()
				.canCreate(true)
				.reason(null)
				.build();
	}
	
	/*
	 * 수동 영구삭제 API와 30일 @Scheduled 배치가 이 메서드 하나를 재사용한다 (CLAUDE.md REQ-F-205).
	 * 삭제 순서 고정: diary_person → mention_candidate(CuratorCleanupService) → stickers → diaries.
	 * MemoryPerson은 다른 일기에서도 참조돼서 여기서 지우면 안 됨 (CuratorCleanupService가 알아서 그 둘만 지움).
	 */
	@Transactional
	public void permanentlyDeleteDiary(UUID userId, UUID diaryId) {
		Diary diary = diaryRepository.findByIdForUpdate(diaryId)
				.orElseThrow(() -> new CustomException("일기를 찾을 수 없습니다.", 404));

		if (!diary.getUserId().equals(userId)) {
			throw new CustomException("접근 권한이 없습니다.", 403);
		}

		if (diary.isActive()) {
			throw new CustomException("휴지통에 있는 일기만 영구 삭제할 수 있습니다.", 400);
		}

		curatorCleanupService.deleteByDiaryId(diaryId);
		stickerRepository.deleteByDiaryId(diaryId);
		diaryRepository.delete(diary);
	}

	/*
	 * generate-sticker / regenerate-sticker 공용 트리거 (AI_ENDPOINT_CONFIRMATION_REPLY.md, 상훈 확인).
	 * 재생성은 별도 엔드포인트 없이 같은 keyword로 이 메서드를 한 번 더 호출하는 것으로 처리한다.
	 * DB 저장 없음 - 사용자가 최종 확인해야 POST /api/diaries로 저장된다.
	 *
	 * DB 작업이 전혀 없는데 클래스 레벨 @Transactional(readOnly = true)를 상속받으면
	 * n8n 웹훅 호출 + Supabase 업로드(외부 네트워크 I/O) 동안 커넥션 풀에서 커넥션을
	 * 계속 점유하게 된다(상훈 PR #32 리뷰) - NOT_SUPPORTED로 트랜잭션 경계를 끊는다.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public GenerateStickerResponse generateSticker(UUID userId, String keyword) {
		byte[] imageBytes = stickerWebhookClient.requestStickerImage(keyword);

		String objectName = userId + "/" + UUID.randomUUID() + STICKER_OBJECT_EXTENSION;
		try {
			String imageUrl = supabaseStorageClient.uploadFromBytes(imageBytes, objectName);
			return new GenerateStickerResponse(imageUrl);
		} catch (RestClientException e) {
			log.error("스티커 이미지 업로드 실패 (userId={})", userId, e);
			throw new CustomException("스티커 업로드에 실패했습니다. 다시 시도해주세요.", 502);
		}
	}
}
