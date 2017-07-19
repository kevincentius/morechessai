package chess.ai;

import java.util.ArrayList;

public class RepetitionTable {
	
	private class Position {
		public long hash1;
		public long hash2;
		public Position(long hash1, long hash2) {
			super();
			this.hash1 = hash1;
			this.hash2 = hash2;
		}
	}
	
	private static final int shortHashLength = 16; // 2^20 --> 1M

	private int biggestBucketSize;
	
	private static final long shortHashMask = Long.MAX_VALUE >> (63 - shortHashLength);
	
	@SuppressWarnings("unchecked")
	ArrayList<Position>[] analyzedPositions = new ArrayList[(int) Math.pow(2, shortHashLength)];
	
	public RepetitionTable() {
		for (int i = 0; i < analyzedPositions.length; i++) {
			analyzedPositions[i] = new ArrayList<>();
		}
	}
	
	public boolean exists(long hash1, long hash2) {
		int tablePos = (int) (hash2 & shortHashMask);
		ArrayList<Position> bucket = analyzedPositions[tablePos];
		for (Position pos : bucket) {
			if (pos.hash1 == hash1 && pos.hash2 == hash2) {
				return true;
			}
		}
		return false;
	}
	
	public void savePosition(long hash1, long hash2) {
		int tablePos = (int) (hash2 & shortHashMask);
		ArrayList<Position> bucket = analyzedPositions[tablePos];
		bucket.add(new Position(hash1, hash2));
		if (bucket.size() > biggestBucketSize) {
			biggestBucketSize = bucket.size();
		}
	}

	public void removePosition(long hash1, long hash2) {
		int tablePos = (int) (hash2 & shortHashMask);
		ArrayList<Position> bucket = analyzedPositions[tablePos];
		for (int i = 0; i < bucket.size(); i++) {
			if (bucket.get(i).hash1 == hash1 && bucket.get(i).hash2 == hash2) {
				bucket.set(i, bucket.get(bucket.size() - 1));
				bucket.remove(bucket.size() - 1);
				return;
			}
		}
		throw new RuntimeException("Position not found when removing position from repetition table!");
	}

	public int getBiggestBucketSize() {
		return biggestBucketSize;
	}

}
