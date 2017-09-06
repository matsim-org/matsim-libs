package playground.clruch.trb18.scenario.stages;

import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.pt.PtConstants;

public class TRBPlanModifier {
    final private Network filteredNetwork;
    final private boolean allowMultimodalPlans;

    public TRBPlanModifier(Network filteredNetwork, boolean allowMultimodalPlans) {
        this.filteredNetwork = filteredNetwork;
        this.allowMultimodalPlans = allowMultimodalPlans;
    }

    /**
     * Checks if a trip can be converted to AV:
     * - if it is either car or pt
     * - AND if it is within the operating (filtered) network
     */
    private boolean canBeConvertedToAV(TripStructureUtils.Trip trip) {
        if (isCarTrip(trip) || isPtTrip(trip)) {
            if (filteredNetwork.getLinks().containsKey(trip.getOriginActivity().getLinkId()) && filteredNetwork.getLinks().containsKey(trip.getDestinationActivity().getLinkId())) {
                return true;
            }
        }

        return false;
    }

    private void validateTrip(TripStructureUtils.Trip trip) {
        if (trip.getLegsOnly().size() > 1) throw new RuntimeException();
        if (trip.getLegsOnly().get(0).getMode().equals("transit_walk")) throw new RuntimeException();
    }

    private boolean isCarTrip(TripStructureUtils.Trip trip) {
        return trip.getLegsOnly().get(0).getMode().equals("car");
    }

    private boolean isPtTrip(TripStructureUtils.Trip trip) {
        return trip.getLegsOnly().get(0).getMode().equals("pt");
    }

    /**
     * Checks whether an AV leg can be incorporated into a plan
     * - If the agent uses car: All car legs must be within operating area, otherwise no conversion!
     * - If the agent does not use car: There must be at least one PT leg within the operating area, otherwise no conversion!
     * - Otherwise no conversion!
     *
     * - If allowMultimodalPlans == false: Only allow plans where ALL trips can be converted to AV
     */
    public boolean canUseAVs(Plan plan) {
        List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));

        long numberOfTrips = 0;
        long numberOfCarTrips = 0;
        long numberOfConvertableCarTrips = 0;
        long numberOfConvertablePtTrips = 0;

        for (TripStructureUtils.Trip trip : trips) {
            validateTrip(trip);

            if (isCarTrip(trip)) {
                numberOfCarTrips++;

                if (canBeConvertedToAV(trip)) {
                    numberOfConvertableCarTrips++;
                }
            }

            if (isPtTrip(trip) && canBeConvertedToAV(trip)) {
                numberOfConvertablePtTrips++;
            }

            numberOfTrips++;
        }

        if (!allowMultimodalPlans) {
            return numberOfTrips == numberOfConvertableCarTrips + numberOfConvertablePtTrips;
        } else {
            if (numberOfCarTrips > 0) {
                return numberOfCarTrips == numberOfConvertableCarTrips;
            }

            return numberOfConvertablePtTrips > 0;
        }
    }

    /**
     * Modifies the trips in a plan
     * - modified all trips that can be converted according to canBeConvertedToAv
     * - does NOT check whether the whole plan is eligible (check with canUseAv before!)
     */
    public Plan createModifiedPlan(Plan plan) {
        Plan duplicate = PlanUtils.createCopy(plan);

        List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(duplicate, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));

        for (TripStructureUtils.Trip trip : trips) {
            if (canBeConvertedToAV(trip)) {
                Leg leg = trip.getLegsOnly().get(0);
                leg.setMode("av");
                leg.setRoute(null);
            }
        }

        return duplicate;
    }
}
