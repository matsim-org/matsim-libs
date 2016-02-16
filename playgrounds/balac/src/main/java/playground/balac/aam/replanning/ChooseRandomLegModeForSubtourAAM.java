package playground.balac.aam.replanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.balac.aam.router.AAMRoutingModule;

/**
 * Changes the transportation mode of one random non-empty subtour in a plan to a randomly chosen
 * different mode given a list of possible modes, considering that the means of transport
 * follows the law of mass conservation.
 * When changing the subtour to PT each trip can be changed to either walk or pt leg based on the score a person would get in order to increase it.
 *
 * @author balac
 * @see SubtourModeChoice
 */
public class ChooseRandomLegModeForSubtourAAM implements PlanAlgorithm {

	private static Logger logger = Logger.getLogger(ChooseRandomLegModeForSubtourAAM.class);
	
	private static class Candidate {
		final Subtour subtour;
		final String newTransportMode;

		public Candidate(
				final Subtour subtour,
				final String newTransportMode) {
			this.subtour = subtour;
			this.newTransportMode = newTransportMode;
		}
	}

	private Collection<String> modes;
	private final Collection<String> chainBasedModes;
	private Collection<String> singleTripSubtourModes;

	private final StageActivityTypes stageActivityTypes;
	private final MainModeIdentifier mainModeIdentifier;

	private final Random rng;

	private PermissibleModesCalculator permissibleModesCalculator;

	private boolean anchorAtFacilities = false;
	
	private CharyparNagelScoringParameters params;
	
	private final double beeLineFactor;
	private final double walkSpeed;
	private final double ptSpeed;
	private final AAMRoutingModule routingModuleAAM;
	
	private Scenario scenario;
	public ChooseRandomLegModeForSubtourAAM (
			final StageActivityTypes stageActivityTypes,
			final MainModeIdentifier mainModeIdentifier,
			final PermissibleModesCalculator permissibleModesCalculator,
			final String[] modes,
			final String[] chainBasedModes,
			final Random rng,
			CharyparNagelScoringParameters params,
			double beeLineFactor,
			double walkSpeed,
			double ptSpeed,
			Scenario scenario) {
		this.scenario = scenario;
		this.routingModuleAAM = new AAMRoutingModule(scenario);
		this.stageActivityTypes = stageActivityTypes;
		this.mainModeIdentifier = mainModeIdentifier;
		this.permissibleModesCalculator = permissibleModesCalculator;
		this.modes = Arrays.asList(modes);
		this.chainBasedModes = Arrays.asList(chainBasedModes);
		this.params = params;
		this.beeLineFactor = beeLineFactor;
		this.walkSpeed = walkSpeed;
		this.ptSpeed = ptSpeed;
		
		String[] x = new String[1];
		x[0] = "walk";
		this.singleTripSubtourModes = Arrays.asList(x);
		
		
		this.rng = rng;
		logger.info("Chain based modes: " + this.chainBasedModes.toString());
	}

	/**
	 * Some subtour consist of only a single trip, e.g. if a trip starts and ends on the same link or facility.
	 * By default, for those modes, the normal chain based modes are available. But in certain cases, not all
	 * the modes should be available for such trips (e.g. car-sharing does not make much sense for such a trip),
	 * thus the list of modes available for single-trip subtours can be specified independently. As mentioned,
	 * it is initialized by the constructor to the full list of chain based modes.
	 * 
	 * @param singleTripSubtourModes
	 */
	public void setSingleTripSubtourModes(final String[] singleTripSubtourModes) {
		this.singleTripSubtourModes = Arrays.asList(singleTripSubtourModes);
	}
	
	@Override
	public void run(final Plan plan) {
		if (plan.getPlanElements().size() <= 1) {
			return;
		}

		final Id homeLocation = anchorAtFacilities ?
			((Activity) plan.getPlanElements().get(0)).getFacilityId() :
			((Activity) plan.getPlanElements().get(0)).getLinkId();
		Collection<String> permissibleModesForThisPlan = permissibleModesCalculator.getPermissibleModes(plan);

		List<Candidate> choiceSet =
			determineChoiceSet(
					homeLocation,
					TripStructureUtils.getTrips( plan , stageActivityTypes ),
					TripStructureUtils.getSubtours(
						plan,
						stageActivityTypes,
						anchorAtFacilities),
					permissibleModesForThisPlan);

		if (!choiceSet.isEmpty()) {
			Candidate whatToDo = choiceSet.get(rng.nextInt(choiceSet.size()));
			applyChange( whatToDo , plan );
		}
	}

	private List<Candidate> determineChoiceSet(
			final Id homeLocation,
			final List<Trip> trips,
			final Collection<Subtour> subtours,
			final Collection<String> permissibleModesForThisPerson) {
		final ArrayList<Candidate> choiceSet = new ArrayList<Candidate>();
		for ( Subtour subtour : subtours ) {
			if ( !subtour.isClosed() ) {
				continue;
			}

			if ( containsUnknownMode( subtour ) ) {
				continue;
			}

			final Set<String> usableChainBasedModes = new HashSet<String>();
			final Id subtourStartLocation = anchorAtFacilities ?
				subtour.getTrips().get( 0 ).getOriginActivity().getFacilityId() :
				subtour.getTrips().get( 0 ).getOriginActivity().getLinkId();
			
			final Collection<String> testingModes =
				subtour.getTrips().size() == 1 ?
					singleTripSubtourModes :
					chainBasedModes;

			for (String mode : testingModes) {
				Id vehicleLocation = homeLocation;
				Activity lastDestination =
					findLastDestinationOfMode(
						trips.subList(
							0,
							trips.indexOf( subtour.getTrips().get( 0 ) )),
						mode);
				if (lastDestination != null) {
					vehicleLocation = getLocationId( lastDestination );
				}
				if (vehicleLocation.equals(subtourStartLocation)) {
					usableChainBasedModes.add(mode);
				}
			}
			
			Set<String> usableModes = new HashSet<String>();
			if (isMassConserving(subtour)) { // We can only replace a subtour if it doesn't itself move a vehicle from one place to another
				if (subtour.getTrips().size() == 1) {
					
					usableModes.addAll(singleTripSubtourModes);
					
				}
				else {
				for (String candidate : permissibleModesForThisPerson) {
					
					{
						if (chainBasedModes.contains(candidate)) {
							if (usableChainBasedModes.contains(candidate)) {
								usableModes.add(candidate);
							}
						} else {
							usableModes.add(candidate);
						}
					} 
				}
			}
			}
			usableModes.remove(getTransportMode(subtour));
			
			
			for (String transportMode : usableModes) {
				choiceSet.add(
						new Candidate(
							subtour,
							transportMode ));
			}
		}
		return choiceSet;
	}

	private boolean containsUnknownMode(final Subtour subtour) {
		for (Trip trip : subtour.getTrips()) {
			if (!modes.contains( mainModeIdentifier.identifyMainMode( trip.getTripElements() ))) {
				return true;
			}
		}
		return false;
	}

	private boolean isMassConserving(final Subtour subtour) {
		for (String mode : chainBasedModes) {
			if (!isMassConserving(subtour, mode)) {
				return false;
			} 
		}
		return true;
	}

	private boolean isMassConserving(
			final Subtour subtour,
			final String mode) {
		final Activity firstOrigin =
			findFirstOriginOfMode(
					subtour.getTrips(),
					mode);

		if (firstOrigin == null) {
			return true;
		}

		final Activity lastDestination =
			findLastDestinationOfMode(
					subtour.getTrips(),
					mode);

		return atSameLocation(firstOrigin, lastDestination);
	}

	private Id getLocationId(Activity activity) {
		return anchorAtFacilities ?
			activity.getFacilityId() :
			activity.getLinkId();
	}
	
	private boolean atSameLocation(Activity firstLegUsingMode,
			Activity lastLegUsingMode) {
		return anchorAtFacilities ?
			firstLegUsingMode.getFacilityId().equals(
					lastLegUsingMode.getFacilityId() ) :
			firstLegUsingMode.getLinkId().equals(
					lastLegUsingMode.getLinkId() );
	}

	private Activity findLastDestinationOfMode(
			final List<Trip> tripsToSearch,
			final String mode) {
		final List<Trip> reversed = new ArrayList<Trip>( tripsToSearch );
		Collections.reverse( reversed );
		for (Trip trip : reversed) {
			if ( mode.equals( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) ) {
				return trip.getDestinationActivity();
			}
		}
		return null;
	}
	
	private Activity findFirstOriginOfMode(
			final List<Trip> tripsToSearch,
			final String mode) {
		for (Trip trip : tripsToSearch) {
			if ( mode.equals( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) ) {
				return trip.getOriginActivity();
			}
		}
		return null;
	}

	private String getTransportMode(final Subtour subtour) {
		return mainModeIdentifier.identifyMainMode(
				subtour.getTrips().get( 0 ).getTripElements() );
	}

	private void applyChange(
			final Candidate whatToDo,
			final Plan plan) {
		
		if (whatToDo.newTransportMode.equals("pt")) {
			for (Trip trip : whatToDo.subtour.getTrips()) {				
				
				List<? extends PlanElement> tripAAM = this.routingModuleAAM.calcRouteFromTLink((trip.getOriginActivity().getLinkId()),
						(trip.getDestinationActivity().getLinkId()), 3600, plan.getPerson());
				
				
				if (scorePTLeg(trip.getOriginActivity(), trip.getDestinationActivity()) > 
					scoreAAMTrip(tripAAM)) {
					TripRouter.insertTrip(
						plan,
						trip.getOriginActivity(),
						Collections.singletonList( new LegImpl( "pt" ) ),
						trip.getDestinationActivity());
				}
				else {
					TripRouter.insertTrip(
							plan,
							trip.getOriginActivity(),
							tripAAM,
							trip.getDestinationActivity());
				}
			}
			
		}
		else {
		
			for (Trip trip : whatToDo.subtour.getTrips()) {
				TripRouter.insertTrip(
						plan,
						trip.getOriginActivity(),
						Collections.singletonList( new LegImpl( whatToDo.newTransportMode ) ),
						trip.getDestinationActivity());
			}
		}
	}

	public void setAnchorSubtoursAtFacilitiesInsteadOfLinks(
			final boolean anchorAtFacilities) {
		this.anchorAtFacilities = anchorAtFacilities;
	}
	
	private double scorePTLeg(Activity originActivity, Activity destinationActivity) {
		double score = 0.0D;

		double travelTime = CoordUtils.calcEuclideanDistance(originActivity.getCoord(), destinationActivity.getCoord()) * this.beeLineFactor/ this.ptSpeed;
				
		score += travelTime * this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s;
		score += this.params.modeParams.get(TransportMode.pt).constant;

		return score;
		
		
	}
		
	private double scoreAAMTrip(List<? extends PlanElement> trip) {
		double score = 0.0D;
		double travelTime = 0.0;
		for(PlanElement pe: trip) {
			
			if (pe instanceof Leg) {
				
				travelTime += ((Leg) pe).getTravelTime();
				
				
			}
		}
		
	//	double travelTime = CoordUtils.calcDistance(originActivity.getCoord(), destinationActivity.getCoord()) * this.beeLineFactor / this.walkSpeed;
				
		score += travelTime * this.params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s;
		score += this.params.modeParams.get(TransportMode.walk).constant;

		return score;
		
		
	}

}
