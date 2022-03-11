package org.matsim.contrib.shared_mobility.analysis;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.shared_mobility.service.SharingService;

/**
 * @author steffenaxer
 */
public interface SharingLegCollector {
	
	Map<Id<SharingService>, Collection<SharingLeg>> getSharingLegs();
	
}
