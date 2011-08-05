/**
 * 
 */
package vrp.basics;

import org.matsim.core.utils.geometry.CoordUtils;

import vrp.api.Costs;
import vrp.api.Node;


/**
 * @author stefan schroeder
 *
 */
public class CrowFlyDistance implements Costs{
	
	public int speed;
	
	public synchronized Double getCost(Node from, Node to) {
		return CoordUtils.calcDistance(from.getCoord(), to.getCoord());
	}

	public synchronized Double getDistance(Node from, Node to) {
		return getCost(from,to);
	}


	public synchronized Double getTime(Node from, Node to) {
		return getDistance(from, to)/speed;
	}

}
