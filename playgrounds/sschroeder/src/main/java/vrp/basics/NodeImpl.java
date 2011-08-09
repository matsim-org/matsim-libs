/**
 * 
 */
package vrp.basics;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import vrp.api.Node;


/**
 * @author stefan schroeder
 *
 */
public class NodeImpl implements Comparable<NodeImpl>, Node {
	
	private Id id;
	
	private int matrixId;
	
	private Coord coord;
	
	/* (non-Javadoc)
	 * @see core.basic.Node#getCoord()
	 */
	public Coord getCoord() {
		return coord;
	}

	/* (non-Javadoc)
	 * @see core.basic.Node#setCoord(org.matsim.api.core.v01.Coord)
	 */
	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	public NodeImpl(Id id) {
		super();
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see core.basic.Node#getId()
	 */
	public Id getId() {
		return id;
	}

	public int getMatrixId() {
		return matrixId;
	}

	public void setMatrixId(int matrixId) {
		this.matrixId = matrixId;
	}

	/* (non-Javadoc)
	 * @see core.basic.Node#getDemand()
	 */
	
	public String toString(){
		return "[id="+id+"]";
	}


	public int compareTo(NodeImpl o) {
		return 0;
	}
	
}
