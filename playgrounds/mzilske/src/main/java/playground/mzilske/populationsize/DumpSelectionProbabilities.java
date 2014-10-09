/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DumpSelectionProbabilities.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.populationsize;

import com.google.gson.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DumpSelectionProbabilities {

    public static void main(String[] args) {
        final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
        final RegimeResource uncongested = experiment.getRegime("uncongested");
        RunResource run = uncongested.getMultiRateRun("wurst").getRateRun("0", "1");
        Scenario outputScenario = run.getOutputScenario();

        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Id.class, new IdSerializer()).create();
        for (Person person : outputScenario.getPopulation().getPersons().values()) {
            MyPerson myPerson = new MyPerson();
            myPerson.id = person.getId();
            for (Plan plan : person.getPlans()) {
                MyPlan myPlan = new MyPlan();
                myPlan.selectionProbability = ExpBetaPlanSelector.getSelectionProbability(new ExpBetaPlanSelector<Plan, Person>(1.0), person, plan);
                myPerson.plans.add(myPlan);
            }
            gson.toJson(myPerson, System.out);
        }
    }

    private static class MyPerson {
        Id<Person> id;
        List<MyPlan> plans = new ArrayList<>();
    }

    private static class MyPlan {
        double selectionProbability;
    }

    private static class IdSerializer<T> implements JsonSerializer<Id<T>> {
        @Override
        public JsonElement serialize(Id<T> tId, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(tId.toString());
        }
    }

}

