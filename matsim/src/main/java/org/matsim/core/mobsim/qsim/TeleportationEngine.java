/**
 * 
 */
package org.matsim.core.mobsim.qsim;

import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.vis.snapshotwriters.VisData;

/**
 * "Combined" interface necessary to make the teleportation engine pluggable within the qsim.
 * 
 * @author kainagel
 */
interface TeleportationEngine extends DepartureHandler, MobsimEngine,
VisData {

}
