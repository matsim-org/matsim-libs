/**
 * 
 */
package city2000w;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Level;
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
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.city2000w.AgentObserver;
import playground.mzilske.city2000w.CheapestCarrierTSPPlanBuilder;
import playground.mzilske.city2000w.City2000WMobsimFactory;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierAgentTrackerBuilder;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.TransportServiceProviders;
import freight.CarrierPlanReader;
import freight.CarrierPlanWriter;
import freight.CarrierUtils;
import freight.CommodityFlow;
import freight.ShipperAgentTracker;
import freight.ShipperImpl;
import freight.ShipperPlan;
import freight.ShipperPlanReader;
import freight.ShipperUtils;
import freight.Shippers;
import freight.TSPPlanReader;
import freight.TSPUtils;

/**
 * @author stefan schroeder
 *
 */
public class MobSimRunnnerWithShipper implements StartupListener, ScoringListener, ReplanningListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener, ShutdownListener {

	
	private static Logger logger = Logger.getLogger(MobSimRunnnerWithShipper.class);

	private Carriers carriers;

	private CarrierAgentTracker carrierAgentTracker;
	
	private TSPAgentTracker tspAgentTracker;
	
	private ShipperAgentTracker shipperAgentTracker;
	
	private Shippers shippers;
	
	private TransportServiceProviders tsProviders;

	private ScenarioImpl scenario;
	
	private String NETWORK_FILENAME = "../playgrounds/sschroeder/input/grid.xml";

	private String CARRIERPLANFILE = "../playgrounds/sschroeder/input/carrierPlans.xml";
	
	private String TSPPLANFILE= "../playgrounds/sschroeder/input/tspPlans.xml";
	
	private String SHIPPERPLANFILE = "../playgrounds/sschroeder/input/shipperPlans.xml";
	
	private boolean liveModus = true;
	
	private String outputDirectory = "../playgrounds/sschroeder/output/";
	
	private AgentObserver agentObserver;


	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Logger.getRootLogger().setLevel(Level.INFO);
		MobSimRunnnerWithShipper runner = new MobSimRunnnerWithShipper();
		runner.run();
	}
	

	public void notifyStartup(StartupEvent event) {
		
		Controler controler = event.getControler();
	
		readCarrierPlans();
		
		readTSPPlans();
		
		readShipperPlans();
		
		shipperAgentTracker = new ShipperAgentTracker(shippers.getShippers());
		
		tspAgentTracker = new TSPAgentTracker(tsProviders.getTransportServiceProviders());
		
//		createShipperPlans();

		CarrierAgentTrackerBuilder carrierAgentTrackerBuilder = new CarrierAgentTrackerBuilder();
		carrierAgentTrackerBuilder.setCarriers(carriers.getCarriers().values());
		carrierAgentTrackerBuilder.setRouter(controler.createRoutingAlgorithm());
		carrierAgentTrackerBuilder.setNetwork(controler.getNetwork());
		carrierAgentTrackerBuilder.setEventsManager(controler.getEvents());
		carrierAgentTracker = carrierAgentTrackerBuilder.build();
			
		tspAgentTracker.setOfferMaker(new CarrierCostRequester(carrierAgentTracker));
		
		createTSPContracts();
		
		createTSPPlans();
		
		createCarrierContracts();
	
		createCarrierPlans();
		
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, carrierAgentTracker);
		mobsimFactory.setUseOTFVis(liveModus);
		event.getControler().setMobsimFactory(mobsimFactory);

		agentObserver = new AgentObserver("foo",scenario.getNetwork());
		agentObserver.setOutFile(outputDirectory + "observedAgents_rrpd.txt");
		event.getControler().getEvents().addHandler(agentObserver);
		
	}


	private void readShipperPlans() {
		shippers = new Shippers();
		ShipperPlanReader planReader = new ShipperPlanReader(shippers.getShippers());
		planReader.read(SHIPPERPLANFILE);
	}


	private void createTSPContracts() {
		Collection<TSPContract> tspContracts = shipperAgentTracker.createTSPContracts();
		for(TSPContract c : tspContracts){
			Id tspId = c.getOffer().getTspId();
			TransportServiceProviderImpl tsp = getTSP(tspId);
			if(tsp == null){
				throw new NullPointerException("tsp " + tspId.toString() + " does not exist in tspList");
			}
			TSPContract contract = TSPUtils.createTSPContract(c.getShipments(), c.getOffer());
			tsp.getContracts().add(contract);
		}
	}


	private void createTSPPlans() {
		for(TransportServiceProviderImpl tsp : tsProviders.getTransportServiceProviders()){
			CheapestCarrierTSPPlanBuilder planBuilder = new CheapestCarrierTSPPlanBuilder();
			planBuilder.setCarrierAgentTracker(carrierAgentTracker);
			planBuilder.setTransshipmentCentres(tsp.getTspCapabilities().getTransshipmentCentres());
			TSPPlan plan = planBuilder.buildPlan(tsp.getContracts(), tsp.getTspCapabilities());
			tsp.setSelectedPlan(plan);
		}
	}


	private TransportServiceProviderImpl getTSP(Id tspId) {
		for(TransportServiceProviderImpl tsp : tsProviders.getTransportServiceProviders()){
			if(tspId.equals(tsp.getId())){
				return tsp;
			}
		}
		return null;
	}


	private void createCarrierContracts() {
		Collection<Contract> contracts = tspAgentTracker.createCarrierContracts();
		for(Contract c : contracts){
			Id carrierId = c.getOffer().getCarrierId();
			CarrierImpl carrier = carriers.getCarriers().get(carrierId);
			if(carrier == null){
				throw new NullPointerException("carrier " + carrierId.toString() + " does not exist in carrierList");
			}
			CarrierUtils.createAndAddContract(carrier, c.getShipment(), c.getOffer());
		}
	}


	private void createCarrierPlans() {
		for(CarrierImpl carrier : carriers.getCarriers().values()){
			RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder planBuilder = new RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder(scenario.getNetwork());
			carrier.setSelectedPlan(planBuilder.buildPlan(carrier.getCarrierCapabilities(),carrier.getContracts()));
		}
	}


	private void readTSPPlans() {
		tsProviders = new TransportServiceProviders();
		TSPPlanReader planReader = new TSPPlanReader(tsProviders.getTransportServiceProviders());
		planReader.read(TSPPLANFILE);
	}


	private void readCarrierPlans() {
		carriers = new Carriers();
		Collection<CarrierImpl> carrierCol = new ArrayList<CarrierImpl>();
		CarrierPlanReader planReader = new CarrierPlanReader(carrierCol);
		planReader.read(CARRIERPLANFILE);
		for(CarrierImpl c : carrierCol){
			carriers.getCarriers().put(c.getId(), c);
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

	}

	public void notifyIterationEnds(IterationEndsEvent event) {

	}

	public void notifyReplanning(ReplanningEvent event) {
		
	}


	public void notifyShutdown(ShutdownEvent event)  {
		CarrierPlanWriter planWriter = new CarrierPlanWriter(carriers.getCarriers().values());
		planWriter.write(outputDirectory + "newCarrierPlans.xml");
		agentObserver.reset(1);
		agentObserver.writeStats();
	}

	private void run(){
		Config config = new Config();
		config.addCoreModules();
		
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
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

}
