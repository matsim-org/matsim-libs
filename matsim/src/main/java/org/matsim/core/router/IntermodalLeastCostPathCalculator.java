package org.matsim.core.router;

import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator;

public interface IntermodalLeastCostPathCalculator extends LeastCostPathCalculator {

	/**
	 * Restricts the router to only use links that have at least on of the given modes set as allowed.
	 * Set to <code>null</code> to disable any restrictions, i.e. to use all available modes.
	 *
	 * @param modeRestriction {@link TransportMode}s that can be used to find a route
	 *
	 * @see Link#setAllowedModes(Set)
	 */
	public void setModeRestriction(final Set<String> modeRestriction);

}