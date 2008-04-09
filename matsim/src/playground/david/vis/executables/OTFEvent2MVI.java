package playground.david.vis.executables;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.run.Events2Snapshot;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.utils.vis.snapshots.writers.SnapshotWriterI;
import org.matsim.world.World;

import playground.david.vis.OTFQuadFileHandler;
import playground.david.vis.handler.OTFAgentsListHandler;

public class OTFEvent2MVI extends OTFQuadFileHandler.Writer implements SnapshotWriterI{
	private final   String netFileName = "";
	private  String vehFileName = "";
	private  String outFileName = "";
	private static final int BUFFERSIZE = 100000000;


	private final OTFAgentsListHandler.Writer writer = new OTFAgentsListHandler.Writer();

	public OTFEvent2MVI(QueueNetworkLayer net, String EventFileName, String outFileName, double startTime, double intervall_s) {
		super(intervall_s, net, outFileName);
		this.vehFileName = EventFileName;
		this.outFileName = outFileName;
		this.intervall_s = intervall_s;
	}

	@Override
	protected void onAdditionalQuadData() {
		this.quad.addAdditionalElement(this.writer);
	}

	ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);
	private final int cntPositions=0;
	private double lastTime=-1;
	private final int cntTimesteps=0;

	private void convert() {

		open();
		// read and convert data from veh-file

		// create SnapshotGenerator
		Gbl.startMeasurement();
		Gbl.getConfig().simulation().setSnapshotFormat("none");
		Gbl.getConfig().simulation().setSnapshotPeriod(this.intervall_s);
		Events2Snapshot app = new Events2Snapshot();
		app.addExternalSnapshotWriter(this);
		app.run(new File(this.vehFileName), Gbl.getConfig(), this.net.getNetworkLayer());
		finishIT();
	}


	private void finishIT() {
	try {
		close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}


	public static void main(String[] args) {

		if ( args.length==0 )
			args = new String[] {"../../tmp/studies/berlin-wip/config_ds.xml"};

		Gbl.createConfig(args);
		Gbl.startMeasurement();
		World world = Gbl.createWorld();

		String netFileName = Gbl.getConfig().getParam("network","inputNetworkFile");
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);
		QueueNetworkLayer qnet = new QueueNetworkLayer(net);

		String eventFile = null;
		eventFile = "output/current/ITERS/it.0/0.events.txt.gz";
		eventFile = "../../tmp/studies/berlin-wip/run125/200.events.txt.gz";

		OTFEvent2MVI test  = new OTFEvent2MVI(qnet, eventFile, "output/ds_fromEvent.mvi",0 , 600);
		test.convert();
	}

	public void addAgent(PositionInfo position) {
		this.writer.positions.add(new OTFAgentsListHandler.ExtendedPositionInfo(position, 0,0));
	}

	public void beginSnapshot(double time) {
		this.writer.positions.clear();
		this.lastTime = time;
	}

	public void endSnapshot() {
		try {
			dump((int)this.lastTime);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void finish() {
		// TODO Auto-generated method stub

	}


}
