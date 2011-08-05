/**
 * 
 */
package city2000w;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.mzilske.city2000w.AgentObserver;
import playground.mzilske.city2000w.City2000WMobsimFactory;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierPlanBuilder;
import playground.mzilske.freight.CarrierTotalCostListener;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TSPTotalCostListener;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.TransportServiceProviders;
import playground.mzilske.freight.TSPPlanBuilder;
import freight.CarrierAgentFactoryImpl;
import freight.CarrierPlanReader;
import freight.CarrierPlanWriter;
import freight.api.ShipperAgentFactory;
import freight.listener.CarrierCostChartListener;
import freight.listener.ComFlowCostChartListener;
import freight.listener.ShipperCostConsolePrinter;
import freight.listener.ShipperDetailedCostListener;
import freight.listener.ShipperTotalCostListener;
import freight.listener.TLCCostChartListener;
import freight.listener.TSPCostChartListener;
import freight.offermaker.OfferSelectorImpl;
import freight.replanning.AnotherShipperPlanStrategy;
import freight.replanning.ShipperPlanStrategy;
import freight.replanning.TSPPlanStrategy;
import freight.utils.OfferRecorder;
import freight.utils.OfferRecorder.OfferRecord;
import freight.ShipperAgentFactoryImpl;
import freight.ShipperAgentTracker;
import freight.ShipperContract;
import freight.ShipperImpl;
import freight.ShipperPlan;
import freight.ShipperPlanReader;
import freight.ShipperPlanWriter;
import freight.Shippers;
import freight.TSPAgentFactoryImpl;
import freight.TSPPlanReader;
import freight.TSPPlanWriter;

/**
 * @author schroeder
 *
 */
public class RunTRBScenario implements ScoringListener, StartupListener, ShutdownListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener, ReplanningListener {

	static class ConfigRecorder {
		private List<Tuple<String,String>> configMap = new ArrayList<Tuple<String,String>>();
		
		public void add(String key,String value){
			configMap.add(new Tuple<String,String>(key,value));
		}
		
		public List<Tuple<String,String>> getConfigMap(){
			return configMap;
		}
	}
	
	private static Logger logger = Logger.getLogger(RunTRBScenario.class);
	
	private Carriers carriers;
	
	private TransportServiceProviders transportServiceProviders;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private TSPAgentTracker tspAgentTracker;
	
	private ShipperAgentTracker shipperAgentTracker;
	
	private Shippers shippers;

	private ScenarioImpl scenario;
	
	private AgentObserver agentObserver;
	
	private OfferSelectorImpl<CarrierOffer> carrierOfferSelector;
	
	private OfferSelectorImpl<TSPOffer> tspOfferSelector;
	
	private boolean liveModus = false;
	
	private static final String NETWORK_FILENAME = "../playgrounds/sschroeder/networks/gridWithSpike.xml";
	
	private static final String TSPPLAN_FILENAME = "../playgrounds/sschroeder/input/trbCase/trbTsps.xml";
	
	private static final String CARRIERNPLAN_FILENAME = "../playgrounds/sschroeder/input/trbCase/trbCarriers.xml";

	private static final String SHIPPERPLAN_FILENAME = "../playgrounds/sschroeder/input/trbCase/trbManyShippers2Days.xml";
	
	private static long SEED = 4711;
	
	private static long DEFAULT_SEED = 4711;
	
	private int nOfIteration = 50;
	
	private File outputDirectory;
	
	private ConfigRecorder configRecorder = new ConfigRecorder();
	
	private boolean firstShipperReplan = true;
	
	private OfferRecorder offerRecorder;
	
	private int iteration = 0;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(org.apache.log4j.Level.WARN);
		int nOfIterations = 10;
		for(int i=0;i<nOfIterations;i++){
			RunTRBScenario runner = new RunTRBScenario();
			runner.init(i);
			runner.run();
		}
	}
	
	public void init(int iteration){
		SEED = DEFAULT_SEED + iteration*23l;
		this.iteration = iteration;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		offerRecorder = new OfferRecorder();
		
		String outputFolder = "case_heavyProhibitions_toll/" + this.iteration + "/";
//		SEED = Math.round(Long.MAX_VALUE/Math.PI);
		MatsimRandom.getRandom().setSeed(SEED);
		configRecorder.add("seed", ("" + SEED));
		configRecorder.add("iterations", ("" + nOfIteration));
		
		outputDirectory  = new File("../playgrounds/sschroeder/output/" + outputFolder);
		outputDirectory.mkdirs();
		
		RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder.WORKING_PERIOD = 24*3600;
//		RandomTSPPlanBuilder.TRANSHIPMENT_TIMESPAN = 12*3600;
		NotSoRandomTSPPlanBuilder.TRANSHIPMENT_TIMESPAN = 12*3600;
		
		Controler controler = event.getControler();
		
		readCarriers();
		
		readTransportServiceProviders();
		
		readShippersWithContracts();
		
		setNoGoLocationForCarriers();
		setTSPAgentKnowledge();
		
		ShipperAgentFactory shipperAgentFactory = new ShipperAgentFactoryImpl();
		shipperAgentTracker = new ShipperAgentTracker(shippers.getShippers(), shipperAgentFactory);
		shipperAgentTracker.getDetailedCostListeners().add(new ShipperCostConsolePrinter());
		
		shipperAgentTracker.getDetailedCostListeners().add(new ComFlowCostChartListener(outputDirectory.getAbsolutePath() + "/trbComFlowTLC.png"));
	
		shipperAgentTracker.getTotalCostListeners().add(new ShipperCostConsolePrinter());
		shipperAgentTracker.getTotalCostListeners().add(new TLCCostChartListener(outputDirectory.getAbsolutePath() + "/trbTLC.png"));
		
		CarrierAgentFactoryImpl carrierAgentFactory = new CarrierAgentFactoryImpl(scenario.getNetwork(), controler.createRoutingAlgorithm());
		carrierAgentFactory.setOfferRecorder(offerRecorder);
		carrierAgentTracker = new CarrierAgentTracker(carriers.getCarriers().values(), controler.createRoutingAlgorithm(), scenario.getNetwork(), carrierAgentFactory);
		carrierAgentTracker.getTotalCostListeners().add(new CarrierCostChartListener(outputDirectory.getAbsolutePath() + "/trbCarrierCosts.png"));
		
		
		TSPAgentFactoryImpl tspAgentFactory = new TSPAgentFactoryImpl(carrierAgentTracker);
		tspAgentFactory.setNetwork(scenario.getNetwork());
		carrierOfferSelector = new OfferSelectorImpl<CarrierOffer>(0.005,0.05,nOfIteration);
		
		tspOfferSelector = new OfferSelectorImpl<TSPOffer>(0.0005,0.1,nOfIteration);
		
		configRecorder.add("beta_start", "" + carrierOfferSelector.beta_start);
		configRecorder.add("beta_end", "" + carrierOfferSelector.beta_end);
		
		tspAgentFactory.setOfferSelector(carrierOfferSelector);
		
		tspAgentTracker = new TSPAgentTracker(transportServiceProviders.getTransportServiceProviders(), tspAgentFactory);
		tspAgentTracker.getTotalCostListeners().add(new TSPCostChartListener(outputDirectory.getAbsolutePath() + "/trbTSPCosts.png"));
		
		carrierAgentTracker.getShipmentStatusListeners().add(tspAgentTracker);
		carrierAgentTracker.getCostListeners().add(tspAgentTracker);
		
		createShipperPlans();
		
		createTspContracts(shipperAgentTracker.createTSPContracts());
		
		createTspPlans();
		
		createCarrierContracts(tspAgentTracker.createCarrierContracts());
		
		createCarrierPlans();
		
		event.getControler().getScenario().addScenarioElement(carriers);
		
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, carrierAgentTracker);
		mobsimFactory.setUseOTFVis(liveModus);
		event.getControler().setMobsimFactory(mobsimFactory);
		
		agentObserver = new AgentObserver("foo",scenario.getNetwork());
		agentObserver.setOutFile(outputDirectory.getAbsolutePath() + "/trbCase.txt");
		event.getControler().getEvents().addHandler(agentObserver);
	
		
	}

	private void setTSPAgentKnowledge() {
		for(TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()){
			if(tsp.getId().toString().equals("tsp_withTSC")){
				tsp.getKnowledge().getKnownCarriers().add(new IdImpl("carrier_heavy"));
			}
		}
		
	}

	private void setNoGoLocationForCarriers() {
		CarrierImpl carrier = carriers.getCarriers().get(new IdImpl("carrier_heavy"));
		Set<Id> noGo = new HashSet<Id>();
		for(ShipperImpl s : shippers.getShippers()){
			for(ShipperContract c : s.getContracts()){
				noGo.add(c.getCommodityFlow().getTo());
			}
		}
		for(Id noGoLocation : noGo){
			carrier.getKnowledge().getNoGoLocations().add(noGoLocation);
		}
	}

	private void readShippersWithContracts() {
		shippers = new Shippers();
		new ShipperPlanReader(shippers.getShippers()).read(SHIPPERPLAN_FILENAME);
	}

	private void createShipperPlans() {
		for(ShipperImpl shipper : shippers.getShippers()){
			TRBShipperPlanBuilder planBuilder = new TRBShipperPlanBuilder();
			planBuilder.setOfferSelector(tspOfferSelector);
			planBuilder.setTspAgentTracker(tspAgentTracker);
			ShipperPlan plan = planBuilder.buildPlan(shipper.getShipperKnowledge(), shipper.getContracts());
			shipper.setSelectedPlan(plan);
		}
		configRecorder.add("iniShipperCreation", TRBShipperPlanBuilder.class.toString());
	}

	private void createTspPlans() {
		for(TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()){
//			RandomTSPPlanBuilder tspPlanBuilder = new RandomTSPPlanBuilder(scenario.getNetwork());
			NotSoRandomTSPPlanBuilder tspPlanBuilder = new NotSoRandomTSPPlanBuilder(scenario.getNetwork());
			tspPlanBuilder.setCarrierAgentTracker(carrierAgentTracker);
			tspPlanBuilder.setTspAgentTracker(tspAgentTracker);
			tspPlanBuilder.setOfferSelector(carrierOfferSelector);
			int nOfContracts = tsp.getContracts().size();
			TSPPlan plan = tspPlanBuilder.buildPlan(tsp.getContracts(),tsp.getTspCapabilities());
			int nOfChains = plan.getChains().size();
			assertEquals(nOfContracts,nOfChains);
			tsp.getPlans().add(plan);
			tsp.setSelectedPlan(plan);
		}
		configRecorder.add("iniTSPPlanBuilder", RandomTSPPlanBuilder.class.toString());
		new TSPPlanWriter(transportServiceProviders.getTransportServiceProviders()).write(outputDirectory.getAbsolutePath() + "/tspPlans2Check.xml");
	}

	private void assertEquals(int nOfContracts, int nOfChains) {
		if(nOfChains == nOfContracts){
			return;
		}
		throw new IllegalStateException("nOfContracts must be equal to nOfTransportChains, but it is " + nOfContracts + " and " + nOfChains);
	}

	private void createTspContracts(Collection<TSPContract> tspContracts) {
		for(TSPContract c : tspContracts){
			TransportServiceProviderImpl tsp = findTsp(c.getOffer().getId());
			tsp.getContracts().add(c);
		}
		
	}

	private TransportServiceProviderImpl findTsp(Id tspId) {
		for(TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()){
			if(tsp.getId().equals(tspId)){
				return tsp;
			}
		}
		return null;
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
		carrierAgentTracker.reset(event.getIteration());
//		agentObserver.reset(0);
//		agentObserver.writeStats();
		carrierOfferSelector.reset(event.getIteration());
		System.out.println("iteration " + event.getIteration() + " done ");
	}


	@Override
	public void notifyReplanning(ReplanningEvent event) {
		RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder carrierPlanBuilder = 
			new RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder(scenario.getNetwork());
		
//		RandomTSPPlanBuilder tspPlanBuilder = new RandomTSPPlanBuilder(scenario.getNetwork());
		NotSoRandomTSPPlanBuilder tspPlanBuilder = new NotSoRandomTSPPlanBuilder(scenario.getNetwork());
		tspPlanBuilder.setCarrierAgentTracker(carrierAgentTracker);
		tspPlanBuilder.setTspAgentTracker(tspAgentTracker);	
		tspPlanBuilder.setOfferSelector(carrierOfferSelector);
		
		replanShipper(carrierPlanBuilder,tspPlanBuilder);
		replanTspAndCarrier(carrierPlanBuilder,tspPlanBuilder);
//		replanTsp(carrierPlanBuilder,tspPlanBuilder);
		replanCarrier();
	}

	private void replanTspAndCarrier(CarrierPlanBuilder carrierPlanBuilder,
			TSPPlanBuilder tspPlanBuilder) {

		for(ShipperImpl shipper : shippers.getShippers()){
			ShipperPlanStrategy shipperPlanStrategy = new ShipperPlanStrategy();
			shipperPlanStrategy.setShipperAgentTracker(shipperAgentTracker);
			shipperPlanStrategy.setCarrierAgentTracker(carrierAgentTracker);
			shipperPlanStrategy.setTspAgentTracker(tspAgentTracker);
			shipperPlanStrategy.setNetwork(scenario.getNetwork());
			shipperPlanStrategy.setCarrierPlanBuilder(carrierPlanBuilder);
			shipperPlanStrategy.setTspPlanBuilder(tspPlanBuilder);
			shipperPlanStrategy.setTspOfferSelector(tspOfferSelector);
			shipperPlanStrategy.run(shipper);
		}
		
	}

	private void replanCarrier() {
		
	}

	private void replanTsp(CarrierPlanBuilder carrierPlanBuilder, TSPPlanBuilder tspPlanBuilder) {
		List<TransportServiceProviderImpl> tsps = new ArrayList<TransportServiceProviderImpl>();
		tsps.addAll(transportServiceProviders.getTransportServiceProviders());
		int randIndex = MatsimRandom.getRandom().nextInt(tsps.size());
		TransportServiceProviderImpl tsp = tsps.get(randIndex);
//		for(TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()){
			TSPPlanStrategy tspPlanStrategy = new TSPPlanStrategy();
			tspPlanStrategy.share2replan = 0.2;
			tspPlanStrategy.setTspPlanBuilder(tspPlanBuilder);
			tspPlanStrategy.setCarrierAgentTracker(carrierAgentTracker);
			tspPlanStrategy.setCarrierPlanBuilder(carrierPlanBuilder);
			tspPlanStrategy.setTspAgentTracker(tspAgentTracker);
			tspPlanStrategy.run(tsp);
//		}
	}

	private void replanShipper(CarrierPlanBuilder carrierPlanBuilder, TSPPlanBuilder tspPlanBuilder) {
		List<ShipperImpl> shipperList = new ArrayList<ShipperImpl>();
		shipperList.addAll(shippers.getShippers());
		int randIndex = MatsimRandom.getRandom().nextInt(shipperList.size());
		ShipperImpl shipper = shipperList.get(randIndex);
		if(MatsimRandom.getRandom().nextDouble() < 1){
				AnotherShipperPlanStrategy shipperPlanStrategy = new AnotherShipperPlanStrategy();
				shipperPlanStrategy.setShipperAgentTracker(shipperAgentTracker);
				shipperPlanStrategy.setCarrierAgentTracker(carrierAgentTracker);
				shipperPlanStrategy.setTspAgentTracker(tspAgentTracker);
				shipperPlanStrategy.setNetwork(scenario.getNetwork());
				shipperPlanStrategy.setCarrierPlanBuilder(carrierPlanBuilder);
				shipperPlanStrategy.setTspPlanBuilder(tspPlanBuilder);
				shipperPlanStrategy.setTspOfferSelector(tspOfferSelector);
				shipperPlanStrategy.run(shipper);
			
		}

		if(firstShipperReplan){
			firstShipperReplan = false;
			configRecorder.add("shipperReplanner.carrierPlanBuilder", RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder.class.toString());
			configRecorder.add("shipperReplanner.tspPlanBuilder", RandomTSPPlanBuilder.class.toString());
			configRecorder.add("shipperReplanner.shipperPlanStrategy", ShipperPlanStrategy.class.toString());
		}
	}

	
	private void run(){
		Config config = new Config();
		config.addCoreModules();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(nOfIteration);
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
//			MarginalCostCalculator costCalculator = new MarginalCostCalculator();
			RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder planBuilder = 
				new RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder(scenario.getNetwork());
			
//			planBuilder.setMarginalCostCalculator(costCalculator);
			
//			RAndRPickupAndDeliveryCarrierPlanBuilder planBuilder = new RAndRPickupAndDeliveryCarrierPlanBuilder(scenario.getNetwork());
//			RuinAndRecreateCarrierPlanBuilder planBuilder = new RuinAndRecreateCarrierPlanBuilder(scenario.getNetwork());
//			ClarkeAndWrightCarrierPlanBuilder planBuilder = new ClarkeAndWrightCarrierPlanBuilder(scenario.getNetwork());
			CarrierPlan plan = planBuilder.buildPlan(carrier.getCarrierCapabilities(), carrier.getContracts());
//			costCalculator.printTables();
//			memorizeCosts(carrier,costCalculator,plan.getScore());
			carrier.getPlans().add(plan);
			carrier.setSelectedPlan(plan);
		}
		configRecorder.add("iniCarrierPlans", RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder.class.toString());
		new CarrierPlanWriter(carriers.getCarriers().values()).write(outputDirectory.getAbsolutePath() + "/carrierPlans2Check.xml");
	}
	
	private void createCarrierContracts(List<Contract> contracts) {
		for(Contract contract : contracts){
			Id carrierId = contract.getOffer().getId();
			carriers.getCarriers().get(carrierId).getContracts().add(contract);
		}
	}

	private void readCarriers() {
		Collection<CarrierImpl> carriers = new ArrayList<CarrierImpl>();
		new CarrierPlanReader(carriers).read(CARRIERNPLAN_FILENAME);
		this.carriers = new Carriers(carriers);
	}
	
	private void setNoGoLocations(CarrierImpl carrier){
		
	}

	private void readTransportServiceProviders() {
		transportServiceProviders = new TransportServiceProviders();
		new TSPPlanReader(transportServiceProviders.getTransportServiceProviders()).read(TSPPLAN_FILENAME);
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		new ShipperPlanWriter(shippers.getShippers()).write(outputDirectory.getAbsolutePath() + "/trbShippersAfterIteration.xml");
		new TSPPlanWriter(transportServiceProviders.getTransportServiceProviders()).write(outputDirectory.getAbsolutePath() + "/trbTspAfterIteration.xml");
		new CarrierPlanWriter(carriers.getCarriers().values()).write(outputDirectory.getAbsolutePath() + "/trbCarrierAfterIteration.xml");
		for(ShipperTotalCostListener l : shipperAgentTracker.getTotalCostListeners()){
			l.finish();
		}
		for(ShipperDetailedCostListener l : shipperAgentTracker.getDetailedCostListeners()){
			l.finish();
		}
		for(CarrierTotalCostListener l : carrierAgentTracker.getTotalCostListeners()){
			l.finish();
		}
		for(TSPTotalCostListener l : tspAgentTracker.getTotalCostListeners()){
			l.finish();
		}
		writeConfiguration(outputDirectory.getAbsolutePath() + "/configuration.txt");
		writeOffers(outputDirectory.getAbsolutePath() + "/offers.txt");
//		
	}

	private void writeOffers(String string){
		BufferedWriter writer = IOUtils.getBufferedWriter(string);
		try {
			for(OfferRecord or : offerRecorder.getRecords()){
				writer.write(or.id.toString() + ";");
				writer.write(or.from.toString() + ";");
				writer.write(or.to.toString() + ";");
				writer.write(or.size + ";");
				writer.write(or.price + ";");
				writer.write(or.omStrat + ";");
				writer.write("\n");
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void writeConfiguration(String filename) {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		for(Tuple<String,String> t : configRecorder.getConfigMap()){
			try {
				writer.write(t.getFirst() + "=" + t.getSecond() + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		carrierAgentTracker.calculateCosts();
		tspAgentTracker.calculateCosts();
		shipperAgentTracker.scorePlans();
	}

}
