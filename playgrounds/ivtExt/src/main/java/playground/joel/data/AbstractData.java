package playground.joel.data;

/**
 * Created by Joel on 28.02.2017.
 */

import java.io.File;

import org.matsim.core.api.experimental.events.EventsManager;

abstract class AbstractData {
    abstract void initialize(EventsManager events);

    abstract void writeXML(File directory);
}
