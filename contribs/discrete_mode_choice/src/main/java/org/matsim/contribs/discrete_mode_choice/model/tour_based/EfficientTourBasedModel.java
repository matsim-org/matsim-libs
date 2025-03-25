package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.components.constraints.VehicleTourConstraint;
import org.matsim.contribs.discrete_mode_choice.components.estimators.CumulativeTourEstimator;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.components.utils.LocationUtils;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.CompositeTourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.constraints.CompositeTourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.constraints.TourFromTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGeneratorFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import java.util.*;
import java.util.stream.Collectors;


/**
 * This is an alternative implementation of the {@link TourBasedModel} feature that aims to be more computationally efficient.
 * The default implementations follows 5 steps:
 * - 1 - enumerates all possible mode chains for a tour without routing the trips
 * - 2 - checks 'before estimation' constraints to filter out some alternatives
 * - 3 - performs the routing and the estimation of the tour
 * - 4 - checks 'after estimation' constraints to filter out more alternatives
 * - 5 - select the 'best' tour alternative according to the specified selector
 * The main drawback if the approach is that the explicit and independent enumeration of possible alternatives.
 * Alternatives with the same modes for the first n-trips are not explicitly linked (even though the routing cache will prevent routing the same trip twice with the same mode)
 *
 *
 * In this class, a tree-based approach is followed. We start from the origin and keep expanding the tree, each time with the set of possible modes until we reach the destination in every leaf.
 * At each node, we only expand with the modes that do not violate the constraints, this way, we are able to quickly abandon alternatives that would violate a {@link TripConstraint} or a {@link VehicleTourConstraint}.
 * When we reach the leaves, we then just verify the remaining {@link TourConstraint} items.
 */

public class EfficientTourBasedModel implements DiscreteModeChoiceModel {
    final private static Logger logger = LogManager.getLogger(EfficientTourBasedModel.class);

    final private TourFinder tourFinder;
    final private TourFilter tourFilter;
    final private CumulativeTourEstimator estimator;
    final private ModeAvailability modeAvailability;
    final private CompositeTourConstraintFactory constraintFactory;
    final private UtilitySelectorFactory selectorFactory;
	final private FallbackBehaviour fallbackBehaviour;
    final private TimeInterpretation timeInterpretation;

    public EfficientTourBasedModel(CumulativeTourEstimator estimator, ModeAvailability modeAvailability,
								   CompositeTourConstraintFactory constraintFactory, TourFinder tourFinder, TourFilter tourFilter,
                                   UtilitySelectorFactory selectorFactory, ModeChainGeneratorFactory modeChainGeneratorFactory,
                                   FallbackBehaviour fallbackBehaviour, TimeInterpretation timeInterpretation) {
        this.estimator = estimator;
        this.modeAvailability = modeAvailability;
        this.constraintFactory = constraintFactory;
        this.tourFinder = tourFinder;
        this.tourFilter = tourFilter;
        this.selectorFactory = selectorFactory;
		this.fallbackBehaviour = fallbackBehaviour;
        this.timeInterpretation = timeInterpretation;
    }

    @Override
    public List<TripCandidate> chooseModes(Person person, List<DiscreteModeChoiceTrip> trips, Random random) throws NoFeasibleChoiceException {
        List<String> modes = new ArrayList<>(modeAvailability.getAvailableModes(person, trips));
        CompositeTourConstraint constraint = constraintFactory.createConstraint(person, trips, modes);

        List<TourCandidate> tourCandidates = new LinkedList<>();

        int tripIndex = 1;
        TimeTracker timeTracker = new TimeTracker(timeInterpretation);

        for (List<DiscreteModeChoiceTrip> tourTrips : tourFinder.findTours(trips)) {
            timeTracker.addActivity(tourTrips.getFirst().getOriginActivity());

            // We pass the departure time through the first origin activity
            tourTrips.getFirst().setDepartureTime(timeTracker.getTime().seconds());

            TourCandidate finalTourCandidate = null;

            if (tourFilter.filter(person, tourTrips)) {
				ModeChoiceModelTree modeChoiceModelTree = new ModeChoiceModelTree(person, tourTrips, constraint, estimator.getDelegate(), modes, tourCandidates, timeInterpretation);
                UtilitySelector selector = selectorFactory.createUtilitySelector();
                modeChoiceModelTree.build();
                for(TourCandidate tourCandidate: modeChoiceModelTree.getTourCandidates()) {
                    selector.addCandidate(tourCandidate);
                }
                Optional<UtilityCandidate> selectedCandidate = selector.select(random);

                if (selectedCandidate.isEmpty()) {
                    switch (fallbackBehaviour) {
                        case INITIAL_CHOICE:
                            logger.warn(
                                    buildFallbackMessage(tripIndex, person, "Setting tour modes back to initial choice."));
                            selectedCandidate = Optional.of(createFallbackCandidate(person, tourTrips, tourCandidates));
                            break;
                        case IGNORE_AGENT:
                            return handleIgnoreAgent(tripIndex, person, tourTrips);
                        case EXCEPTION:
                            throw new NoFeasibleChoiceException(buildFallbackMessage(tripIndex, person, ""));
                    }
                }

                finalTourCandidate = (TourCandidate) selectedCandidate.get();
            } else {
                finalTourCandidate = createFallbackCandidate(person, tourTrips, tourCandidates);
            }

            tourCandidates.add(finalTourCandidate);

            tripIndex += tourTrips.size();

            for (int i = 0; i < tourTrips.size(); i++) {
                if (i > 0) { // Our time object is already at the end of the first activity
                    timeTracker.addActivity(tourTrips.get(i).getOriginActivity());
                }

                timeTracker.addDuration(finalTourCandidate.getTripCandidates().get(i).getDuration());
            }
        }

        return createTripCandidates(tourCandidates);
    }

    private TourCandidate createFallbackCandidate(Person person, List<DiscreteModeChoiceTrip> tourTrips,
                                                  List<TourCandidate> tourCandidates) {
        List<String> initialModes = tourTrips.stream().map(DiscreteModeChoiceTrip::getInitialMode)
                .collect(Collectors.toList());
        return estimator.estimateTour(person, initialModes, tourTrips, tourCandidates);
    }

    private List<TripCandidate> createTripCandidates(List<TourCandidate> tourCandidates) {
        return tourCandidates.stream().map(TourCandidate::getTripCandidates).flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<TripCandidate> handleIgnoreAgent(int tripIndex, Person person, List<DiscreteModeChoiceTrip> trips) {
        List<TourCandidate> tourCandidates = new LinkedList<>();

        for (List<DiscreteModeChoiceTrip> tourTrips : tourFinder.findTours(trips)) {
            List<String> tourModes = tourTrips.stream().map(DiscreteModeChoiceTrip::getInitialMode)
                    .collect(Collectors.toList());
            tourCandidates.add(estimator.estimateTour(person, tourModes, tourTrips, tourCandidates));
        }

        logger.warn(buildFallbackMessage(tripIndex, person, "Setting whole plan back to initial modes."));
        return createTripCandidates(tourCandidates);
    }

    private String buildFallbackMessage(int tripIndex, Person person, String appendix) {
        return String.format("No feasible mode choice candidate for tour starting at trip %d of agent %s. %s",
                tripIndex, person.getId().toString(), appendix);
    }

    private String buildIllegalUtilityMessage(int tripIndex, Person person, TourCandidate candidate) {
        TripCandidate trip = candidate.getTripCandidates().get(tripIndex);

        return String.format(
                "Received illegal utility for for tour starting at trip %d (%s) of agent %s. Continuing with next candidate.",
                tripIndex, trip.getMode(), person.getId().toString());
    }

    public static class ModeChoiceModelTree {
        public List<DiscreteModeChoiceTrip> getTourTrips() {
            return tourTrips;
        }

        public List<TourConstraint> getTourConstraints() {
            return tourConstraints;
        }

        public List<TripConstraint> getTripConstraints() {
            return tripConstraints;
        }

        public TripEstimator getTripEstimator() {
            return tripEstimator;
        }

        public Collection<String> getModes() {
            return modes;
        }

        private final List<DiscreteModeChoiceTrip> tourTrips;
        private final List<TourConstraint> tourConstraints;
        private final List<TripConstraint> tripConstraints;
        private final TripEstimator tripEstimator;
        private final Collection<String> modes;
        private final List<TourCandidate> previousTourCandidates;
        private final Person person;
        private final TimeInterpretation timeInterpretation;
        private ModeChoiceModelTreeNode root;
        private final Set<String> restrictedModes = new HashSet<>();
        private Id<? extends BasicLocation> vehicleLocationId;

        public List<TourCandidate> getPreviousTourCandidates() {
            return previousTourCandidates;
        }

        public Person getPerson() {
            return person;
        }

        public TimeInterpretation getTimeInterpretation() {
            return this.timeInterpretation;
        }

        public ModeChoiceModelTree(Person person, List<DiscreteModeChoiceTrip> tourTrips, CompositeTourConstraint tourConstraint, TripEstimator tripEstimator, Collection<String> modes, List<TourCandidate> previousTourCandidates, TimeInterpretation timeInterpretation) {
            this.person = person;
            this.tourTrips = tourTrips;
            this.previousTourCandidates = previousTourCandidates;
            this.tourConstraints = new ArrayList<>();
            this.tripEstimator = tripEstimator;
            this.modes = modes;
            this.tripConstraints = new ArrayList<>();
            for(TourConstraint innerTourConstraint: tourConstraint.getConstraints()) {
                if(innerTourConstraint instanceof TourFromTripConstraint tourFromTripConstraint) {
                    this.tripConstraints.add(tourFromTripConstraint.getConstraint());
                } else if (innerTourConstraint instanceof VehicleTourConstraint vehicleTourConstraint){
                    if(this.vehicleLocationId != null) {
                        throw new IllegalStateException("Two EqasimVehicleTourConstraints");
                    }
                    this.restrictedModes.addAll(vehicleTourConstraint.getRestrictedModes());
                    this.vehicleLocationId = vehicleTourConstraint.getVehicleLocationId();
                } else {
                    this.tourConstraints.add(innerTourConstraint);
                }
            }
            this.timeInterpretation = timeInterpretation;
        }

        public void build() {
            TimeTracker timeTracker = new TimeTracker(timeInterpretation);
            timeTracker.setTime(this.tourTrips.getFirst().getDepartureTime());
            this.root = new ModeChoiceModelTreeNode(this, this.previousTourCandidates.stream().flatMap(tourCandidate -> tourCandidate.getTripCandidates().stream()).toList(), new ArrayList<>(), this.tourTrips, timeTracker, this.modes, 0, new HashMap<>());
            root.expand();
        }

        public List<TourCandidate> getTourCandidates() {
            return this.root.getTourCandidates();
        }
    }
    public static class ModeChoiceModelTreeNode {
        private final List<TripCandidate> allPreviousTrips;
        private final List<TripCandidate> currentTourPreviousTrips;
        private final List<DiscreteModeChoiceTrip> remainingTrips;
        private final TimeTracker currentTimeTracker;
        private final Collection<String> modes;
        private final double currentUtility;
        private final ModeChoiceModelTree tree;
        private final Map<String, Id<? extends BasicLocation>> currentVehicleLocations;
        private final Collection<ModeChoiceModelTreeNode> children;
        private TourCandidate tourCandidate;

        public ModeChoiceModelTreeNode(ModeChoiceModelTree tree, List<TripCandidate> allPreviousTrips, List<TripCandidate> currentTourPreviousTrips, List<DiscreteModeChoiceTrip> remainingTrips, TimeTracker currentTimeTracker, Collection<String> modes, double currentUtility, Map<String, Id<? extends BasicLocation>> currentVehicleLocations) {
            this.allPreviousTrips = allPreviousTrips;
            this.currentTourPreviousTrips = currentTourPreviousTrips;
            this.remainingTrips = remainingTrips;
            this.currentUtility = currentUtility;
            this.currentTimeTracker = currentTimeTracker;
            this.modes = modes;
            this.tree = tree;
            this.children = new ArrayList<>();
            this.tourCandidate = null;
            this.currentVehicleLocations = currentVehicleLocations;
        }

        public boolean expand() {
            this.children.clear();
			// No trip remains, the actual node is actually a leave
            if(this.remainingTrips.isEmpty()) {
                return true;
            }
            DiscreteModeChoiceTrip currentTrip = this.remainingTrips.getFirst();
            this.currentTimeTracker.addActivity(currentTrip.getOriginActivity());
            currentTrip.setDepartureTime(currentTimeTracker.getTime().seconds());

			List<String> allPreviousModes = this.allPreviousTrips.stream().map(TripCandidate::getMode).toList();
            List<String> currentTourPreviousModes = this.currentTourPreviousTrips.stream().map(TripCandidate::getMode).toList();

            for(String mode: modes)  {
                Map<String, Id<? extends BasicLocation>> vehiclesLocations = new HashMap<>(this.currentVehicleLocations);
				Id<? extends BasicLocation> currentTripOriginLocationId = LocationUtils.getLocationId(currentTrip.getOriginActivity());

                if(this.tree.restrictedModes.contains(mode)) {
					// If the current mode is concerned by a vehicle tour constraint
                    if(!currentTourPreviousModes.isEmpty()) {
                        if(!currentTripOriginLocationId.equals(this.currentVehicleLocations.get(mode))) {
                            continue;
                        }

                    }
                    vehiclesLocations.put(mode, LocationUtils.getLocationId(currentTrip.getDestinationActivity()));
                }
				// Checking trip constraints for the current trip
                if(this.tree.tripConstraints.stream().anyMatch(tripConstraint -> !tripConstraint.validateBeforeEstimation(currentTrip, mode, allPreviousModes))) {
                    continue;
                }

                List<DiscreteModeChoiceTrip> remainingTrips = new ArrayList<>(this.remainingTrips);
                remainingTrips.removeFirst();
                if(remainingTrips.isEmpty()) {
                    boolean breakingVehicleContinuity = false;
                    for(String restrictedMode: this.tree.restrictedModes) {
						// If it is the last trip, we make sure that all vehicles are where they are supposed to be at the end of the day
                        Id<? extends BasicLocation> lastVehicleLocation = vehiclesLocations.get(restrictedMode);
                        if(lastVehicleLocation != null && !lastVehicleLocation.equals(LocationUtils.getLocationId(currentTrip.getDestinationActivity())) && !lastVehicleLocation.equals(tree.vehicleLocationId)) {
                            breakingVehicleContinuity = true;
                            break;
                        }
                    }
                    if(breakingVehicleContinuity) {
                        continue;
                    }
                }
                TripCandidate tripCandidate = this.tree.getTripEstimator().estimateTrip(this.tree.getPerson(), mode, currentTrip, this.allPreviousTrips);
                TimeTracker timeTracker = new TimeTracker(this.tree.getTimeInterpretation());
                timeTracker.setTime(currentTimeTracker.getTime().seconds());
                timeTracker.addDuration(tripCandidate.getDuration());
                double utility = currentUtility + tripCandidate.getUtility();
                if(this.tree.tripConstraints.stream().anyMatch(tripConstraint -> !tripConstraint.validateAfterEstimation(currentTrip, tripCandidate, allPreviousTrips))) {
                    continue;
                }
                List<TripCandidate> allPreviousTrips = new ArrayList<>(this.allPreviousTrips);
                allPreviousTrips.add(tripCandidate);
                List<TripCandidate> currentTourPreviousTrips = new ArrayList<>(this.currentTourPreviousTrips);
                currentTourPreviousTrips.add(tripCandidate);
                List<String> newPreviousModes = currentTourPreviousTrips.stream().map(TripCandidate::getMode).toList();
                ModeChoiceModelTreeNode child = new ModeChoiceModelTreeNode(this.tree, allPreviousTrips, currentTourPreviousTrips, remainingTrips, timeTracker, this.modes, utility, vehiclesLocations);

				if(remainingTrips.isEmpty()) {
                    child.tourCandidate = new DefaultTourCandidate(utility, currentTourPreviousTrips);
                    if(this.tree.tourConstraints.stream().anyMatch(tourConstraint ->
                    {
                        if(!tourConstraint.validateBeforeEstimation(tree.tourTrips, newPreviousModes, this.tree.previousTourCandidates.stream().map(tourCandidate -> tourCandidate.getTripCandidates().stream().map(TripCandidate::getMode).toList()).toList())) {
                            return true;
                        }
						return !tourConstraint.validateAfterEstimation(tree.tourTrips, child.tourCandidate, this.tree.previousTourCandidates);
					})) {
                        continue;
                    }

                }
                if(child.expand()) {
                    this.children.add(child);
                }
            }
            return !children.isEmpty();
        }

        public List<TourCandidate> getTourCandidates() {
            List<TourCandidate> tourCandidates = new ArrayList<>();
            getTourCandidates(tourCandidates);
            return tourCandidates;
        }

        private void getTourCandidates(List<TourCandidate> candidatesList) {
            if(this.tourCandidate == null) {
                this.children.forEach(child -> child.getTourCandidates(candidatesList));
            } else {
                candidatesList.add(this.tourCandidate);
            }
        }
    }

}
