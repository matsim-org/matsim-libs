/**
 * The places a discretionary activity can be performed at, as the capability analysis sees them.
 * <p>
 * A view on standard MATSim facilities rather than a replacement for them:
 * {@link org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacility} wraps an
 * {@link org.matsim.facilities.ActivityFacility} and adds the capability vocabulary - what can be done
 * there, which kind of place it is, when it is open - and
 * {@link org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilityExtractor} reads that
 * vocabulary out of an ordinary facilities container. One instance stands for one type offered at one
 * facility, so a mall one can both shop and eat at is several instances sharing an id;
 * {@link org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilities} is the plural - those
 * instances indexed by type and by facility.
 */
package org.matsim.contrib.msp.capability.facilities;
