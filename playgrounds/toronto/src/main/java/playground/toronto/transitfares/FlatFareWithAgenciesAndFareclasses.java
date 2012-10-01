package playground.toronto.transitfares;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.vehicles.Vehicle;

import playground.toronto.transitfares.fareclasser.FareClasser;

public class FlatFareWithAgenciesAndFareclasses implements FareCalculator {

	private double defaultFare;
	private Map<String, Map<String, Double>> fares; //Agency -> Fare class name -> Flat fare value 
	private Map<String, FareClasser> agencyFareClasses;
	private Map<Id, String> lineOperatorMap; //LineId -> Agency
	
	@Override
	public double getDisutilityOfTransferFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {
		// TODO Auto-generated method stub
		Id lineId = link.getToNode().line.getId();
		String agency = lineOperatorMap.get(lineId);
		if (agency != null){
			String fareClass = this.agencyFareClasses.get(agency).getFareClass(person);
			if (fareClass != null){
				Double fare = fares.get(agency).get(fareClass);
				if (fare != null){
					return fare;
				}
			}
		}
		
		return defaultFare;
	}

	@Override
	public double getDisutilityOfInVehicleFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {
		return 0;
	}

}
