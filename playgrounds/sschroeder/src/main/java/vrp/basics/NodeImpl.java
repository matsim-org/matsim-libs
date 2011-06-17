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
	
	private Relation relationship;

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

	
	/* (non-Javadoc)
	 * @see core.basic.Node#getRelationship()
	 */
	public Relation getRelationship() {
		return relationship;
	}

	/* (non-Javadoc)
	 * @see core.basic.Node#setRelationship(core.basic.NodeImpl.Relationship)
	 */
	public void setRelationship(Relation relationship) {
		this.relationship = relationship;
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
