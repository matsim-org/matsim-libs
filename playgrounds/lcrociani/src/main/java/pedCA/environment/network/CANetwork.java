package pedCA.environment.network;

import java.util.ArrayList;

import pedCA.environment.markers.Destination;
import pedCA.environment.markers.MarkerConfiguration;
import pedCA.environment.markers.TacticalDestination;

public class CANetwork{
	private ArrayList <CANode> nodes;
	private ArrayList <CAEdge> edges;
	
	public CANetwork(MarkerConfiguration markerConfiguration){
		nodes = new ArrayList <CANode>();
		edges = new ArrayList <CAEdge>();
		buildNetwork(markerConfiguration);
	}
	
	private void addNode(CANode node){
		nodes.add(node);
	}
	
	private void createBidirectionalEdge(CANode n1, CANode n2){
		edges.add(new CAEdge(n1,n2));
		edges.add(new CAEdge(n2,n1));
	}
	
	public void buildNetwork(MarkerConfiguration markerConfiguration){
		for (Destination destination : markerConfiguration.getDestinations())
			if (destination instanceof TacticalDestination){
				TacticalDestination td = (TacticalDestination) destination;
				CANode node = new CANode(td.getID(), td.getCoordinates(), td.getWidth());
				addNode(node);
			}
			
		for(int i=0;i<nodes.size();i++)
			for(int j=i+1; j<nodes.size();j++)
				createBidirectionalEdge(nodes.get(i),nodes.get(j));
	}
	
	public ArrayList <CANode> getNodes(){
		return nodes;
	}
	
	public ArrayList <CAEdge> getEdges(){
		return edges;
	}
	
	public String toString(){
		String result = "";
		result+="NODES\n";
		for (CANode node : nodes)
			result+=node.toString()+"\n";
		result+="\nEDGES\n";
		for (CAEdge edge : edges)
			result+=edge.toString()+"\n";
		return result;
	}
}
