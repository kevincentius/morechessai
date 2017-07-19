package util;

import util.MurmurHash3.LongPair;

public class MurmurHashTest {
	
	public static void main(String[] args) {
		
		
		byte[] key = new byte[]{ 40, 40, 40, 25, 68, 46, 24, 63, 43, 63 };
		LongPair out = new LongPair();
		MurmurHash3.murmurhash3_x64_128(key, 0, key.length, 0, out);
		System.out.println(out.val1 + ", " + out.val2);
	}
	
}
