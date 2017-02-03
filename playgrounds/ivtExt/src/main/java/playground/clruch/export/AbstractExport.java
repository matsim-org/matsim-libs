package playground.clruch.export;

import java.io.File;

import org.matsim.core.api.experimental.events.EventsManager;

/**
 * Created by Claudio on 2/2/2017.
 */
abstract class AbstractExport {
    abstract void initialize(EventsManager events);

    abstract void writeXML(File directory);
}
