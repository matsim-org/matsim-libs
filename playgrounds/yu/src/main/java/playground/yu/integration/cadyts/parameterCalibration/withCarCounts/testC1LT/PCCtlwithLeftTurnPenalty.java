/**
 * 
 */
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testC1LT;

import org.matsim.core.config.Config;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testLeftTurn.PlansScoringWithLeftTurnPenalty4PC;
import playground.yu.scoring.withAttrRecorder.leftTurn.CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty;

/**
 * @author yu
 * 
 */
public class PCCtlwithLeftTurnPenalty extends BseParamCalibrationControler {

	public PCCtlwithLeftTurnPenalty(Config config) {
		super(config);
		extension = new PCCtlListener();
		addControlerListener(extension);
	}

	/**
	 * please check the method in super class, when the super class
	 * {@code org.matsim.core.controler.Controler} is changed sometimes
	 */
	@Override
	protected void loadCoreListeners() {
		addCoreControlerListener(new CoreControlerListener());

		// ******DEACTIVATE SCORING & ROADPRICING IN MATSIM******
		// the default handling of plans
		plansScoring4PC = new PlansScoringWithLeftTurnPenalty4PC();
		addCoreControlerListener(plansScoring4PC);

		// load road pricing, if requested
		// if (this.config.roadpricing().getTollLinksFile() != null) {
		// this.areaToll = new RoadPricing();
		// this.addCoreControlerListener(areaToll);
		// }
		// ******************************************************

		addCoreControlerListener(new PlansReplanning());
		addCoreControlerListener(new PlansDumping());
		// EventsHanding ... very important
		addCoreControlerListener(new EventsHandling(events));
	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty(
				config, network);
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new PCStrMn(network, getFirstIteration(),
				config);
		StrategyManagerConfigLoader.load(this, manager);

		return manager;
	}
}
