/**
 * 
 */
package playground.mzilske.city2000w;

import java.util.Arrays;
import java.util.Collection;
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


import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.FreightAgentTracker;
import playground.mzilske.freight.FreightAgentTrackerBuilder;
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
public class RunCarriersAndTSPs implements StartupListener, ScoringListener, ReplanningListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

	private static final String NETWORK_FILENAME = "../../matsim/examples/equil/network.xml";
	
	private static Logger logger = Logger.getLogger(RunCarriersAndTSPs.class);
	
	private Carriers carriers;
	private TransportServiceProviders transportServiceProviders;
	
	private FreightAgentTracker freightAgentTracker;
	private TSPAgentTracker tspAgentTracker;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RunCarriersAndTSPs runner = new RunCarriersAndTSPs();
		runner.run();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Controler controler = event.getControler();
		
		createTransportServiceProviderWithContracts();
		
		createCarrier();
		
		tspAgentTracker = new TSPAgentTracker(transportServiceProviders.getTransportServiceProviders());
		tspAgentTracker.getCostListeners().add(new DefaultLSPShipmentTracker());
		
		createCarrierContracts(tspAgentTracker.createCarrierShipments());
		
		createCarrierPlans();
		
		event.getControler().getScenario().addScenarioElement(carriers);
		
		FreightAgentTrackerBuilder freightAgentTrackerBuilder = new FreightAgentTrackerBuilder();
		freightAgentTrackerBuilder.setCarriers(controler.getScenario().getScenarioElement(Carriers.class).getCarriers());
		freightAgentTrackerBuilder.setRouter(controler.createRoutingAlgorithm());
		freightAgentTrackerBuilder.setNetwork(controler.getNetwork());
		freightAgentTrackerBuilder.setEventsManager(controler.getEvents());
		freightAgentTrackerBuilder.addCarrierCostListener(tspAgentTracker);
		freightAgentTracker = freightAgentTrackerBuilder.build();
		
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, freightAgentTracker);
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
		replanCarriers();
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
		config.controler().setLastIteration(1);
		ScenarioImpl scenario = new ScenarioImpl(config);
		new MatsimNetworkReader(scenario).readFile(NETWORK_FILENAME);
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

	private void createCarrierPlans() {
		for(CarrierImpl carrier : carriers.getCarriers()){
			TrivialCarrierPlanBuilder trivialCarrierPlanBuilder = new TrivialCarrierPlanBuilder();
			CarrierPlan plan = trivialCarrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrier.getContracts());
			carrier.getPlans().add(plan);
			carrier.setSelectedPlan(plan);
		}
	}

	private void createCarrierContracts(Map<Id, List<Shipment>> carrierShipments) {
		for(Id id : carrierShipments.keySet()){
			for(CarrierImpl carrier : carriers.getCarriers()){
				if(carrier.getId().equals(id)){
					for(Shipment s : carrierShipments.get(id)){
						carrier.getContracts().add(new Contract(Arrays.asList(s)));
					} 
				}
			}
		}	
		
	}

	private void createCarrier() {
		CarrierImpl c1 = new CarrierImpl(new IdImpl("hans"), new IdImpl("1"));
		CarrierCapabilities cc = new CarrierCapabilities();
		c1.setCarrierCapabilities(cc);
		CarrierVehicle cv_hans_1 = new CarrierVehicle(new IdImpl("hans-erstes-auto"), new IdImpl("1"));
		CarrierVehicle cv_hans_2 = new CarrierVehicle(new IdImpl("hans-zweites-auto"), new IdImpl("2"));
		cc.getCarrierVehicles().add(cv_hans_1);
		cc.getCarrierVehicles().add(cv_hans_2);


		CarrierImpl c2 = new CarrierImpl(new IdImpl("ulrich"), new IdImpl("2"));
		CarrierCapabilities cc2 = new CarrierCapabilities();
		c2.setCarrierCapabilities(cc2);
		CarrierVehicle cv_ulrich_1 = new CarrierVehicle(new IdImpl("ulrichs-erstes-auto"), new IdImpl("1"));
		CarrierVehicle cv_ulrich_2 = new CarrierVehicle(new IdImpl("ulrichs-zweites-auto"), new IdImpl("2"));
		cc2.getCarrierVehicles().add(cv_ulrich_1);
		cc2.getCarrierVehicles().add(cv_ulrich_2);

		carriers = new Carriers();
		carriers.getCarriers().add(c1);
		carriers.getCarriers().add(c2);
	}

	private void createTransportServiceProviderWithContracts() {
		TransportServiceProviderImpl tsp = new TransportServiceProviderImpl(new IdImpl("guenter"));
		logger.debug("TransportServiceProvider " + tsp.getId() + " has come into play");
		tsp.getContracts().add(new TSPContract(Arrays.asList(
				new TSPShipment(new IdImpl(20), new IdImpl(1), 20,
						new TSPShipment.TimeWindow(0.0, 24*3600), new TSPShipment.TimeWindow(0.0,24*3600)))));
		tsp.getContracts().add(new TSPContract(Arrays.asList(
				new TSPShipment(new IdImpl(12), new IdImpl(20), 15,
						new TSPShipment.TimeWindow(0.0, 24*3600), new TSPShipment.TimeWindow(0.0,24*3600)))));
		logger.debug("he has " + tsp.getContracts().size() + " contracts");
		printContracts(tsp.getContracts());
		TSPCapabilities cap = new TSPCapabilities();
		cap.getTransshipmentCentres().add(new IdImpl(14));
		cap.getTransshipmentCentres().add(new IdImpl(18));
		tsp.setTspCapabilities(cap);
		printCap(cap);
		
		TrivialTSPPlanBuilder tspPlanBuilder = new TrivialTSPPlanBuilder();
		TSPPlan plan = tspPlanBuilder.buildPlan(tsp.getContracts(),tsp.getTspCapabilities());
		printTSPPlan(plan);
		tsp.getPlans().add(plan);
		tsp.setSelectedPlan(plan);
		transportServiceProviders = new TransportServiceProviders();
		transportServiceProviders.getTransportServiceProviders().add(tsp);
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
