package com.tada.tada.curator.policy;

public final class PersonMatchingPolicy {

	public static final int CERTAIN_SCORE = 100;

	public static final int STRONG_SCORE = 30;

	public static final int WEAK_SCORE = 10;

	public static final int SIMILAR_MIN_SCORE = 60;

	public static final int SIMILAR_MIN_SCORE_GAP = 30;

	public static final int LONG_NAME_MIN_LENGTH = 3;

	public static final int MAX_EDIT_DISTANCE = 1;

	public static final int NORMALIZED_HISTORY_MIN_COUNT = 2;

	public static final double NORMALIZED_HISTORY_MIN_DOMINANCE_RATIO = 0.80;

	public static final int NORMALIZED_HISTORY_MIN_COUNT_GAP = 2;

	private PersonMatchingPolicy() {
	}
}