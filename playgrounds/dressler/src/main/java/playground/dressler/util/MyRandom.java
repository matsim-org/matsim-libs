package playground.dressler.util;

public class MyRandom {
	final static long a = 0xffffda61L;
	static long x = 123456678 & 0xffffffffL;

	public static int nextInt() {
	  x = (a * (x & 0xffffffffL)) + (x >>> 32);
	  return (int) x;
	}
	
	public static int nextInt(int n) {
		int k = nextInt() % n;
		if (k < 0) return k + n;
		return k;
	}
	
	public static void main(String[] args) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		
		for (int i = 0; i < 10000; i++) {
			int n = nextInt();
			min = Math.min(n, min);
			max = Math.max(n, max);
		}
		System.out.println(min / (double) Integer.MIN_VALUE);
		System.out.println(max / (double) Integer.MAX_VALUE);
	}
}