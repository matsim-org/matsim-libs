package playground.wrashid.DES.util;
public class Timer {
	private long startTime = 0;
	private long endTime = 0;

	public void startTimer() {
		startTime = System.currentTimeMillis();
	}

	public void endTimer() {
		endTime = System.currentTimeMillis();
	}

	public void resetTimer() {
		startTime = 0;
		endTime = 0;
	}

	public long getMeasuredTime() {
		return endTime - startTime;
	}
	
	public void printMeasuredTime(String label){
		System.out.println(label + getMeasuredTime());
	}
}
