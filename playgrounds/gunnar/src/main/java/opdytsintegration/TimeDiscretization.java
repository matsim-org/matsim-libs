package opdytsintegration;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TimeDiscretization {

	private final int startTime_s;

	private final int binSize_s;

	private final int binCnt;

	public TimeDiscretization(final int startTime_s, final int binSize_s,
			final int binCnt) {
		this.startTime_s = startTime_s;
		this.binSize_s = binSize_s;
		this.binCnt = binCnt;
	}

	public int getStartTime_s() {
		return this.startTime_s;
	}

	public int getBinSize_s() {
		return this.binSize_s;
	}

	public int getBinCnt() {
		return this.binCnt;
	}
	
	// TODO NEW
	public int getBinCenterTime_s(final int bin) {
		return bin * this.binSize_s + this.binSize_s / 2;
	}
}
