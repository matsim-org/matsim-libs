package playground.anhorni.crossborder;

import java.util.ArrayList;
import java.util.Iterator;

public class Zone {
	
	private int id;
	private ArrayList<Integer> nodes;
	

	public Zone() {
		nodes=new ArrayList<Integer>();
	}
	
	public Zone(int id) {
		super();
		this.id=id;
	}
		
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ArrayList<Integer> getNodes() {
		return nodes;
	}

	public void setNodes(ArrayList<Integer> nodes) {
		Iterator<Integer> n_it = nodes.iterator();
		while (n_it.hasNext()) {
			Integer n_i=n_it.next();
			this.nodes.add(n_i);
		}
	}
	
	public void setNode(Integer node) {
		nodes.add(node);
	}
	

}
