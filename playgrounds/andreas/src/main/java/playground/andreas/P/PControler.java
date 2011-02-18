package playground.andreas.P;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;

import playground.andreas.P.init.CreateInitialTimeSchedule;
import playground.andreas.P.init.CreateStops;
import playground.andreas.P.replan.ReplanTimeSchedule;
import playground.andreas.bvgScoringFunction.BvgScoringFunctionConfigGroup;
import playground.andreas.bvgScoringFunction.BvgScoringFunctionFactory;

/**
 * @author aneumann
 */
public class PControler extends Controler {

	private final static Logger log = Logger.getLogger(PControler.class);

	private boolean useOTFVis = false;
	
	public PControler(Config config) {
		super(config);
	}
	
	public PControler(ScenarioImpl scenario) {
		super(scenario);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void runMobSim() {
		
		log.info("Overriding runMobSim()");

		QSim simulation = (QSim) new QSimFactory().createMobsim(this.getScenario(), this.getEvents());

		simulation.getTransitEngine().setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//		this.events.addHandler(new LogOutputEventHandler());

		if (this.useOTFVis) {
			OTFVisMobsimFeature otfVisQSimFeature = new OTFVisMobsimFeature(simulation);
			otfVisQSimFeature.setVisualizeTeleportedAgents(simulation.getScenario().getConfig().otfVis().isShowTeleportedAgents());
			simulation.addFeature(otfVisQSimFeature);
		}

		if (simulation instanceof IOSimulation){
			((IOSimulation)simulation).setControlerIO(this.getControlerIO());
			((IOSimulation)simulation).setIterationNumber(this.getIterationNumber());
		}
		if (simulation instanceof ObservableSimulation){
			for (SimulationListener l : this.getQueueSimulationListener()) {
				((ObservableSimulation)simulation).addQueueSimulationListeners(l);
			}
		}
		simulation.run();
	}	
	
	void setUseOTFVis(boolean useOTFVis) {
		this.useOTFVis = useOTFVis;
	}

	public static void main(final String[] args) {
		
		String configFile = args[0];
		Config config;
		
		try {
			
			String baseDir = "F:/pTest/";
			
			for (int i = 0; i < 10; i++) {
				
				// create output dir
				String outPutBase = baseDir + "_out/" + "it." + i + "/"; 

				String transitScheduleOutFile = outPutBase + "transitSchedule.xml";
				String vehiclesOutFile  = baseDir + "_in/"+ "transitVehicles.xml";
				
				// reading the config file:
				config = ConfigUtils.loadConfig(configFile);
				config.setParam("transit", "transitScheduleFile", transitScheduleOutFile);
				config.setParam("transit", "vehiclesFile", vehiclesOutFile);
				config.setParam("controler", "outputDirectory", outPutBase);
				
				if(i == 0) {
					File outDir = new File(outPutBase);
					outDir.mkdir();
					CreateInitialTimeSchedule.createInitialTimeSchedule(config.getParam("network", "inputNetworkFile"), transitScheduleOutFile, vehiclesOutFile, 1500, new CoordImpl(0, 0), new CoordImpl(6000, 6000), 100);
				}
				
				// reading the scenario (based on the config):
				ScenarioLoaderImpl scLoader = new ScenarioLoaderImpl(config) ;
				ScenarioImpl sc = (ScenarioImpl) scLoader.loadScenario() ;
				
				PControler tc = new PControler(sc);

//				if(args.length > 1 && args[1].equalsIgnoreCase("true")){
//					tc.setUseOTFVis(true);
//				}
				tc.setOverwriteFiles(true);
//				tc.setCreateGraphs(false);
				tc.run();
				
				File outDir = new File(baseDir + "_out/" + "it." + (i+1) + "/");
				outDir.mkdir();
				ReplanTimeSchedule replanTS = new ReplanTimeSchedule();
				replanTS.replan(sc.getNetwork(), outPutBase + "ITERS/it.0/0.events.xml.gz", transitScheduleOutFile, baseDir + "_out/" + "it." + (i+1) + "/"  + "transitSchedule.xml", 4);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
