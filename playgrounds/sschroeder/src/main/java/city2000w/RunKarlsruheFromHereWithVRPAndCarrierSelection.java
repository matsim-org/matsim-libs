/**
 * 
 */
package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.city2000w.AgentObserver;
import playground.mzilske.city2000w.City2000WMobsimFactory;
import playground.mzilske.city2000w.DefaultLSPShipmentTracker;
import playground.mzilske.freight.CarrierAgentFactory;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierKnowledge;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.Offer;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.TransportServiceProviders;
import freight.AnotherCarrierAgentFactory;
import freight.ShipperAgentTracker;
import freight.ShipperImpl;
import freight.ShipperUtils;
import freight.Shippers;
import freight.TSPUtils;

/**
 * @author schroeder
 *
 */
public class RunKarlsruheFromHereWithVRPAndCarrierSelection implements ScoringListener, ReplanningListener, StartupListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

	private static final int NUMBEROFCARRIERS = 5;
	
	private static final int NUMBEROFTSPSHIPMENTS = 20;
	
	private static Logger logger = Logger.getLogger(RunKarlsruheFromHereWithVRPAndCarrierSelection.class);
	
	private Carriers carriers;
	
	private TransportServiceProviders transportServiceProviders;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private TSPAgentTracker tspAgentTracker;
	
	private ShipperAgentTracker shipperAgentTracker;
	
	private Shippers shippers;

	private ScenarioImpl scenario;
	
	private AgentObserver agentObserver;
	
	private static final String NETWORK_FILENAME = "../playgrounds/sschroeder/networks/karlsruheNetwork.xml";
	
	MarginalCostCalculator marginalCostCalculator;

	private boolean liveModus = false;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
		RunKarlsruheFromHereWithVRPAndCarrierSelection runner = new RunKarlsruheFromHereWithVRPAndCarrierSelection();
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
		
		createShippers();
		
		shipperAgentTracker = new ShipperAgentTracker(shippers.getShippers());
		
		
		event.getControler().getScenario().addScenarioElement(carriers);
		
		CarrierAgentFactory carrierAgentFactory = new AnotherCarrierAgentFactory(scenario.getNetwork(), controler.createRoutingAlgorithm());
		carrierAgentTracker = new CarrierAgentTracker(carriers.getCarriers().values(), controler.createRoutingAlgorithm(), scenario.getNetwork(), carrierAgentFactory);
		carrierAgentTracker.getShipmentStatusListeners().add(tspAgentTracker);
		carrierAgentTracker.getCostListeners().add(tspAgentTracker);
		
		
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, carrierAgentTracker);
		mobsimFactory.setUseOTFVis(liveModus );
		event.getControler().setMobsimFactory(mobsimFactory);
		
		agentObserver = new AgentObserver("foo",scenario.getNetwork());
		agentObserver.setOutFile("../playgrounds/sschroeder/output/karlsruhe.txt");
		event.getControler().getEvents().addHandler(agentObserver);
	
		
	}

	private void createShippers() {
//		shippers = new Shippers();
//		ShipperImpl shipper = ShipperUtils.createShipper("shipper_stefan", locationId);
//		shippers.getShippers().add(arg0);
		
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
		carrierAgentTracker.reset(event.getIteration());
		tspAgentTracker.reset();
		agentObserver.reset(0);
		agentObserver.writeStats();
	}


	@Override
	public void notifyReplanning(ReplanningEvent event) {
		replanTsp();
		clearCarrierContracts();
		createCarrierContracts(tspAgentTracker.createCarrierContracts());
		replanCarriers();
		
//		Collection<CarrierImpl> carriersToReplan = replanOneTspAndGetCarriers2BeReplanned();
//		replanSelectedCarriers(carriersToReplan);
		
		
	}

	private Collection<CarrierImpl> replanOneTspAndGetCarriers2BeReplanned() {
		Collection<CarrierImpl> carriers = new ArrayList<CarrierImpl>();
		
		return null;
	}

	private void clearCarrierContracts() {
		for(CarrierImpl c : carriers.getCarriers().values()){
			c.getContracts().clear();
		}
	}

	private void replanCarriers() {
		for(CarrierImpl carrier : carriers.getCarriers().values()){
			RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder planBuilder = new RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder(scenario.getNetwork());
			CarrierPlan plan = planBuilder.buildPlan(carrier.getCarrierCapabilities(), carrier.getContracts());
			carrier.getPlans().add(plan);
			carrier.setSelectedPlan(plan);
		}
		
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		for(CarrierImpl c : carriers.getCarriers().values()){
			logger.info("carrierId=" + c.getId() + " planScore=" + c.getSelectedPlan().getScore());
		}
		
	}

	private void replanTsp() {
		logger.info("offerTime");
		Collection<Id> carriersToReplan = new ArrayList<Id>();
		Id from = makeId("160038764_160038765");
		Id to_stefantje = makeId("160324487_160089302");
		Id to_10 = makeId("160420669_160086968");
		int size = 10;
		Collection<Offer>  offers = carrierAgentTracker.getOffers(from, to_10, size, 0.0, 0.0, 0.0, 0.0);
		Offer bestOffer = null;
		for(Offer o : offers){
			if(o == null){
				continue;
			}
			logger.info("carrierId=" + o.getCarrierId() + " offer=" + o.getPrice());
			if(bestOffer == null){
				bestOffer = o;
			}
			else if(o.getPrice() < bestOffer.getPrice()){
				bestOffer = o;
			}
		}
		TransportServiceProviderImpl tsp = transportServiceProviders.getTransportServiceProviders().iterator().next();
		TSPShipment tspShipment = TSPUtils.createTSPShipment(from, to_stefantje, size, 0.0, 0.0, 0.0, 0.0);
		TSPUtils.createAndAddTSPContract(tsp, tspShipment);
		TransportChainBuilder chainBuilder = new TransportChainBuilder(tspShipment);
		chainBuilder.schedulePickup(tspShipment.getFrom(), tspShipment.getPickUpTimeWindow());
		chainBuilder.scheduleLeg(bestOffer);
		chainBuilder.scheduleDelivery(tspShipment.getTo(), tspShipment.getDeliveryTimeWindow());
		TransportChain chain = chainBuilder.build();
//		Collection<Contract> carrierContracts = tspAgentTracker.registerChainAndGetCarrierContracts(tsp,chain);
		tsp.getSelectedPlan().getChains().add(chain);
		carriersToReplan.add(bestOffer.getCarrierId());
		
//		Collection<Offer>  otherOffers = carrierAgentTracker.getOffers(from, to_stefantje, size, 0.0, 0.0, 0.0, 0.0);
//		for(Offer o : otherOffers){
//			if(o == null){
//				continue;
//			}
//			logger.info("carrierId=" + o.getCarrierId() + " offer=" + o.getPrice());
//		}
	}

	private void run(){
		Config config = new Config();
		config.addCoreModules();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
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
			RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder planBuilder = new RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder(scenario.getNetwork());
//			RuinAndRecreateCarrierPlanBuilder planBuilder = new RuinAndRecreateCarrierPlanBuilder(scenario.getNetwork());
//			ClarkeAndWrightCarrierPlanBuilder planBuilder = new ClarkeAndWrightCarrierPlanBuilder(scenario.getNetwork());
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
			c.setKnowledge(new CarrierKnowledge());
		}
	}

	private IdImpl makeId(String idString) {
		return new IdImpl(idString);
	}

	private void createTransportServiceProviderWithContracts(int nOfShipments) {
		TransportServiceProviderImpl tsp = new TransportServiceProviderImpl(new IdImpl("guenter"));
		TSPCapabilities cap = new TSPCapabilities();
		IdImpl tscEttlingen = makeId("160356161_160281801");
		cap.getTransshipmentCentres().add(tscEttlingen);
		tsp.setTspCapabilities(cap);
		printCap(cap);
		makeKarlsruheContracts(tsp);
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
		tspPlanBuilder.setTransshipmentCentres(Collections.EMPTY_LIST);
		TSPPlan directPlan = tspPlanBuilder.buildPlan(tsp.getContracts(),tsp.getTspCapabilities());
		printTSPPlan(directPlan);
		tsp.getPlans().add(directPlan);
		tsp.setSelectedPlan(directPlan);
	}

	private TSPContract createContract(Id sourceLinkId, Id destinationLinkId) {
		TSPOffer offer = new TSPOffer();
		TSPShipment tspShipment = new TSPShipment(sourceLinkId, destinationLinkId, 5, new TSPShipment.TimeWindow(0.0, 24*3600), new TSPShipment.TimeWindow(0.0,24*3600));
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

}
