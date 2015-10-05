package gunnar.ihop2.transmodeler.networktransformation;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerSegment extends TransmodelerElement implements
		Comparable<TransmodelerSegment> {

	// -------------------- MEMBERS --------------------

	private final int lanes;

	private final double length;

	// using negative positions in BA direction
	private final Integer position;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerSegment(final String id, final int lanes, final double length,
			final Integer position) {
		super(id);
		this.lanes = lanes;
		this.length = length;
		this.position = position;
	}

	// -------------------- GETTERS --------------------

	int getLanes() {
		return lanes;
	}

	double getLength() {
		return length;
	}

	// -------------------- IMPLEMENTATION OF Comparable --------------------

	@Override
	public int compareTo(final TransmodelerSegment o) {
		return (this.position).compareTo(o.position);
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(lanes=" + this.lanes
				+ ", length=" + this.length + ", position=" + this.position
				+ ")";
	}
}
