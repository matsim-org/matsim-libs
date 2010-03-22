package playground.david.otfivs.executables;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vis.netvis.streaming.SimStateWriterI;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkTravelTimesHandler;
import org.matsim.vis.otfvis.opengl.layer.ColoredStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;
import org.matsim.vis.otfvis.server.OnTheFlyServer;
import org.matsim.world.MatsimWorldReader;


/**
 * @author DS
 *
 */
public class OnTheFlyQueueSimQuadLinkSpeed extends QSim{
	private final List<SimStateWriterI> writers = new ArrayList<SimStateWriterI>();
	private OnTheFlyServer myOTFServer = null;

	@Override
	protected void prepareSim() {
		this.myOTFServer = OnTheFlyServer.createInstance("AName1", this.network, getEventsManager(), false);

		super.prepareSim();

		// FOR TESTING ONLY!
		OTFConnectionManager connect = new OTFConnectionManager();
		connect.connectQLinkToWriter(OTFLinkTravelTimesHandler.Writer.class);
		connect.connectWriterToReader(OTFLinkTravelTimesHandler.Writer.class, OTFLinkTravelTimesHandler.class);
		connect.connectReaderToReceiver(OTFLinkTravelTimesHandler.class, ColoredStaticNetLayer.QuadDrawerLinkSpeed.class);
		connect.connectReceiverToLayer(ColoredStaticNetLayer.QuadDrawerLinkSpeed.class, ColoredStaticNetLayer.class);
		connect.connectWriterToReader(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect.connectWriterToReader(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
		connect.connectReaderToReceiver(OTFAgentsListHandler.class,  AgentPointDrawer.class);

		OTFClientLive client = new OTFClientLive("rmi:127.0.0.1:4019:AName1", connect);
		client.start();
		try {
			this.myOTFServer.pause();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void cleanupSim(double time) {
//		if (myOTFServer != null) myOTFServer.stop();
		this.myOTFServer.cleanup();
		this.myOTFServer = null;
		super.cleanupSim(time);
	}

	@Override
	protected void afterSimStep(final double time) {
		super.afterSimStep(time);
		int status = 0;

		//Gbl.printElapsedTime();
//		myOTFServer.updateOut(time);
		this.myOTFServer.updateStatus(time);

	}

	public OnTheFlyQueueSimQuadLinkSpeed(final Scenario scenario, final EventsManagerImpl events) {
		super(scenario, events);
	}

	public static void main(final String[] args) {

//		String studiesRoot = "../";
		String localDtdBase = "../matsimGIS/dtd/";

//		String netFileName = studiesRoot + "berlin-wip/network/wip_net.xml";
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter001car_hwh.routes_wip.plans.xml.gz"; // 15931 agents
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car_hwh.routes_wip.plans.xml.gz"; // 160171 agents
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car.routes_wip.plans.xml.gz";  // 299394 agents
//		String worldFileName = studiesRoot + "berlin-wip/synpop-2006-04/world_TVZ.xml";

		String runBase = "/Volumes/data/work/cvsRep/vsp-cvs/runs/";

		String runDir = "run415/";

		String netFileName = runBase + runDir + "output_network.xml.gz";

		String popFileName = runBase + runDir + "output_plans.xml.gz";

		String worldFileName = runBase + runDir + "output_world.xml.gz";


		Config config = Gbl.createConfig(args);

		config.global().setLocalDtdBase(localDtdBase);

		config.controler().setOutputDirectory(runBase + runDir);

		config.simulation().setStartTime(Time.parseTime("00:00:00"));
		config.simulation().setEndTime(Time.parseTime("00:00:00"));

		config.simulation().setFlowCapFactor(0.1);
		config.simulation().setStorageCapFactor(0.5);


		if(args.length >= 1) {
			netFileName = config.network().getInputFile();
			popFileName = config.plans().getInputFile();
			worldFileName = config.world().getInputFile();
		}

		ScenarioImpl scenario = new ScenarioImpl(config);

		if (worldFileName != null) {
			MatsimWorldReader world_parser = new MatsimWorldReader(scenario);
			world_parser.readFile(worldFileName);
		}

		NetworkLayer net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFileName);

		PopulationImpl population = scenario.getPopulation();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(popFileName);
		System.out.println("agents read: " + population.getPersons().size());

		EventsManagerImpl events = new EventsManagerImpl();

		OnTheFlyQueueSimQuadLinkSpeed sim = new OnTheFlyQueueSimQuadLinkSpeed(scenario, events);

		config.simulation().setSnapshotFormat("none");// or just set the snapshotPeriod to zero ;-)


		sim.run();

	}


}
