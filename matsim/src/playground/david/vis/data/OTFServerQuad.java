package playground.david.vis.data;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.utils.collections.QuadTree;

import playground.david.vis.interfaces.OTFDataReader;
import playground.david.vis.interfaces.OTFServerRemote;


public class OTFServerQuad extends QuadTree<OTFDataWriter> implements Serializable {

	private final List<OTFDataWriter> additionalElements= new LinkedList<OTFDataWriter>();
	
	class ConvertToClientExecutor extends Executor<OTFDataWriter> {
		final OTFConnectionManager connect;
		final OTFClientQuad client;

		public ConvertToClientExecutor(OTFConnectionManager connect2, OTFClientQuad client) {
			this.connect = connect2;
			this.client = client;
		}
		@SuppressWarnings("unchecked")
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


	class WriteDataExecutor extends Executor<OTFDataWriter> {
		final ByteBuffer out;
		boolean writeConst;

		public WriteDataExecutor(ByteBuffer out, boolean writeConst) {
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
	}
	
	public void addAdditionalElement(OTFDataWriter element) {
		additionalElements.add(element);
	}

	public OTFClientQuad convertToClient(String id, final OTFServerRemote host, final OTFConnectionManager connect) {
		final OTFClientQuad client = new OTFClientQuad(id, host, 0.,0.,maxEasting - minEasting, maxNorthing - minNorthing);

		int colls = this.execute(0.,0.,maxEasting - minEasting,maxNorthing - minNorthing,
				this.new ConvertToClientExecutor(connect,client));

		for(OTFDataWriter element : additionalElements) {
			Collection<Class> readerClasses = connect.getEntries(element.getClass());
			for (Class readerClass : readerClasses) {
				try {
					Object reader = readerClass.newInstance();
					client.addAdditionalElement((OTFDataReader)reader);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			
		}
		return client;
	}

	public void writeConstData(ByteBuffer out) {
		int colls = this.execute(0.,0.,maxEasting - minEasting,maxNorthing - minNorthing,
				this.new WriteDataExecutor(out,true));
		
		for(OTFDataWriter element : additionalElements) {
			try {
				element.writeConstData(out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void writeDynData(QuadTree.Rect bounds, ByteBuffer out) {
		int colls = this.execute(bounds, this.new WriteDataExecutor(out,false));

		for(OTFDataWriter element : additionalElements) {
			try {
				element.writeDynData(out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Internally we hold the coordinates from 0,0 to max -min .. to optimize use of float in visualizer
	/* (non-Javadoc)
	 * @see org.matsim.utils.collections.QuadTree#getMaxEasting()
	 */
	@Override
	public double getMaxEasting() {
		return maxEasting;
	}

	/* (non-Javadoc)
	 * @see org.matsim.utils.collections.QuadTree#getMaxNorthing()
	 */
	@Override
	public double getMaxNorthing() {
		return maxNorthing;
	}

	/* (non-Javadoc)
	 * @see org.matsim.utils.collections.QuadTree#getMinEasting()
	 */
	@Override
	public double getMinEasting() {
		return minEasting;
	}

	/* (non-Javadoc)
	 * @see org.matsim.utils.collections.QuadTree#getMinNorthing()
	 */
	@Override
	public double getMinNorthing() {
		return minNorthing;
	}

}
