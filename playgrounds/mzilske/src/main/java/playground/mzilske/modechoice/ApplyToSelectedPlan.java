package playground.mzilske.modechoice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.population.algorithms.PersonAlgorithm;

public class ApplyToSelectedPlan implements PersonAlgorithm {

	private SubtourModeChoice algorithm;

	public ApplyToSelectedPlan(SubtourModeChoice changeLegModeOfOneSubtour) {
		this.algorithm = changeLegModeOfOneSubtour;
	}

	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		algorithm.getPlanAlgoInstance().run(plan);
	}

}
