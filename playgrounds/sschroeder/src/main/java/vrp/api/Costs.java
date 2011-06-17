package vrp.api;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface Costs {

	public Double getCost(Node from, Node to);
	
	public Double getDistance(Node from, Node to);
	
	public Double getTime(Node from, Node to);

}