package gunnar.ihop2.transmodeler.networktransformation;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerSegment implements Comparable<TransmodelerSegment> {

	private final int lanes;

	private final double length;

	private final Integer position;

	TransmodelerSegment(final int lanes, final double length,
			final Integer position) {
		this.lanes = lanes;
		this.length = length;
		this.position = position;
	}

	int getLanes() {
		return lanes;
	}

	double getLength() {
		return length;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(lanes=" + this.lanes
				+ ", length=" + this.length + ", position=" + this.position
				+ ")";
	}

	@Override
	public int compareTo(final TransmodelerSegment o) {
		return (this.position).compareTo(o.position);
	}

}
