package org.matsim.contrib.common.util;

/**
 * A half-open time interval {@code [start, end)}, in seconds.
 * <p>
 * Two intervals that merely touch (one ends exactly where the other starts) do not overlap.
 * A zero-length interval ({@code start == end}) is treated as the instant {@code start}: it overlaps any interval
 * that {@link #contains(double) contains} that instant.
 *
 * @author mfrawley / MOIA
 */
public record TimeInterval(double start, double end) {

	public TimeInterval {
		if (Double.isNaN(start) || Double.isNaN(end)) {
			throw new IllegalArgumentException("Interval bounds must not be NaN: [" + start + ", " + end + ")");
		}
		if (end < start) {
			throw new IllegalArgumentException("Interval end must not precede its start: [" + start + ", " + end + ")");
		}
	}

	/**
	 * @return true if this and {@code other} share at least one instant.
	 */
	public boolean overlaps(TimeInterval other) {
		if (isInstant()) {
			return other.contains(start);
		}
		if (other.isInstant()) {
			return contains(other.start);
		}
		return start < other.end && other.start < end;
	}

	/**
	 * @return true if {@code time} lies in {@code [start, end)}, or equals {@code start} for a zero-length interval.
	 */
	public boolean contains(double time) {
		return isInstant() ? time == start : start <= time && time < end;
	}

	private boolean isInstant() {
		return start == end;
	}

	/**
	 * @return a copy widened by {@code buffer} seconds on both sides.
	 */
	public TimeInterval expandBy(double buffer) {
		return new TimeInterval(start - buffer, end + buffer);
	}
}
