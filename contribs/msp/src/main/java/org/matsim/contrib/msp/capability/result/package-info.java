/**
 * What the capability analysis writes: three levels that aggregate into one another.
 * <p>
 * {@link org.matsim.contrib.msp.capability.result.InsertionRecord} is one concrete opportunity - this
 * place, in this stretch of the day - and carries the parameters its own utility was computed with.
 * {@link org.matsim.contrib.msp.capability.result.FacilityRecord} is one place, worth its best
 * insertion, and {@link org.matsim.contrib.msp.capability.result.PersonRecord} is one person, mode and
 * activity type, with the nested logsum over the places they can reach.
 * {@link org.matsim.contrib.msp.capability.result.CapabilityWriter} writes the three, plus the blocks of
 * the agent's day they sit on.
 * <p>
 * Every row carries the parameters that produced its own numbers, so a downstream analysis can rebuild
 * an upper level from a lower one - or rebuild it differently - from the csv alone.
 */
package org.matsim.contrib.msp.capability.result;
