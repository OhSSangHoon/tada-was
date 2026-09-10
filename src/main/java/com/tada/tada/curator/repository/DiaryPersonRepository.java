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
	 * MemoryPerson은 여러 Diary가 공유하므로 지우지 않고 연결 행만 제거한다.
	 */
	void deleteByDiaryId(
			UUID diaryId
	);

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
	 * 대표 Sticker = 최근 ACTIVE 일기의 Sticker. 그 일기에 없으면 null이며,
	 * 옛 일기로 내려가며 찾지 않는다 (카드 날짜와 그림의 일기가 어긋나는 것을 방지).
	 * 실제 Supabase의 (user_id, entry_date) WHERE status = 'ACTIVE'
	 * partial unique index를 전제로 최대 entry_date 일기는 사용자당 한 건이다.
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
