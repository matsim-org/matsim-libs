package playground.balac.aam.replanning;

import java.util.Collections;
import java.util.List;
import java.util.Random;

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
public class ChooseRandomTripMode implements PlanAlgorithm {
	
	//private boolean ignoreCarAvailability = true;

	private final Random rng;
	
	private final StageActivityTypes stageActivityTypes;
	public ChooseRandomTripMode(final String[] possibleModes, final Random rng, final StageActivityTypes stageActivityTypes) {
		
		this.rng = rng;
		this.stageActivityTypes = stageActivityTypes;
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
		
		
		setRandomTripMode(t.get(rndIdx), plan);
		
	}

	private void setRandomTripMode(final Trip trip, final Plan plan) {		
		
		//movingpathways is the new trip
		
		TripRouter.insertTrip(
				plan,
				trip.getOriginActivity(),
				Collections.singletonList( new LegImpl( "movingpathways" ) ),
				trip.getDestinationActivity());
		
	
	}
	public void setIgnoreCarAvailability(boolean ignoreCarAvailability2) {
		// TODO Auto-generated method stub
		
		
		
	}

}
