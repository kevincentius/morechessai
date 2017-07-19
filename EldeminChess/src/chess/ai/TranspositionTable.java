package chess.ai;

import java.util.ArrayDeque;

public class TranspositionTable {
	
	public static class AnalysedPosition {
		public long hash1;
		public long hash2;
		public double score;
		public double depthLeft;
		public AnalysedPosition(long hash1, long hash2, double score, double depthLeft) {
			super();
			this.hash1 = hash1;
			this.hash2 = hash2;
			this.score = score;
			this.depthLeft = depthLeft;
		}
	}
	
	private static final int shortHashLength = 20; // 2^20 --> 1M 
	private static final int bucketSizeLimit = 4;
	// 24 byte per element

	private int checkCount;
	private int hitCount;
	private int posCount;
	private int delCount;
	private int delOnUpdateCount;
	private int biggestBucketSize;
	
	private static final long shortHashMask = Long.MAX_VALUE >> (63 - shortHashLength);
	
	@SuppressWarnings("unchecked")
	ArrayDeque<AnalysedPosition>[] analyzedPositions = new ArrayDeque[(int) Math.pow(2, shortHashLength)];
	
	public TranspositionTable() {
		for (int i = 0; i < analyzedPositions.length; i++) {
			analyzedPositions[i] = new ArrayDeque<>();
		}
	}
	
	public AnalysedPosition checkPosition(long hash1, long hash2) {
		checkCount++;
		int tablePos = (int) (hash2 & shortHashMask);
		ArrayDeque<AnalysedPosition> bucket = analyzedPositions[tablePos];
		for (AnalysedPosition pos : bucket) {
			if (pos.hash1 == hash1 && pos.hash2 == hash2) {
				hitCount++;
				return pos;
			}
		}
		return null;
	}
	
	public void savePosition(long hash1, long hash2, double score, double depthLeft) {
		posCount++;
		int tablePos = (int) (hash2 & shortHashMask);
		ArrayDeque<AnalysedPosition> bucket = analyzedPositions[tablePos];
		if (bucket.size() >= bucketSizeLimit) {
			bucket.removeLast();
			delCount++;
		}
		bucket.push(new AnalysedPosition(hash1, hash2, score, depthLeft));
		if (bucket.size() > biggestBucketSize) {
			biggestBucketSize = bucket.size();
		}
	}
	
	public void updatePosition(long hash1, long hash2, double score, double depthLeft) {
		int tablePos = (int) (hash2 & shortHashMask);
		ArrayDeque<AnalysedPosition> bucket = analyzedPositions[tablePos];
		
		for (AnalysedPosition pos : bucket) {
			if (pos.hash1 == hash1 && pos.hash2 == hash2) {
				pos.score = score;
				return;
			}
		}

		if (bucket.size() != bucketSizeLimit) {
			throw new RuntimeException("DEBUG ERROR: bucket is not full, but position not found during `updatePosition(...)`");
		}
		
		// position not found anymore - it has been thrown away. Now, reinsert:
		bucket.removeLast();
		delOnUpdateCount++;
		bucket.push(new AnalysedPosition(hash1, hash2, score, depthLeft));
	}
	
	public int getCheckCount() {
		return checkCount;
	}

	public int getHitCount() {
		return hitCount;
	}

	public int getPosCount() {
		return posCount;
	}

	public int getDelCount() {
		return delCount;
	}
	
	public int getDelOnUpdateCount() {
		return delOnUpdateCount;
	}

	public int getMaxBucketSize() {
		return biggestBucketSize;
	}
	
	public double getAvgBucketSize() {
		double total = 0;
		for (ArrayDeque<AnalysedPosition> deque : analyzedPositions) {
			if (!deque.isEmpty()) {
				total++;
			}
		}
		return total / analyzedPositions.length;
	}
	
}
