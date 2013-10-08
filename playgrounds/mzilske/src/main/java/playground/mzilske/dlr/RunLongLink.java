package playground.mzilske.dlr;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.mobsim.qsim.qnetsimengine.FIFOVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNode;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.pt.otfvis.FacilityDrawer;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFVisMobsimListener;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;

public class RunLongLink {
	
	private static class LongId implements Id {
		
		private static final long serialVersionUID = 1L;

		private final Long id;

		public LongId(final long id) {
			this.id = id;
		}

		@Override
		public boolean equals(final Object other) {
			/*
			 * This is not consistent with compareTo(Id)! compareTo(Id) states that
			 * o1 and o2 are equal (in terms of order) if toString() returns the
			 * same character sequence. However equals() can return false even if
			 * other.toString() equals this.id (in case other is not of type IdImpl)!
			 * joh aug09
			 */
			if (!(other instanceof LongId)) return false;
			if (other == this) return true;
			return this.id.equals(((LongId)other).id);
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}

		@Override
		public int compareTo(final Id o) {
			return -1 * this.id.compareTo(((LongId) o).id);
		}


		@Override
		public String toString() {
			return this.id.toString();
		}

		
	}

	private static final int CUTOFF = 500;
	private static final Random RANDOM = new Random();
	private static final double N_LANES = 2.0;
	private static final double CAP_PER_LANE = 0.4;
	private static final int N_VEH = 1500;
	private static final double FREESPEED = 100.0;
	private static final double P_TRUCK = 0.1;
	private static final double P_MED = 0.5;
	private static final double P_FAST = 0.4;
	private PrintStream writer;

	public static void main(String[] args) {
		RunLongLink runLongLink = new RunLongLink();
		runLongLink.openFile();
		for (double p = 0.03; p <= 1.2; p += 0.03) {
			runLongLink.run(p);
		}
		runLongLink.closeFile();
	}

	private void closeFile() {
		writer.close();
	}

	private void openFile() {
		try {
			writer = new PrintStream("/Users/zilske/dlr/traveltimes.txt");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void run(double p) {
		Config config = ConfigUtils.createConfig();
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setMainModes(Arrays.asList("fast", "med", "truck"));
		qSimConfigGroup.setSnapshotStyle("queue");
		Scenario scenario = ScenarioUtils.createScenario(config);
		((NetworkImpl) scenario.getNetwork()).setCapacityPeriod(1.0);
		makeLinks(scenario.getNetwork());
		makeDemand(scenario.getPopulation(), scenario.getNetwork(), p);
		//		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write("/Users/zilske/dlr/population.xml");

		EventsManager events = EventsUtils.createEventsManager();
		EventWriterXML handler = new EventWriterXML("/Users/zilske/dlr/events.xml");
		events.addHandler(handler);
		final Map<String, Integer> ns = new HashMap<String, Integer>();
		final Map<String, Integer> ttsums = new HashMap<String, Integer>();

		final int[] outqueue = new int[N_VEH - CUTOFF];

		EventsToLegs eventsToLegs = new EventsToLegs();
		eventsToLegs.setLegHandler(new LegHandler() {

			int idx = 0;

			@Override
			public void handleLeg(Id agentId, Leg leg) {
				if (idx >= CUTOFF) {
					// System.out.println(agentId + " " + leg.getMode() + " " + leg.getTravelTime());
					int n = ns.get(leg.getMode()) != null ? ns.get(leg.getMode()) : 0;
					int ttsum = ttsums.get(leg.getMode()) != null ? ttsums.get(leg.getMode()) : 0;
					++n;
					ttsum += leg.getTravelTime();
					ns.put(leg.getMode(), n);
					ttsums.put(leg.getMode(), ttsum);
					outqueue[idx - CUTOFF] = Integer.parseInt(agentId.toString());
				}
				++idx;
			}

		});
		events.addHandler(eventsToLegs);
		QSim qSim1 = new QSim(scenario, events);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = new QNetsimEngineFactory() {
		
					@Override
					public QNetsimEngine createQSimEngine(Netsim sim) {
						NetsimNetworkFactory<QNode, QLinkImpl> netsimNetworkFactory = new NetsimNetworkFactory<QNode, QLinkImpl>() {
		
							@Override
							public QLinkImpl createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
								return new QLinkImpl(link, network, toQueueNode, new FIFOVehicleQ());
							}
		
							@Override
							public QNode createNetsimNode(final Node node, QNetwork network) {
								return new QNode(node, network);
							}
		
		
						};
						return new QNetsimEngine((QSim) sim, netsimNetworkFactory) ;
					}
				}.createQSimEngine(qSim1);
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim1.addMobsimEngine(teleportationEngine);

		QSim qSim = qSim1;

		//		QSim qSim = new QSim(scenario, events, new DefaultQSimEngineFactory());

		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();
		VehicleType truck = VehicleUtils.getFactory().createVehicleType(new IdImpl("truck"));
		truck.setPcuEquivalents(1.0);
		truck.setMaximumVelocity(25.0);
		modeVehicleTypes.put("truck", truck);
		VehicleType med = VehicleUtils.getFactory().createVehicleType(new IdImpl("med"));
		med.setPcuEquivalents(1.0);
		med.setMaximumVelocity(30.0);
		modeVehicleTypes.put("med", med);
		VehicleType fast = VehicleUtils.getFactory().createVehicleType(new IdImpl("fast"));
		fast.setPcuEquivalents(1.0);
		fast.setMaximumVelocity(35.0);
		modeVehicleTypes.put("fast", fast);

		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
		agentSource.setModeVehicleTypes(modeVehicleTypes);
		qSim.addAgentSource(agentSource);

		//				OnTheFlyServer server = startServerAndRegisterWithQSim(config,scenario, events, qSim);
		//				OTFClientLive.run(config, server);


		qSim.run();
		handler.closeFile();

		writer.format("%.2f\t",p);
		writer.format("%.2f\t",(double) ttsums.get("truck") / (double) ns.get("truck"));
		writer.format("%.2f\t",(double) ttsums.get("med") / (double) ns.get("med"));
		writer.format("%.2f\t",(double) ttsums.get("fast") / (double) ns.get("fast"));
		writer.format("%d%n", countInversions(outqueue));
	}

	public static OnTheFlyServer startServerAndRegisterWithQSim(Config config, Scenario scenario, EventsManager events, QSim qSim) {
		OnTheFlyServer server = OnTheFlyServer.createInstance(scenario, events);
		OTFVisMobsimListener queueSimulationFeature = new OTFVisMobsimListener(server);
		qSim.addQueueSimulationListeners(queueSimulationFeature);
		server.setSimulation(qSim);

		if (config.scenario().isUseTransit()) {

			Network network = scenario.getNetwork();
			TransitSchedule transitSchedule = ((ScenarioImpl) scenario).getTransitSchedule();
			TransitQSimEngine transitEngine = qSim.getTransitEngine();
			TransitStopAgentTracker agentTracker = transitEngine.getAgentTracker();
			AgentSnapshotInfoFactory snapshotInfoFactory = qSim.getVisNetwork().getAgentSnapshotInfoFactory();
			FacilityDrawer.Writer facilityWriter = new FacilityDrawer.Writer(network, transitSchedule, agentTracker, snapshotInfoFactory);
			server.addAdditionalElement(facilityWriter);
		}

		server.pause();
		return server;
	}

	private static void makeDemand(Population population, Network network, double pin) {
		int vehId = 0;
		for (int t=0; t<100000; ++t) {
			for (int lane=0; lane<2; ++lane) {
				if (RANDOM.nextFloat() < pin) {
					if (lane == 0) { // slow lane
						if (RANDOM.nextFloat() < P_TRUCK / (P_TRUCK + 0.5*P_MED)) {
							makeDeparture(t, vehId, lane, "truck", population, network);
						} else {
							makeDeparture(t, vehId, lane, "med", population, network);
						}
					} else { // other lane
						if (RANDOM.nextFloat() < P_FAST / (P_FAST + 0.5*P_MED)) {
							makeDeparture(t, vehId, lane, "fast", population, network);
						} else {
							makeDeparture(t, vehId, lane, "med", population, network);
						}
					}
					++vehId;
				}
				if (vehId >= N_VEH) break;
			}
			if (vehId >= N_VEH) break;
		}
	}

	private static void makeDeparture(int t, int vehId, int lane, String carType, Population population, Network network) {
		// Person person = population.getFactory().createPerson(new IdImpl(vehId));
		Person person = population.getFactory().createPerson(new LongId(vehId));
		
		Plan plan = population.getFactory().createPlan();
		Activity a1 = population.getFactory().createActivityFromLinkId("a1", new IdImpl("0to1"));
		a1.setEndTime(t);
		plan.addActivity(a1);
		Leg l = population.getFactory().createLeg(carType);
		List<Id> route = new ArrayList<Id>();
		route.add(new IdImpl("0to1"));
		route.add(new IdImpl("1to2"));
		route.add(new IdImpl("2to3"));
		l.setRoute(RouteUtils.createNetworkRoute(route, network));
		plan.addLeg(l);
		Activity a2 = population.getFactory().createActivityFromLinkId("a2", new IdImpl("2to3"));
		plan.addActivity(a2);
		person.addPlan(plan);
		population.addPerson(person);
	}

	private static void makeLinks(Network network) {
		Node node0 = network.getFactory().createNode(new IdImpl("node0"), new CoordImpl(-200.0,0.0));
		network.addNode(node0);
		Node node1 = network.getFactory().createNode(new IdImpl("node1"), new CoordImpl(0.0,0.0));
		network.addNode(node1);
		Node node2 = network.getFactory().createNode(new IdImpl("node2"), new CoordImpl(+9800.0,0.0));
		network.addNode(node2);
		Node node3 = network.getFactory().createNode(new IdImpl("node3"), new CoordImpl(+10000.0,0.0));
		network.addNode(node3);
		Link _0to1 = network.getFactory().createLink(new IdImpl("0to1"), node0, node1);
		_0to1.setFreespeed(FREESPEED);
		_0to1.setNumberOfLanes(N_LANES);
		_0to1.setCapacity(10 * N_LANES * CAP_PER_LANE);
		_0to1.setLength(200.0);
		network.addLink(_0to1);
		Link _1to2 = network.getFactory().createLink(new IdImpl("1to2"), node1, node2);
		_1to2.setFreespeed(FREESPEED);
		_1to2.setNumberOfLanes(N_LANES);
		_1to2.setCapacity(N_LANES * CAP_PER_LANE);
		_1to2.setLength(9800.0);
		network.addLink(_1to2);
		Link _2to3 = network.getFactory().createLink(new IdImpl("2to3"), node2, node3);
		network.addLink(_2to3);
		_2to3.setFreespeed(FREESPEED);
		_2to3.setNumberOfLanes(N_LANES);
		_2to3.setCapacity(10 * N_LANES * CAP_PER_LANE);
		_2to3.setLength(200.0);
	}

	public static int countInversions(int nums[])
	/*  This function will count the number of inversions in an
        array of numbers.  (Recall that an inversion is a pair
        of numbers that appear out of numerical order in the list.

        We use a modified version of the MergeSort algorithm to 
        do this, so it's a recursive function.  We split the
        list into two (almost) equal parts, recursively count
        the number of inversions in each part, and then count
        inversions caused by one element from each part of 
        the list. 

        The merging is done is a separate procedure given below,
        in order to simplify the presentation of the algorithm
        here. 

        Note:  I am assuming that the integers are distinct, but
        they need *not* be integers { 1, 2, ..., n} for some n.  

	 */
	{  
		int mid = nums.length/2, k;
		int countLeft, countRight, countMerge;

		/*  If the list is small, there's nothing to do.  */ 
		if (nums.length <= 1) 
			return 0;

		/*  Otherwise, we create new arrays and split the list into 
          two (almost) equal parts.   
		 */
		int left[] = new int[mid];
		int right[] = new int[nums.length - mid];

		for (k = 0; k < mid; k++)
			left[k] = nums[k];
		for (k = 0; k < nums.length - mid; k++)
			right[k] = nums[mid+k];

		/*  Recursively count the inversions in each part. 
		 */
		countLeft = countInversions (left);
		countRight = countInversions (right);

		/*  Now merge the two sublists together, and count the
          inversions caused by pairs of elements, one from
          each half of the original list.  
		 */ 
		int result[] = new int[nums.length];
		countMerge = mergeAndCount (left, right, result);

		/*  Finally, put the resulting list back into the original one.
          This is necessary for the recursive calls to work correctly.
		 */
		for (k = 0; k < nums.length; k++)
			nums[k] = result[k];

		/*  Return the sum of the values computed to 
          get the total number of inversions for the list.
		 */
		return (countLeft + countRight + countMerge);  

	}  /*  end of "countInversions" procedure  */

	public static int mergeAndCount (int left[], int right[], int result[])
	/*  This procudure will merge the two lists, and count the number of
        inversions caused by the elements in the "right" list that are 
        less than elements in the "left" list.  
	 */ 
	{
		int a = 0, b = 0, count = 0, i, k=0;

		while ( ( a < left.length) && (b < right.length) )
		{
			if ( left[a] <= right[b] )
				result [k] = left[a++];
			else       /*  You have found (a number of) inversions here.  */  
			{
				result [k] = right[b++];
				count += left.length - a;
			}
			k++;
		}

		if ( a == left.length )
			for ( i = b; i < right.length; i++)
				result [k++] = right[i];
		else
			for ( i = a; i < left.length; i++)
				result [k++] = left[i];

		return count;
	} 


}
