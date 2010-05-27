/**
 * 
 */
package org.matsim.vis.snapshots.writers;

import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel
 *
 */
public interface VisVehicle {
	
	Vehicle getBasicVehicle() ;
	
	PersonDriverAgent getDriver() ;

}
