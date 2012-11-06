package playground.toronto.transitfares;

import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.vehicles.Vehicle;

import playground.toronto.transitfares.fareclasser.FareClasser;

/**
 * A calculator for a system or agency with a single flat fare for defined passenger
 * fare classes, and for a simulation with a global disutility-of-money.
 * 
 * @author pkucirek
 *
 */
public class FlatFareWithFareClassesWithFlatDisutilityOfMoney implements FareCalculator {

	private final double defaultFare;
	private final double globalDisutilityOfMoney;
	private final Map<String, Double> fares; //Fare class name -> Flat fare value 
	private final FareClasser classifier;
	
	public FlatFareWithFareClassesWithFlatDisutilityOfMoney(double disutilityOfMoney, double defaultFare,
			Map<String, Double> fares, FareClasser classifier){
		this.defaultFare = defaultFare;
		this.globalDisutilityOfMoney = disutilityOfMoney;
		this.fares = fares;
		this.classifier = classifier;
	}
	
	@Override
	public double getDisutilityOfTransferFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {

		String fareClass = this.classifier.getFareClass(person);
		if (fareClass != null){
			Double fare = fares.get(fareClass);
			if (fare != null){
				return fare * this.globalDisutilityOfMoney;
			}
		}
				
		return defaultFare * this.globalDisutilityOfMoney;
	}

	@Override
	public double getDisutilityOfInVehicleFare(Person person, Vehicle vehicle,
			TransitRouterNetworkLink link, double now) {
		return 0;
	}

}
