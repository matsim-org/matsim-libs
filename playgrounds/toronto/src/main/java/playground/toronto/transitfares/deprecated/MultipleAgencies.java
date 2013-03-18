package playground.toronto.transitfares.deprecated;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicle;


/**
 * A {@link FareCalculator} wrapper which allows different agencies to have their own fare
 * schemes. Each {@link TransitLine} must map to only one agency.
 * 
 * @author pkucirek
 *
 */
public class MultipleAgencies implements FareCalculator {

	private final Map<String, FareCalculator> agencyFareRules; //Agency -> FareCalculator
	private final Map<Id, String> lineAgencyMap; //TransitLine Id -> Agency
	private final double defaultFare;
	
	/**
	 * @param agencyFareSchemes A dictionary mapping agency names to {@link FareCalculator}.
	 * @param lineAgencyMap A dictionary mapping {@link TransitLine} to agency name.
	 * @param defaultFare A default fare in case the mapping fails.
	 */
	public MultipleAgencies(Map<String, FareCalculator> agencyFareSchemes, Map<Id, String> lineAgencyMap, double defaultFare){
		this.agencyFareRules = agencyFareSchemes;
		this.lineAgencyMap = lineAgencyMap;
		this.defaultFare = defaultFare;
	}
	
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
