/**
 * One agent's day, reduced to what the capability analysis needs.
 * <p>
 * {@link org.matsim.contrib.msp.capability.structure.FundamentalPlanExtractor} strips a plan down to the
 * activities the agent cannot choose not to perform,
 * {@link org.matsim.contrib.msp.capability.structure.FreePeriod} splits what is left into the stretches
 * a discretionary activity could be inserted into - which is where home being elastic comes in - and
 * {@link org.matsim.contrib.msp.capability.structure.CapabilityTimeline} lays the same day out as blocks
 * on a 00:00-24:00 axis for drawing.
 */
package org.matsim.contrib.msp.capability.structure;
