package gunnar.ihop2.transmodeler.networktransformation;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerNode extends TransmodelerElement {

	private final double longitude;

	private final double latitude;

	TransmodelerNode(final String id, final double longitude,
			final double latitude) {
		super(id);
		this.longitude = longitude;
		this.latitude = latitude;
	}

	double getLongitude() {
		return this.longitude;
	}

	double getLatitude() {
		return this.latitude;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(id=" + this.getId()
				+ ", lon=" + this.longitude + ", lat=" + this.latitude + ")";
	}
}
