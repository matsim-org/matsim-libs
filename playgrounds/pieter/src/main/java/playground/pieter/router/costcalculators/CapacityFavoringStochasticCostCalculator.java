package playground.pieter.router.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

class CapacityFavoringStochasticCostCalculator extends
		TravelTimeAndDistanceBasedTravelDisutility {

	public CapacityFavoringStochasticCostCalculator(TravelTime timeCalculator,
			PlanCalcScoreConfigGroup cnScoringGroup) {
		super(timeCalculator, cnScoringGroup);
	}
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		double disutil = super.getLinkTravelDisutility(link, travelTime, person, vehicle);
		double uniform = MatsimRandom.getRandom().nextDouble();
		double error = disutil*( - 1.0 * Math.log(-Math.log(1.0 * uniform)));
		double capacityPreference = -Math.log10(link.getCapacity());

		return disutil+error+capacityPreference;
		
	}
	public double getLinkMinimumTravelDisutility(final Link link)
	{
		
		double disutil= super.getLinkMinimumTravelDisutility(link);
		double uniform = MatsimRandom.getRandom().nextDouble();
		double error = disutil*( - 1.0 * Math.log(-Math.log(1.0 * uniform)));
		double capacityPreference = -Math.log10(link.getCapacity());

		return disutil+error+capacityPreference;
		
	}

}
