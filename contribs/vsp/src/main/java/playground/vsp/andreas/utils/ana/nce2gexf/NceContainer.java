package playground.vsp.andreas.utils.ana.nce2gexf;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

/**
 *  
 * @author aneumann
 *
 */
public class NceContainer {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(NceContainer.class);
	static final String toStringHeader = "# from node id; from node x; from node y; to node id; to node x; to node y; diffPerLink"; 

	private Id fromNodeId;
	private Coord fromNodeCoord;
	private Id toNodeId;
	private Coord toNodeCoord;
	private double diffPerLink;

	public NceContainer(Id fromNodeId, Coord fromNodeCoord, Id toNodeId, Coord toNodeCoord, double diffPerLink) {
		this.fromNodeId = fromNodeId;
		this.fromNodeCoord = fromNodeCoord;
		this.toNodeId = toNodeId;
		this.toNodeCoord = toNodeCoord;
		this.diffPerLink = diffPerLink;
	}
	
	public Id getFromNodeId() {
		return fromNodeId;
	}

	public Coord getFromNodeCoord() {
		return fromNodeCoord;
	}

	public Id getToNodeId() {
		return toNodeId;
	}

	public Coord getToNodeCoord() {
		return toNodeCoord;
	}

	public double getDiffPerLink() {
		return diffPerLink;
	}
	
	@Override
	public String toString() {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(this.fromNodeId); strBuffer.append("; ");
		strBuffer.append(this.fromNodeCoord.getX()); strBuffer.append("; ");
		strBuffer.append(this.fromNodeCoord.getY()); strBuffer.append("; ");
		strBuffer.append(this.toNodeId); strBuffer.append("; ");
		strBuffer.append(this.toNodeCoord.getX()); strBuffer.append("; ");
		strBuffer.append(this.toNodeCoord.getY()); strBuffer.append("; ");
		strBuffer.append(this.diffPerLink);
		return strBuffer.toString();
	}	
}