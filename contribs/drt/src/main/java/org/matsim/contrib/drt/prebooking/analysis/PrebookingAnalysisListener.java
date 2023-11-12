package org.matsim.contrib.drt.prebooking.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

public class PrebookingAnalysisListener implements IterationStartsListener, IterationEndsListener {
	private final String mode;

	private final OutputDirectoryHierarchy outputHierarchy;
	private final EventsManager eventsManager;

	private PrebookingAnalysisHandler handler;

	public PrebookingAnalysisListener(String mode, EventsManager eventsManager,
			OutputDirectoryHierarchy outputHierarchy) {
		this.mode = mode;
		this.eventsManager = eventsManager;
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		handler = new PrebookingAnalysisHandler(mode);
		eventsManager.addHandler(handler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		eventsManager.removeHandler(handler);

		String outputPath = outputHierarchy.getIterationFilename(event.getIteration(), getOutputFileName());
		new PrebookingAnalysisWriter(outputPath).write(handler.getRecords());
	}

	private String getOutputFileName() {
		return String.format("prebooking_%s.csv", mode);
	}
}
