package playground.southafrica.gauteng.roadpricingscheme;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public interface TollFactorI {

	public abstract SanralTollVehicleType typeOf(Id idObj);

	public abstract double getTollFactor(Person person, Id linkId, double time);

}