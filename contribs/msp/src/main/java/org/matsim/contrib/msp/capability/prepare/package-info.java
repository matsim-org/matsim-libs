/**
 * Preparing the inputs of the capability analysis.
 * <p>
 * Two different kinds of preparation, both upstream of the analysis itself:
 * <ul>
 *     <li><b>the population</b> - {@link org.matsim.contrib.msp.capability.prepare.CapabilityPersonFilter}
 *     and its default implementation decide which persons are analysed at all, and therefore what the
 *     numbers are a statement about.</li>
 *     <li><b>the places</b> - {@link org.matsim.contrib.msp.capability.prepare.DiscretionaryActivityFacilityCsv}
 *     is the single entry point for where discretionary activities can be performed, and
 *     {@link org.matsim.contrib.msp.capability.prepare.RunCreateDiscretionaryActivityFacilities} turns that csv
 *     into a MATSim facilities file. A source that is not a hand maintained list - an OpenStreetMap
 *     extract, say - writes that csv rather than building facilities of its own.</li>
 * </ul>
 */
package org.matsim.contrib.msp.capability.prepare;
