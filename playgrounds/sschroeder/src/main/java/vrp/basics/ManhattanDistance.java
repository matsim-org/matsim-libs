package vrp.basics;

import vrp.api.Costs;
import vrp.api.Node;

/**
 * 
 * @author stefan schroeder
 *
 */

public class ManhattanDistance implements Costs {

	public double speed = 1;
	
	public Double getCost(Node from, Node to) {
		return getDistance(from, to);
	}

	public Double getDistance(Node from, Node to) {
		return calculateDistance(from, to);
	}

	public Double getTime(Node from, Node to) {
		double time = calculateDistance(from, to)/speed;
		return time;
	}
	
	private double calculateDistance(Node from, Node to){
		double distance = Math.abs(from.getCoord().getX() - to.getCoord().getX()) + Math.abs(from.getCoord().getY() - to.getCoord().getY());
		return distance;
	}

}
