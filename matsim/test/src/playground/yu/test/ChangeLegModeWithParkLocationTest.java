/**
 * 
 */
package playground.yu.test;

import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.testcases.MatsimTestCase;

import playground.yu.scoring.CharyparNagelScoringFunctionFactoryWithWalk;

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
				criterion = "|walk|walk|walk|walk|walk|walk|walk|walk|";
				break;
			case 1:
				criterion = "|pt|walk|walk|walk|walk|walk|walk|walk|";
				break;
			case 2:
				criterion = "|pt|car|car|car|walk|walk|walk|walk|";
				break;
			case 3:
				criterion = "|pt|car|car|car|car|car|walk|walk|";
				break;
			case 4:
				criterion = "|car|car|car|car|car|car|car|car|";
				break;
			case 5:
				criterion = "|walk|walk|walk|walk|walk|walk|walk|walk|";
				break;
			case 6:
				criterion = "|walk|walk|walk|walk|car|car|walk|walk|";
				break;
			case 7:
				criterion = "|walk|walk|walk|walk|walk|walk|walk|pt|";
				break;
			case 8:
				criterion = "|walk|car|car|car|car|car|walk|walk|";
				break;
			case 9:
				criterion = "|pt|car|car|car|walk|walk|walk|walk|";
				break;
			case 10:
				criterion = "|car|car|car|car|walk|walk|car|car|";
				break;
			}
			System.out.println("----->currentIteration=" + itr);
			Plan plan = ctl.getPopulation().getPersons().values().iterator()
					.next().getSelectedPlan();
			StringBuilder legChainModes = new StringBuilder("|");
			for (PlanElement pe : plan.getPlanElements())
				if (pe instanceof Leg)
					legChainModes.append(((Leg) pe).getMode() + "|");
			assertEquals("different legChainModes?", criterion, legChainModes
					.toString());
		}
	}

	public void testLegChainModes() {
		String[] args = new String[] { getInputDirectory() + "config.xml" };
		Config config = Gbl.createConfig(args);
		Controler ctl = new ChangeLegModeWithParkLocationControler(args);
		ctl.addControlerListener(new LegChainModesListener());
		ctl.setCreateGraphs(false);
		ctl.setWriteEventsInterval(0);
		ctl
				.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactoryWithWalk(
						config.charyparNagelScoring()));
		ctl.run();
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
			// PlanStrategy strategy1 = new PlanStrategy(new
			// ExpBetaPlanChanger());
			// manager.addStrategy(strategy1, 0.1);

			PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
			strategy2.addStrategyModule(new ChangeLegModeWithParkLocation(
					this.config));
			strategy2.addStrategyModule(new ReRoute(this));
			manager.addStrategy(strategy2, 0.5);

			// PlanStrategy strategy3 = new PlanStrategy(new
			// RandomPlanSelector());
			// strategy3.addStrategyModule(new ReRoute(this));
			// manager.addStrategy(strategy3, 0.4);

			return manager;
		}
	}
}
