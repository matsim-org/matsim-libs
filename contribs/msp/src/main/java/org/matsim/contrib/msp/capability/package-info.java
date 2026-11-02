/**
 * The capability analysis: how much a person can actually do with their day.
 * <p>
 * This package holds what a user of the analysis touches - the configuration, the orchestrator and the
 * two ways of running it - while the steps of the method live in sub packages:
 * <ul>
 *     <li>{@link org.matsim.contrib.msp.capability.prepare} - preparing the inputs: which persons are
 *     analysed, and the csv the places are built from.</li>
 *     <li>{@link org.matsim.contrib.msp.capability.facilities} - the places a discretionary activity can be
 *     performed at, as the analysis sees them.</li>
 *     <li>{@link org.matsim.contrib.msp.capability.structure} - one agent's day, reduced to what the
 *     method needs: the fundamental plan, its free periods, its blocks on a time axis.</li>
 *     <li>{@link org.matsim.contrib.msp.capability.measure} - turning a day and a place into numbers:
 *     travel times, insertion, flexibility, logsums.</li>
 *     <li>{@link org.matsim.contrib.msp.capability.result} - the rows that are written.</li>
 * </ul>
 * The dependencies run from this package into the sub packages and between them in the order above;
 * only {@link org.matsim.contrib.msp.capability.CapabilityConfigGroup} is read by all of them.
 *
 * @see org.matsim.contrib.msp.capability.CapabilityAnalysis for the measure itself
 * @see org.matsim.contrib.msp.capability.RunCapabilityAnalysis to run it on a finished run
 * @see org.matsim.contrib.msp.capability.CapabilityModule to chain it to the end of a run
 */
package org.matsim.contrib.msp.capability;
