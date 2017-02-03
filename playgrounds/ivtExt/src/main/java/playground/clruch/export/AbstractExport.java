package playground.clruch.export;

import org.matsim.core.api.experimental.events.EventsManager;

import java.io.File;

/**
 * Created by Claudio on 2/2/2017.
 */
public abstract class AbstractExport {
    abstract void initialize(EventsManager events);
    abstract void writeXML(File directory);
}
