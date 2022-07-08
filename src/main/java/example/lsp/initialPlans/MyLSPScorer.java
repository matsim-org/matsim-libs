package example.lsp.initialPlans;

import lsp.LSP;
import lsp.LSPScorer;
import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.contrib.freight.events.LSPTourEndEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPServiceEndEventHandler;
import org.matsim.contrib.freight.events.eventhandler.LSPTourEndEventHandler;

/**
 * Todo: Put in some plausible values ... ->
 * - fixed, time and distance costs for Tours.
 * - time for services (duration)
 *
 * TODO: Wie komme ich beim TourEndEvent an die passenden Daten ran um das vernüftig zu scoren?
 * Was ist mit handling in Hubs? ist das in "Service" mit drinnen??
 *
 * @author Kai Martins-Turner (kturner)
 */
class MyLSPScorer implements LSPScorer, LSPTourEndEventHandler, LSPServiceEndEventHandler {
	private double score = 0.;

	@Override
	public double computeScoreForCurrentPlan() {
		return score;
	}

	@Override
	public void setEmbeddingContainer(LSP pointer) {
	}

	@Override
	public void handleEvent(LSPTourEndEvent event) {
		score++;
		// use event handlers to compute score.  In this case, score is incremented by one every time a service and a tour ends.
	}

	@Override
	public void reset(int iteration) {
		score = 0.;
	}

	@Override
	public void handleEvent(LSPServiceEndEvent event) {
		score++;
		// use event handlers to compute score.  In this case, score is incremented by one every time a service and a tour ends.
	}
}
