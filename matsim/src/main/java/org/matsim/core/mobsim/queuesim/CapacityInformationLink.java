/**
 * 
 */
package org.matsim.core.mobsim.queuesim;

import org.matsim.api.core.v01.network.Link;

/**
 * @author nagel
 *
 */
@Deprecated // only there because christoph uses it.  kai, may'10
public interface CapacityInformationLink {
	@Deprecated // only there because christoph uses it.  kai, may'10
	public double getSimulatedFlowCapacity() ;
	@Deprecated // only there because christoph uses it.  kai, may'10
	public double getStorageCapacity() ;
	
	Link getLink() ;
}
