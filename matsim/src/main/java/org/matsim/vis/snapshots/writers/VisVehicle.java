/**
 * 
 */
package org.matsim.vis.snapshots.writers;

import org.matsim.core.mobsim.framework.PlanDriverAgent;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel
 *
 */
public interface VisVehicle {
	
	/**
	 * @return the <code>Vehicle</code> that this simulation vehicle represents
	 */
	public Vehicle getVehicle();

	PlanDriverAgent getDriver() ;
	// yy presumably, this should return DriverAgent

}
