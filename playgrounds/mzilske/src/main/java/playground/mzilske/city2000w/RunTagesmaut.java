/**
 * 
 */
package playground.mzilske.city2000w;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierAgentTrackerBuilder;
import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.TransportServiceProviders;

/**
 * @author schroeder
 *
 */
public class RunTagesmaut implements StartupListener, ScoringListener, ReplanningListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

	private static final int GRID_SIZE = 8;

	private static Logger logger = Logger.getLogger(RunTagesmaut.class);

	private Carriers carriers;
	private TransportServiceProviders transportServiceProviders;

	private CarrierAgentTracker freightAgentTracker;
	private TSPAgentTracker tspAgentTracker;

	private ScenarioImpl scenario;

	private static final String NETWORK_FILENAME = "output/grid.xml";


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RunTagesmaut runner = new RunTagesmaut();
		runner.run();
	}

	@Override
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
		freightAgentTracker = freightAgentTrackerBuilder.build();
		freightAgentTracker.getShipmentStatusListeners().add(tspAgentTracker);


		createTSPPlans();

		giveContractsToCarriers(tspAgentTracker.createCarrierContracts());

		createCarrierPlans();

		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, freightAgentTracker);
		mobsimFactory.setUseOTFVis(true);
		event.getControler().setMobsimFactory(mobsimFactory);


	}

	private void createTSPPlans() {
		for (TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()) {
			createInitialPlans(tsp);
		}
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		freightAgentTracker.createPlanAgents();
		Controler controler = event.getControler();
		controler.getEvents().addHandler(freightAgentTracker);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().removeHandler(freightAgentTracker);
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		logger.info("carrierAgents are calculating costs ...");
		freightAgentTracker.calculateCostsScoreCarriersAndInform();
		logger.info("transportServiceProvider are calculating costs ...");
		tspAgentTracker.calculateCostsScoreTSPAndInform();

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		logger.info("Reset costs/score of tspAgents");
		freightAgentTracker.reset(event.getIteration());
		tspAgentTracker.reset();
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {

		createTSPPlans();
		giveContractsToCarriers(tspAgentTracker.createCarrierContracts());

		replanCarriers();
	}


	private void replanCarriers() {
		createCarrierPlans();
	}



	private void run(){
		Config config = new Config();
		config.addCoreModules();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
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
		CarrierImpl carrier1 = createCarrier(0);
		carrier1.getCarrierCapabilities().getCarrierVehicles().iterator().next().setCapacity(10);
		CarrierImpl carrier2 = createCarrier(GRID_SIZE);
		carriers.getCarriers().put(carrier1.getId(), carrier1);
		carriers.getCarriers().put(carrier2.getId(), carrier2);
		scenario.addScenarioElement(carriers);
	}

	

	private CarrierImpl createCarrier(int j) {
		CarrierImpl carrier = new CarrierImpl(new IdImpl("carrier-"+j), makeLinkId(GRID_SIZE/2, j));
		CarrierCapabilities cc = new CarrierCapabilities();
		carrier.setCarrierCapabilities(cc);
		CarrierVehicle carrierVehicle = new CarrierVehicle(new IdImpl("carrier-"+j+"-vehicle"), makeLinkId(GRID_SIZE/2, j));
		cc.getCarrierVehicles().add(carrierVehicle);
		return carrier;
	}

	private void createTransportServiceProviderWithContracts() {
		transportServiceProviders = new TransportServiceProviders();
		TransportServiceProviderImpl tsp = new TransportServiceProviderImpl(new IdImpl("guenter"));
		TSPCapabilities cap = new TSPCapabilities();
		cap.getTransshipmentCentres().add(new IdImpl("j(2,4)"));
		tsp.setTspCapabilities(cap);
		logger.debug("TransportServiceProvider " + tsp.getId() + " has come into play");
		printCap(cap);
		createContracts(tsp);

		transportServiceProviders.getTransportServiceProviders().add(tsp);
	}

	private void createInitialPlans(TransportServiceProviderImpl tsp) {
		createAndSelectAPlan(tsp);
	}

	private void createAndSelectAPlan(TransportServiceProviderImpl tsp) {
		CheapestCarrierTSPPlanBuilder tspPlanBuilder = new CheapestCarrierTSPPlanBuilder();
		tspPlanBuilder.setCarrierAgentTracker(freightAgentTracker);
		List<Id> emptyList = Collections.emptyList();
		tspPlanBuilder.setTransshipmentCentres(emptyList);
		TSPPlan directPlan = tspPlanBuilder.buildPlan(tsp.getContracts(),tsp.getTspCapabilities());
		printTSPPlan(directPlan);
		tsp.getPlans().add(directPlan);
		tsp.setSelectedPlan(directPlan);
	}

	private void createManyContracts(TransportServiceProviderImpl tsp) {
		for (int sourceColumn = 0; sourceColumn <= GRID_SIZE; sourceColumn++) {
			Id sourceLinkId = makeLinkId(1, sourceColumn);
			for (int destinationColumn = 0; destinationColumn <= GRID_SIZE; destinationColumn++) {
				Id destinationLinkId = makeLinkId(GRID_SIZE, destinationColumn);
				tsp.getContracts().add(createContract(tsp.getId(),sourceLinkId, destinationLinkId));
			}
		}
		logger.debug("he has " + tsp.getContracts().size() + " contracts");
		printContracts(tsp.getContracts());
	}

	private void createContracts(TransportServiceProviderImpl tsp) {

		for (int destinationColumn = 0; destinationColumn <= GRID_SIZE; destinationColumn++) {
	//	for (int destinationColumn = 0; destinationColumn <= 0; destinationColumn++) {
			Id sourceLinkId = makeLinkId(GRID_SIZE, destinationColumn);
			Id destinationLinkId = makeLinkId(1, destinationColumn);
			tsp.getContracts().add(createContract(tsp.getId(),sourceLinkId, destinationLinkId));
		}
//		for (int destinationColumn = 5; destinationColumn <= 5; destinationColumn++) {
//			Id sourceLinkId = makeLinkId(1, destinationColumn);
//			Id destinationLinkId = makeLinkId(GRID_SIZE, destinationColumn);
//			tsp.getContracts().add(createContract(sourceLinkId, destinationLinkId));
//		}
		logger.debug("he has " + tsp.getContracts().size() + " contracts");
		printContracts(tsp.getContracts());
	}

	private TSPContract createContract(Id tspId, Id sourceLinkId, Id destinationLinkId) {
		TSPShipment tspShipment = new TSPShipment(sourceLinkId, destinationLinkId, 5, new TSPShipment.TimeWindow(0.0, 24*3600), new TSPShipment.TimeWindow(0.0,24*3600));
		TSPOffer offer = new TSPOffer();
		offer.setTspId(tspId);
		TSPContract tspContract = new TSPContract(tspShipment, offer);
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
