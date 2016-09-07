package contrib.publicTransitMapping.mapping;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitLine;
import contrib.publicTransitMapping.mapping.linkCandidateCreation.LinkCandidate;
import contrib.publicTransitMapping.mapping.pseudoRouter.ArtificialLink;
import contrib.publicTransitMapping.mapping.pseudoRouter.PseudoGraph;
import contrib.publicTransitMapping.mapping.pseudoRouter.PseudoSchedule;
import contrib.publicTransitMapping.mapping.pseudoRouter.PseudoTransitRoute;

/**
 * Generates and calculates the {@link PseudoTransitRoute} for each queued
 * {@link TransitLine} using a {@link PseudoGraph}. Stores the PseudoTransitRoutes
 * in a {@link PseudoSchedule}.<p/>
 *
 * If no path on the network can be found, an {@link ArtificialLink} between
 * {@link LinkCandidate}s can be added to a network.
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
