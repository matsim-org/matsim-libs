package playground.andreas.bln.ana.delayatstop;

public class DelayCountBox {
	
	private int numberOfEntries = 0;
	private double accumulatedDelay = 0.0;

	public void addEntry(double delay){
		this.numberOfEntries++;
		this.accumulatedDelay += delay;
	}

	public int getNumberOfEntries(){
		return this.numberOfEntries;
	}

	public double getAccumulatedDelay(){
		return this.accumulatedDelay;
	}

	public double getAverageDelay(){
		return this.accumulatedDelay / this.numberOfEntries;
	}
}