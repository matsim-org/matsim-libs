package org.matsim.contrib.util;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;


public class PopulationUtils
{
    public static void convertLegModes(List<String> passengerIds, String mode, Scenario scenario)
    {
        Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();

        for (String id : passengerIds) {
            Person person = persons.get(Id.create(id, Person.class));

            for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
                if (pe instanceof Leg) {
                    ((Leg)pe).setMode(mode);
                }
            }
        }
    }


    public static void removePersonsNotUsingMode(String mode, Scenario scenario)
    {
        Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();
        Iterator<? extends Person> personIter = persons.values().iterator();

        while (personIter.hasNext()) {
            Plan selectedPlan = personIter.next().getSelectedPlan();

            if (!hasLegOfMode(selectedPlan, mode)) {
                personIter.remove();
            }
        }
    }


    private static boolean hasLegOfMode(Plan plan, String mode)
    {
        for (PlanElement pe : plan.getPlanElements()) {
            if (pe instanceof Leg) {
                if ( ((Leg)pe).getMode().equals(mode)) {
                    return true;
                }
            }
        }

        return false;
    }
}
