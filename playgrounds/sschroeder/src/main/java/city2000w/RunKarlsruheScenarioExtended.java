/**
 * 
 */
package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.city2000w.City2000WMobsimFactory;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TransportChainAgentFactoryImpl;
import playground.mzilske.freight.TransportServiceProvider;
import playground.mzilske.freight.TransportServiceProviders;
import playground.mzilske.freight.api.TSPAgentFactory;
import playground.mzilske.freight.carrier.Carrier;
import playground.mzilske.freight.carrier.CarrierAgentTracker;
import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierDriverAgentFactoryImpl;
import playground.mzilske.freight.carrier.CarrierPlan;
import playground.mzilske.freight.carrier.CarrierPlanReader;
import playground.mzilske.freight.carrier.CarrierPlanWriter;
import playground.mzilske.freight.carrier.Carriers;
import city2000w.replanning.CarrierContractLandscapeChangedResponder;
import city2000w.replanning.CarrierPlanStrategy;
import city2000w.replanning.CarrierSelector;
import city2000w.replanning.FrequencyAndTSPSelector;
import city2000w.replanning.PlanStrategyManager;
import city2000w.replanning.ReRouteVehicles;
import city2000w.replanning.ShipperPlanStrategy;
import city2000w.replanning.TSPContractLandscapeChangedResponder;
import city2000w.replanning.TSPPlanStrategy;
import freight.ShipperAgentTracker;
import freight.ShipperImpl;
import freight.ShipperPlanReader;
import freight.ShipperPlanWriter;
import freight.Shippers;
import freight.TSPPlanReader;
import freight.TSPPlanWriter;
import freight.TlcCostFunction;
import freight.utils.TimePeriod;
import freight.vrp.RRPDTWSolverFactory;


/**
 * @author schroeder
 *
 */
public class RunKarlsruheScenarioExtended implements StartupListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener, ReplanningListener, ScoringListener, ShutdownListener {

	private static Logger logger = Logger.getLogger(RunKarlsruheScenarioExtended.class);
	
	private Carriers carriers;
	
	private TransportServiceProviders transportServiceProviders;
	
	private Shippers shippers;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private TSPAgentTracker tspAgentTracker;
	
	private ShipperAgentTracker shipperAgentTracker;

	private ScenarioImpl scenario;

	private boolean liveModus = false;
	
	private static final String NETWORK_FILENAME = "../playgrounds/sschroeder/networks/karlsruheNetwork.xml";
	
	private static final String TSPPLAN_FILENAME = "../playgrounds/sschroeder/anotherInput/karlsruheEmptyTspPlans.xml";
	
	private static final String CARRIERNPLAN_FILENAME = "../playgrounds/sschroeder/anotherInput/karlsruheCarriers.xml";
	
	private static final String SHIPPERPLAN_FILENAME = "../playgrounds/sschroeder/anotherInput/karlsruheShipperPlans.xml";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
		RunKarlsruheScenarioExtended runner = new RunKarlsruheScenarioExtended();
		runner.run();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Controler controler = event.getControler();
		
		readCarriers();
		
		readTransportServiceProviders();
		
		readShippers();
		
		shipperAgentTracker = new ShipperAgentTracker(shippers.getShippers(), new KarlsruheShipperAgentFactory());
		
		KarlsruheCarrierAgentFactory carrierAgentFactory = new KarlsruheCarrierAgentFactory(controler.createRoutingAlgorithm(), new CarrierDriverAgentFactoryImpl());
		carrierAgentFactory.setNetwork(scenario.getNetwork());
		carrierAgentTracker = new CarrierAgentTracker(carriers.getCarriers().values(), controler.createRoutingAlgorithm(), scenario.getNetwork(), carrierAgentFactory);
		
		TSPAgentFactory tspAgentFactory = new KarlsruheTSPAgentFactory(new TransportChainAgentFactoryImpl());
		tspAgentTracker = new TSPAgentTracker(transportServiceProviders.getTransportServiceProviders(),tspAgentFactory);
		
		carrierAgentTracker.getEventsManager().addHandler(tspAgentTracker);
		carrierAgentTracker.getEventsManager().addHandler(new PickupAndDeliveryConsoleWriter());
		
		tspAgentTracker.getEventsManager().addHandler(carrierAgentTracker);
		tspAgentTracker.getEventsManager().addHandler(tspAgentTracker);
		
		shipperAgentTracker.getEventsManager().addHandler(tspAgentTracker);
		shipperAgentTracker.getEventsManager().addHandler(shipperAgentTracker);
		
		createTSPContracts(shipperAgentTracker.createTSPContracts());
		
		createInitialTSPPlans();
		
		createCarrierContracts(tspAgentTracker.createCarrierContracts());
		
		createInitialCarrierPlans();
		
		event.getControler().getScenario().addScenarioElement(carriers);
		
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, carrierAgentTracker);
		mobsimFactory.setUseOTFVis(liveModus );
		event.getControler().setMobsimFactory(mobsimFactory);		
	}

	private void createInitialTSPPlans() {
		for(TransportServiceProvider tsp : transportServiceProviders.getTransportServiceProviders()){
			KarlsruheTSPPlanBuilder tspPlanBuilder = new KarlsruheTSPPlanBuilder(tspAgentTracker, tsp);
			TSPPlan plan = tspPlanBuilder.buildPlan(tsp.getContracts(), tsp.getTspCapabilities());
			tsp.setSelectedPlan(plan);
		}
	}

	private void createTSPContracts(Collection<TSPContract> contracts) {
		for(TSPContract contract : contracts){
			for(TransportServiceProvider tsp : transportServiceProviders.getTransportServiceProviders()){
				if(tsp.getId().equals(contract.getSeller())){
					tsp.getContracts().add(contract);
				}
			}
		}
	}

	private void readShippers() {
		shippers = new Shippers();
		new ShipperPlanReader(shippers.getShippers()).read(SHIPPERPLAN_FILENAME);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().addHandler(carrierAgentTracker);
		carrierAgentTracker.createPlanAgents();
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().removeHandler(carrierAgentTracker);
	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		logger.info("Reset costs/score of tspAgents");
		tspAgentTracker.reset();
	}


	private void run(){
		Config config = new Config();
		config.addCoreModules();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(20);
		scenario = (ScenarioImpl)ScenarioUtils.createScenario(config);
		readNetwork(NETWORK_FILENAME);
		NetworkCleaner networkCleaner = new NetworkCleaner();
		networkCleaner.run(scenario.getNetwork());
		
		Controler controler = new Controler(scenario);
		/*
		 * muss ich auf 'false' setzen, da er mir sonst eine exception wirft, weil er das matsim-logo nicht finden kann
		 * ich hab keine ahnung wo ich den pfad des matsim-logos setzen kann
		 * 
		 */
		controler.setCreateGraphs(false);
		controler.addControlerListener(this);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	private void readNetwork(String networkFilename) {
		new MatsimNetworkReader(scenario).readFile(networkFilename);
	}

	private void createInitialCarrierPlans() {
		for(Carrier carrier : carriers.getCarriers().values()){
			VRPCarrierPlanBuilder planBuilder = new VRPCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), scenario.getNetwork());
			planBuilder.setVrpSolverFactory(new RRPDTWSolverFactory());
			CarrierPlan plan = planBuilder.buildPlan();
			carrier.getPlans().add(plan);
			carrier.setSelectedPlan(plan);
		}
	}

	private void createCarrierContracts(List<CarrierContract> contracts) {
		for(CarrierContract contract : contracts){
			Id carrierId = contract.getSeller();
			carriers.getCarriers().get(carrierId).getContracts().add(contract);
		}
	}

	private void readCarriers() {
		Collection<Carrier> carriers = new ArrayList<Carrier>();
		new CarrierPlanReader(carriers).read(CARRIERNPLAN_FILENAME);
		this.carriers = new Carriers(carriers);
	}

	private void readTransportServiceProviders() {
		transportServiceProviders = new TransportServiceProviders();
		new TSPPlanReader(transportServiceProviders.getTransportServiceProviders()).read(TSPPLAN_FILENAME);
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		replanShipper();
		replanTSP();
		replanCarrier();
	}

	private void replanShipper() {
		ShipperPlanStrategy shipperPlanStrategy = new ShipperPlanStrategy();
		FrequencyAndTSPSelector frequencyAndTSPSelector = new FrequencyAndTSPSelector(shipperAgentTracker);
		TlcCostFunction shippersCostFunction = new TlcCostFunction();
		shippersCostFunction.capitalCostRate = 1.0;
		frequencyAndTSPSelector.setCostFunction(shippersCostFunction);
		frequencyAndTSPSelector.frequencies.add(1);
		frequencyAndTSPSelector.frequencies.add(2);
		frequencyAndTSPSelector.timePeriods.addTimePeriod(new TimePeriod(0.0, 2*3600));
		frequencyAndTSPSelector.timePeriods.addTimePeriod(new TimePeriod(2*3600, 4*3600));
		shipperPlanStrategy.addModule(frequencyAndTSPSelector);
		
		PlanStrategyManager<ShipperImpl> stratManager = new PlanStrategyManager<ShipperImpl>();
		stratManager.addStrategy(shipperPlanStrategy, 1.0);
		
		for(ShipperImpl shipper : shippers.getShippers()){
			stratManager.nextStrategy().run(shipper);
		}
	}

	private void replanTSP() {
		TSPPlanStrategy planStrategy = new TSPPlanStrategy();
		planStrategy.addModule(new TSPContractLandscapeChangedResponder(tspAgentTracker));
		planStrategy.addModule(new CarrierSelector(tspAgentTracker));
		
		PlanStrategyManager<TransportServiceProvider> stratManager = new PlanStrategyManager<TransportServiceProvider>();
		stratManager.addStrategy(planStrategy, 1.0);
		
		for(TransportServiceProvider tsp : transportServiceProviders.getTransportServiceProviders()){
			stratManager.nextStrategy().run(tsp);
		}
	}

	private void replanCarrier() {
		CarrierPlanStrategy planStrat = new CarrierPlanStrategy();
		CarrierContractLandscapeChangedResponder contractChangedResponder = new CarrierContractLandscapeChangedResponder(scenario.getNetwork(),new RRPDTWSolverFactory());
		planStrat.addModule(contractChangedResponder);
		CarrierPlanStrategy planStrat2 = new CarrierPlanStrategy();
		planStrat2.addModule(contractChangedResponder);
		planStrat2.addModule(new ReRouteVehicles(scenario.getNetwork(), new RRPDTWSolverFactory()));
		
		PlanStrategyManager<Carrier> stratManager = new PlanStrategyManager<Carrier>();
		stratManager.addStrategy(planStrat, 0.5);
		stratManager.addStrategy(planStrat2, 0.5);
		
		for(Carrier carrier : carriers.getCarriers().values()){
			stratManager.nextStrategy().run(carrier);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		new ShipperPlanWriter(shippers.getShippers()).write("../playgrounds/sschroeder/anotherInput/karlsruheShipperPlans_after.xml");
		new TSPPlanWriter(transportServiceProviders.getTransportServiceProviders()).write("../playgrounds/sschroeder/anotherInput/karlsruheTSPPlans_after.xml");
		new CarrierPlanWriter(carriers.getCarriers().values()).write("../playgrounds/sschroeder/anotherInput/karlsruheCarrierPlans_after.xml");
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		carrierAgentTracker.calculateCosts();
		tspAgentTracker.calculateCosts();
	}
}
