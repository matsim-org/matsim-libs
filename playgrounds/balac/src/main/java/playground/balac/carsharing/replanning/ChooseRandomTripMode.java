package playground.balac.carsharing.replanning;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
/**
 * @author balacm
 */
public class ChooseRandomTripMode implements PlanAlgorithm {
	
	private final String[] possibleModes;
	private boolean ignoreCarAvailability = true;

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
		Person p = plan.getPerson();
		if (cnt == 0) {
			return;
		}
		int rndIdx = this.rng.nextInt(cnt);
		
		for(Leg l:t.get(rndIdx).getLegsOnly()) 
			if (l.getMode() == "car" || l.getMode() == "bike")
				return;
		
		if (PersonUtils.hasLicense(p) && PersonUtils.getTravelcards(p) != null && PersonUtils.getTravelcards(p).contains("ch-HT-mobility"))
			setRandomTripMode(t.get(rndIdx), plan);
	}

	private void setRandomTripMode(final Trip trip, final Plan plan) {		
		
		//carsharing is the new trip
		
		TripRouter.insertTrip(
				plan,
				trip.getOriginActivity(),
				Collections.singletonList( PopulationUtils.createLeg("carsharing") ),
				trip.getDestinationActivity());
	
	}
	public void setIgnoreCarAvailability(boolean ignoreCarAvailability2) {
		// TODO Auto-generated method stub
		
		
		
	}

}
