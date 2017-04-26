package playground.singapore.springcalibration.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.vehicles.Vehicle;

public class SubpopTravelDisutility implements TravelDisutility {
	
	private final TravelTime timeCalculator;
	private ScoringParametersForPerson params;
	private String mode = TransportMode.car;
	
	private final static Logger log = Logger.getLogger(SubpopTravelDisutility.class);
	
	public SubpopTravelDisutility(TravelTime timeCalculator, ScoringParametersForPerson params, String mode) {
		this.timeCalculator = timeCalculator;
		this.params = params;
		this.mode = mode;
	}
	
	public static class Builder implements TravelDisutilityFactory {
		private ScoringParametersForPerson params;
		private String mode;

		public Builder( final String mode, ScoringParametersForPerson params) {
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
		
		double marginalUtilityOfPerforming_util_s = -1.0 * params.getScoringParameters(person).marginalUtilityOfPerforming_s; // make negativ for router: it is lost
		double marginalUtilityOfTime_util_s = params.getScoringParameters(person).modeParams.get(this.mode).marginalUtilityOfTraveling_s + marginalUtilityOfPerforming_util_s;
		double marginalUtilityOfDistance_util_m = params.getScoringParameters(person).modeParams.get(this.mode).marginalUtilityOfDistance_m;
		double monetaryDistanceRate = params.getScoringParameters(person).modeParams.get(this.mode).monetaryDistanceCostRate;
		double marginalUtilityOfMoney = params.getScoringParameters(person).marginalUtilityOfMoney;		
		final double marginalUtilityOfDistanceFromMoney_util_m = monetaryDistanceRate * marginalUtilityOfMoney; // [money/m]*[util/money]
		
		// Needs to be positive: 
		return -1.0 *
				(marginalUtilityOfTime_util_s * travelTime 
					+ (marginalUtilityOfDistance_util_m + marginalUtilityOfDistanceFromMoney_util_m) * link.getLength());
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		double constantPerforming = - 6.0 / 3600.0; // this is a hack! TODO: take from config
		double marginalUtilityOfTime_util_s = params.getScoringParameters(null).modeParams.get(this.mode).marginalUtilityOfTraveling_s + constantPerforming;
		double marginalUtilityOfDistance_util_m = params.getScoringParameters(null).modeParams.get(this.mode).marginalUtilityOfDistance_m;
		double monetaryDistanceRate = params.getScoringParameters(null).modeParams.get(this.mode).monetaryDistanceCostRate;
		double marginalUtilityOfMoney = params.getScoringParameters(null).marginalUtilityOfMoney;		
		final double marginalUtilityOfDistanceFromMoney_util_m = monetaryDistanceRate * marginalUtilityOfMoney; // [money/m]*[util/money]
		
		// Needs to be positive: 
		return -1.0 * 
				(marginalUtilityOfTime_util_s * (link.getLength() / link.getFreespeed()) 
				+ (marginalUtilityOfDistance_util_m + marginalUtilityOfDistanceFromMoney_util_m) * link.getLength());
	}

}
