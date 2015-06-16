package playground.nmviljoen.network;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;

public class NmvNode implements Comparable<NmvNode>{
	public String intID;
	public String id;
	public double X;
	public double Y;

	public NmvNode(String intID, String id, double X, double Y) {
		this.intID = intID;
		this.id = id;
		this.X = X;
		this.Y = Y;
	}
	
	public String toString() {
		return "id: " + id + " ("+ X + ";"+ Y + ")";
	}  
	
	public String slimString(){
		return id;
	}
	
	public String getId(){
		return this.id;
	}
	
	public String getXAsString(){
		return Double.toString(X);
	}
	
	public String getYAsString(){
		return Double.toString(Y);
	}

	@Override
	public int compareTo(NmvNode o) {
		return this.getId().compareTo(o.getId().toString());
	}
	
	@Override
	public boolean equals(Object o){
		return this.getId().equalsIgnoreCase(((NmvNode)o).getId());
	}
	
	public static void main(String[] args){
		Logger log = Logger.getLogger(NmvNode.class);
		NmvNode n1 = new NmvNode("1", "1", 0.0, 0.0);
		NmvNode n2 = new NmvNode("2", "2", 0.0, 0.0);
		NmvNode n11 = new NmvNode("1", "1", 0.0, 0.0);
		log.info("n1.compareTo(n2): " + n1.compareTo(n2));
		log.info("n1.compareTo(n3): " + n1.compareTo(n11));
		log.info("n2.compareTo(n3): " + n2.compareTo(n11));
		
		NmvNode n3 = new NmvNode("3", "3", 0.0, 0.0);
		Map<NmvNode, Double> map = new TreeMap<NmvNode, Double>();
		map.put(n1, 1.0);
		map.put(n2, 2.0);
		map.put(n3, 3.0);
		String s = "";
		for(NmvNode n : map.keySet()){
			s += " ... " + n.getId();
		}
		log.info(s);
		
		log.info("Is n1 in there? " + map.containsKey(n11));
		log.info("Is n1 and n11 the same? " + n1.equals(n11));
		log.info("Check keyset: " + map.keySet().contains(n11));
		
		/* Play with a small network. */
		Graph<NmvNode, NmvLink> graph = new DirectedSparseGraph<NmvNode, NmvLink>();
		graph.addVertex(n1);
		log.info("Check if graph contains n11: " + graph.containsVertex(n11));
		
	}

}