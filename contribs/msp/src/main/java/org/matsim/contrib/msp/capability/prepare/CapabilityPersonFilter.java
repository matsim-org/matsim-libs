package org.matsim.contrib.msp.capability.prepare;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.msp.capability.CapabilityAnalysis;

/**
 * Decides which persons the capability analysis looks at.
 * <p>
 * {@link DefaultCapabilityPersonFilter} is what the analysis uses unless told otherwise: the adult
 * residents of the study area whose day is a home based day. The socio-demographic filtering it does not
 * do yet - car availability, reduced mobility - goes there, or into an implementation of its own;
 * {@link #acceptAll()} switches the filtering off altogether.
 * <p>
 * Note that the structural requirements of the method are checked by {@link CapabilityAnalysis} itself:
 * whether a plan can be analysed at all - it has to start and end at home and stay inside the analysed
 * day - is a property of the method rather than a choice about which population to study. The default
 * filter checks them on the full plan as well, so that the persons the analysis reports on are also the
 * persons it counts.
 */
public interface CapabilityPersonFilter {

	boolean accept(Person person);

	/**
	 * Writes what the filter did to the log. Called by {@link CapabilityAnalysis} once the population has
	 * been through the filter, so that an implementation that counts its rejections can report them.
	 */
	default void logSummary() {
	}

	/**
	 * @return a filter that accepts everybody
	 */
	static CapabilityPersonFilter acceptAll() {
		return person -> true;
	}

}
