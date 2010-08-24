package playground.wrashid.tryouts.performance;

import org.matsim.testcases.MatsimTestCase;

public class TestFastQueue extends MatsimTestCase {

	public void testAdd() {
		FastQueue<Integer> fq = new FastQueue<Integer>();
		fq.add(1);
		assertEquals(true, fq.poll() == 1);
		assertEquals(true, fq.size() == 0);
	}

	public void testAdd1() {
		FastQueue<Integer> fq = new FastQueue<Integer>();

		for (int i = 0; i < 100000; i++) {
			fq.add(i);
		}

		for (int i = 0; i < 100000; i++) {
			assertEquals(true, fq.poll() == i);
		}

		assertEquals(true, fq.size() == 0);
	}

	public void testGet() {
		FastQueue<Integer> fq = new FastQueue<Integer>();

		for (int i = 0; i < 100000; i++) {
			fq.add(i);
		}

		for (int i = 0; i < 100000; i++) {
			assertEquals(true, fq.get(i) == i);
		}
	}

	// this tests also a cyclic turn arround
	public void testAddPoll() {
		FastQueue<Integer> fq = new FastQueue<Integer>(256, 3);
		for (int i = 0; i < 10000000; i++) {
			fq.add(i);
			assertEquals(true, fq.poll() == i);
		}
	}
}
