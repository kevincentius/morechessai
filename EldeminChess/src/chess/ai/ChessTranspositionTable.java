package chess.ai;

public class ChessTranspositionTable {
	
	public static enum EntryType {
		UPPERBOUND, LOWERBOUND, EXACT
	}
	
	public static class TpEntry {
		public long hash1;
		public long hash2;
		public double depth;
		public double score;
		public EntryType type;
		public TpEntry(long hash1, long hash2, double depth, double score, EntryType type) {
			super();
			this.hash1 = hash1;
			this.hash2 = hash2;
			this.depth = depth;
			this.score = score;
			this.type = type;
		}
	}
	
	private static final int shortHashLength = 23; // 2^20 --> 1M
	// 24 byte per element

	private int checkCount;
	private int hitCount;
	private int delCount;
	
	private static final long shortHashMask = Long.MAX_VALUE >> (63 - shortHashLength);
	
	TpEntry[] entries = new TpEntry[(int) Math.pow(2, shortHashLength)];
	
	public TpEntry checkPosition(long hash1, long hash2) {
		checkCount++;
		TpEntry entry = entries[(int) (hash2 & shortHashMask)];
		if (entry != null && entry.hash1 == hash1 && entry.hash2 == hash2) {
			hitCount++;
			return entry;
		} else {
			return null;
		}
	}
	
	public void savePosition(TpEntry entry) {
		if (entries[(int) (entry.hash2 & shortHashMask)] != null) {
			delCount++;
		}
		entries[(int) (entry.hash2 & shortHashMask)] = entry;
	}
	
	public void updatePosition(TpEntry entry) {
		entries[(int) (entry.hash2 & shortHashMask)] = entry;
	}
	
	public int getCheckCount() {
		return checkCount;
	}

	public int getHitCount() {
		return hitCount;
	}

	public int getDelCount() {
		return delCount;
	}
	
	public double getAvgBucketSize() {
		double total = 0;
		for (TpEntry entry : entries) {
			if (entry != null) {
				total++;
			}
		}
		return total / entries.length;
	}
	
}
