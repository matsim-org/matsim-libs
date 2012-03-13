/**
 * 
 */
package playground.yu.scoring.withAttrRecorder.leftTurn;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.yu.scoring.PlansScoringI;
import playground.yu.scoring.withAttrRecorder.Controler4AttrRecorder;
import playground.yu.scoring.withAttrRecorder.ScorAttrWriteTrigger;

/**
 * @author yu
 * 
 */
public class LeftTurnPenaltyControler extends Controler4AttrRecorder {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config;
		if (args.length < 1) {
			config = ConfigUtils
					.loadConfig("test/input/2car1ptRoutes/writeScorAttrs/cfgCarTrav-3.5leftTurn-0.3.xml");
		} else/* args.length>=1 */{
			config = ConfigUtils.loadConfig(args[0]);
		}
		LeftTurnPenaltyControler controler = new LeftTurnPenaltyControler(
				config);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		controler.run();
	}

	private PlansScoringWithLeftTurnPenalty plansScoringLTP = null;

	public LeftTurnPenaltyControler(Config config) {
		super(config);
		// ---------------------------------------------------
		addControlerListener(new ScorAttrWriteTrigger());
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	}

	@Override
	public PlansScoringI getPlansScoring4AttrRecorder() {
		return plansScoringLTP;
	}

	@Override
	protected void loadCoreListeners() {
		addCoreControlerListener(new CoreControlerListener());

		// ------DEACTIVATE SCORING & ROADPRICING IN MATSIM------
		plansScoringLTP = new PlansScoringWithLeftTurnPenalty();
		addCoreControlerListener(plansScoringLTP);
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// load road pricing, if requested
		// if (this.config.roadpricing().getTollLinksFile() != null) {
		// this.areaToll = new RoadPricing();
		// this.addCoreControlerListener(areaToll);
		// }
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		addCoreControlerListener(new PlansReplanning());
		addCoreControlerListener(new PlansDumping());
		// EventsHandling ... very important
		addCoreControlerListener(new EventsHandling(events));
	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		// ---------------------------------------------------
		return new CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty(
				config, network);
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	}
}
