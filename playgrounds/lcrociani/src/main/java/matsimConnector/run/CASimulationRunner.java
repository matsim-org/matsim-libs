package matsimConnector.run;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.inject.Provider;
import matsimConnector.congestionpricing.MSACongestionHandler;
import matsimConnector.congestionpricing.MSAMarginalCongestionPricingContolerListener;
import matsimConnector.congestionpricing.MSATollDisutilityCalculatorFactory;
import matsimConnector.congestionpricing.MSATollHandler;
import matsimConnector.engine.CAMobsimFactory;
import matsimConnector.engine.CATripRouterFactory;
import matsimConnector.network.HybridNetworkBuilder;
import matsimConnector.scenario.CAEnvironment;
import matsimConnector.scenario.CAScenario;
import matsimConnector.utility.Constants;
import matsimConnector.visualizer.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import matsimConnector.visualizer.debugger.eventsbaseddebugger.InfoBox;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioUtils;

import pedCA.output.FundamentalDiagramWriter;

public class CASimulationRunner implements IterationStartsListener{

	//MATSim Logger
	private static final Logger log = Logger.getLogger(CASimulationRunner.class);




	//private Controler controller;
	private static EventBasedVisDebuggerEngine dbg;

	@SuppressWarnings("deprecation")
	public static void main(String [] args) {
		String inputPath= Constants.INPUT_PATH;
		if (args.length>0){
			inputPath = Constants.FD_TEST_PATH+args[0]+"/input";
			Constants.SIMULATION_ITERATIONS = 1;
			Constants.CA_TEST_END_TIME = 1800.;
			Constants.SIMULATION_DURATION = 2000.;
		}
		Config c = ConfigUtils.loadConfig(inputPath+"/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(c);
		CAScenario scenarioCA = new CAScenario(inputPath+"/CAScenario");
		HybridNetworkBuilder.buildNetwork(scenarioCA.getCAEnvironment(Id.create("0", CAEnvironment.class)), scenarioCA);
		scenarioCA.connect(scenario);

		Network net = scenario.getNetwork();
		
		if (Constants.BRAESS_WL) {
			//Breass experiment
			net.removeLink(Id.createLinkId("HybridNode_53-->HybridNode_12"));
		}
		//new NetworkWriter(scenario.getNetwork()).write(c.network().getInputFile());

		c.controler().setWriteEventsInterval(1);
		c.controler().setLastIteration(Constants.SIMULATION_ITERATIONS-1);
		c.qsim().setEndTime(Constants.SIMULATION_DURATION);

		final Controler controller = new Controler(scenario);


		if (Constants.MARGINAL_SOCIAL_COST_OPTIMIZATION) {
			//////////////------------THIS IS FOR THE SYSTEM OPTIMUM SEARCH
			MSATollHandler tollHandler = new MSATollHandler(controller.getScenario());
			final MSATollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new MSATollDisutilityCalculatorFactory(tollHandler);
			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});
			controller.addControlerListener(new MSAMarginalCongestionPricingContolerListener(controller.getScenario(), tollHandler, new MSACongestionHandler(controller.getEvents(), controller.getScenario())));
			//////////////------------
		}


		controller.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		CATripRouterFactory tripRouterFactoryCA = new CATripRouterFactory(scenario);
		controller.setTripRouterFactory(tripRouterFactoryCA);

		final CAMobsimFactory factoryCA = new CAMobsimFactory();
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if (getConfig().controler().getMobsim().equals(Constants.CA_MOBSIM_MODE)) {
					bind(Mobsim.class).toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return factoryCA.createMobsim(controller.getScenario(), controller.getEvents());
						}
					});
				}
			}
		});

		if (Constants.VIS) {
			dbg = new EventBasedVisDebuggerEngine(scenario);
			InfoBox iBox = new InfoBox(dbg, scenario);
			dbg.addAdditionalDrawer(iBox);
			controller.getEvents().addHandler(dbg);
		}
		
		if (args.length==0){

			//			EnvironmentGrid environmentGrid = scenarioCA.getEnvironments().get(Id.create("0",CAEnvironment.class)).getContext().getEnvironmentGrid();
			//			AgentTracker tracker = new AgentTracker(Constants.OUTPUT_PATH+"/agentTrajectories.txt",environmentGrid.getRows(),environmentGrid.getColumns());
			//			controller.getEvents().addHandler(tracker);
			//			controller.addControlerListener(tracker);
		}else{
			controller.getEvents().addHandler(new FundamentalDiagramWriter(Double.parseDouble(args[0]),scenario.getPopulation().getPersons().size(), Constants.FD_TEST_PATH+"fd_data.csv"));
		}

		CASimulationRunner runner = new CASimulationRunner();
		controller.addControlerListener(runner);
		controller.run();

		//		new TrajectoryFlipTranslate(Constants.OUTPUT_PATH+"/agentTrajectories.txt", 
		//				Constants.OUTPUT_PATH+"/agentTrajectoriesFlippedTranslated.txt", -2.41, 2.79).run();
		//		new TrajectoryCleaner(Constants.OUTPUT_PATH+"/agentTrajectoriesFlippedTranslated.txt", 
		//				Constants.OUTPUT_PATH+"/agentTrajectoriesFlippedTranslatedCleaned.txt", 
		//				Constants.RESOURCE_PATH +"/simpleGeo.shp").run();
		//
		//		//if the referenced external binaries are available the following should work
		//		String pathToJPSReport = "/Users/laemmel/svn/jpsreport/bin/jpsreport";
		//		String pathToGnuplot = "/usr/local/bin/gnuplot";
		//
		//		String gnuplotDataFile = "datafile='"+Constants.OUTPUT_PATH+ "/Output/Fundamental_Diagram/Classical_Voronoi/rho_v_Voronoi_agentTrajectoriesFlippedTranslatedCleaned.txt_id_1.dat'";
		//		
		//		File newPwd = new File(Constants.OUTPUT_PATH).getAbsoluteFile();
		//		if (System.setProperty("user.dir", newPwd.getAbsolutePath()) == null) {
		//			throw new RuntimeException("could not change working directory");
		//		}
		//
		//		try {
		//			Files.copy(Paths.get(Constants.RESOURCE_PATH+"/90deg.xml"), 
		//					Paths.get(Constants.OUTPUT_PATH+"/90deg.xml"),
		//					StandardCopyOption.REPLACE_EXISTING);
		//			Files.copy(Paths.get(Constants.RESOURCE_PATH+"/jpsGeo.xml"), 
		//					Paths.get(Constants.OUTPUT_PATH+"/jpsGeo.xml"),
		//					StandardCopyOption.REPLACE_EXISTING);
		//			Files.copy(Paths.get(Constants.RESOURCE_PATH+"/plotFlowAndSpeed.p"), 
		//					Paths.get(Constants.OUTPUT_PATH+"/plotFlowAndSpeed.p"),
		//					StandardCopyOption.REPLACE_EXISTING);
		//		} catch (IOException e1) {
		//			throw new RuntimeException(e1);
		//		}
		//
		//		try {
		//			Process p1 = new ProcessBuilder(pathToJPSReport, Constants.OUTPUT_PATH+"/90deg.xml").start();
		//			logToLog(p1);
		//			p1.waitFor();
		////			Process p2 = new ProcessBuilder(pathToGnuplot,"-e",gnuplotDataFile, Constants.OUTPUT_PATH+"/plotFlowAndSpeed.p").start();
		////			logToLog(p2);
		//		} catch (IOException | InterruptedException e) {
		//			throw new RuntimeException(e);
		//		}		
	}

	private static void logToLog(Process p1) throws IOException {
		{
			InputStream is = p1.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String l = br.readLine();
			while (l != null) {
				log.info(l);
				l = br.readLine();
			}
		}
		{
			InputStream is = p1.getErrorStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String l = br.readLine();
			while (l != null) {
				log.error(l);
				l = br.readLine();
			}
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (dbg != null)
			dbg.startIteration(event.getIteration()); 
	}

}
