package playground.ssix;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import playgrounds.ssix.MZilskePassingVehicleQ;
import playgrounds.ssix.ModeData;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * @author ssix
 */

public class LangeStreckeSzenario{
	
	//CONFIGURATION: static variables used for aggregating configuration options
	public final static int NETWORK_CAPACITY = 27000;//in PCU/h
	public final static double END_TIME = 54000;
	public final static boolean PASSING_ALLOWED = true;
	private final static String OUTPUT_DIR = "C:\\Users\\Gautier\\workspace2\\playgrounds\\ssix\\output\\data.txt";
	private final static String OUTPUT_EVENTS = "C:\\Users\\Gautier\\workspace2\\playgrounds\\ssix\\output\\events.xml";
	
	public final static int NUMBER_OF_MEMORIZED_FLOWS = 10;
	public final static int NUMBER_OF_MODES = 2;
	public final static int NUMBER_OF_LANES = 1;
	public final static String[] NAMES= {"trucks","cars"};	//identification of the different modes
	public final static Double[] Probabilities = {0.15, 0.85}; //modal split
	public final static Double[] Pcus = {1., 5/7.}; 			//PCUs of the different possible modes
	public final static Double[] Speeds = {25., 42.};		//maximum velocities of the vehicle types, in m/s

	private PrintStream writer;
	private Scenario scenario;
	private static DataAnalyzer analyzer;
	private Map<Id, ModeData> modesData;
		
	
	public LangeStreckeSzenario(int networkCapacity){
		//Checking that configuration data has the appropriate size:
		if (NAMES.length != NUMBER_OF_MODES){ throw new RuntimeException("There should be "+NUMBER_OF_MODES+" names for the different modes. Check your static variable NAMES!");}
		if (Probabilities.length != NUMBER_OF_MODES){ throw new RuntimeException("There should be "+NUMBER_OF_MODES+" probabilities for the different modes. Check your static variable Probabilities!");}
		if (Pcus.length != NUMBER_OF_MODES){ throw new RuntimeException("There should be "+NUMBER_OF_MODES+" PCUs for the different modes. Check your static variable Pcus!");}
		if (Speeds.length != NUMBER_OF_MODES){ throw new RuntimeException("There should be "+NUMBER_OF_MODES+" speeds for the different modes. Check your static variable Speeds!");}
		
		//Initializing scenario Config file
		Config config = ConfigUtils.createConfig();
		config.qsim().setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE) ;
		config.qsim().setMainModes(Arrays.asList(NAMES));
		config.qsim().setStuckTime(100*3600.);//allows to overcome maximal density regime
		config.qsim().setEndTime(END_TIME);
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", VspExperimentalConfigGroup.ABORT) ;
		this.scenario = ScenarioUtils.createScenario(config);
	}
	
	public static void main(String[] args) {
		LangeStreckeSzenario autobahn = new LangeStreckeSzenario(NETWORK_CAPACITY);
		autobahn.fillNetworkData();
		autobahn.openFile(OUTPUT_DIR);
		autobahn.runExperiment();

		autobahn.closeFile();
	}

	private void runExperiment() {
		this.createPopulation();
		
		EventsManager events = EventsUtils.createEventsManager();
		
		DataAnalyzer analyze = new DataAnalyzer(this.scenario);
		LangeStreckeSzenario.analyzer =  analyze;
		events.addHandler(analyze);
		events.addHandler(new EventWriterXML(OUTPUT_EVENTS));
		
		Netsim qSim = createModifiedQSim(this.scenario, events);
		
		//OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, (QSim)qSim);
		//OTFClientLive.run(scenario.getConfig(), server);
		
		qSim.run();
		analyzer.analyze();
		
		for (Integer interval : analyzer.getIntervalNVeh().keySet()){
			writer.format("%.2f\t", analyzer.getIntervalMeanSpeed().get(interval));
			writer.format("%.2f\n", analyzer.getIntervalFlow().get(interval));
		}
		
	}

	private void fillNetworkData(){
		Network n = scenario.getNetwork();
		
		//NODES
		Node node0 = scenario.getNetwork().getFactory().createNode(  new IdImpl(0)  ,   scenario.createCoord(-200., 0.));
		Node node1 = scenario.getNetwork().getFactory().createNode(  new IdImpl(1)  ,   scenario.createCoord(0., 0.));
		Node node2 = scenario.getNetwork().getFactory().createNode(  new IdImpl(2)  ,   scenario.createCoord(2500., 0.));
		Node node3 = scenario.getNetwork().getFactory().createNode(  new IdImpl(3)  ,   scenario.createCoord(5000., 0.));
		Node node4 = scenario.getNetwork().getFactory().createNode(  new IdImpl(4)  ,   scenario.createCoord(5500., 0.));
		Node node5 = scenario.getNetwork().getFactory().createNode(  new IdImpl(5)  ,   scenario.createCoord(8000., 0.));
		Node node6 = scenario.getNetwork().getFactory().createNode(  new IdImpl(6)  ,   scenario.createCoord(9800., 0.));
		Node node7 = scenario.getNetwork().getFactory().createNode(  new IdImpl(7)  ,   scenario.createCoord(10000., 0.));
		Node node8 = scenario.getNetwork().getFactory().createNode(  new IdImpl(8)  ,   scenario.createCoord(10200., 0.));
		n.addNode(node0);
		n.addNode(node1);
		n.addNode(node2);
		n.addNode(node3);
		n.addNode(node4);
		n.addNode(node5);
		n.addNode(node6);
		n.addNode(node7);
		n.addNode(node8);
		
		//LINKS
		double speedUp = 35;
		double speedDown = 25;
		
		Link link = this.scenario.getNetwork().getFactory().createLink( new IdImpl("0to1")  ,  n.getNodes().get(new IdImpl(0))  ,   n.getNodes().get(new IdImpl(1)));
		link.setCapacity(LangeStreckeSzenario.NETWORK_CAPACITY);
		link.setFreespeed(speedUp);
		link.setLength(200.);
		link.setNumberOfLanes(1.);
		n.addLink(link);
		
		link = this.scenario.getNetwork().getFactory().createLink( new IdImpl("1to2")  ,  n.getNodes().get(new IdImpl(1))  ,   n.getNodes().get(new IdImpl(2)));
		link.setCapacity(LangeStreckeSzenario.NETWORK_CAPACITY);
		link.setFreespeed(speedUp);
		link.setLength(2500.);
		link.setNumberOfLanes(1.);
		n.addLink(link);
		
		link = this.scenario.getNetwork().getFactory().createLink( new IdImpl("2to3")  ,  n.getNodes().get(new IdImpl(2))  ,   n.getNodes().get(new IdImpl(3)));
		link.setCapacity(LangeStreckeSzenario.NETWORK_CAPACITY);
		link.setFreespeed(speedUp);
		link.setLength(2500.);
		link.setNumberOfLanes(1.);
		n.addLink(link);
		
		link = this.scenario.getNetwork().getFactory().createLink( new IdImpl("3to4")  ,  n.getNodes().get(new IdImpl(3))  ,   n.getNodes().get(new IdImpl(4)));
		link.setCapacity(LangeStreckeSzenario.NETWORK_CAPACITY);
		link.setFreespeed(speedUp);
		link.setLength(500.);
		link.setNumberOfLanes(1.);
		n.addLink(link);
		
		link = this.scenario.getNetwork().getFactory().createLink( new IdImpl("4to5")  ,  n.getNodes().get(new IdImpl(4))  ,   n.getNodes().get(new IdImpl(5)));
		link.setCapacity(LangeStreckeSzenario.NETWORK_CAPACITY);
		link.setFreespeed(speedUp);
		link.setLength(2500.);
		link.setNumberOfLanes(1.);
		n.addLink(link);
		
		link = this.scenario.getNetwork().getFactory().createLink( new IdImpl("5to6")  ,  n.getNodes().get(new IdImpl(5))  ,   n.getNodes().get(new IdImpl(6)));
		link.setCapacity(LangeStreckeSzenario.NETWORK_CAPACITY);
		link.setFreespeed(speedDown);
		link.setLength(1800.);
		link.setNumberOfLanes(1.);
		n.addLink(link);
		
		link = this.scenario.getNetwork().getFactory().createLink( new IdImpl("6to7")  ,  n.getNodes().get(new IdImpl(6))  ,   n.getNodes().get(new IdImpl(7)));
		link.setCapacity(LangeStreckeSzenario.NETWORK_CAPACITY);
		link.setFreespeed(speedDown);
		link.setLength(200.);
		link.setNumberOfLanes(1.);
		n.addLink(link);
		
		link = this.scenario.getNetwork().getFactory().createLink( new IdImpl("7to8")  ,  n.getNodes().get(new IdImpl(7))  ,   n.getNodes().get(new IdImpl(8)));
		link.setCapacity(LangeStreckeSzenario.NETWORK_CAPACITY);
		link.setFreespeed(speedDown);
		link.setLength(200.);
		link.setNumberOfLanes(1.);
		n.addLink(link);
		
		//check with .xml and Visualizer
		NetworkWriter writer = new NetworkWriter(n);
		writer.write("./output/diagrams_network_mastsim.xml");
	}
	
	private void createPopulation(){
		double tMax = LangeStreckeSzenario.END_TIME * 8 / 10;
		double pInMax = 0.55;
		int vehID = 0;
		Population population = scenario.getPopulation();
		population.getPersons().clear();
		
		String transportMode;
		for (int t=1; t<tMax+1; t++){
			double factor = Math.sin(Math.PI*t/tMax);
			for (int lane=0; lane<NUMBER_OF_LANES; lane++){
				if (Math.random() < factor * pInMax){
					vehID +=1 ;
					if (Math.random() < (NUMBER_OF_LANES-1 - lane)){
						transportMode = "trucks";
					} else {
						transportMode = "cars";
					}
					
					Person person = population.getFactory().createPerson(new IdImpl(vehID));
					Map<String, Object> customMap = person.getCustomAttributes();
					
					Plan plan = population.getFactory().createPlan();
					plan.addActivity(createHome(t));
					
					//Assigning transport mode
					customMap.put("transportMode", transportMode);
					Leg leg = population.getFactory().createLeg(transportMode);
					
					//The one and only route
					final Id startLinkId = new IdImpl("0to1");
					List<Id<Link>> routeDescription = new ArrayList<Id<Link>>();
						routeDescription.add(new IdImpl("1to2"));
						routeDescription.add(new IdImpl("2to3"));
						routeDescription.add(new IdImpl("3to4"));
						routeDescription.add(new IdImpl("4to5"));
						routeDescription.add(new IdImpl("5to6"));
						routeDescription.add(new IdImpl("6to7"));
						routeDescription.add(new IdImpl("7to8"));
					final Id endLinkId = new IdImpl("7to8");
					NetworkRoute route = new LinkNetworkRouteImpl(startLinkId, endLinkId);
					route.setLinkIds(startLinkId, routeDescription, endLinkId);
					leg.setRoute(route);
					//end of route definition
					plan.addLeg(leg);
					plan.addActivity(createWork());
					
					person.addPlan(plan);
					population.addPerson(person);
				}
			}
		}
	}
	
	private Netsim createModifiedQSim(Scenario sc, EventsManager events) {
		//From QSimFactory inspired code
		QSimConfigGroup conf = sc.getConfig().qsim();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }
        /**/
        QSim qSim = new QSim(sc, events);
        ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		
		//First modification: Mobsim needs to create queue links with mzilske's passing queue
        NetsimNetworkFactory<QNode, QLinkImpl> netsimNetworkFactory = new NetsimNetworkFactory<QNode, QLinkImpl>() {

            @Override
            public QLinkImpl createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
                if (PASSING_ALLOWED){
                    return new QLinkImpl(link, network, toQueueNode, new MZilskePassingVehicleQ());
                } else {
                    return new QLinkImpl(link, network, toQueueNode, new FIFOVehicleQ());
                }
            }

            @Override
            public QNode createNetsimNode(final Node node, QNetwork network) {
                return new QNode(node, network);
            }


        };
        QNetsimEngine netsimEngine = new QNetsimEngine(qSim, netsimNetworkFactory);
		////////////////////////////////////////////////////////
		
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);
        
		AgentFactory agentFactory;
		if (sc.getConfig().scenario().isUseTransit()) {
			agentFactory = new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			agentFactory = new DefaultAgentFactory(qSim);
		}
		
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
        
        //Second modification: Mobsim needs to know the different vehicle types (and their respective speeds)
        Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();
        
        for (int i=0; i < NUMBER_OF_MODES; i++){
			Id modeId = new IdImpl(NAMES[i]);
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(modeId);
			vehicleType.setPcuEquivalents(Pcus[i]);
			vehicleType.setMaximumVelocity(Speeds[i]);
			VehicleCapacity cap = new VehicleCapacityImpl();
			cap.setSeats(3);//this is default for now, could be improved with mode-dependent vehicle capacity
			vehicleType.setCapacity(cap);
			modeVehicleTypes.put(modeId.toString(), vehicleType);
		}
        agentSource.setModeVehicleTypes(modeVehicleTypes);
        //////////////////////////////////////////////////////
        
        qSim.addAgentSource(agentSource);
        return qSim;
	}
	
	private void openFile(String dir) {
		try {
			writer = new PrintStream(dir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void closeFile() {
		writer.close();
	}
	
	private Activity createHome(int endTime){
		Id homeLinkId = new IdImpl("0to1");
		Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("home", homeLinkId);
		activity.setEndTime(endTime);
		
		return activity;
	}
	
	private Activity createWork(){
		Id workLinkId = new IdImpl("7to8");
		Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("work", workLinkId);
		return activity;
	}
}