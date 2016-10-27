package playground.polettif.publicTransitMapping.mapping;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitLine;
import playground.polettif.publicTransitMapping.mapping.pseudoRouter.*;
import playground.polettif.publicTransitMapping.mapping.linkCandidateCreation.*;

/**
 * Generates and calculates the {@link PseudoTransitRoute} for each queued
 * {@link TransitLine} using a {@link PseudoGraph}. Stores the PseudoTransitRoutes
 * in a {@link PseudoSchedule}.<p></p>
 *
 * If no path on the network can be found, an {@link ArtificialLink} between
 * {@link LinkCandidate}s can be added to a network.
 *
 * @author polettif
 */
@Deprecated
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
