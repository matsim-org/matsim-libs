package playground.ciarif.models.subtours;

import java.util.List;
import java.util.Vector;

import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.identifiers.IdI;

public class Subtour {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private int id;
	private List<IdI> nodes;
	private String purpose;
	private Coord start_coord;
	
	public Subtour() {
		super();
		this.nodes=new Vector<IdI>();
	}

	
	//////////////////////////////////////////////////////////////////////
	// Setters methods
	//////////////////////////////////////////////////////////////////////
	
	public void setStart_coord(Coord start_coord) {
		this.start_coord = start_coord;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setNodes(List<IdI> nodes) {
		this.nodes = nodes;
	}
	
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	
	public void setNode (IdI node) {
		this.nodes.add(node);
	}
	
	//////////////////////////////////////////////////////////////////////
	// Getters methods
	//////////////////////////////////////////////////////////////////////
	
	public Coord getStart_coord() {
		return start_coord;
	}
	
	public int getId() {
		return id;
	}
	
	public List<IdI> getNodes() {
		return nodes;
	}
	
	public String getPurpose() {
		return purpose;
	}
			
}
