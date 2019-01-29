/**
 * 
 */
package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel
 *
 */
public interface VisVehicle extends Identifiable<Vehicle> {
	
	/**
	 * @return the <code>Vehicle</code> that this simulation vehicle represents
	 */
	Vehicle getVehicle();

	MobsimDriverAgent getDriver() ;
	
	double getSizeInEquivalents() ;

}
