package playground.balac.allcsmodestest.replanning;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.population.algorithms.PlanAlgorithm;
/**
 * @author balacm
 */
public class ChooseRandomTripMode implements PlanAlgorithm {
	
	private final String[] possibleModes;
	//private boolean ignoreCarAvailability = true;

	private final Random rng;
	
	private final StageActivityTypes stageActivityTypes;
	public ChooseRandomTripMode(final String[] possibleModes, final Random rng, final StageActivityTypes stageActivityTypes) {
		this.possibleModes = possibleModes.clone();
		this.rng = rng;
		this.stageActivityTypes = stageActivityTypes;
	}
	@Override
	public void run(Plan plan) {
		
		List<Trip> t = TripStructureUtils.getTrips(plan, stageActivityTypes);
		
		int cnt = t.size();
		PersonImpl p = (PersonImpl) plan.getPerson();
		if (cnt == 0) {
			return;
		}
		int rndIdx = this.rng.nextInt(cnt);
		
		for(Leg l:t.get(rndIdx).getLegsOnly()) 
			if (l.getMode().equals( "car" ) || l.getMode().equals( "bike" ) || l.getMode().equals("twowaycarsharing"))
				return;
		
		if (p.hasLicense() &&  (p.getTravelcards() != null)  && p.getTravelcards().contains("ch-HT-mobility"))
			//don't change the trips between the same links
			if (!t.get(rndIdx).getOriginActivity().getLinkId().toString().equals(t.get(rndIdx).getDestinationActivity().getLinkId().toString()))
				setRandomTripMode(t.get(rndIdx), plan);
			else return;
		else return;
	}

	private void setRandomTripMode(final Trip trip, final Plan plan) {		
		
		//carsharing is the new trip
		if (possibleModes.length == 2) {
		if(rng.nextBoolean()) {
			TripRouter.insertTrip(
					plan,
					trip.getOriginActivity(),
					Collections.singletonList( new LegImpl( "onewaycarsharing" ) ),
					trip.getDestinationActivity());
		}
		else {
			TripRouter.insertTrip(
					plan,
					trip.getOriginActivity(),
					Collections.singletonList( new LegImpl( "freefloating" ) ),
					trip.getDestinationActivity());
		}
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
	public void setIgnoreCarAvailability(boolean ignoreCarAvailability2) {
		// TODO Auto-generated method stub
		
		
		
	}

}
