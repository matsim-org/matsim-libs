/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;

/** @author Claudio Ruch */
public enum ShiftedPerson {
    ;

    public static Person of(Person randomP, double timediff, //
            Id<Person> newID, PopulationFactory populationFactory) {
        Person newPerson = populationFactory.createPerson(newID);
        randomP.getPlans().stream().forEach(p -> newPerson.addPlan(p));

        // TODO attributes are lost in current implementation.

        for (Plan plan : randomP.getPlans()) {
            Plan planShifted = populationFactory.createPlan();
            planShifted.setPerson(newPerson);
            planShifted.setScore(plan.getScore());
            planShifted.setType(plan.getType());

            for (PlanElement pE : plan.getPlanElements()) {
                if (pE instanceof Activity) {
                    Activity actOld = (Activity) pE;
                    Activity actNew = populationFactory.createActivityFromCoord(actOld.getType(), actOld.getCoord());
                    actNew.setStartTime(actOld.getStartTime() + timediff);
                    actNew.setEndTime(actOld.getEndTime() + timediff);
                    actNew.setLinkId(actOld.getLinkId());
                    actNew.setFacilityId(actOld.getFacilityId());
                    planShifted.addActivity(actNew);
                }
                if (pE instanceof Leg) {
                    Leg leg = (Leg) pE;
                    leg.setDepartureTime(leg.getDepartureTime() + timediff);
                    planShifted.addLeg(leg);
                }
            }
            newPerson.addPlan(planShifted);
            newPerson.setSelectedPlan(planShifted);
            newPerson.removePlan(plan);
        }
        return newPerson;
    }

    public static void printPerson(Person person) {
        for (Plan plan : person.getPlans()) {
            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Leg) {
                    Leg leg = (Leg) planElement;
                }
                if (planElement instanceof Activity) {
                    Activity act = (Activity) planElement;
                }
            }
        }
    }
}
