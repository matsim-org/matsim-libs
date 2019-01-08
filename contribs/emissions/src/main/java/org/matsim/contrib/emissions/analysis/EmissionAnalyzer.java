package org.matsim.contrib.emissions.analysis;

import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import java.nio.file.Path;

public class EmissionAnalyzer {

    private final int binSize;
    private final Path eventsFile;

    public EmissionAnalyzer(final int binSizeInSeconds, final Path eventsFile) {
        this.binSize = binSizeInSeconds;
        this.eventsFile = eventsFile;
    }

    public void process() {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        EmissionEventsReader eventsReader = new EmissionEventsReader(eventsManager);
        //eventsManager.addHandler(new WarmEmissionEventHandler());
    }
}
