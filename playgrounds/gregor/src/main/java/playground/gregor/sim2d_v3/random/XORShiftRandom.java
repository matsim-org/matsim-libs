package playground.gregor.sim2d_v3.random;

public class XORShiftRandom {

	private long x;

	public XORShiftRandom (long seed) {
		this.x = seed;
	}

	public long randomLong() {
		this.x ^= (this.x << 21);
		this.x ^= (this.x >>> 35);
		this.x ^= (this.x << 4);
		return this.x;
	}

	public double nextDouble() {
		long l = randomLong();
		return (double)l/ (double)Long.MAX_VALUE;
	}
}
