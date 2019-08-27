package org.matsim.contrib.roadpricing;

/**
 * A single, time-dependent toll-amount for a roadpricing scheme.
 *
 * @author mrieser
 */
public final class CostInfo implements Comparable<CostInfo> {
	public final double startTime;
	public final double endTime;
	public final double amount;

	public CostInfo( final double startTime, final double endTime, final double amount ) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.amount = amount;
	}

	@Override
	public String toString() {
		return "startTime: " + this.startTime + " endTime: " + this.endTime + " amount: " + this.amount;
	}

	@Override
	public int compareTo( CostInfo o ) {
		return Double.compare(this.startTime, o.startTime);
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof CostInfo)){
			throw new IllegalArgumentException("Can only compare two Cost elements");
		}
		CostInfo otherCost = (CostInfo) obj;
		return this.startTime == otherCost.startTime &&
				this.endTime == otherCost.endTime &&
				this.amount == otherCost.amount;
	}
}
