package chess.ai.evaluator;

public class DoubleUtil {
	
	private static final double epsForMaxExp = Double.longBitsToDouble(0x7CA0000000000000l);

	public static double getNthMaxDouble(int n) {
		return (Double.MAX_VALUE - epsForMaxExp * n);
	}
	
	public static boolean isMaxDouble(double n) {
		return n > (Double.MAX_VALUE - epsForMaxExp * 8192);
	}
	
	public static double decrementMaxDouble(double n) {
		return n - epsForMaxExp;
	}
	
	public static double incrementMaxDouble(double n) {
		return n + epsForMaxExp;
	}

}
