package org.matsim.contrib.ev.withinday;

import org.matsim.api.core.v01.population.Person;

/**
 * This is a convenience class that helps preapring the relevant scenario
 * attributes for within-day electric vehicle charging.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class WithinDayEvUtils {
    private WithinDayEvUtils() {
    }

    /**
     * Checks whether a person is managed by within-day electric vehicle charging
     */
    static public boolean isActive(Person person) {
        return WithinDayEvEngine.isActive(person);
    }

    /**
     * Sets a person to be active in within-day electric vehicle charging or not
     */
    static public void setActive(Person person, boolean isActive) {
        WithinDayEvEngine.setActive(person, isActive);
    }

    /**
     * Activates a person for within-day electric vehicle charging
     */
    static public void activate(Person person) {
        WithinDayEvEngine.activate(person);
    }

    /**
     * Retrieves the maximum queue time for a person before an attempt is aborted
     */
    static public Double getMaximumQueueTime(Person person) {
        return WithinDayEvEngine.getMaximumQueueTime(person);
    }

    /**
     * Sets the maximum queue time for a person before an attempt is aborted
     */
    static public void setMaximumQueueTime(Person person, double maximumQueueTime) {
        WithinDayEvEngine.setMaximumQueueTime(person, maximumQueueTime);
    }
}
