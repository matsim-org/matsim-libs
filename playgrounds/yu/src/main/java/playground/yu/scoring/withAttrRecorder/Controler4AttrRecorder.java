/**
 * 
 */
package playground.yu.scoring.withAttrRecorder;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * @author yu
 * 
 */
public class Controler4AttrRecorder extends Controler {
	public static void main(String[] args) {
		Config config;
		if (args.length < 1) {
			config = ConfigUtils
			.loadConfig("test/input/2car1ptRoutes/writeScorAttrs/cfgCar-4_0.xml");
		} else/* args.length>=1 */{
			config = ConfigUtils.loadConfig(args[0]);
		}
		Controler4AttrRecorder controler = new Controler4AttrRecorder(config);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		controler.run();
	}

	private PlansScoring4AttrRecorder planScoring4AttrRecorder = null;

	public Controler4AttrRecorder(Config config) {
		super(config);
		// ---------------------------------------------------
		addControlerListener(new ScorAttrWriteTrigger());
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	}

	public PlansScoring4AttrRecorder getPlanScoring4AttrRecorder() {
		return planScoring4AttrRecorder;
	}

	@Override
	protected void loadCoreListeners() {
		addCoreControlerListener(new CoreControlerListener());

		// ------DEACTIVATE SCORING & ROADPRICING IN MATSIM------
		planScoring4AttrRecorder = new PlansScoring4AttrRecorder();
		addCoreControlerListener(planScoring4AttrRecorder);
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// load road pricing, if requested
		// if (this.config.roadpricing().getTollLinksFile() != null) {
		// this.areaToll = new RoadPricing();
		// this.addCoreControlerListener(areaToll);
		// }
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		addCoreControlerListener(new PlansReplanning());
		addCoreControlerListener(new PlansDumping());
		// EventsHanding ... very important
		addCoreControlerListener(new EventsHandling(events));
	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		// ---------------------------------------------------
		return new CharyparNagelScoringFunctionFactory4AttrRecorder(
				config.planCalcScore(), network);
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	}
}
