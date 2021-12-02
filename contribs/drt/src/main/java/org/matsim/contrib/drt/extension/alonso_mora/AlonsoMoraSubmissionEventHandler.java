package org.matsim.contrib.drt.extension.alonso_mora;

import org.matsim.core.events.handler.EventHandler;

/**
 * Handles a submission event for the dispatcher by Alonso-Mora et al.
 * 
 * @author sebhoerl
 */
public interface AlonsoMoraSubmissionEventHandler extends EventHandler {
	public void handleEvent(AlonsoMoraSubmissionEvent event);
}
