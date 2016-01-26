package playground.dziemke.feathersMatsim.ikea.Simulation;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class ResidentialAreaDisutilityCalculatorFactory implements TravelDisutilityFactory{

	
	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator,PlanCalcScoreConfigGroup cnScoringGroup) {
		final ResidentialAreaTravelDisutilityCalculator slagboomCalculator = new ResidentialAreaTravelDisutilityCalculator(timeCalculator, cnScoringGroup);
		
		return new TravelDisutility(){

			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				double linkTravelDisutility = slagboomCalculator.getLinkTravelDisutility(link, time, person, vehicle);
				return linkTravelDisutility;
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
		return slagboomCalculator.getLinkMinimumTravelDisutility(link);
			}
			
		};
	}

}
