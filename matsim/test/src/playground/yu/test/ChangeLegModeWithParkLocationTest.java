/**
 * 
 */
package playground.yu.test;

import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author yu
 * 
 */
public class ChangeLegModeWithParkLocationTest extends MatsimTestCase {
	private static class LegChainModesListener implements IterationEndsListener {
		public void notifyIterationEnds(IterationEndsEvent event) {
			Controler ctl = event.getControler();
			int itr = event.getIteration();
			String criterion = "";
			switch (itr) {
			case 0:
				criterion = "|car|car|";
				break;
			case 1:
				criterion = "|car|car|";
				break;
			case 2:
				criterion = "|car|car|";
				break;
			case 3:
				criterion = "|car|car|";
				break;
			case 4:
				criterion = "|car|car|";
				break;
			case 5:
				criterion = "|car|car|";
				break;
			case 6:
				criterion = "|car|car|";
				break;
			case 7:
				criterion = "|car|car|";
				break;
			case 8:
				criterion = "|car|car|";
				break;
			case 9:
				criterion = "|car|car|";
				break;
			case 10:
				criterion = "|car|car|";
				break;
			}
			System.out.println("----->currentIteration=" + itr);
			Plan plan = ctl.getPopulation().getPersons().get(new IdImpl(4))
					.getSelectedPlan();
			StringBuilder legChainModes = new StringBuilder("|");
			for (PlanElement pe : plan.getPlanElements())
				if (pe instanceof Leg)
					legChainModes.append(((Leg) pe).getMode() + "|");
			assertEquals("different legChainModes?", criterion, legChainModes
					.toString());
		}
	}

	// protected void tearDown() throws Exception {
	// super.tearDown();
	// Gbl.reset();
	// }

	public void testLegChainModes() {
		Controler ctl = new Controler(new String[] { getInputDirectory()
				+ "config.xml" });
		ctl.addControlerListener(new LegChainModesListener());
		ctl.setCreateGraphs(false);
		ctl.setWriteEventsInterval(0);
		ctl.run();
		ctl.getStrategyManager().addStrategy(
				new PlanStrategy4ChangeLegModeWithParkLocation(
						new ExpBetaPlanChanger()), 0.9);
		//TODO override load...
		// ctl.run();
		// System.out.println(">>>>>"
		// + ctl.getStrategyManager().getMaxPlansPerAgent());

	}
}
