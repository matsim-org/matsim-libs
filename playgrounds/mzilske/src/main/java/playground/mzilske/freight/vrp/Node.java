/**
 * 
 */
package playground.mzilske.freight.vrp;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;



/**
 * @author stefan
 *
 */
public class Node implements Comparable<Node> {
	
	private Id id;
	
	private int matrixId;
	
	private double demand;
	
	private Coord coord;
	
	private Collection<Relationship> relationships = new ArrayList<Relationship>();

	public Coord getCoord() {
		return coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	public Node(Id id) {
		super();
		this.id = id;
	}

	public Id getId() {
		return id;
	}

	public Collection<Relationship> getRelationships() {
		return relationships;
	}

	public int getMatrixId() {
		return matrixId;
	}

	public void setMatrixId(int matrixId) {
		this.matrixId = matrixId;
	}

	public double getDemand() {
		return demand;
	}

	public void setDemand(int demand) {
		this.demand = demand;
	}
	
	public String toString(){
		return "[id="+id+"][demand="+demand+"]";
	}

	@Override
	public int compareTo(Node o) {
		return 0;
	}
	
}
