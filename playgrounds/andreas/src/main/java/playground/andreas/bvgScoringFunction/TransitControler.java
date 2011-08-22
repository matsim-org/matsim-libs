package playground.andreas.bvgScoringFunction;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.matsim.vis.otfvis.OnTheFlyServer;

/**
 * @author aneumann
 */
public class TransitControler extends Controler {

	private final static Logger log = Logger.getLogger(TransitControler.class);

	private boolean useOTFVis = false;
	
	public TransitControler(Config config) {
		super(config);
	}
	
	public TransitControler(ScenarioImpl scenario) {
		super(scenario);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void runMobSim() {
		
		log.info("Overriding runMobSim()");

		QSim simulation = (QSim) new QSimFactory().createMobsim(this.getScenario(), this.getEvents());

		simulation.getTransitEngine().setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//		this.events.addHandler(new LogOutputEventHandler());


		if (simulation instanceof ObservableSimulation){
			for (SimulationListener l : this.getQueueSimulationListener()) {
				((ObservableSimulation)simulation).addQueueSimulationListeners(l);
			}
		}
		if (this.useOTFVis) {
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config,getScenario(), events, simulation);
			OTFClientLive.run(config, server);
		}
		simulation.run();
	}	
	
	void setUseOTFVis(boolean useOTFVis) {
		this.useOTFVis = useOTFVis;
	}

	public static void main(final String[] args) {
		
		String configFile = args[0];		
		Config config;
		
		// reading the config file:
		config = ConfigUtils.loadConfig(configFile);
		
		// manipulate config
		// add "pt interaction" cause controler.init() is called too late and in a protected way
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		config.planCalcScore().addActivityParams(transitActivityParams);
		
		// reading the scenario (based on the config):
		ScenarioLoaderImpl scLoader = new ScenarioLoaderImpl(config) ;
		ScenarioImpl sc = (ScenarioImpl) scLoader.loadScenario() ;
		
		TransitControler tc = new TransitControler(sc);
		tc.setScoringFunctionFactory(new BvgScoringFunctionFactory(config.planCalcScore(), new BvgScoringFunctionConfigGroup(config), tc.getNetwork()));
		


		// Not needed to use own scoring function

		if(args.length > 1 && args[1].equalsIgnoreCase("true")){
			tc.setUseOTFVis(true);
		}
		tc.setOverwriteFiles(true);
//			tc.setCreateGraphs(false);
		tc.run();
		
	}
}
