package playground.toronto.transitfares;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * 
 * @author pkucirek
 *
 */
public class MultipleAgencies implements FareCalculator {

	private Map<String, FareCalculator> agencyFareRules; //Agency -> FareCalculator
	private Map<Id, String> lineAgencyMap; //TransitLine Id -> Agency
	private double defaultFare;
	
	@Override
	public double getDisutilityOfTransferFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {
		
		Id lineId = link.getToNode().line.getId();
		if (this.lineAgencyMap.containsKey(lineId)){
			String agency = this.lineAgencyMap.get(lineId);
			if (this.agencyFareRules.containsKey(agency)){
				return this.agencyFareRules.get(agency).getDisutilityOfTransferFare(person, vehicle, link, now);
			}
		}
		
		return this.defaultFare;
	}

	@Override
	public double getDisutilityOfInVehicleFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {

		Id lineId = link.getLine().getId();
		if (this.lineAgencyMap.containsKey(lineId)){
			String agency = this.lineAgencyMap.get(lineId);
			if (this.agencyFareRules.containsKey(agency)){
				return this.agencyFareRules.get(agency).getDisutilityOfInVehicleFare(person, vehicle, link, now);
			}
		}
		
		return this.defaultFare;
		
	}

}
