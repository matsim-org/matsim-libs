package org.matsim.contrib.ev.strategic.replanning.innovator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.reservation.ChargerReservability;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider.ChargerRequest;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlanActivity;
import org.matsim.contrib.ev.strategic.replanning.innovator.chargers.ChargerSelector;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.ActivityBasedCandidate;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.LegBasedCandidate;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.common.base.Preconditions;

public class InnovationHelper {
    private final TimeInterpretation timeInterpretation;
    private final ChargingSlotFinder candidateFinder;
    private final ChargerReservability reservability;

    protected InnovationHelper(Plan plan, List<Activity> activities, Map<Activity, Double> startTimes,
            Map<Activity, Double> endTimes, TimeInterpretation timeInterpretation, ChargingSlotFinder candidateFinder,
            ChargerReservability reservability) {
        this.plan = plan;
        this.activities = activities;
        this.startTimes = startTimes;
        this.endTimes = endTimes;
        this.timeInterpretation = timeInterpretation;
        this.candidateFinder = candidateFinder;
        this.reservability = reservability;
    }

    // INDEXING AND TIMING FUNCTIONALITY

    private final List<Activity> activities;
    private final Map<Activity, Double> startTimes;
    private final Map<Activity, Double> endTimes;

    public int startActivityIndex(ActivityBasedCandidate candidate) {
        return activities.indexOf(candidate.startActivity());
    }

    public int endActivityIndex(ActivityBasedCandidate candidate) {
        return activities.indexOf(candidate.endActivity());
    }

    public double startTime(ActivityBasedCandidate candidate) {
        return startTimes.get(candidate.startActivity());
    }

    public double endTime(ActivityBasedCandidate candidate) {
        return endTimes.get(candidate.endActivity());
    }

    public double startTime(LegBasedCandidate candidate) {
        return endTimes.get(precedingActivity(candidate));
    }

    public double endTime(LegBasedCandidate candidate) {
        return startTimes.get(candidate.followingActivity());
    }

    public double duration(ActivityBasedCandidate candidate) {
        return endTime(candidate) - startTime(candidate);
    }

    public int followingActivityIndex(LegBasedCandidate candidate) {
        return activities.indexOf(candidate.followingActivity());
    }

    public int precedingActivityIndex(LegBasedCandidate candidate) {
        return followingActivityIndex(candidate) - 1;
    }

    public Activity precedingActivity(LegBasedCandidate candidate) {
        return activities.get(precedingActivityIndex(candidate));
    }

    // FILTERING FUNCTIONALITY

    public void filterByDuration(List<ActivityBasedCandidate> candidates, double minimumActivityChargingDuration,
            double maximumActivityChargingDuration) {
        candidates.removeIf(candidate -> {
            double duration = duration(candidate);
            return duration < minimumActivityChargingDuration || duration > maximumActivityChargingDuration;
        });
    }

    public void filterByDriveTime(List<LegBasedCandidate> candidates, double minimumDriveTime) {
        candidates.removeIf(candidate -> {
            return timeInterpretation.decideOnLegTravelTime(candidate.leg()).seconds() < minimumDriveTime;
        });
    }

    // PLAN CONSTRUCTION FUNCTIONALITY

    private final ChargingPlan chargingPlan = new ChargingPlan();

    private final List<ActivityBasedCandidate> selectedActivityBased = new LinkedList<>();
    private final List<LegBasedCandidate> selectedLegBased = new LinkedList<>();

    private boolean calledActivityBasedSearch = false;
    private boolean calledLegBasedSearch = false;

    private final Plan plan;

    /**
     * Makes sure that compatibility with previously selected leg-based candidates
     * is checked.
     */
    public List<ActivityBasedCandidate> findActivityBased() {
        Preconditions.checkState(!calledActivityBasedSearch);
        calledActivityBasedSearch = true;

        List<ActivityBasedCandidate> candidates = candidateFinder.findActivityBased(plan.getPerson(), plan);

        if (selectedLegBased.size() > 0) {
            candidateFinder.reduceActivityBased(candidates, selectedLegBased, plan.getPlanElements());
        }

        return candidates;
    }

    /**
     * Makes sure that compatibility with previously selected activity-based
     * candidates is checked.
     */
    public List<LegBasedCandidate> findLegBased() {
        Preconditions.checkState(!calledLegBasedSearch);
        calledLegBasedSearch = true;

        List<LegBasedCandidate> candidates = candidateFinder.findLegBased(plan.getPerson(), plan);

        if (selectedActivityBased.size() > 0) {
            candidateFinder.reduceLegBased(candidates, selectedActivityBased, plan.getPlanElements());
        }

        return candidates;
    }

    public boolean push(ActivityBasedCandidate candidate, ChargerSpecification charger, boolean withReservation) {
        if (charger == null) {
            return false;
        }

        selectedActivityBased.add(candidate);

        chargingPlan.addChargingActivity(
                new ChargingPlanActivity(startActivityIndex(candidate), endActivityIndex(candidate),
                        charger.getId(), withReservation));

        return true;
    }

    public boolean push(LegBasedCandidate candidate, double duration, ChargerSpecification charger,
            boolean withReservation) {
        if (charger == null) {
            return false;
        }

        selectedLegBased.add(candidate);

        chargingPlan.addChargingActivity(new ChargingPlanActivity(followingActivityIndex(candidate),
                duration,
                charger.getId(), withReservation));

        return true;
    }

    public ChargingPlan getChargingPlan() {
        return chargingPlan;
    }

    // CHARGER SELECTION

    public ChargerSpecification selectCharger(ActivityBasedCandidate slot, ChargerProvider chargerProvider,
            ChargerSelector selector, boolean withReservation) {
        List<ChargerSpecification> chargers = new LinkedList<>(
                chargerProvider.findChargers(plan.getPerson(), plan,
                        new ChargerRequest(slot.startActivity(), slot.endActivity())));

        double startTime = startTime(slot);
        double endTime = endTime(slot);

        if (withReservation) {
            chargers.removeIf(charger -> {
                return !reservability.isReservable(charger, startTime, endTime);
            });
        }

        if (chargers.size() > 0) {
            return selector.select(slot, chargers);
        } else {
            return null;
        }
    }

    public ChargerSpecification selectCharger(LegBasedCandidate slot, double duration, ChargerProvider chargerProvider,
            ChargerSelector selector, boolean withReservation) {
        List<ChargerSpecification> chargers = new LinkedList<>(
                chargerProvider.findChargers(plan.getPerson(), plan,
                        new ChargerRequest(slot.leg(), duration)));

        double startTime = startTime(slot);
        double endTime = endTime(slot);

        if (withReservation) {
            chargers.removeIf(charger -> {
                return !reservability.isReservable(charger, startTime, endTime);
            });
        }

        if (chargers.size() > 0) {
            return selector.select(slot, duration, chargers);
        } else {
            return null;
        }
    }

    static public InnovationHelper build(Plan plan, TimeInterpretation timeInterpretation,
            ChargingSlotFinder candidateFinder, ChargerReservability reservability) {
        List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(),
                StageActivityHandling.ExcludeStageActivities);
        Map<Activity, Double> startTimes = new HashMap<>();
        Map<Activity, Double> endTimes = new HashMap<>();

        TimeTracker timeTracker = new TimeTracker(timeInterpretation);
        for (PlanElement element : plan.getPlanElements()) {
            double startTime = timeTracker.getTime().seconds();
            timeTracker.addElement(element);

            if (element instanceof Activity activity) {
                if (!TripStructureUtils.isStageActivityType(activity.getType())) {
                    if (activity == activities.get(0)) {
                        startTime = Double.NEGATIVE_INFINITY;
                    }

                    final double endTime;
                    if (activity == activities.get(activities.size() - 1)) {
                        endTime = Double.POSITIVE_INFINITY;
                    } else {
                        endTime = timeTracker.getTime().orElse(Double.POSITIVE_INFINITY);
                    }

                    startTimes.put(activity, startTime);
                    endTimes.put(activity, endTime);
                }
            }
        }

        return new InnovationHelper(plan, activities, startTimes, endTimes, timeInterpretation, candidateFinder,
                reservability);
    }
}
