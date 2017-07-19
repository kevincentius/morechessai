package chess.rule.piece;

public class SlideRule {
	
	public static class SlideRuleData extends SlideRule {
		public int[][] dirs;
	}
	
	public int minDist;
	public int maxDist;
	public int[] distList;
	
	public SlideRule() {
		super();
	}

	public SlideRule(int minDist, int maxDist, int[] distList) {
		super();
		this.minDist = minDist;
		this.maxDist = maxDist;
		this.distList = distList;
	}
	
}
