package playground.andreas.P;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.run.OTFVis;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.andreas.P.init.CreateInitialTimeSchedule;
import playground.andreas.P.init.PConfigGroup;
import playground.andreas.P.replan.ReplanTimeSchedule;

/**
 * @author aneumann
 */
public class PControler extends Controler {

	private final static Logger log = Logger.getLogger(PControler.class);

	private boolean useOTFVis = false;
	
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


		if (simulation instanceof IOSimulation){
			((IOSimulation)simulation).setControlerIO(this.getControlerIO());
			((IOSimulation)simulation).setIterationNumber(this.getIterationNumber());
		}
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
		
		for (int i = 0; i < Integer.parseInt(args[1]); i++) {
			
			// reading the config file:
			config = ConfigUtils.loadConfig(configFile);
			
			String currentOutputBase = config.getParam("controler", "outputDirectory") + "it." + i + "/";
			String nextOutputBase = config.getParam("controler", "outputDirectory") + "it." + (i+1) + "/";
			
			PConfigGroup pConfig = new PConfigGroup(config);
			pConfig.setCurrentOutPutBase(currentOutputBase);
			pConfig.setNextOutPutBase(nextOutputBase);				
			
			String transitScheduleOutFile = pConfig.getCurrentOutputBase() + "transitSchedule.xml";
			config.setParam("transit", "transitScheduleFile", transitScheduleOutFile);				
			config.setParam("controler", "outputDirectory", currentOutputBase);
			String vehiclesOutFile  = pConfig.getCurrentOutputBase() + "transitVehicles.xml";
			config.setParam("transit", "vehiclesFile", vehiclesOutFile);
			
			if(i == 0) {
				File currentOutDir = new File(currentOutputBase);
				currentOutDir.mkdir();
				CreateInitialTimeSchedule.createInitialTimeSchedule(pConfig);
			}
			
			// reading the scenario (based on the config):
			ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.loadScenario(config);
			
			PControler tc = new PControler(sc);

//				if(args.length > 1 && args[1].equalsIgnoreCase("true")){
//					tc.setUseOTFVis(true);
//				}
			tc.setOverwriteFiles(true);
//				tc.setCreateGraphs(false);
			tc.run();
			
			File nextOutDir = new File(pConfig.getNextOutputBase());
			nextOutDir.mkdir();
			ReplanTimeSchedule replanTS = new ReplanTimeSchedule();
			replanTS.replan(pConfig, sc.getNetwork());
			
			new VehicleWriterV1(sc.getVehicles()).writeFile(pConfig.getNextOutputBase() + "transitVehicles.xml");
		}
		
	}
}
