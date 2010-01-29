/**
 * 
 */
package playground.yu.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * @author yu
 * 
 */
public class TollTools {

	/**
	 * @param loc
	 * @param toll
	 * @return a boolean value, whether a <code>Link</code> belongs to toll
	 *         area.
	 */
	public static boolean isInRange(Id linkId, RoadPricingScheme toll) {
		return toll.getLinkIdSet().contains(linkId);
	}

}
