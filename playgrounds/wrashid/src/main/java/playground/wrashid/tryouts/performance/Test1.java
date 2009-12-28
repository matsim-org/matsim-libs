package playground.wrashid.tryouts.performance;

import org.matsim.core.mobsim.jdeqsim.util.Timer;

public class Test1 {

	/**
	 * @param args
	 */

	public volatile int g = 0;

	// just compares, no/with synchronized and volatile
	/*
	 * time without synchronized: 93
	 * time with synchronized: 2486 (26 times slower)
	 * time with volatile: 1828
	 */
	public static void main(String[] args) {
		Timer t = new Timer();
		Test1 t1 = new Test1();
		int target = 100000000;
		t.startTimer();
		for (int i = 0; i < target; i++) {
			
		}
		t.endTimer();
		t.printMeasuredTime("time without synchronized: ");

		t.resetTimer();
		t.startTimer();
		for (int i = 0; i < target; i++) {
			synchronized (t) {

			}
		}
		t.endTimer();
		t.printMeasuredTime("time with synchronized: ");

		t.resetTimer();
		t.startTimer();
		for (int i = 0; i < target; i++) {
			t1.g = 0;
		}
		t.endTimer();
		t.printMeasuredTime("time with volatile: ");

	}

}
