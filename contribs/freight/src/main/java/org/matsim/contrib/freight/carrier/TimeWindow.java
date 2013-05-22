package org.matsim.contrib.freight.carrier;

public class TimeWindow {
	
	public static TimeWindow newInstance(double start, double end){
		return new TimeWindow(start,end);
	}

	private final double start;

	private final double end;

	private TimeWindow(final double start, final double end) {
		this.start = start;
		this.end = end;
	}

	public double getStart() {
		return start;
	}

	public double getEnd() {
		return end;
	}

	@Override
	public String toString() {
		return "[start=" + start + ", end=" + end + "]";
	}

}