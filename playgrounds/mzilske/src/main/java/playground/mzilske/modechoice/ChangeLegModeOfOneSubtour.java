package playground.mzilske.modechoice;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ChangeLegModeOfOneSubtour extends AbstractMultithreadedModule {

	private String[] availableModes = new String[] { TransportMode.car, TransportMode.pt };

	public ChangeLegModeOfOneSubtour() {
		super(1);
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		ChooseRandomLegModeForSubtour chooseRandomLegMode = new ChooseRandomLegModeForSubtour(this.availableModes, MatsimRandom.getLocalInstance());
		return chooseRandomLegMode;
	}

}
