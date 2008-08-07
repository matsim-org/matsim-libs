package playground.david.otfivs.executables;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Population;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.netvis.streaming.SimStateWriterI;
import org.matsim.utils.vis.otfvis.data.OTFConnectionManager;
import org.matsim.utils.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.utils.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.utils.vis.otfvis.handler.OTFLinkTravelTimesHandler;
import org.matsim.utils.vis.otfvis.opengl.OnTheFlyClientQuad;
import org.matsim.utils.vis.otfvis.opengl.layer.ColoredStaticNetLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;
import org.matsim.utils.vis.otfvis.server.OnTheFlyServer;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;


/**
 * @author DS
 *
 */
public class OnTheFlyQueueSimQuadLinkSpeed extends QueueSimulation{
	private final List<SimStateWriterI> writers = new ArrayList<SimStateWriterI>();
	private OnTheFlyServer myOTFServer = null;

	@Override
	protected void prepareSim() {
		this.myOTFServer = OnTheFlyServer.createInstance("AName1", this.network, this.plans, events, false);

		super.prepareSim();

		// FOR TESTING ONLY!
		OTFConnectionManager connect = new OTFConnectionManager();
		connect.add(QueueLink.class, OTFLinkTravelTimesHandler.Writer.class);
		connect.add(OTFLinkTravelTimesHandler.Writer.class, OTFLinkTravelTimesHandler.class);
		connect.add(OTFLinkTravelTimesHandler.class, ColoredStaticNetLayer.QuadDrawerLinkSpeed.class);
		connect.add(ColoredStaticNetLayer.QuadDrawerLinkSpeed.class, ColoredStaticNetLayer.class);
		connect.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		connect.add(AgentPointDrawer.class, OGLAgentPointLayer.class);
		connect.add(OTFAgentsListHandler.class,  AgentPointDrawer.class);

		OnTheFlyClientQuad client = new OnTheFlyClientQuad("rmi:127.0.0.1:4019", connect);
		client.start();
		try {
			this.myOTFServer.pause();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void cleanupSim() {
//		if (myOTFServer != null) myOTFServer.stop();
		this.myOTFServer.cleanup();
		this.myOTFServer = null;
		super.cleanupSim();
	}

	@Override
	protected void afterSimStep(double time) {
		super.afterSimStep(time);
		int status = 0;

		//Gbl.printElapsedTime();
//		myOTFServer.updateOut(time);
		status = this.myOTFServer.updateStatus(time);

	}

	public OnTheFlyQueueSimQuadLinkSpeed(NetworkLayer net, Population plans, Events events) {
		super(net, plans, events);
	}

	public static void main(String[] args) {

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

		World world = Gbl.createWorld();

		if (worldFileName != null) {
			MatsimWorldReader world_parser = new MatsimWorldReader(Gbl.getWorld());
			world_parser.readFile(worldFileName);
		}

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);

		Population population = new Population();
		MatsimPlansReader plansReader = new MatsimPlansReader(population);
		plansReader.readFile(popFileName);
		population.printPlansCount();

		Events events = new Events();

		OnTheFlyQueueSimQuadLinkSpeed sim = new OnTheFlyQueueSimQuadLinkSpeed(net, population, events);

		config.simulation().setSnapshotFormat("none");// or just set the snapshotPeriod to zero ;-)


		sim.run();

	}


}
