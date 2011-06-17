/**
 * 
 */
package city2000w;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.city2000w.AgentObserver;
import playground.mzilske.city2000w.City2000WMobsimFactory;
import playground.mzilske.city2000w.DefaultLSPShipmentTracker;
import playground.mzilske.city2000w.SelectedPlanReplicator;
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
public class RunKarlsruheFromHereWithUmschlagWithVRP implements StartupListener, ShutdownListener, ScoringListener, ReplanningListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

	private static final int GRID_SIZE = 8;

	private static final int NUMBEROFCARRIERS = 100;
	
	private static final int NUMBEROFTSPSHIPMENTS = 500;
	
	private static final Long SEED = Long.MAX_VALUE;
	
	private static Logger logger = Logger.getLogger(RunKarlsruheFromHereWithUmschlagWithVRP.class);
	
	private Carriers carriers;
	private TransportServiceProviders transportServiceProviders;
	
	private CarrierAgentTracker freightAgentTracker;
	private TSPAgentTracker tspAgentTracker;

	private ScenarioImpl scenario;
	
	private AgentObserver simStats;
	
	private static final String NETWORK_FILENAME = "../FreightModel/input/karlsruheNetwork.xml";
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.INFO);
		RunKarlsruheFromHereWithUmschlagWithVRP runner = new RunKarlsruheFromHereWithUmschlagWithVRP();
		runner.run();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		
		Controler controler = event.getControler();
		
		createCarriers(NUMBEROFCARRIERS);
		
		createTransportServiceProviderWithContracts(NUMBEROFTSPSHIPMENTS);
		
		tspAgentTracker = new TSPAgentTracker(transportServiceProviders.getTransportServiceProviders());
		
		tspAgentTracker.getCostListeners().add(new DefaultLSPShipmentTracker());
		
		createCarrierContracts(tspAgentTracker.createCarrierContracts());
		
		createCarrierPlans();
		
		event.getControler().getScenario().addScenarioElement(carriers);
		
		CarrierAgentTrackerBuilder freightAgentTrackerBuilder = new CarrierAgentTrackerBuilder();
		freightAgentTrackerBuilder.setCarriers(controler.getScenario().getScenarioElement(Carriers.class).getCarriers().values());
		freightAgentTrackerBuilder.setRouter(controler.createRoutingAlgorithm());
		freightAgentTrackerBuilder.setNetwork(controler.getNetwork());
		freightAgentTrackerBuilder.setEventsManager(controler.getEvents());
		freightAgentTrackerBuilder.addCarrierCostListener(tspAgentTracker);
		freightAgentTracker = freightAgentTrackerBuilder.build();
		freightAgentTracker.getShipmentStatusListeners().add(tspAgentTracker);
		
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, freightAgentTracker);
		mobsimFactory.setUseOTFVis(false);
		event.getControler().setMobsimFactory(mobsimFactory);
		
		simStats = new AgentObserver("Karlsruhe with umschlag", scenario.getNetwork());
		simStats.setOutFile("../FreightModel/output/ka_umschlag_cw.txt");
		event.getControler().getEvents().addHandler(simStats);
		
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().addHandler(freightAgentTracker);
		freightAgentTracker.createPlanAgents();
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().removeHandler(freightAgentTracker);
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
//		logger.info("carrierAgents are calculating costs ...");
//		freightAgentTracker.calculateCostsScoreCarriersAndInform();
//		logger.info("transportServiceProvider are calculating costs ...");
//		tspAgentTracker.calculateCostsScoreTSPAndInform();
//		
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

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		simStats.reset(1);
		simStats.writeStats();
	}

	private void replanTSP() {
//		// TODO Auto-generated method stub
//		for (TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()) {
//			List<TSPPlan> plans = (List) tsp.getPlans();
//			TSPPlan selectedPlan = tsp.getSelectedPlan();
//			logger.info("The plan we just used got us "+selectedPlan.getScore()+" points. Now trying the other one.");
//			tsp.setSelectedPlan(plans.get(1 - plans.indexOf(selectedPlan)));
//		}
	}

	private void replanCarriers() {
		for(CarrierImpl carrier : carriers.getCarriers().values()){
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

	private void createCarrierPlans() {
		for(CarrierImpl carrier : carriers.getCarriers().values()){
//			RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder planBuilder = new RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder(scenario.getNetwork());
//			RuinAndRecreateCarrierPlanBuilder planBuilder = new RuinAndRecreateCarrierPlanBuilder(scenario.getNetwork());
			ClarkeAndWrightCarrierPlanBuilder planBuilder = new ClarkeAndWrightCarrierPlanBuilder(scenario.getNetwork());
			CarrierPlan plan = planBuilder.buildPlan(carrier.getCarrierCapabilities(), carrier.getContracts());
			carrier.getPlans().add(plan);
			carrier.setSelectedPlan(plan);
		}
	}

	private void createCarrierContracts(List<Contract> contracts) {
		for(Contract contract : contracts){
			Id carrierId = contract.getOffer().getCarrierId();
			carriers.getCarriers().get(carrierId).getContracts().add(contract);
		}	
	}

	private void createCarriers(int nOfCarriers) {
		carriers = new Carriers();
		createKarlsurheCarriers();
	}

	private void createKarlsurheCarriers() {
		CarrierImpl c_durlacherTor = new CarrierImpl(makeId("durlacherTor-carrier"), makeId("160093860_160093861"));
		carriers.getCarriers().put(c_durlacherTor.getId(), c_durlacherTor);
		CarrierImpl c_ettlingerTor = new CarrierImpl(makeId("ettlingerTor-carrier"), makeId("160622804_160606750"));
		carriers.getCarriers().put(c_ettlingerTor.getId(),c_ettlingerTor);
		CarrierImpl c_muehlburgerTor = new CarrierImpl(makeId("muehlburgerTor-carrier"), makeId("160057858_160057859"));
		carriers.getCarriers().put(c_muehlburgerTor.getId(), c_muehlburgerTor);
		CarrierImpl c_neureut = new CarrierImpl(makeId("neureut-carrier"), makeId("160499832_160207664"));
		carriers.getCarriers().put(c_neureut.getId(), c_neureut);
		CarrierImpl c_daxlanden = new CarrierImpl(makeId("daxlanden-carrier"), makeId("160260604_160168056"));
		carriers.getCarriers().put(c_daxlanden.getId(), c_daxlanden);
		CarrierImpl c_blankenloch = new CarrierImpl(makeId("blankenloch-carrier"), makeId("160200856_160095443"));
		carriers.getCarriers().put(c_blankenloch.getId(), c_blankenloch);	
		
		for(CarrierImpl c : carriers.getCarriers().values()){
			CarrierCapabilities cc = new CarrierCapabilities();
			c.setCarrierCapabilities(cc);
			CarrierVehicle carrierVehicle = new CarrierVehicle(makeId(c.getId().toString() + "-vehicle"), c.getDepotLinkId());
			cc.getCarrierVehicles().add(carrierVehicle);
			carrierVehicle.setCapacity(15);
		}
	}

	private IdImpl makeId(String idString) {
		return new IdImpl(idString);
	}

	private CarrierImpl createCarrier(int linkPosInNetwork) {
		Id linkId = pickLinkIdFromNetwork(linkPosInNetwork);
		CarrierImpl carrier = new CarrierImpl(new IdImpl("carrier-"+linkPosInNetwork), linkId);
		CarrierCapabilities cc = new CarrierCapabilities();
		carrier.setCarrierCapabilities(cc);
		CarrierVehicle carrierVehicle = new CarrierVehicle(new IdImpl("carrier-"+linkPosInNetwork+"-vehicle"), linkId);
		cc.getCarrierVehicles().add(carrierVehicle);
		return carrier;
	}

	private void createTransportServiceProviderWithContracts(int nOfShipments) {
		TransportServiceProviderImpl tsp = new TransportServiceProviderImpl(new IdImpl("guenter"));
		TSPCapabilities cap = new TSPCapabilities();
		IdImpl tscEttlingen = makeId("160356161_160281801");
		cap.getTransshipmentCentres().add(tscEttlingen);
		tsp.setTspCapabilities(cap);
		printCap(cap);
		makeKarlsruheContracts(tsp);
		//createContracts(tsp,nOfShipments);
		createInitialPlans(tsp);
		transportServiceProviders = new TransportServiceProviders();
		transportServiceProviders.getTransportServiceProviders().add(tsp);
	}

	private void makeKarlsruheContracts(TransportServiceProviderImpl tsp) {
		Id from = makeId("160038764_160038765");
		
		Id to_stefantje = makeId("160324487_160089302");
		tsp.getContracts().add(createContract(from, to_stefantje));
		
		Id to_lars = makeId("160685639_160099410");
		tsp.getContracts().add(createContract(from, to_lars));
		
		Id to_aaron = makeId("160096359_160609343");
		tsp.getContracts().add(createContract(from, to_aaron));
		
		Id to_4 = makeId("160425761_160339189");
		tsp.getContracts().add(createContract(from, to_4));
		
		Id to_5 = makeId("160087060_160018372");
		tsp.getContracts().add(createContract(from, to_5));
		
		Id to_6 = makeId("160337225_160337224");
		tsp.getContracts().add(createContract(from, to_6));
		
		Id to_7 = makeId("160161936_160271340");
		tsp.getContracts().add(createContract(from, to_7));
		
		Id to_8 = makeId("160117522_160337532");
		tsp.getContracts().add(createContract(from, to_8));
		
		Id to_9 = makeId("160175985_160236797");
		tsp.getContracts().add(createContract(from, to_9));
		
		Id to_10 = makeId("160420669_160086968");
		tsp.getContracts().add(createContract(from, to_10));
		
		Id to_11 = makeId("160535693_160112497");
		tsp.getContracts().add(createContract(from, to_11));
	}
	
	private void createInitialPlans(TransportServiceProviderImpl tsp) {
		SimpleTSPPlanBuilder tspPlanBuilder = new SimpleTSPPlanBuilder(scenario.getNetwork());
		tspPlanBuilder.setCarriers(carriers.getCarriers().values());
		tspPlanBuilder.setTransshipmentCentres(tsp.getTspCapabilities().getTransshipmentCentres());
		TSPPlan directPlan = tspPlanBuilder.buildPlan(tsp.getContracts(),tsp.getTspCapabilities());
		printTSPPlan(directPlan);
		tsp.getPlans().add(directPlan);
		tsp.setSelectedPlan(directPlan);
	}

	private void createContracts(TransportServiceProviderImpl tsp, int nOfShipments) {
		Random random = new Random(SEED);
		Id sourceLinkId = new IdImpl("160038764_160038765"); //bruchsal
		for(int i=0; i<nOfShipments; i++){
			Id destinationLinkId = pickLinkIdFromNetwork(random.nextInt(scenario.getNetwork().getLinks().values().size()));
			tsp.getContracts().add(createContract(sourceLinkId, destinationLinkId));
		}
	}

	private Id pickLinkIdFromNetwork(int i) {
		int count=0;
		for(Link link : scenario.getNetwork().getLinks().values()){
			if(i==count){
				return link.getId();
			}
			count++;
		}
		throw new RuntimeException("no linkId found");
	}

	private TSPContract createContract(Id sourceLinkId, Id destinationLinkId) {
		TSPShipment tspShipment = new TSPShipment(sourceLinkId, destinationLinkId, 5, new TSPShipment.TimeWindow(0.0, 24*3600), new TSPShipment.TimeWindow(0.0,24*3600));
		TSPOffer offer = new TSPOffer();
		TSPContract tspContract = new TSPContract(tspShipment,offer);
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
