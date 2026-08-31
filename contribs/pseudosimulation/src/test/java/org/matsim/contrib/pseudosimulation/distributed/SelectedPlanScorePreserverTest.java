package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

class SelectedPlanScorePreserverTest {

    @Test
    void iterationZeroReplacesCaughtPlansWithEverySelectedPlan() {
        Population population = populationWithSelectedPlans(10.0, 20.0);
        PlanCatcher catcher = new PlanCatcher();
        catcher.addPlansForPsim(population.getPersons().get(Id.createPersonId("1")).getSelectedPlan());
        SelectedPlanScorePreserver preserver = new SelectedPlanScorePreserver();

        preserver.beforeMobsim(0, population, catcher);

        assertEquals(2, catcher.getPlansForPSim().size());
        assertEquals(2, preserver.executedPlanCount());
        preserver.afterMobsim(population);
        assertEquals(10.0, selectedPlan(population, "1").getScore());
        assertEquals(20.0, selectedPlan(population, "2").getScore());
    }

    @Test
    void restoresScoresOnlyForPlansNotExecutedByPsim() {
        Population population = populationWithSelectedPlans(10.0, 20.0);
        PlanCatcher catcher = new PlanCatcher();
        catcher.init();
        catcher.addPlansForPsim(selectedPlan(population, "1"));
        SelectedPlanScorePreserver preserver = new SelectedPlanScorePreserver();

        preserver.beforeMobsim(3, population, catcher);
        selectedPlan(population, "1").setScore(101.0);
        selectedPlan(population, "2").setScore(202.0);
        preserver.afterMobsim(population);

        assertEquals(101.0, selectedPlan(population, "1").getScore());
        assertEquals(20.0, selectedPlan(population, "2").getScore());
        assertEquals(1, preserver.executedPlanCount());
    }

    @Test
    void preservesNullScoresAndAccumulatesExecutedPlanCountUntilReset() {
        Population population = populationWithSelectedPlans(null, 2.0);
        PlanCatcher catcher = new PlanCatcher();
        catcher.init();
        catcher.addPlansForPsim(selectedPlan(population, "2"));
        SelectedPlanScorePreserver preserver = new SelectedPlanScorePreserver();

        preserver.beforeMobsim(1, population, catcher);
        preserver.beforeMobsim(2, population, catcher);
        selectedPlan(population, "1").setScore(9.0);
        preserver.afterMobsim(population);

        assertNull(selectedPlan(population, "1").getScore());
        assertEquals(2, preserver.executedPlanCount());
        preserver.resetExecutedPlanCount();
        assertEquals(0, preserver.executedPlanCount());
    }

    private Population populationWithSelectedPlans(Double... scores) {
        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        for (int i = 0; i < scores.length; i++) {
            Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId(Integer.toString(i + 1)));
            Plan plan = PopulationUtils.createPlan(person);
            plan.setScore(scores[i]);
            person.addPlan(plan);
            person.setSelectedPlan(plan);
            population.addPerson(person);
        }
        return population;
    }

    private Plan selectedPlan(Population population, String id) {
        return population.getPersons().get(Id.createPersonId(id)).getSelectedPlan();
    }
}
