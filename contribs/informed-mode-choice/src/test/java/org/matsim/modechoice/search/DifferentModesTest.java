package org.matsim.modechoice.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import org.junit.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;

public class DifferentModesTest extends ScenarioTest {

  @Test
  public void topK() {

    group.setTopK(1024);

    TopKChoicesGenerator generator = injector.getInstance(TopKChoicesGenerator.class);

    Person person =
        controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(0));

    PlanModel model = PlanModel.newInstance(person.getSelectedPlan());

    group.setRequireDifferentModes(false);

    Collection<PlanCandidate> candidates = generator.generate(model);

    assertThat(candidates).extracting(PlanCandidate::getModes).contains(model.getCurrentModes());
  }
}
