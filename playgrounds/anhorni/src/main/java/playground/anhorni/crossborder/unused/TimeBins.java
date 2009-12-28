package playground.anhorni.crossborder.unused;

/* Remarks about the idea of using time bins:
 * We want to have all the plans starting time uniformly distributed in one hour
 * For a perfect uniform distribution we would have to know the exact number of plans in that hour
 * and for that we would have to make to loop in FMAParser.parse() twice.
 * Using time beens is a first work around and moves the gap given by the difference between totalVolume and 
 * number of plans into the time bins.
 */

public class TimeBins {
	
	private int hour;
	private double delta;
	
	//20 bins a 300 secons
	private int actualTimeBin;
	// in seconds
	private double actualBinDelta;
	
	
	public TimeBins() {
		this.actualTimeBin=0;
		this.actualBinDelta=0.0;
	}
	
	public int getHour() {
		return hour;
	}

	public void setHour(int hour) {
		this.hour = hour;
	}

	public double getDelta() {
		return delta;
	}

	public void setDelta(double delta) {
		this.delta = delta;
	}	
	
	private void go2NextBin() {	
		
		if (this.actualTimeBin==11) {
			// In this step we go to the first bin again -> set next binDelta
			this.nextBinDelta();
		}
		this.actualTimeBin=(this.actualTimeBin+1) % 12;
	}
	
	private void nextBinDelta() {
		this.actualBinDelta+=delta;
	}
	
	public int getStartTime() {	
		int time=(int)(this.actualTimeBin*300+this.actualBinDelta);	
		this.go2NextBin();
		return time;
	}
}
