package playground.thibautd.analysis.spacetimeprismjoinabletrips;

import org.matsim.api.core.v01.Id;

// /////////////////////////////////////////////////////////////////////////
// classes
// /////////////////////////////////////////////////////////////////////////
class Od implements Comparable<Od> {
	// this is use for caching in a tree map: comparison should
	// be as efficient as possible. Storing as strings removes
	// the cost of repetidly unboxing ids without loss of information.
	private final String origin, destination;

	public Od(
			final Id origin,
			final Id destination) {
		this.origin = origin.toString();
		this.destination = destination.toString();
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof Od &&
			((Od) other).origin.equals( origin ) &&
			((Od) other).destination.equals( destination );
	}

	@Override
	public int hashCode() {
		return origin.hashCode() + 1000 * destination.hashCode();
	}

	@Override
	public final int compareTo(final Od other) {
		final int comp = origin.compareTo( other.origin );
		return comp == 0 ? destination.compareTo( other.destination ) : comp;
	}
}