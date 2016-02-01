package pedCA.environment.network;

import java.util.ArrayList;

import matsimConnector.utility.MathUtility;
import pedCA.environment.grid.FloorFieldsGrid;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.markers.Destination;
import pedCA.environment.markers.MarkerConfiguration;
import pedCA.environment.markers.TacticalDestination;
import pedCA.utility.Constants;

public class CANetwork{
	private ArrayList <CANode> nodes;
	private ArrayList <CAEdge> edges;
	
	public CANetwork(MarkerConfiguration markerConfiguration, FloorFieldsGrid floorFieldsGrid){
		nodes = new ArrayList <CANode>();
		edges = new ArrayList <CAEdge>();
		buildNetwork(markerConfiguration, floorFieldsGrid);
	}
	
	private void addNode(CANode node){
		nodes.add(node);
	}
	
	private void createBidirectionalEdge(MarkerConfiguration markerConfiguration, CANode n1, CANode n2, double ffDistance){
		boolean isStairs = ((TacticalDestination)markerConfiguration.getDestination(n1.getDestinationId())).isStairsBorder() && ((TacticalDestination)markerConfiguration.getDestination(n2.getDestinationId())).isStairsBorder();
		edges.add(new CAEdge(n1,n2, ffDistance, isStairs));
		edges.add(new CAEdge(n2,n1, ffDistance, isStairs));
	}
	
	public void buildNetwork(MarkerConfiguration markerConfiguration, FloorFieldsGrid floorFieldsGrid){
		for (Destination destination : markerConfiguration.getDestinations())
			if (destination instanceof TacticalDestination){
				TacticalDestination td = (TacticalDestination) destination;
				CANode node = new CANode(td.getID(), td.getCoordinates(), td.getWidth());
				addNode(node);
			}
			
		for(int i=0;i<nodes.size();i++)
			for(int j=i+1; j<nodes.size();j++) {
				double ffDistance = getFFDistance(nodes.get(i), nodes.get(j), markerConfiguration, floorFieldsGrid);
				//WARNING: TRICK FOR THE HOOGENDOORN EXPERIMENT. REMOVE THIS "IF" AND RESTORE AFTER THE "ELSE" (REMOVE ALSO i>1 &&)
				/*if (i==0){
					if (j==1){
						ffDistance = MathUtility.average(ffDistance, getFFDistance(nodes.get(j), nodes.get(i), markerConfiguration, floorFieldsGrid));
						createBidirectionalEdge(nodes.get(i),nodes.get(j), ffDistance*Constants.CELL_SIZE);
					}
				}
				else if (i>1 && ffDistance!= Constants.MAX_FF_VALUE){*/
				if (ffDistance!= Constants.MAX_FF_VALUE){
					ffDistance = MathUtility.average(ffDistance, getFFDistance(nodes.get(j), nodes.get(i), markerConfiguration, floorFieldsGrid));
					createBidirectionalEdge(markerConfiguration, nodes.get(i),nodes.get(j), ffDistance*Constants.CELL_SIZE);
				}
			}
	}
	
	private double getFFDistance(CANode caNode1, CANode caNode2, MarkerConfiguration markerConfiguration, FloorFieldsGrid floorFieldsGrid) {
		int fieldLevel1 = caNode1.getDestinationId();
		int fieldLevel2 = caNode2.getDestinationId();
		TacticalDestination td = (TacticalDestination)markerConfiguration.getDestination(fieldLevel2);
		GridPoint td_center = td.getCells().get(td.getCells().size()/2);
		return floorFieldsGrid.getCellValue(fieldLevel1, td_center);
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
