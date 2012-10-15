package playground.muelleki.smalltasks;

public class TimeKeeper {
	
	private long l;

	public TimeKeeper() {
		l = System.nanoTime();
	}
	
	public long add() {
		long ln = System.nanoTime();
		long dl = ln - l;
		l = ln;
		return dl;
	}
}
