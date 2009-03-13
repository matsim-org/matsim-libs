/**
 * 
 */
package playground.yu.utils;

import org.matsim.interfaces.core.v01.Link;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * @author yu
 * 
 */
public class TollTools {

	public static boolean isInRange(Link loc, RoadPricingScheme toll) {
		return toll.getLinks().contains(loc);
	}
}
