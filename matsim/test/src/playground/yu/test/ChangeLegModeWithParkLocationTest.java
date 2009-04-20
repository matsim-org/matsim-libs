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
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.testcases.MatsimTestCase;

import playground.yu.bottleneck.TimeAllocationMutatorBottleneck;

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
	public void testLegChainModes() {
		Controler ctl = new ChangeLegModeWithParkLocationControler(new String[] { getInputDirectory()
				+ "config.xml" });
		ctl.addControlerListener(new LegChainModesListener());
		ctl.setCreateGraphs(false);
		ctl.setWriteEventsInterval(0);
		ctl.run();
		// ctl.getStrategyManager().addStrategy(
		// new PlanStrategy4ChangeLegModeWithParkLocation(
		// new ExpBetaPlanChanger()), 0.9);
		// TODO override load...
	}

	private static class ChangeLegModeWithParkLocationControler extends
			Controler {
		public ChangeLegModeWithParkLocationControler(String[] args) {
			super(args);
		}
		protected StrategyManager loadStrategyManager() {
			StrategyManager manager = new StrategyManager();
			manager.setMaxPlansPerAgent(5);
			//
			PlanStrategy strategy1 = new PlanStrategy(new ExpBetaPlanChanger());
			manager.addStrategy(strategy1, 0.1);

			PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
			strategy2.addStrategyModule(new ChangeLegModeWithParkLocation(this.config));
			strategy2.addStrategyModule(new ReRoute(this));
			manager.addStrategy(strategy2, 0.9);
			return manager;
		}
	}
}
