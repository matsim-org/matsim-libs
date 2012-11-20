package playground.thibautd.initialdemandgeneration.activitychainsextractor;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

class MzEtappe implements Identifiable {
	// /////////////////////////////////////////////////////////////////////////
	// static fields
	// /////////////////////////////////////////////////////////////////////////
	// attributes
	private final Id personId;
	private final Id wegId;
	private final Id id;
	private final double distance;
	private final String mode;

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public MzEtappe(
			 final Id personId,
			 final Id wegId,
			 final Id id,
			 final double distance,
			 final String mode) {
		this.personId = personId;
		this.wegId = wegId;
		this.id = id;
		this.distance = distance;
		this.mode = mode;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Id getId() {
		return id;
	}

	public String getMode() {
		return mode;
	}

	public double getDistance() {
		return distance;
	}

	public Id getPersonId() {
		return personId;
	}

	public Id getWegId() {
		return wegId;
	}
}
