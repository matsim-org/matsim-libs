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
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.city2000w.AgentObserver;
import playground.mzilske.city2000w.City2000WMobsimFactory;
import playground.mzilske.freight.Carrier;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierDriverAgentFactoryImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.TransportServiceProviders;
import playground.mzilske.freight.api.CarrierAgentFactory;
import playground.mzilske.freight.api.Offer;
import playground.mzilske.freight.api.TSPAgentFactory;
import playground.mzilske.freight.events.OfferAcceptEvent;
import playground.mzilske.freight.events.OfferRejectEvent;
import playground.mzilske.freight.events.QueryOffersEvent;
import playground.mzilske.freight.events.Service;
import freight.TRBCarrierAgentFactoryImpl;
import freight.CarrierPlanReader;
import freight.CarrierUtils;
import freight.TSPAgentFactoryImpl;
import freight.TSPPlanReader;

/**
 * @author schroeder
 *
 */
public class RunKarlsruheScenario implements StartupListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener, ReplanningListener {

	private static Logger logger = Logger.getLogger(RunKarlsruheScenario.class);
	
	private Carriers carriers;
	
	private TransportServiceProviders transportServiceProviders;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private TSPAgentTracker tspAgentTracker;

	private ScenarioImpl scenario;
	
	private AgentObserver agentObserver;
	
	private static final String NETWORK_FILENAME = "../playgrounds/sschroeder/networks/karlsruheNetwork.xml";
	
	private static final String TSPPLAN_FILENAME = "../playgrounds/sschroeder/anotherInput/karlsruheTspPlans.xml";
	
	private static final String CARRIERNPLAN_FILENAME = "../playgrounds/sschroeder/anotherInput/karlsruheCarriers.xml";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
		RunKarlsruheScenario runner = new RunKarlsruheScenario();
		runner.run();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Controler controler = event.getControler();
		
		readCarriers();
		
		readTransportServiceProviders();
		
		
//		CarrierAgentFactory carrierAgentFactory = new TRBCarrierAgentFactoryImpl(scenario.getNetwork(), controler.createRoutingAlgorithm(), new CarrierDriverAgentFactoryImpl());
		CarrierAgentFactory carrierAgentFactory = new KarlsruheCarrierAgentFactory(controler.createRoutingAlgorithm(), new CarrierDriverAgentFactoryImpl());
		carrierAgentTracker = new CarrierAgentTracker(carriers.getCarriers().values(), controler.createRoutingAlgorithm(), scenario.getNetwork(), carrierAgentFactory);
		
		TSPAgentFactory tspAgentFactory = new TSPAgentFactoryImpl(carrierAgentTracker);
		tspAgentTracker = new TSPAgentTracker(transportServiceProviders.getTransportServiceProviders(),tspAgentFactory);
		createCarrierContracts(tspAgentTracker.createCarrierContracts());
		
		createCarrierPlans();
		
		event.getControler().getScenario().addScenarioElement(carriers);
		
		carrierAgentTracker.getEventsManager().addHandler(tspAgentTracker);
		carrierAgentTracker.getEventsManager().addHandler(new PickupAndDeliveryConsoleWriter());
		
		tspAgentTracker.getEventsManager().addHandler(carrierAgentTracker);
		
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, carrierAgentTracker);
		mobsimFactory.setUseOTFVis(false);
		event.getControler().setMobsimFactory(mobsimFactory);
		
		agentObserver = new AgentObserver("foo",scenario.getNetwork());
		agentObserver.setOutFile("../playgrounds/sschroeder/output/karlsruhe.txt");
		event.getControler().getEvents().addHandler(agentObserver);
	
		
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
		agentObserver.reset(0);
		agentObserver.writeStats();
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
		for(Carrier carrier : carriers.getCarriers().values()){
			RRCarrierPlanBuilder planBuilder = new RRCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), scenario.getNetwork());
			CarrierPlan plan = planBuilder.buildPlan();
			carrier.getPlans().add(plan);
			carrier.setSelectedPlan(plan);
		}
	}

	private void createCarrierContracts(List<Contract> contracts) {
		for(Contract contract : contracts){
			Id carrierId = contract.getOffer().getId();
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
		for(TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()){
			TSPContract c  = tsp.getContracts().iterator().next();
			Service s = new Service(c.getShipment().getFrom(),c.getShipment().getTo(), c.getShipment().getSize(), c.getShipment().getPickUpTimeWindow().getStart(),
					c.getShipment().getPickUpTimeWindow().getEnd(),c.getShipment().getDeliveryTimeWindow().getStart(),c.getShipment().getDeliveryTimeWindow().getEnd());
			Collection<Offer> offers = new ArrayList<Offer>();
			QueryOffersEvent queryEvent = new QueryOffersEvent(offers, s);
			tspAgentTracker.processEvent(queryEvent);
			Offer bestOffer = queryEvent.getOffers().iterator().next();
			Shipment shipment = CarrierUtils.createShipment(c.getShipment().getFrom(), c.getShipment().getTo(), c.getShipment().getSize(), c.getShipment().getPickUpTimeWindow().getStart(), 
					c.getShipment().getPickUpTimeWindow().getEnd(), c.getShipment().getDeliveryTimeWindow().getStart(), c.getShipment().getDeliveryTimeWindow().getEnd());
			tspAgentTracker.processEvent(new OfferAcceptEvent(new Contract(shipment,bestOffer)));
			for(Offer o : offers){
				if(o != bestOffer){
					tspAgentTracker.processEvent(new OfferRejectEvent(o));
				}
			}
		}
		
	}

}
