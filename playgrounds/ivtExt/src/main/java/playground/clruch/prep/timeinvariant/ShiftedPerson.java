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

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/** @author Claudio Ruch */
public enum ShiftedPerson {
    ;

    public static Person of(Person oldPerson, Id<Person> newID, PopulationFactory populationFactory) {
        Person newPerson = populationFactory.createPerson(newID);
        Interval minMax = MinMaxTime.of(oldPerson);
        Double timediff = validTimeShiftFor(minMax);
        if (timediff == null)
            return null;

        // TODO attributes are lost in current implementation.

        for (Plan plan : oldPerson.getPlans()) {
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

        }
        return newPerson;
    }

    public static Double validTimeShiftFor(Interval minMax) {
        boolean timeShiftOk = false;
        int counter = 0;
        while (counter < 1000 && !timeShiftOk) {
            counter++;
            double timeDiff = TimeConstants.getRandomDayTimeShift();

            Tensor lbNew = minMax.getLb().add(Tensors.vector(timeDiff));
            Tensor ubNew = minMax.getUb().add(Tensors.vector(timeDiff));

            Interval iNew = new Interval(lbNew, ubNew);

            if (TimeConstants.getDayInterval().contains(iNew)) {
                return timeDiff;
            }
        }
        return null;
    }
}
