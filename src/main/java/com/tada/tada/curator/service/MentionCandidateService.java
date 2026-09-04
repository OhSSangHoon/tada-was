package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MentionCandidateService {
	
	private final MentionCandidateRepository mentionCandidateRepository;
	
	public MentionCandidate save(MentionCandidate candidate) {
		return mentionCandidateRepository.save(candidate);
	}
	
	public List<MentionCandidate> findAllByDiaryId(UUID diaryId) {
		return mentionCandidateRepository.findAllByDiaryId(diaryId);
	}
}