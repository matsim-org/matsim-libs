package playground.gregor;
import java.util.Random;

import org.matsim.core.gbl.MatsimRandom;



public class RandomTest {

	public static void main (String [] args) {

		Random rand1 = MatsimRandom.getRandom();
		XORShiftRandom rand2 = new XORShiftRandom();

		long before1 = System.currentTimeMillis();
		for (long i = 0; i < 100000000; i++) {
			rand1.nextLong();
		}
		long after1 = System.currentTimeMillis();
		System.out.println("run time java.util.Random:" + (after1-before1));

		long before2 = System.currentTimeMillis();
		for (long i = 0; i < 100000000; i++) {
			rand2.randomLong();
		}
		long after2 = System.currentTimeMillis();
		System.out.println("run time XORShift:" + (after2-before2));


	}


	private static class XORShiftRandom {

		private long x;

		public XORShiftRandom () {
			this.x = System.nanoTime();
		}

		public long randomLong() {
			this.x ^= (this.x << 21);
			this.x ^= (this.x >>> 35);
			this.x ^= (this.x << 4);
			return this.x;
		}
	}

}
