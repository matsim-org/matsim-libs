/**
 * 
 */
package org.matsim.vis.snapshots.writers;

import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.vehicles.BasicVehicle;

/**
 * @author nagel
 *
 */
public interface VisVehicle {
	
	BasicVehicle getBasicVehicle() ;
	
	PersonDriverAgent getDriver() ;

}
