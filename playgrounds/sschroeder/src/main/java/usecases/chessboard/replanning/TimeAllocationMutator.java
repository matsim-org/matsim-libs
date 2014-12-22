package usecases.chessboard.replanning;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;

public class TimeAllocationMutator implements GenericPlanStrategyModule<CarrierPlan>{

	private double mutationProbability = .1;
	
	public TimeAllocationMutator(double mutationProbability) {
		super();
		this.mutationProbability = mutationProbability;
	}

	@Override
	public void handlePlan(CarrierPlan carrierPlan) {
		Collection<ScheduledTour> newTours = new ArrayList<ScheduledTour>() ;
		for ( ScheduledTour tour : carrierPlan.getScheduledTours() ) {
			if(MatsimRandom.getRandom().nextDouble() < mutationProbability){
				double departureTime = tour.getDeparture() + ( MatsimRandom.getRandom().nextDouble() - 0.5 ) * 3600. * 3. ;
				if ( departureTime < tour.getVehicle().getEarliestStartTime() ) {
					departureTime = tour.getVehicle().getEarliestStartTime();
				}
				newTours.add( ScheduledTour.newInstance(tour.getTour(), tour.getVehicle(), departureTime) ) ;
			}
			else newTours.add(tour); 
		}
		carrierPlan.getScheduledTours().clear(); 
		carrierPlan.getScheduledTours().addAll( newTours ) ;
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}

	@Override
	public void finishReplanning() {
	}


}
