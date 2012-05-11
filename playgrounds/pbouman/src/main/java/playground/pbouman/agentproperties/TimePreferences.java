package playground.pbouman.agentproperties;

public class TimePreferences
{
	private double startDevUtility = 0;
	private double startMean = 0;
	private double startStdDev = Double.POSITIVE_INFINITY;
	
	private double endDevUtility = 0;
	private double endMean = 0;
	private double endStdDev = Double.POSITIVE_INFINITY;
	
	private double durationUtility = 0;
	private double durationMean = 0;
	private double durationStdDev = Double.POSITIVE_INFINITY;
	
	public double getStartDevUtility() {
		return startDevUtility;
	}

	public void setStartDevUtility(double startDevUtility) {
		this.startDevUtility = startDevUtility;
	}

	public double getStartMean() {
		return startMean;
	}

	public void setStartMean(double startMean) {
		this.startMean = startMean;
	}

	public double getStartStdDev() {
		return startStdDev;
	}

	public void setStartStdDev(double startStdDev) {
		this.startStdDev = startStdDev;
	}

	public double getEndDevUtility() {
		return endDevUtility;
	}

	public void setEndDevUtility(double endDevUtility) {
		this.endDevUtility = endDevUtility;
	}

	public double getEndMean() {
		return endMean;
	}

	public void setEndMean(double endMean) {
		this.endMean = endMean;
	}

	public double getEndStdDev() {
		return endStdDev;
	}

	public void setEndStdDev(double endStdDev) {
		this.endStdDev = endStdDev;
	}

	public double getDurationUtility() {
		return durationUtility;
	}

	public void setDurationUtility(double durationUtility) {
		this.durationUtility = durationUtility;
	}

	public double getDurationMean() {
		return durationMean;
	}

	public void setDurationMean(double durationMean) {
		this.durationMean = durationMean;
	}

	public double getDurationStdDev() {
		return durationStdDev;
	}

	public void setDurationStdDev(double durationStdDev) {
		this.durationStdDev = durationStdDev;
	}
}
