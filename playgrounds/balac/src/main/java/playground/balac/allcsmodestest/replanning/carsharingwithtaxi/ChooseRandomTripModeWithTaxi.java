package playground.balac.allcsmodestest.replanning.carsharingwithtaxi;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.population.algorithms.PlanAlgorithm;
/**
 * @author balacm
 */
public class ChooseRandomTripModeWithTaxi implements PlanAlgorithm {
	
	private final String[] possibleModes;
	//private boolean ignoreCarAvailability = true;

	private final Random rng;
	private final Scenario scenario;
	
	private final StageActivityTypes stageActivityTypes;
	public ChooseRandomTripModeWithTaxi(final Scenario scenario, final String[] possibleModes, final Random rng, final StageActivityTypes stageActivityTypes) {
		this.possibleModes = possibleModes.clone();
		this.rng = rng;
		this.stageActivityTypes = stageActivityTypes;
		this.scenario = scenario;
	}
	@Override
	public void run(Plan plan) {
		
		List<Trip> t = TripStructureUtils.getTrips(plan, stageActivityTypes);
		
		int cnt = t.size();
		if (cnt == 0) {
			return;
		}
		int rndIdx = this.rng.nextInt(cnt);
		
		for(Leg l:t.get(rndIdx).getLegsOnly()) 
			if (l.getMode().equals( "car" ) || l.getMode().equals( "bike" ) || l.getMode().equals("twowaycarsharing"))
				return;
		
			//don't change the trips between the same links
			if (!t.get(rndIdx).getOriginActivity().getLinkId().toString().equals(t.get(rndIdx).getDestinationActivity().getLinkId().toString()))
				setRandomTripMode(t.get(rndIdx), plan);
			else return;
	}

	private void setRandomTripMode(final Trip trip, final Plan plan) {		
		
		//carsharing is the new trip
		int temp = rng.nextInt(6);
		
		if (this.scenario.getPopulation().getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "CS_CARD").equals("freefloating")) {
		
			if (possibleModes.length == 3) {			
				
					TripRouter.insertTrip(
							plan,
							trip.getOriginActivity(),
							Collections.singletonList( new LegImpl( possibleModes[temp % 3] ) ),
							trip.getDestinationActivity());
			
			}
			else if (possibleModes.length == 2) {
				
				TripRouter.insertTrip(
						plan,
						trip.getOriginActivity(),
						Collections.singletonList( new LegImpl( possibleModes[temp % 2] ) ),
						trip.getDestinationActivity());
				
				
			}
			else if (possibleModes.length == 1 && possibleModes[0] != null){
				TripRouter.insertTrip(
						plan,
						trip.getOriginActivity(),
						Collections.singletonList( new LegImpl( possibleModes[0] ) ),
						trip.getDestinationActivity());
			}
			else
				TripRouter.insertTrip(
						plan,
						trip.getOriginActivity(),
						trip.getTripElements(),
						trip.getDestinationActivity());
		}
		else 
			TripRouter.insertTrip(
					plan,
					trip.getOriginActivity(),
					Collections.singletonList( new LegImpl( possibleModes[0] ) ),
					trip.getDestinationActivity());
	
	}
	public void setIgnoreCarAvailability(boolean ignoreCarAvailability2) {
		// TODO Auto-generated method stub
		
		
		
	}

}
