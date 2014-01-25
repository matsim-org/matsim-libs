package playground.southafrica.gauteng.roadpricingscheme;

import org.matsim.api.core.v01.Id;

public interface TollFactorI {

	public abstract SanralTollVehicleType typeOf(Id idObj);

	public abstract double getTollFactor(Id personId, Id vehicleId, Id linkId, double time);

}