package playground.andreas.bln.net.simplex;

public class SimplexDelayCountBox {
	
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
		if(this.numberOfEntries == 0){
			return 0.0;
		} 
		return this.accumulatedDelay / this.numberOfEntries;		
	}
}