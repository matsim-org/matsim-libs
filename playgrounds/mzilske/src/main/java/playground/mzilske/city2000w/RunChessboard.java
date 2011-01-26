/**
 * 
 */
package playground.mzilske.city2000w;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
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

import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierAgentTrackerBuilder;
import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.HyperNetworkBuilder;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.TransportServiceProviders;

/**
 * @author schroeder
 *
 */
public class RunChessboard implements StartupListener, ScoringListener, ReplanningListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

	private static final int GRID_SIZE = 8;

	private static Logger logger = Logger.getLogger(RunChessboard.class);
	
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
		RunChessboard runner = new RunChessboard();
		runner.run();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Controler controler = event.getControler();
		
		createCarriers();
		createTransportServiceProviderWithContracts();
		
		
		
		tspAgentTracker = new TSPAgentTracker(transportServiceProviders.getTransportServiceProviders());
		tspAgentTracker.getCostListeners().add(new DefaultLSPShipmentTracker());
		
		createCarrierContracts(tspAgentTracker.createCarrierShipments());
		
		createCarrierPlans();
		
		event.getControler().getScenario().addScenarioElement(carriers);
		
		CarrierAgentTrackerBuilder freightAgentTrackerBuilder = new CarrierAgentTrackerBuilder();
		freightAgentTrackerBuilder.setCarriers(controler.getScenario().getScenarioElement(Carriers.class).getCarriers());
		freightAgentTrackerBuilder.setRouter(controler.createRoutingAlgorithm());
		freightAgentTrackerBuilder.setNetwork(controler.getNetwork());
		freightAgentTrackerBuilder.setEventsManager(controler.getEvents());
		freightAgentTrackerBuilder.addCarrierCostListener(tspAgentTracker);
		freightAgentTracker = freightAgentTrackerBuilder.build();
		freightAgentTracker.getShipmentStatusListeners().add(tspAgentTracker);
		
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
		tspAgentTracker.reset();
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		replanTSP();
		// replanCarriers();
	}

	private void replanTSP() {
		// TODO Auto-generated method stub
		for (TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()) {
			List<TSPPlan> plans = (List) tsp.getPlans();
			TSPPlan selectedPlan = tsp.getSelectedPlan();
			logger.info("The plan we just used got us "+selectedPlan.getScore()+" points. Now trying the other one.");
			tsp.setSelectedPlan(plans.get(1 - plans.indexOf(selectedPlan)));
		}
	}

	private void replanCarriers() {
		for(CarrierImpl carrier : carriers.getCarriers()){
			replan(carrier);
		}
	}

	private void replan(CarrierImpl carrier) {
		SelectedPlanReplicator replanner = new SelectedPlanReplicator();
		CarrierPlan newPlan = replanner.replan(carrier.getCarrierCapabilities(),carrier.getContracts(),carrier.getSelectedPlan());
		carrier.getPlans().add(newPlan);
		carrier.setSelectedPlan(newPlan);
	}

	private void scoreLogisticServiceProvider() {
		// do something
	}	
	

	private void run(){
		Config config = new Config();
		config.addCoreModules();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		scenario = new ScenarioImpl(config);
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
		for(CarrierImpl carrier : carriers.getCarriers()){
			TrivialCarrierPlanBuilder trivialCarrierPlanBuilder = new TrivialCarrierPlanBuilder();
			CarrierPlan plan = trivialCarrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrier.getContracts());
			carrier.getPlans().add(plan);
			carrier.setSelectedPlan(plan);
		}
	}

	private void createCarrierContracts(Map<Id, List<Shipment>> carrierShipments) {
		for(Id carrierId : carrierShipments.keySet()){
			for(CarrierImpl carrier : carriers.getCarriers()){
				if(carrier.getId().equals(carrierId)){
					for(Shipment s : carrierShipments.get(carrierId)){
						carrier.getContracts().add(new Contract(Arrays.asList(s)));
					} 
				}
			}
		}	
	}

	private void createCarriers() {
		carriers = new Carriers();
		for (int j = 0; j <= GRID_SIZE; j++) {
			CarrierImpl carrier = createCarrier(j);
			carriers.getCarriers().add(carrier);
		}
		CarrierImpl vorlaufCarrier = createVorlaufCarrier();
		carriers.getCarriers().add(vorlaufCarrier);
	}

	private CarrierImpl createVorlaufCarrier() {
		CarrierImpl carrier = new CarrierImpl(new IdImpl("vorlauf-carrier"), new IdImpl("j(2,4)"));
		CarrierCapabilities cc = new CarrierCapabilities();
		carrier.setCarrierCapabilities(cc);
		CarrierVehicle carrierVehicle = new CarrierVehicle(new IdImpl("vorlauf-carrier-vehicle"), new IdImpl("j(2,4)"));
		cc.getCarrierVehicles().add(carrierVehicle);
		return carrier;
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
		TransportServiceProviderImpl tsp = new TransportServiceProviderImpl(new IdImpl("guenter"));
		TSPCapabilities cap = new TSPCapabilities();
		cap.getTransshipmentCentres().add(new IdImpl("j(2,4)"));
		tsp.setTspCapabilities(cap);
		logger.debug("TransportServiceProvider " + tsp.getId() + " has come into play");
		printCap(cap);
		createContracts(tsp);
		createInitialPlans(tsp);
		transportServiceProviders = new TransportServiceProviders();
		transportServiceProviders.getTransportServiceProviders().add(tsp);
		createHyperNetwork(tsp);
	}

	private void createHyperNetwork(TransportServiceProviderImpl tsp) {
		Collection<Id> locations = new HashSet<Id>();
		for (TSPContract contract : tsp.getContracts()) {
			for (TSPShipment shipment : contract.getShipments()) {
				locations.add(shipment.getFrom());
				locations.add(shipment.getTo());
			}
		}
		
		HyperNetworkBuilder hnb = new HyperNetworkBuilder(tsp.getTspCapabilities(), locations, carriers);
		
	}

	private void createInitialPlans(TransportServiceProviderImpl tsp) {
		// SimpleTSPPlanBuilder tspPlanBuilder = new SimpleTSPPlanBuilder();
		MinimumDepotDistanceToDestinationTSPPlanBuilder tspPlanBuilder = new MinimumDepotDistanceToDestinationTSPPlanBuilder(scenario.getNetwork());
		tspPlanBuilder.setCarriers(carriers.getCarriers());
		List<Id> emptyList = Collections.emptyList();
		tspPlanBuilder.setTransshipmentCentres(emptyList);
		TSPPlan directPlan = tspPlanBuilder.buildPlan(tsp.getContracts(),tsp.getTspCapabilities());
		printTSPPlan(directPlan);
		tsp.getPlans().add(directPlan);
		tsp.setSelectedPlan(directPlan);
		MinimumDepotDistanceToDestinationTSPPlanBuilder viaPlanBuilder = new MinimumDepotDistanceToDestinationTSPPlanBuilder(scenario.getNetwork());
		viaPlanBuilder.setTransshipmentCentres(Collections.singletonList(tsp.getTspCapabilities().getTransshipmentCentres().get(0)));
		viaPlanBuilder.setCarriers(carriers.getCarriers());
		TSPPlan viaPlan = viaPlanBuilder.buildPlan(tsp.getContracts(), tsp.getTspCapabilities());
		tsp.getPlans().add(viaPlan);
		tsp.setSelectedPlan(viaPlan);
	}

	private void createManyContracts(TransportServiceProviderImpl tsp) {
		for (int sourceColumn = 0; sourceColumn <= GRID_SIZE; sourceColumn++) {
			Id sourceLinkId = makeLinkId(1, sourceColumn);
			for (int destinationColumn = 0; destinationColumn <= GRID_SIZE; destinationColumn++) {
				Id destinationLinkId = makeLinkId(GRID_SIZE, destinationColumn);
				tsp.getContracts().add(createContract(sourceLinkId, destinationLinkId));
			}
		}
		logger.debug("he has " + tsp.getContracts().size() + " contracts");
		printContracts(tsp.getContracts());
	}
	
	private void createContracts(TransportServiceProviderImpl tsp) {
		for (int sourceColumn = 0; sourceColumn <= 0; sourceColumn++) {
			Id sourceLinkId = makeLinkId(1, sourceColumn);
			for (int destinationColumn = 0; destinationColumn <= GRID_SIZE; destinationColumn++) {
				Id destinationLinkId = makeLinkId(GRID_SIZE, destinationColumn);
				tsp.getContracts().add(createContract(sourceLinkId, destinationLinkId));
			}
		}
		logger.debug("he has " + tsp.getContracts().size() + " contracts");
		printContracts(tsp.getContracts());
	}

	private TSPContract createContract(Id sourceLinkId, Id destinationLinkId) {
		TSPContract tspContract = new TSPContract(Arrays.asList(new TSPShipment(sourceLinkId, destinationLinkId, 5, new TSPShipment.TimeWindow(0.0, 24*3600), new TSPShipment.TimeWindow(0.0,24*3600))));
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
