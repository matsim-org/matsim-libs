package playground.david.vis.data;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.utils.misc.QuadTree;

import playground.david.vis.interfaces.OTFDataReader;


public class OTFServerQuad extends QuadTree<OTFDataWriter> implements Serializable {

	class ConvertToClientExecutor extends Executor<OTFDataWriter> {
		final OTFConnectionManager connect;
		final OTFClientQuad client;

		public ConvertToClientExecutor(OTFConnectionManager connect2, OTFClientQuad client) {
			this.connect = connect2;
			this.client = client;
		}
		@Override
		public void execute(double x, double y, OTFDataWriter writer)  {
			Collection<Class> readerClasses = connect.getEntries(writer.getClass());
			for (Class readerClass : readerClasses) {
				try {
					Object reader = readerClass.newInstance();
					client.put(x, y, (OTFDataReader)reader);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
	}

	class ConvertToServerExecutor extends Executor<OTFDataWriter> {
		final OTFServerQuad newServer;

		public ConvertToServerExecutor(OTFServerQuad server) {
			this.newServer = server;
		}

		@Override
		public void execute(double x, double y, OTFDataWriter writer)  {
				newServer.put(x, y, writer);
		}
	}

	class WriteDataExecutor extends Executor<OTFDataWriter> {
		final DataOutputStream out;
		boolean writeConst;

		public WriteDataExecutor(DataOutputStream out, boolean writeConst) {
			this.out = out;
			this.writeConst = writeConst;
		}
		@Override
		public void execute(double x, double y, OTFDataWriter writer)  {
			try {
				if (writeConst) writer.writeConstData(out);
				else writer.writeDynData(out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private double minEasting;
	private double maxEasting;
	private double minNorthing;
	private double maxNorthing;
	transient private QueueNetworkLayer net;

	// Change this, find better way to transport this info into Writers
	public static double offsetEast;
	public static double offsetNorth;

	public OTFServerQuad(QueueNetworkLayer net) {
		super(0,0,0,0);
		updateBoundingBox(net);
		// has to be done later, as we do not know the writers yet!
		// fillQuadTree(net);
	}

	public OTFServerQuad(double minX, double minY, double maxX, double maxY) {
		super(minX, minY, maxX, maxY);
		this.minEasting = minX;
		this.maxEasting = maxX;
		this.minNorthing = minY;
		this.maxNorthing = maxY;
	}

	public void updateBoundingBox(QueueNetworkLayer net){
		minEasting = Double.POSITIVE_INFINITY;
		maxEasting = Double.NEGATIVE_INFINITY;
		minNorthing = Double.POSITIVE_INFINITY;
		maxNorthing = Double.NEGATIVE_INFINITY;

		for (Iterator<? extends QueueNode> it = net.getNodes().values().iterator(); it.hasNext();) {
			QueueNode node = it.next();
			minEasting = Math.min(minEasting, node.getCoord().getX());
			maxEasting = Math.max(maxEasting, node.getCoord().getX());
			minNorthing = Math.min(minNorthing, node.getCoord().getY());
			maxNorthing = Math.max(maxNorthing, node.getCoord().getY());
		}

		this.net = net;
		offsetEast = minEasting;
		offsetNorth = minNorthing;
	}

	public void fillQuadTree(OTFNetWriterFactory writers) {
		final double easting = maxEasting - minEasting;
		final double northing = maxNorthing - minNorthing;
		// set top node
		setTopNode(0, 0, easting, northing);

    	for (QueueNode node : net.getNodes().values()) {
    		OTFDataWriter<QueueNode> writer = writers.getNodeWriter();
    		if (writer != null) writer.setSrc(node);
    		put(node.getCoord().getX() - minEasting, node.getCoord().getY() - minNorthing, writer);
    	}
    	for (QueueLink link : net.getLinks().values()) {
    		double middleEast = (link.getToNode().getCoord().getX() + link.getFromNode().getCoord().getX())*0.5 - minEasting;
    		double middleNorth = (link.getToNode().getCoord().getY() + link.getFromNode().getCoord().getY())*0.5 - minNorthing;
    		OTFDataWriter<QueueLink> writer = writers.getLinkWriter();
    		// null means take the default handler
    		if (writer != null) writer.setSrc(link);
    		put(middleEast, middleNorth, writer);
    	}
		System.out.println("SERVER execute  == " + " = links sum " + (net.getLinks().values().size()) +"objects time");
	}

	public OTFClientQuad convertToClient(final OTFConnectionManager connect) {
		final OTFClientQuad client = new OTFClientQuad(0.,0.,maxEasting - minEasting, maxNorthing - minNorthing);

		Gbl.startMeasurement();
		int colls = this.execute(0.,0.,maxEasting - minEasting,maxNorthing - minNorthing,
				this.new ConvertToClientExecutor(connect,client));
		System.out.println("SERVER execute  == " + colls  +"objects time");
		Gbl.printElapsedTime();

		return client;
	}

	public OTFServerQuad convertToServer() {
		final OTFServerQuad newServer = new OTFServerQuad(0.,0.,maxEasting - minEasting,maxNorthing - minNorthing);

		Gbl.startMeasurement();
		int colls = this.execute(0.,0.,maxEasting - minEasting,maxNorthing - minNorthing,
				this.new ConvertToServerExecutor(newServer));
		System.out.println("SERVER execute  == " + colls  +"objects time");
		Gbl.printElapsedTime();

		return newServer;
	}

	public void writeConstData(DataOutputStream out) {
		Gbl.startMeasurement();
		int colls = this.execute(0.,0.,maxEasting - minEasting,maxNorthing - minNorthing,
				this.new WriteDataExecutor(out,true));
		System.out.println("execute half  == " + colls + " objects time");
		Gbl.printElapsedTime();


	}

	public void writeDynData(DataOutputStream out) {
		Gbl.startMeasurement();
		int colls = this.execute(0.,0.,maxEasting - minEasting,maxNorthing - minNorthing,
				this.new WriteDataExecutor(out,false));
		System.out.println("execute half  == " + colls + " objects time");
		Gbl.printElapsedTime();
	}

}
