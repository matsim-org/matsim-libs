package playground.mzilske.modechoice;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.ChooseRandomLegMode;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ChangeLegModeOfOneSubtour extends AbstractMultithreadedModule {
	
	private TransportMode[] availableModes = new TransportMode[] { TransportMode.car, TransportMode.pt };

	public ChangeLegModeOfOneSubtour() {
		super(1);
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		ChooseRandomLegMode chooseRandomLegMode = new ChooseRandomLegMode(this.availableModes, MatsimRandom.getLocalInstance());
		chooseRandomLegMode.setChangeOnlyOneSubtour(true);
		return chooseRandomLegMode;
	}

}
