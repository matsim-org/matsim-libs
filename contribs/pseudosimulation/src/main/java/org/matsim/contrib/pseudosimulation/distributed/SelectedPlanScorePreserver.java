package org.matsim.contrib.pseudosimulation.distributed;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;

final class SelectedPlanScorePreserver {
    private Map<Id<Person>, Double> selectedPlanScores;
    private int executedPlanCount;

    void beforeMobsim(int iteration, Population population, PlanCatcher planCatcher) {
        selectedPlanScores = new HashMap<>(population.getPersons().size());
        if (iteration == 0) {
            planCatcher.init();
            for (Person person : population.getPersons().values()) {
                planCatcher.addPlansForPsim(person.getSelectedPlan());
            }
        } else {
            for (Person person : population.getPersons().values()) {
                selectedPlanScores.put(person.getId(), person.getSelectedPlan().getScore());
            }
            for (Plan plan : planCatcher.getPlansForPSim()) {
                selectedPlanScores.remove(plan.getPerson().getId());
            }
        }
        executedPlanCount += planCatcher.getPlansForPSim().size();
    }

    void afterMobsim(Population population) {
        for (Map.Entry<Id<Person>, Double> entry : selectedPlanScores.entrySet()) {
            population.getPersons().get(entry.getKey()).getSelectedPlan().setScore(entry.getValue());
        }
    }

    int executedPlanCount() {
        return executedPlanCount;
    }

    void resetExecutedPlanCount() {
        executedPlanCount = 0;
    }
}
