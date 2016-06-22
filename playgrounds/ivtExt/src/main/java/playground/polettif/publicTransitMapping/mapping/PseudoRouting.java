package playground.polettif.publicTransitMapping.mapping;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitLine;
import playground.polettif.publicTransitMapping.mapping.pseudoRouter.PseudoSchedule;

/**
 * Generates and calculates the PseudoRoutes for all the queued
 * TransitLines. If no path on the network can be found, artificial
 * links between link candidates are stored to be created later.
 *
 * @author polettif
 */
public interface PseudoRouting extends Runnable {

	void addTransitLineToQueue(TransitLine transitLine);

	PseudoSchedule getPseudoSchedule();

	void addArtificialLinks(Network network);

}
