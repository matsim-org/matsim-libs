package playground.pieter.router.costcalculators;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

class CapacityFavoringStochasticCostCalculator implements TravelDisutility {
	private final TravelDisutility delegate ;
	private final TravelTime timeCalculator;


	public CapacityFavoringStochasticCostCalculator(TravelTime timeCalculator,
			PlanCalcScoreConfigGroup cnScoringGroup) {
		this.timeCalculator = timeCalculator ;
		
		Builder builder = new RandomizingTimeDistanceTravelDisutility.Builder( TransportMode.car, cnScoringGroup );
		this.delegate = builder.createTravelDisutility(timeCalculator) ; 
		
	}
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		double disutil = this.delegate.getLinkTravelDisutility(link, travelTime, person, vehicle);
		double uniform = MatsimRandom.getRandom().nextDouble();
		double error = disutil*( - 1.0 * Math.log(-Math.log(1.0 * uniform)));
		double capacityPreference = -Math.log10(link.getCapacity());

		return disutil+error+capacityPreference;
		
	}
	@Override
	public double getLinkMinimumTravelDisutility(final Link link)
	{
		
		double disutil= this.delegate.getLinkMinimumTravelDisutility(link);
		double uniform = MatsimRandom.getRandom().nextDouble();
		double error = disutil*( - 1.0 * Math.log(-Math.log(1.0 * uniform)));
		double capacityPreference = -Math.log10(link.getCapacity());

		return disutil+error+capacityPreference;
		
	}

}
