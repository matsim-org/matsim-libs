package gunnar.ihop2.transmodeler.tripswriting;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerTrip implements Comparable<TransmodelerTrip> {

	// -------------------- CONSTANTS --------------------

	final int id;

	final String fromNodeId;

	final String toNodeId;

	final String fromLinkId;

	final int pathId;

	final String toLinkId;

	final Double dptTime_s;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerTrip(final int id, final String fromNodeId,
			final String toNodeId, final String fromLinkId, final int pathId,
			final String toLinkId, final Double dptTime_s) {
		this.id = id;
		this.fromNodeId = fromNodeId;
		this.toNodeId = toNodeId;
		this.fromLinkId = fromLinkId;
		this.pathId = pathId;
		this.toLinkId = toLinkId;
		this.dptTime_s = dptTime_s;
	}

	// -------------------- IMPLEMENTATION OF Comparable --------------------

	@Override
	public int compareTo(final TransmodelerTrip o) {
		return this.dptTime_s.compareTo(o.dptTime_s);
	}

}
