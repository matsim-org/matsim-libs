package playground.singapore.springcalibration.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.vehicles.Vehicle;

public class SubpopTravelDisutility implements TravelDisutility {
	
	private final TravelTime timeCalculator;
	private CharyparNagelScoringParametersForPerson params;
	private String mode = TransportMode.car;
	
	public SubpopTravelDisutility(TravelTime timeCalculator, CharyparNagelScoringParametersForPerson params, String mode) {
		this.timeCalculator = timeCalculator;
		this.params = params;
		this.mode = mode;
	}
	
	public static class Builder implements TravelDisutilityFactory {
		private CharyparNagelScoringParametersForPerson params;
		private String mode;

		public Builder( final String mode, CharyparNagelScoringParametersForPerson params) {
			this.params = params;
			this.mode = mode;
		}

		@Override
		public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
			return new SubpopTravelDisutility(timeCalculator, this.params, this.mode);
		}
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);		
		// what about opportunity costs?
		double marginalUtilityOfTime_util_m = params.getScoringParameters(person).modeParams.get(this.mode).marginalUtilityOfTraveling_s;
		double marginalUtilityOfDistance_util_m = params.getScoringParameters(person).modeParams.get(this.mode).marginalUtilityOfDistance_m;
		double monetaryDistanceRate = params.getScoringParameters(person).modeParams.get(this.mode).monetaryDistanceCostRate;
		double marginalUtilityOfMoney = params.getScoringParameters(person).marginalUtilityOfMoney;		
		final double marginalUtilityOfDistanceFromMoney_util_m = monetaryDistanceRate * marginalUtilityOfMoney; // [money/m]*[util/money]
		
		// Needs to be positive: 
		return -1.0 *
				(marginalUtilityOfTime_util_m * travelTime 
					+ (marginalUtilityOfDistance_util_m + marginalUtilityOfDistanceFromMoney_util_m) * link.getLength());
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		double marginalUtilityOfTime_util_m = params.getScoringParameters(null).modeParams.get(this.mode).marginalUtilityOfTraveling_s;
		double marginalUtilityOfDistance_util_m = params.getScoringParameters(null).modeParams.get(this.mode).marginalUtilityOfDistance_m;
		double monetaryDistanceRate = params.getScoringParameters(null).modeParams.get(this.mode).monetaryDistanceCostRate;
		double marginalUtilityOfMoney = params.getScoringParameters(null).marginalUtilityOfMoney;		
		final double marginalUtilityOfDistanceFromMoney_util_m = monetaryDistanceRate * marginalUtilityOfMoney; // [money/m]*[util/money]
		
		// Needs to be positive: 
		return -1.0 * 
				(marginalUtilityOfTime_util_m * (link.getLength() / link.getFreespeed()) 
				+ (marginalUtilityOfDistance_util_m + marginalUtilityOfDistanceFromMoney_util_m) * link.getLength());
	}

}
