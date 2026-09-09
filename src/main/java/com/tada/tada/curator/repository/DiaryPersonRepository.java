package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.DiaryPerson;
import com.tada.tada.curator.entity.DiaryPersonId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DiaryPersonRepository
		extends JpaRepository<DiaryPerson, DiaryPersonId> {

	List<DiaryPerson> findAllByDiaryId(
			UUID diaryId
	);

	/*
	 * Diary 영구삭제 때 Diary 도메인이 CuratorCleanupService 를 통해 호출한다.
	 *
	 * MemoryPerson 은 여러 Diary 가 공유하므로 여기서 지우지 않는다.
	 * 이 연결 행만 제거한다.
	 */
	void deleteByDiaryId(
			UUID diaryId
	);

	/*
	 * 사람 상세의 `처음 기록일`.
	 * 저장하지 않고 조회할 때 계산한다. 기준은 created_at 이 아니라 entry_date 다.
	 * ACTIVE 일기가 없으면 null.
	 */
	@Query("""
			SELECT MIN(diary.entryDate)
			FROM DiaryPerson diaryPerson, Diary diary
			WHERE diaryPerson.diaryId = diary.id
			  AND diaryPerson.personId = :personId
			  AND diary.userId = :userId
			  AND diary.status = com.tada.tada.diary.entity.DiaryStatus.ACTIVE
			""")
	LocalDate findFirstEntryDate(
			@Param("userId") UUID userId,
			@Param("personId") UUID personId
	);

	/*
	 * 대표 Sticker — 그 사람의 가장 최근 ACTIVE 일기의 Sticker 하나.
	 * 그 일기에 Sticker 가 없으면 행이 아예 없고 호출부가 null 로 채운다.
	 *
	 * `스티커가 나올 때까지 옛날 일기로 내려가며 찾기` 는 쓰지 않는다.
	 * 그러면 카드의 최근 기록일과 그림이 서로 다른 일기를 가리킨다.
	 *
	 * diaries 의 (user_id, entry_date) ACTIVE partial unique 덕분에
	 * 최대 entry_date 를 만족하는 일기는 사람당 한 건이다.
	 */
	@Query("""
			SELECT
				diaryPerson.personId AS personId,
				sticker.imageUrl AS stickerUrl
			FROM DiaryPerson diaryPerson, Diary diary, Sticker sticker
			WHERE diaryPerson.diaryId = diary.id
			  AND sticker.diaryId = diary.id
			  AND diaryPerson.personId IN :personIds
			  AND diary.userId = :userId
			  AND diary.status = com.tada.tada.diary.entity.DiaryStatus.ACTIVE
			  AND diary.entryDate = (
					SELECT MAX(latestDiary.entryDate)
					FROM DiaryPerson latestLink, Diary latestDiary
					WHERE latestLink.diaryId = latestDiary.id
					  AND latestLink.personId = diaryPerson.personId
					  AND latestDiary.userId = :userId
					  AND latestDiary.status = com.tada.tada.diary.entity.DiaryStatus.ACTIVE
			  )
			""")
	List<PersonStickerRow> findRepresentativeStickers(
			@Param("userId") UUID userId,
			@Param("personIds") Collection<UUID> personIds
	);

	interface PersonStickerRow {

		UUID getPersonId();

		String getStickerUrl();
	}
}
