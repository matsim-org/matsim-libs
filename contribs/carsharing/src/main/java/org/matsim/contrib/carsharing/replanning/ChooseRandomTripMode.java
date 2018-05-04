package org.matsim.contrib.carsharing.replanning;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.demand.membership.PersonMembership;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
/**
 * @author balacm
 */
public final class ChooseRandomTripMode implements PlanAlgorithm {
	
	private final String[] possibleModes;
	//private boolean ignoreCarAvailability = true;
	
	private final Random rng;
	private final Scenario scenario;
	private MembershipContainer memberships;
	
	private final StageActivityTypes stageActivityTypes;
	public ChooseRandomTripMode(final Scenario scenario, final String[] possibleModes,
								final Random rng, final StageActivityTypes stageActivityTypes, MembershipContainer memberships) {
		this.possibleModes = possibleModes.clone();
		this.rng = rng;
		this.stageActivityTypes = stageActivityTypes;
		this.scenario = scenario;
		this.memberships = memberships;
	}
	@Override
	public void run(Plan plan) {
		Id<Person> personId = plan.getPerson().getId();
		List<Trip> trips = TripStructureUtils.getTrips(plan, stageActivityTypes);
		boolean ffcard = false;
		boolean owcard = false;
		int cnt = trips.size();
		if (cnt == 0) {
			return;
		}
		
		int rndIdx = this.rng.nextInt(cnt);
		
		for (Leg l:trips.get(rndIdx).getLegsOnly()) {
			if (l.getMode().equals( "car" ) || l.getMode().equals( "bike" ) || l.getMode().equals("twoway")) {
				return;
			}
		}
		
		PersonMembership personMemmbership = this.memberships.getPerPersonMemberships().get(personId);
		
		if (personMemmbership != null) {
			if (personMemmbership.getMembershipsPerCSType().containsKey("freefloating"))
				ffcard = true;
			if (personMemmbership.getMembershipsPerCSType().containsKey("oneway"))
				owcard = true;
		}
		
		//don't change the trips between the same links
		if (!trips.get(rndIdx).getOriginActivity().getLinkId().toString().equals(trips.get(rndIdx).getDestinationActivity().getLinkId().toString()))
			setRandomTripMode(trips.get(rndIdx), plan, ffcard, owcard);
		else return;
	}
	
	private void setRandomTripMode(final Trip trip, final Plan plan, boolean ffcard, boolean owcard) {
		
		//carsharing is the new trip
		if (possibleModes.length == 2) {
			if(rng.nextBoolean()) {
				if (owcard)
					TripRouter.insertTrip(
							plan,
							trip.getOriginActivity(),
							Collections.singletonList( PopulationUtils.createLeg("oneway") ),
							trip.getDestinationActivity());
			}
			else {
				if (ffcard)
					TripRouter.insertTrip(
							plan,
							trip.getOriginActivity(),
							Collections.singletonList( PopulationUtils.createLeg("freefloating") ),
							trip.getDestinationActivity());
			}
		}
		else if (possibleModes.length == 1 && possibleModes[0] != null){
			if ((possibleModes[0].equals("freefloating") && ffcard) || (possibleModes[0].equals("oneway") && owcard))
				TripRouter.insertTrip(
						plan,
						trip.getOriginActivity(),
						Collections.singletonList( PopulationUtils.createLeg(possibleModes[0]) ),
						trip.getDestinationActivity());
		}
		else
			TripRouter.insertTrip(
					plan,
					trip.getOriginActivity(),
					trip.getTripElements(),
					trip.getDestinationActivity());
		
	}
	
	
}
