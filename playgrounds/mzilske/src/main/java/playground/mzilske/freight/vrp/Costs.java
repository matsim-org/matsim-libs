package playground.mzilske.freight.vrp;



public interface Costs {

	public Double getCost(Node from, Node to);
	
	public Double getDistance(Node from, Node to);
	
	public Double getTime(Node from, Node to);

}