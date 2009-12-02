/**
 * 
 */
package playground.yu.utils;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
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
	public static boolean isInRange(Link loc, RoadPricingScheme toll) {
		return toll.getLinks().contains(loc);
	}

	/**
	 * @param biggerToll
	 *            whose area surrounds the surroundedToll
	 * @param surroundedToll
	 *            whose area is surrounded by the biggerToll
	 * @return a <code>Collection</code> with element <code>Link</code>
	 *         containing all the <code>Link</code>s in the difference area
	 *         between biggerToll and surroundedToll
	 */
	public static Collection<Link> getDifferenceToll(
			RoadPricingScheme biggerToll, RoadPricingScheme surroundedToll) {
		biggerToll.getLinks().removeAll(surroundedToll.getLinks());
		return biggerToll.getLinks();
	}
}
