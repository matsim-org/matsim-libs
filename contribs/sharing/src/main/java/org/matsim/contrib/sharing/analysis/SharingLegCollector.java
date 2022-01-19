package org.matsim.contrib.sharing.analysis;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.sharing.service.SharingService;

/**
 * @author steffenaxer
 */
public interface SharingLegCollector {
	
	Map<Id<SharingService>, Collection<SharingLeg>> getSharingLegs();
	
}
