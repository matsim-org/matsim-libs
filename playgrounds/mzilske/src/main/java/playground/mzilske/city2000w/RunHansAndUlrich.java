package playground.mzilske.city2000w;

import java.util.Arrays;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Shipment.TimeWindow;

public class RunHansAndUlrich implements StartupListener, ScoringListener, ReplanningListener, BeforeMobsimListener, AfterMobsimListener {

	private static final String NETWORK_FILENAME = "examples/equil/network.xml";
	private CarrierImpl c1;
	private CarrierImpl c2;
	private Carriers carriers;
	private CarrierAgentTracker freightAgentTracker;
	
	

	public static void main(String[] args) {

		RunHansAndUlrich createSomeCarriers = new RunHansAndUlrich();
		createSomeCarriers.run();

	}

	private void run() {
		Config config = new Config();
		config.addCoreModules();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		ScenarioImpl scenario = new ScenarioImpl(config);
		new MatsimNetworkReader(scenario).readFile(NETWORK_FILENAME);
		Controler controler = new Controler(scenario);
		controler.addControlerListener(this);
		controler.setCreateGraphs(false);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	private static void score(CarrierImpl c1) {
		System.out.println("Scoring " + c1.getId());
	}

	private static void replan(CarrierImpl c1) {
		System.out.println("Replanning " + c1.getId());
		TrivialReplanner trivialReplanner = new TrivialReplanner();
		CarrierPlan newPlan =  trivialReplanner.replan(c1.getCarrierCapabilities(), c1.getContracts(), c1.getSelectedPlan());
		c1.getPlans().add(newPlan);
		c1.setSelectedPlan(newPlan);
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		score(c1);
		score(c2);
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		replan(c1);
		replan(c2);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler();
		c1 = new CarrierImpl(new IdImpl("hans"), new IdImpl("1"));
		c1.getContracts().add(new Contract(Arrays.asList(new Shipment(new IdImpl("12"), new IdImpl("20"), 20, new TimeWindow(0, 500), new TimeWindow(5000, 5500)))));
		c1.getContracts().add(new Contract(Arrays.asList(new Shipment(new IdImpl("20"), new IdImpl("1"), 20, new TimeWindow(0, 500), new TimeWindow(5000, 5500)))));
		c1.getContracts().add(new Contract(Arrays.asList(new Shipment(new IdImpl("12"), new IdImpl("20"), 20, new TimeWindow(0, 500), new TimeWindow(5000, 5500)))));
		CarrierCapabilities cc = new CarrierCapabilities();
		c1.setCarrierCapabilities(cc);
		CarrierVehicle cv_hans_1 = new CarrierVehicle(new IdImpl("hans-erstes-auto"), new IdImpl("1"));
		CarrierVehicle cv_hans_2 = new CarrierVehicle(new IdImpl("hans-zweites-auto"), new IdImpl("2"));
		cc.getCarrierVehicles().add(cv_hans_1);
		cc.getCarrierVehicles().add(cv_hans_2);


		c2 = new CarrierImpl(new IdImpl("ulrich"), new IdImpl("2"));
		c2.getContracts().add(new Contract(Arrays.asList(new Shipment(new IdImpl("20"), new IdImpl("1"), 50, new TimeWindow(0, 500), new TimeWindow(5000, 5500)))));
		c2.getContracts().add(new Contract(Arrays.asList(new Shipment(new IdImpl("12"), new IdImpl("20"), 50, new TimeWindow(0, 500), new TimeWindow(5000, 5500)))));
		c2.getContracts().add(new Contract(Arrays.asList(new Shipment(new IdImpl("1"), new IdImpl("12"), 50, new TimeWindow(0, 500), new TimeWindow(5000, 5500)))));
		CarrierCapabilities cc2 = new CarrierCapabilities();
		c2.setCarrierCapabilities(cc2);
		CarrierVehicle cv_ulrich_1 = new CarrierVehicle(new IdImpl("ulrichs-erstes-auto"), new IdImpl("1"));
		CarrierVehicle cv_ulrich_2 = new CarrierVehicle(new IdImpl("ulrichs-zweites-auto"), new IdImpl("2"));
		cc2.getCarrierVehicles().add(cv_ulrich_1);
		cc2.getCarrierVehicles().add(cv_ulrich_2);

		TrivialCarrierPlanBuilder trivialCarrierPlanBuilder = new TrivialCarrierPlanBuilder();
		CarrierPlan plan = trivialCarrierPlanBuilder.buildPlan(c1.getCarrierCapabilities(), c1.getContracts());
		c1.getPlans().add(plan);
		c1.setSelectedPlan(plan);

		TrivialCarrierPlanBuilder trivialCarrierPlanBuilder2 = new TrivialCarrierPlanBuilder();
		CarrierPlan plan2 = trivialCarrierPlanBuilder2.buildPlan(c2.getCarrierCapabilities(), c2.getContracts());
		c2.getPlans().add(plan2);
		c2.setSelectedPlan(plan2);

		carriers = new Carriers();
		event.getControler().getScenario().addScenarioElement(carriers);
		carriers.getCarriers().add(c1);
		carriers.getCarriers().add(c2);
		
		freightAgentTracker = new CarrierAgentTracker(controler.getScenario().getScenarioElement(Carriers.class).getCarriers(), controler.createRoutingAlgorithm());
		freightAgentTracker.setNetwork(event.getControler().getScenario().getNetwork());
		
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, freightAgentTracker);
		mobsimFactory.setUseOTFVis(true);
		event.getControler().setMobsimFactory(mobsimFactory);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().addHandler(freightAgentTracker);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().removeHandler(freightAgentTracker);
	}

	
	
}
