// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.interfaces;

import java.util.EventListener;

/**
 * Must be implemented for processing commands while user
 * interacts with map viewer.
 *
 * @author Jason Huntley
 *
 */
public interface JMapViewerEventListener extends EventListener {
    void processCommand(JMVCommandEvent command);
}
