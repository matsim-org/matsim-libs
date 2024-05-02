package org.matsim.core.population.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.*;
import org.matsim.utils.objectattributes.attributable.AttributesComparison;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class PopulationComparison {

    private final static double DEFAULT_SCORE_DELTA = 1e-10;

    public enum Result {equal, notEqual}

    private static final Logger log = LogManager.getLogger(PopulationComparison.class);

    private PopulationComparison() {
    }

    public static Result compare(Population population1, Population population2) {
        return compare(population1, population2, DEFAULT_SCORE_DELTA);
    }


    public static Result compare(Population population1, Population population2, double scoreDelta) {
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
                if (Math.abs(plan1.getScore() - plan2.getScore()) > scoreDelta) {

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


    public static boolean equals(List<PlanElement> planElements,
                                 List<PlanElement> planElements2) {
        int nElements = planElements.size();
        if (nElements != planElements2.size()) {
            return false;
        } else {
            for (int i = 0; i < nElements; i++) {
                if (!equals(planElements.get(i), planElements2.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    /* Warning: This is NOT claimed to be correct. (It isn't.)
     *
     */
    private static boolean equals(PlanElement o1, PlanElement o2) {
        if (!AttributesComparison.equals(o1.getAttributes(), o2.getAttributes())) {
            return false;
        }
        if (o1 instanceof Leg) {
            if (o2 instanceof Leg) {
                Leg leg1 = (Leg) o1;
                Leg leg2 = (Leg) o2;
                if (!leg1.getDepartureTime().equals(leg2.getDepartureTime())) {
                    return false;
                }
                if (!leg1.getMode().equals(leg2.getMode())) {
                    return false;
                }
                if (!leg1.getTravelTime().equals(leg2.getTravelTime())) {
                    return false;
                }
            } else {
                return false;
            }
        } else if (o1 instanceof Activity) {
            if (o2 instanceof Activity) {
                Activity activity1 = (Activity) o1;
                Activity activity2 = (Activity) o2;
                if (activity1.getEndTime().isUndefined() ^ activity2.getEndTime().isUndefined()) {
                    return false;
                }
                if (activity1.getEndTime().isDefined() && activity1.getEndTime().seconds()
                        != activity2.getEndTime().seconds()) {
                    return false;
                }
                if (activity1.getStartTime().isUndefined() ^ activity2.getStartTime().isUndefined()) {
                    return false;
                }
                if (activity1.getStartTime().isDefined() && activity1.getStartTime().seconds()
                        != activity2.getStartTime().seconds()) {
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
