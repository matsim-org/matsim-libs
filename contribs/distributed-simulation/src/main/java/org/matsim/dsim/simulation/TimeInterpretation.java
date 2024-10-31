package org.matsim.dsim.simulation;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;

public class TimeInterpretation {

    private final PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation;
    private final boolean onlyAdvance;

    @Inject
    public TimeInterpretation(Config config) {
        this.activityDurationInterpretation = config.plans().getActivityDurationInterpretation();
        onlyAdvance = switch (config.plans().getTripDurationHandling()) {
            case ignoreDelays -> false;
            case shiftActivityEndTimes -> true;
        };
    }

    public double getActivityEndTime(Activity activity, double startTime) {
        return switch (activityDurationInterpretation) {
            case tryEndTimeThenDuration -> tryEndTimeThenDuration(activity, startTime);
            case minOfDurationAndEndTime -> minDurationAndEndTime(activity, startTime);
            default ->
                    throw new RuntimeException("Activity duration interpretation: " + activityDurationInterpretation + " is not supported.");
        };
    }

    private double tryEndTimeThenDuration(Activity activity, double startTime) {
        if (activity.getEndTime().isDefined()) {
            return checkAdvance(activity.getEndTime().seconds(), startTime);
        }
        if (activity.getMaximumDuration().isDefined()) {
            return startTime + activity.getMaximumDuration().seconds();
        }
        return Double.POSITIVE_INFINITY;
    }

    private double minDurationAndEndTime(Activity activity, double startTime) {

        if (activity.getEndTime().isDefined() && activity.getMaximumDuration().isDefined()) {
            double durationBasedEndTime = startTime + activity.getMaximumDuration().seconds();
            double endTime = Math.min(activity.getEndTime().seconds(), durationBasedEndTime);
            return checkAdvance(endTime, startTime);
        }
        if (!activity.getMaximumDuration().isDefined()) {
            return checkAdvance(activity.getEndTime().seconds(), startTime);
        }
        if (!activity.getEndTime().isDefined()) {
            return startTime + activity.getMaximumDuration().seconds();
        }

        return Double.POSITIVE_INFINITY;
    }

    private double checkAdvance(double endTime, double startTime) {
        if (Double.isFinite(endTime) && onlyAdvance) return Math.max(endTime, startTime);
        return endTime;
    }
}
