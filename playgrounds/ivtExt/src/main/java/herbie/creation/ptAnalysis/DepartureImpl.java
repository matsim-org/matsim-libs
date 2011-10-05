package herbie.creation.ptAnalysis;


import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;


/**
 * Describes a single departure along a route in a transit line.
 *
 * @author mrieser
 */
public class DepartureImpl implements Departure {

	private final Id id;
	private final double departureTime;
	private Id vehicleId = null;

	protected DepartureImpl(final Id id, final double departureTime) {
		this.id = id;
		this.departureTime = departureTime;
	}

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public double getDepartureTime() {
		return this.departureTime;
	}

	@Override
	public void setVehicleId(final Id vehicleId) {
		this.vehicleId = vehicleId;
	}

	@Override
	public Id getVehicleId() {
		return this.vehicleId;
	}

}