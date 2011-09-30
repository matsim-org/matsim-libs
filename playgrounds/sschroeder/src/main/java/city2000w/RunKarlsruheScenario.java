/**
 * 
 */
package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

import playground.mzilske.city2000w.City2000WMobsimFactory;
import playground.mzilske.freight.Carrier;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierContract;
import playground.mzilske.freight.CarrierDriverAgentFactoryImpl;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierPlanReader;
import playground.mzilske.freight.CarrierShipment;
import playground.mzilske.freight.CarrierUtils;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChain.ChainLeg;
import playground.mzilske.freight.TransportChainAgentFactoryImpl;
import playground.mzilske.freight.TransportChainBuilder;
import playground.mzilske.freight.TransportServiceProvider;
import playground.mzilske.freight.TransportServiceProviders;
import playground.mzilske.freight.api.CarrierAgentFactory;
import playground.mzilske.freight.api.Offer;
import playground.mzilske.freight.api.TSPAgentFactory;
import playground.mzilske.freight.events.CarrierOfferRejectEvent;
import playground.mzilske.freight.events.OfferUtils;
import playground.mzilske.freight.events.QueryCarrierOffersEvent;
import playground.mzilske.freight.events.Service;
import playground.mzilske.freight.events.TSPCarrierContractAcceptEvent;
import playground.mzilske.freight.events.TSPCarrierContractCanceledEvent;
import playground.mzilske.freight.events.TransportChainAddedEvent;
import playground.mzilske.freight.events.TransportChainRemovedEvent;
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

	private boolean liveModus = false;
	
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
		
		TSPAgentFactory tspAgentFactory = new KarlsruheTSPAgentFactory(new TransportChainAgentFactoryImpl());
		tspAgentTracker = new TSPAgentTracker(transportServiceProviders.getTransportServiceProviders(),tspAgentFactory);
		
		createCarrierContracts(tspAgentTracker.createCarrierContracts());
		
		createCarrierPlans();
		
		event.getControler().getScenario().addScenarioElement(carriers);
		
		carrierAgentTracker.getEventsManager().addHandler(tspAgentTracker);
		carrierAgentTracker.getEventsManager().addHandler(new PickupAndDeliveryConsoleWriter());
		
		tspAgentTracker.getEventsManager().addHandler(carrierAgentTracker);
		tspAgentTracker.getEventsManager().addHandler(tspAgentTracker);
		
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, carrierAgentTracker);
		mobsimFactory.setUseOTFVis(liveModus );
		event.getControler().setMobsimFactory(mobsimFactory);		
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
		replanTSP();
		replanCarrier();
//		removeRandomContract();
	}

	private void replanCarrier() {
		for(Carrier carrier : carriers.getCarriers().values()){
			if(!carrier.getNewContracts().isEmpty()){
				logger.info("hohohohoh. obviously, i have to plan a new contract");
				RRCarrierPlanBuilder planBuilder = new RRCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), scenario.getNetwork());
				CarrierPlan plan = planBuilder.buildPlan();
				carrier.getPlans().add(plan);
				carrier.setSelectedPlan(plan);
				carrier.getNewContracts().clear();
			}
			if(!carrier.getExpiredContracts().isEmpty()){
				logger.info("outsch. a contract was canceled and i must adapt my plan");
				RRCarrierPlanBuilder planBuilder = new RRCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), scenario.getNetwork());
				CarrierPlan plan = planBuilder.buildPlan();
				carrier.getPlans().add(plan);
				carrier.setSelectedPlan(plan);
				carrier.getExpiredContracts().clear();
			}
		}
		
	}

	private void replanTSP() {
		for(TransportServiceProvider tsp : transportServiceProviders.getTransportServiceProviders()){
			TSPContract c  = tsp.getContracts().iterator().next();
			TSPPlan plan = tsp.getSelectedPlan();
			Collection<TransportChain> chains = new ArrayList<TransportChain>();
			boolean firstChain = true;
			for(TransportChain chain : plan.getChains()){
				if(firstChain){
					for(ChainLeg leg : chain.getLegs()){
						tspAgentTracker.processEvent(new TSPCarrierContractCanceledEvent(leg.getContract()));
					}
					TransportChainBuilder chainBuilder = new TransportChainBuilder(chain.getShipment());
					chainBuilder.schedulePickup(chain.getShipment().getFrom(), chain.getShipment().getPickupTimeWindow());
					CarrierOffer bestOffer = getOffer(getService(chain.getShipment()));
					CarrierShipment shipment = CarrierUtils.createShipment(c.getShipment().getFrom(), c.getShipment().getTo(), 10, c.getShipment().getPickupTimeWindow().getStart(), 
							c.getShipment().getPickupTimeWindow().getEnd(), c.getShipment().getDeliveryTimeWindow().getStart(), c.getShipment().getDeliveryTimeWindow().getEnd());
					CarrierContract contract = new CarrierContract(tsp.getId(),bestOffer.getId(), shipment, bestOffer);
					chainBuilder.scheduleLeg(contract);
					chainBuilder.scheduleDelivery(chain.getShipment().getTo(), chain.getShipment().getDeliveryTimeWindow());
					TransportChain newChain = chainBuilder.build();
					chains.add(newChain);
					tspAgentTracker.processEvent(new TSPCarrierContractAcceptEvent(contract));
					tspAgentTracker.processEvent(new TransportChainRemovedEvent(tsp.getId(),chain));
					tspAgentTracker.processEvent(new TransportChainAddedEvent(tsp.getId(),newChain));;
					firstChain = false;
				}
				else{
					chains.add(chain);
				}
			}
			tsp.setSelectedPlan(new TSPPlan(chains));
		}
	}

	private CarrierOffer getOffer(Service service) {
		Collection<Offer> offers = new ArrayList<Offer>();
//		tspAgentTracker.processEvent(new TSPCarrierContractCanceledEvent(c));
		QueryCarrierOffersEvent queryEvent = new QueryCarrierOffersEvent(offers, service);
		tspAgentTracker.processEvent(queryEvent);
		List<Offer> offerList = new ArrayList<Offer>(queryEvent.getOffers());
		Collections.sort(offerList, new Comparator<Offer>(){

			@Override
			public int compare(Offer arg0, Offer arg1) {
				if(arg0.getPrice() < arg1.getPrice()){
					return -1;
				}
				else{
					return 1;
				}
			}
		});
		CarrierOffer bestOffer = (CarrierOffer)offerList.get(0);
		for(Offer o : offers){
			if(o != bestOffer){
				tspAgentTracker.processEvent(new CarrierOfferRejectEvent(o.getId(),o));
			}
		}
		return bestOffer;
	}

	private Service getService(TSPShipment shipment) {
		return OfferUtils.createService(shipment);
	}
}
