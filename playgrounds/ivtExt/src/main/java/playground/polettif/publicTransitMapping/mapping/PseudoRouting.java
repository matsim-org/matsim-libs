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

	/**
	 * Adds a transit line to the queue that is processed in run()
	 */
	void addTransitLineToQueue(TransitLine transitLine);

	/**
	 * Executes the PseudoRouting algorithm
	 */
	void run();

	/**
	 * @return a PseudoSchedule that contains all PseudoRoute for the queued lines
	 */
	PseudoSchedule getPseudoSchedule();

	/**
	 * Adds the necessary artificial links to the network.
	 */
	void addArtificialLinks(Network network);

}
