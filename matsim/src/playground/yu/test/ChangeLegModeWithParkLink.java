/**
 * 
 */
package playground.yu.test;

import java.util.Random;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * this class copys some codes from ChangeLegMode
 * 
 * @author ychen
 * 
 */
public class ChangeLegModeWithParkLink extends AbstractMultithreadedModule {
	private TransportMode[] availableModes = new TransportMode[] {
			TransportMode.car, TransportMode.pt };

	public ChangeLegModeWithParkLink(final TransportMode[] availableModes) {
		super();
		this.availableModes = availableModes.clone();
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new ChooseRandomLegModeWithParkLink(availableModes, MatsimRandom
				.getLocalInstance());
	}

	public static class ChooseRandomLegModeWithParkLink implements
			PlanAlgorithm {
		private TransportMode[] possibleModes;
		private Random rnd;

		public ChooseRandomLegModeWithParkLink(TransportMode[] possibleModes,
				Random rnd) {
			this.possibleModes = possibleModes;
			this.rnd = rnd;
		}

		public void run(Plan plan) {
			if (plan.getPlanElements().size() > 1) {

			}
		}

		private boolean checkParkSensible(Plan plan) {
			int carLegCnt = 0;
			Id OrigPark = null, lastNextPark = null;

			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getMode().equals(TransportMode.car)) {
						Id prePark = plan.getPreviousActivity(leg).getLinkId();
						Id nextPark = plan.getNextActivity(leg).getLinkId();

						if (carLegCnt == 0)
							OrigPark = prePark;
						else if (!prePark.equals(lastNextPark))
							return false;

						lastNextPark = nextPark;
						carLegCnt++;
					}
				}
			}
			return OrigPark.equals(lastNextPark);
		}
	}
}
