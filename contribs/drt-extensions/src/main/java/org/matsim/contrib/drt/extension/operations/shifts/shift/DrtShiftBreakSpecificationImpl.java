package org.matsim.contrib.drt.extension.operations.shifts.shift;

/**
 * @author nkuehnel / MOIA
 */
public class DrtShiftBreakSpecificationImpl implements DrtShiftBreakSpecification {

	private final double earliestStart;
	private final double latestEnd;
	private final double duration;

	private DrtShiftBreakSpecificationImpl(Builder builder) {
		this.earliestStart = builder.earliestStart;
		this.latestEnd = builder.latestEnd;
		this.duration = builder.duration;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(DrtShiftBreakSpecificationImpl copy) {
		Builder builder = new Builder();
		builder.earliestStart = copy.getEarliestBreakStartTime();
		builder.latestEnd = copy.getLatestBreakEndTime();
		builder.duration = copy.getDuration();
		return builder;
	}

	@Override
	public double getEarliestBreakStartTime() {
		return earliestStart;
	}

	@Override
	public double getLatestBreakEndTime() {
		return latestEnd;
	}

	@Override
	public double getDuration() {
		return duration;
	}

	public static final class Builder {
		private double earliestStart;
		private double latestEnd;
		private double duration;

		private Builder() {
		}


		public Builder earliestStart(double val) {
			earliestStart = val;
			return this;
		}

		public Builder latestEnd(double val) {
			latestEnd = val;
			return this;
		}

		public Builder duration(double val) {
			duration = val;
			return this;
		}

		public DrtShiftBreakSpecificationImpl build() {
			return new DrtShiftBreakSpecificationImpl(this);
		}
	}
}
