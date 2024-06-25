package org.matsim.core.population.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.AttributesComparison;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class PopulationComparison {

    private final static double DEFAULT_DELTA = 1e-10;

    public enum Result {equal, notEqual}

    private static final Logger log = LogManager.getLogger(PopulationComparison.class);

    private PopulationComparison() {
    }

    public static Result compare(Population population1, Population population2) {
        return compare(population1, population2, DEFAULT_DELTA);
    }


    public static Result compare(Population population1, Population population2, double delta) {
        Result result = Result.equal;

        Iterator<? extends Person> it1 = population1.getPersons().values().iterator();
        Iterator<? extends Person> it2 = population2.getPersons().values().iterator();

        while (it1.hasNext() || it2.hasNext()) {
            if (!it1.hasNext()) {
                result = Result.notEqual;
                log.warn("");
                log.warn(" different length in populations. ");
                return result;
            }
            if (!it2.hasNext()) {
                result = Result.notEqual;
                log.warn("");
                log.warn(" different length in populations. ");
                return result;
            }

            Person person1 = it1.next();
            Person person2 = it2.next();

            if (!person1.getId().equals(person2.getId())) {
                log.warn("");
                log.warn("persons out of sequence p1: " + person1.getId() + " | p2: " + person2.getId());
                result = Result.notEqual;
                continue;
            }

            if (!AttributesComparison.equals(person1.getAttributes(), person2.getAttributes())) {
                log.warn("");
                log.warn("person attributes different p1: " + person1.getId() + " | p2: " + person2.getId());
            }

            Plan plan1 = person1.getSelectedPlan();
            Plan plan2 = person2.getSelectedPlan();

            if (!AttributesComparison.equals(plan1.getAttributes(), plan2.getAttributes())) {
                log.warn("");
                log.warn("selected plan attributes different p1: " + person1.getId() + " | p2: " + person2.getId());
            }

            Optional<Double> score1 = Optional.ofNullable(plan1.getScore());
            Optional<Double> score2 = Optional.ofNullable(plan2.getScore());

            if (score1.isPresent() && score2.isPresent()) {
                if (!isWithinDelta(plan1.getScore(), plan2.getScore(), delta)) {

                    double maxScore = Double.NEGATIVE_INFINITY;
                    for (Plan plan : person2.getPlans()) {
                        if (plan.getScore() > maxScore) {
                            maxScore = plan.getScore();
                        }
                    }

                    log.warn("");
                    log.warn("personId=" + person1.getId() + "; score1=" + plan1.getScore() + "; score2=" + plan2.getScore() + "; maxScore2=" + maxScore);
                    log.warn("");

                    result = Result.notEqual;

                }
            } else if (score1.isEmpty() && score2.isEmpty()) {
            } else {
                log.warn("");
                log.warn(" selected plan scores not consistently present: p1: " + person1.getId() + " | p2: " + person2.getId());
                result = Result.notEqual;
            }

            if (!equals(plan1.getPlanElements(), plan2.getPlanElements())) {
                log.warn("");
                log.warn(" selected plan elements not equal: p1: " + person1.getId() + " | p2: " + person2.getId());

                for (PlanElement planElement : plan1.getPlanElements()) {
                    log.warn(planElement);
                }
                log.warn("");
                for (PlanElement planElement : plan2.getPlanElements()) {
                    log.warn(planElement);
                }
                log.warn("");
                result = Result.notEqual;
            }
        }
        return result;
    }

    private static boolean isWithinDelta(double double1, double double2, double delta) {
        return Math.abs(double1 - double2) < delta;
    }

    private static boolean isBothUndefinedOrWithinDelta(OptionalTime time1, OptionalTime time2, double delta) {
        if (time1.isUndefined() ^ time2.isUndefined()) {
            return false;
        } else if(time1.isDefined()) {
            return isWithinDelta((int) time1.seconds(), (int) time2.seconds(), delta);
        }
        return true;
    }

    public static boolean equals(List<PlanElement> planElements,
                                 List<PlanElement> planElements2) {
        return equals(planElements, planElements2, DEFAULT_DELTA);
    }

    public static boolean equals(List<PlanElement> planElements,
                                 List<PlanElement> planElements2, double delta) {
        int nElements = planElements.size();
        if (nElements != planElements2.size()) {
            return false;
        } else {
            for (int i = 0; i < nElements; i++) {
                if (!equals(planElements.get(i), planElements2.get(i), delta)) {
                    return false;
                }
            }
        }
        return true;
    }

    /* Warning: This is NOT claimed to be correct. (It isn't.)
     *
     */
    private static boolean equals(PlanElement o1, PlanElement o2, double delta) {
        if (!AttributesComparison.equals(o1.getAttributes(), o2.getAttributes())) {
            return false;
        }
        if (o1 instanceof Leg leg1) {
            if (o2 instanceof Leg leg2) {
                if(!isBothUndefinedOrWithinDelta(leg1.getDepartureTime(), leg2.getDepartureTime(), delta)) {
                    return false;
                }
                if (!leg1.getMode().equals(leg2.getMode())) {
                    return false;
                }
                if (!isBothUndefinedOrWithinDelta(leg1.getTravelTime(), leg2.getTravelTime(), delta)) {
                    return false;
                }
            } else {
                return false;
            }
        } else if (o1 instanceof Activity activity1) {
            if (o2 instanceof Activity activity2) {
                if (!isBothUndefinedOrWithinDelta(activity1.getEndTime(), activity2.getEndTime(), delta)) {
                    return false;
                }
                if (!isBothUndefinedOrWithinDelta(activity1.getStartTime(), activity2.getStartTime(), delta)) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            throw new RuntimeException("Unexpected PlanElement");
        }
        return true;
    }
}
