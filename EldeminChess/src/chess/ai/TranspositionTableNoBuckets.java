package chess.ai;

import java.util.ArrayDeque;

public class TranspositionTableNoBuckets {
	
	private class AnalysedPosition {
		public long hash1;
		public long hash2;
		public double score;
		public AnalysedPosition(long hash1, long hash2, double score) {
			super();
			this.hash1 = hash1;
			this.hash2 = hash2;
			this.score = score;
		}
	}
	
	private static final int shortHashLength = 23; // 2^20 --> 1M
	// 24 byte per element

	private int checkCount;
	private int hitCount;
	private int posCount;
	private int delCount;
	private int delOnUpdateCount;
	private int maxBucketSize;
	
	private static final long shortHashMask = Long.MAX_VALUE >> (63 - shortHashLength);
	
	@SuppressWarnings("unchecked")
	AnalysedPosition[] analysedPositions = new AnalysedPosition[(int) Math.pow(2, shortHashLength)];
	
	public Double checkPosition(long hash1, long hash2) {
		checkCount++;
		AnalysedPosition pos = analysedPositions[(int) (hash2 & shortHashMask)];
		return pos == null ? null : pos.score;
	}
	
	public void savePosition(long hash1, long hash2, double score) {
		posCount++;
		analysedPositions[(int) (hash2 & shortHashMask)] = new AnalysedPosition(hash1, hash2, score);
	}
	
	public void updatePosition(long hash1, long hash2, double score) {
		analysedPositions[(int) (hash2 & shortHashMask)] = new AnalysedPosition(hash1, hash2, score);
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
		return maxBucketSize;
	}
	
	public double getAvgBucketSize() {
		double total = 0;
		for (AnalysedPosition pos : analysedPositions) {
			if (pos != null) {
				total++;
			}
		}
		return total / analysedPositions.length;
	}
	
}
