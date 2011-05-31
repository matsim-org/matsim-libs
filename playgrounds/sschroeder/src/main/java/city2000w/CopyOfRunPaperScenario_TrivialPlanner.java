/**
 * 
 */
package city2000w;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
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
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;


import playground.mzilske.city2000w.CaseStudyCostObserver;
import playground.mzilske.city2000w.CaseStudySimulationObserver;
import playground.mzilske.city2000w.CheapestCarrierChoice;
import playground.mzilske.city2000w.CheapestCarrierWithVorlaufTSPPlanBuilder;
import playground.mzilske.city2000w.City2000WMobsimFactory;
import playground.mzilske.city2000w.DefaultLSPShipmentTracker;
import playground.mzilske.city2000w.TrivialCarrierPlanBuilder;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierAgentTrackerBuilder;
import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierKnowledge;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPKnowledge;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.TransportServiceProviders;

/**
 * @author schroeder
 *
 */
public class CopyOfRunPaperScenario_TrivialPlanner implements StartupListener, ScoringListener, ReplanningListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener, ShutdownListener {

	private static final int GRID_SIZE = 8;
	
	private static int NoITERATION = 1;

	private static Logger logger = Logger.getLogger(CopyOfRunPaperScenario_TrivialPlanner.class);

	private Carriers carriers;
	private TransportServiceProviders transportServiceProviders;

	private CarrierAgentTracker carrierAgentTracker;
	private TSPAgentTracker tspAgentTracker;

	private ScenarioImpl scenario;
	
	private CaseStudySimulationObserver simStats;
	
	private CaseStudyCostObserver costStats;
	
	private static final String NETWORK_FILENAME = "output/gridWithSpike.xml";


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for(int i=0;i<NoITERATION;i++){
			CopyOfRunPaperScenario_TrivialPlanner runner = new CopyOfRunPaperScenario_TrivialPlanner();
			runner.run();
		}
	}


	public void notifyStartup(StartupEvent event) {

		Controler controler = event.getControler();

		createCarriers();
		createTransportServiceProviderWithContracts();

		tspAgentTracker = new TSPAgentTracker(transportServiceProviders.getTransportServiceProviders());
		tspAgentTracker.getCostListeners().add(new DefaultLSPShipmentTracker());

		CarrierAgentTrackerBuilder freightAgentTrackerBuilder = new CarrierAgentTrackerBuilder();
		freightAgentTrackerBuilder.setCarriers(controler.getScenario().getScenarioElement(Carriers.class).getCarriers().values());
		freightAgentTrackerBuilder.setRouter(controler.createRoutingAlgorithm());
		freightAgentTrackerBuilder.setNetwork(controler.getNetwork());
		freightAgentTrackerBuilder.setEventsManager(controler.getEvents());
		freightAgentTrackerBuilder.addCarrierCostListener(tspAgentTracker);
		carrierAgentTracker = freightAgentTrackerBuilder.build();
		carrierAgentTracker.getShipmentStatusListeners().add(tspAgentTracker);

		createTSPPlans();

		giveContractsToCarriers(tspAgentTracker.createCarrierContracts());

		createCarrierPlans();

		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, carrierAgentTracker);
		mobsimFactory.setUseOTFVis(false);
		event.getControler().setMobsimFactory(mobsimFactory);
		
		simStats = new CaseStudySimulationObserver("Foo", scenario.getNetwork());
		Date date = new Date();
		simStats.setOutFile("output/trivialPlanner_noChain_simStats_" + date + ".txt");
		costStats = new CaseStudyCostObserver();
		costStats.setOutFile("output/trivialPlanner_noChain_costStats_" + date + ".txt");
		tspAgentTracker.getCostListeners().add(costStats);
		carrierAgentTracker.getCostListeners().add(costStats);
		
		controler.getEvents().addHandler(simStats);
		
	}

	private void createTSPPlans() {
		for (TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()) {
			createInitialPlans(tsp);
		}
	}


	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		carrierAgentTracker.createPlanAgents();
		Controler controler = event.getControler();
		controler.getEvents().addHandler(carrierAgentTracker);
	}


	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().removeHandler(carrierAgentTracker);
	}

	public void notifyScoring(ScoringEvent event) {
		logger.info("carrierAgents are calculating costs ...");
		carrierAgentTracker.calculateCostsScoreCarriersAndInform();
		logger.info("transportServiceProvider are calculating costs ...");
		tspAgentTracker.calculateCostsScoreTSPAndInform();
	}


	public void notifyIterationEnds(IterationEndsEvent event) {
		logger.info("Reset costs/score of tspAgents");
		//simStats.reset(event.getIteration());
		costStats.reset();
		carrierAgentTracker.reset(event.getIteration());
		tspAgentTracker.reset();
	}


	public void notifyReplanning(ReplanningEvent event) {
		replanTSP(event.getIteration());
		replanCarriers();
	}


	public void notifyShutdown(ShutdownEvent event)  {
		try {
			simStats.writeStats();
			costStats.writeStats();
		} catch (FileNotFoundException e) {
			logger.error(e.toString());
			e.printStackTrace();
		} catch (IOException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
		
	}

	private void replanTSP(int i) {
		for(TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()){
			selectTSPPlan(tsp,i);
			modifyTSPPlan(tsp);
		}
		giveContractsToCarriers(tspAgentTracker.createCarrierContracts());
	}

	private void modifyTSPPlan(TransportServiceProviderImpl tsp) {
		CheapestCarrierChoice carrierChoice = new CheapestCarrierChoice(tsp);
		carrierChoice.setCarrierAgentTracker(carrierAgentTracker);
		carrierChoice.run();
	}

	private void selectTSPPlan(TransportServiceProviderImpl tsp, int i) {
		TSPPlanSelector selector = new TSPPlanSelector(tsp,i);
		selector.run();
	}

	private void replanCarriers() {
		createCarrierPlans();
	}



	private void run(){
		Config config = new Config();
		config.addCoreModules();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		scenario = (ScenarioImpl)ScenarioUtils.loadScenario(config);
		readNetwork(NETWORK_FILENAME);
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

	private Id makeLinkId(int i, int j) {
		return scenario.createId("i("+i+","+j+")");
	}

	private void createCarrierPlans() {
		for(CarrierImpl carrier : carriers.getCarriers().values()){
			TrivialCarrierPlanBuilder trivialCarrierPlanBuilder = new TrivialCarrierPlanBuilder();
			CarrierPlan plan = trivialCarrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrier.getContracts());
//			VRPCarrierPlanBuilder vrpCarrierPlanBuilder = new VRPCarrierPlanBuilder(scenario.getNetwork());
//			CarrierPlan plan = vrpCarrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrier.getContracts());
			carrier.getPlans().add(plan);
			carrier.setSelectedPlan(plan);
		}
	}

	private void giveContractsToCarriers(List<Contract> contracts) {
		for (CarrierImpl carrier : carriers.getCarriers().values()) {
			carrier.getContracts().clear();
		}
		for(Contract contract : contracts) {
			Map<Id, CarrierImpl> carrierMap = carriers.getCarriers();
			carrierMap.get(contract.getOffer().getCarrierId()).getContracts().add(contract);
		}
	}

	private void createCarriers() {
		carriers = new Carriers();
		
		CarrierImpl carrier1 = createCarrier(0,10);
		carriers.getCarriers().put(carrier1.getId(), carrier1);
		
		CarrierImpl carrier2 = createCarrier(GRID_SIZE,10);
		carriers.getCarriers().put(carrier2.getId(), carrier2);
		
		CarrierImpl carrier3 = createVorlaufCarrier(0,20,"c3");
		carriers.getCarriers().put(carrier3.getId(), carrier3);
		
		CarrierImpl carrier4 = createVorlaufCarrier(GRID_SIZE,45,"c4");
//		for(int i=0;i<9;i++){
//			carrier4.getKnowledge().getNoGoLocations().add(scenario.createId("i(1,"+i+")"));
//		}
		carriers.getCarriers().put(carrier4.getId(), carrier4);
		
		scenario.addScenarioElement(carriers);
	}

	private CarrierImpl createVorlaufCarrier(int locationIndex, int vehicleCapacity, String name) {
		CarrierImpl carrier = new CarrierImpl(new IdImpl(name), makeLinkId(GRID_SIZE/2, locationIndex));
		CarrierCapabilities cc = new CarrierCapabilities();
		carrier.setCarrierCapabilities(cc);
		CarrierVehicle carrierVehicle = new CarrierVehicle(new IdImpl("carrier-"+name+"-vehicle"), makeLinkId(GRID_SIZE/2, locationIndex));
		carrierVehicle.setCapacity(vehicleCapacity);
		cc.getCarrierVehicles().add(carrierVehicle);
		CarrierKnowledge knowledge = new CarrierKnowledge();
		carrier.setKnowledge(knowledge);
		return carrier;
	}

	private CarrierImpl createCarrier(int locationIndex, int vehicleCapacity) {
		CarrierImpl carrier = new CarrierImpl(new IdImpl("carrier-"+locationIndex), makeLinkId(GRID_SIZE/2, locationIndex));
		CarrierCapabilities cc = new CarrierCapabilities();
		carrier.setCarrierCapabilities(cc);
		CarrierVehicle carrierVehicle = new CarrierVehicle(new IdImpl("carrier-"+locationIndex+"-vehicle"), makeLinkId(GRID_SIZE/2, locationIndex));
		carrierVehicle.setCapacity(vehicleCapacity);
		cc.getCarrierVehicles().add(carrierVehicle);
		CarrierKnowledge knowledge = new CarrierKnowledge();
		carrier.setKnowledge(knowledge);
		return carrier;
	}

	private void createTransportServiceProviderWithContracts() {
		transportServiceProviders = new TransportServiceProviders();
		TransportServiceProviderImpl tsp = new TransportServiceProviderImpl(new IdImpl("guenter"));
		TSPCapabilities cap = new TSPCapabilities();
		cap.getTransshipmentCentres().add(new IdImpl("j(8,4)"));
		tsp.setTspCapabilities(cap);
		TSPKnowledge knowledge = new TSPKnowledge();
		tsp.setKnowledge(knowledge);
		logger.debug("TransportServiceProvider " + tsp.getId() + " has come into play");
		printCap(cap);
		createContracts(tsp);
		transportServiceProviders.getTransportServiceProviders().add(tsp);
	}

	private void createInitialPlans(TransportServiceProviderImpl tsp) {
		createAndSelectAPlan(tsp);
	}

	private void createAndSelectAPlan(TransportServiceProviderImpl tsp) {
		CheapestCarrierWithVorlaufTSPPlanBuilder tspPlanBuilder = new CheapestCarrierWithVorlaufTSPPlanBuilder();
		tspPlanBuilder.setCarrierAgentTracker(carrierAgentTracker);
		List<Id> emptyList = Collections.emptyList();
		tspPlanBuilder.setTransshipmentCentres(emptyList);
		TSPPlan directPlan = tspPlanBuilder.buildPlan(tsp.getContracts(),tsp.getTspCapabilities(),tsp.getKnowledge());
		printTSPPlan(directPlan);
		tsp.getPlans().add(directPlan);
		tsp.setSelectedPlan(directPlan);
		
//		CheapestCarrierWithVorlaufTSPPlanBuilder logisticCentrePlanBuilder = new CheapestCarrierWithVorlaufTSPPlanBuilder();
//		logisticCentrePlanBuilder.setCarrierAgentTracker(carrierAgentTracker);
//		logisticCentrePlanBuilder.setTransshipmentCentres(tsp.getTspCapabilities().getTransshipmentCentres());
//		TSPPlan logCentrePlan = logisticCentrePlanBuilder.buildPlan(tsp.getContracts(),tsp.getTspCapabilities(), tsp.getKnowledge());
//		tsp.getPlans().add(logCentrePlan);
//		tsp.setSelectedPlan(logCentrePlan);
	}

	private void createContracts(TransportServiceProviderImpl tsp) {
		for (int destinationColumn = 0; destinationColumn <= GRID_SIZE; destinationColumn++) {
			if(destinationColumn == 4){
				continue;
			}
			Id sourceLinkId = scenario.createId("spikeR");
			Id destinationLinkId = makeLinkId(1, destinationColumn);
			tsp.getContracts().add(createContract(sourceLinkId, destinationLinkId));
		}
		logger.debug("he has " + tsp.getContracts().size() + " contracts");
		printContracts(tsp.getContracts());
	}

	private TSPContract createContract(Id sourceLinkId, Id destinationLinkId) {
		TSPShipment tspShipment = new TSPShipment(sourceLinkId, destinationLinkId, 5, new TSPShipment.TimeWindow(0.0, 24*3600), new TSPShipment.TimeWindow(0.0,24*3600));
		TSPContract tspContract = new TSPContract(tspShipment,null);
		return tspContract;
	}

	private void printTSPPlan(TSPPlan plan) {
		logger.debug("transportChains:");
		for(TransportChain chain : plan.getChains()){
			logger.debug(chain);
		}

	}

	private void printCap(TSPCapabilities cap) {
		logger.debug("TransshipmentCentres:");
		for(Id id : cap.getTransshipmentCentres()){
			logger.debug(id);
		}

	}

	private void printContracts(Collection<TSPContract> contracts) {
		int count = 1;
		for(TSPContract c : contracts){
			for(TSPShipment s : c.getShipments()){
				logger.debug("shipment " + count + ": " + s);
				count++;
			}
		}

	}

}
