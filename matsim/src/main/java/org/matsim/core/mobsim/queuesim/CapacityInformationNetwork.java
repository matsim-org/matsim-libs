/**
 * 
 */
package org.matsim.core.mobsim.queuesim;

import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author nagel
 *
 */
@Deprecated // only there because christoph uses it.  kai, may'10
public interface CapacityInformationNetwork {
	@Deprecated // only there because christoph uses it.  kai, may'10
	Map<Id,? extends CapacityInformationLink> getCapacityInformationLinks() ;
}
