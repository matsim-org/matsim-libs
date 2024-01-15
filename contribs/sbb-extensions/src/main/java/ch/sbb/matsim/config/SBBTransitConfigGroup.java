/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * @author mrieser / SBB
 */
public class SBBTransitConfigGroup extends ReflectiveConfigGroup {

    static public final String GROUP_NAME = "SBBPt";

    static private final String PARAM_DETERMINISTIC_SERVICE_MODES = "deterministicServiceModes";
    static private final String PARAM_CREATE_LINK_EVENTS_INTERVAL = "createLinkEventsInterval";

    private final Set<String> deterministicServiceModes = new HashSet<>();
    private int createLinkEventsInterval = 0;

    public SBBTransitConfigGroup() {
        super(GROUP_NAME);
    }

    @StringGetter(PARAM_DETERMINISTIC_SERVICE_MODES)
    private String getDeterministicServiceModesAsString() {
        return CollectionUtils.setToString(this.deterministicServiceModes);
    }

    public Set<String> getDeterministicServiceModes() {
        return this.deterministicServiceModes;
    }

    @StringSetter(PARAM_DETERMINISTIC_SERVICE_MODES)
    private void setDeterministicServiceModes(String modes) {
        setDeterministicServiceModes(CollectionUtils.stringToSet(modes));
    }

    public void setDeterministicServiceModes(Set<String> modes) {
        this.deterministicServiceModes.clear();
        this.deterministicServiceModes.addAll(modes);
    }

    @StringGetter(PARAM_CREATE_LINK_EVENTS_INTERVAL)
    public int getCreateLinkEventsInterval() {
        return this.createLinkEventsInterval;
    }

    @StringSetter(PARAM_CREATE_LINK_EVENTS_INTERVAL)
    public void setCreateLinkEventsInterval(int value) {
        this.createLinkEventsInterval = value;
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(PARAM_DETERMINISTIC_SERVICE_MODES, "Leg modes used by the created transit drivers that should be simulated strictly according to the schedule.");
        comments.put(PARAM_CREATE_LINK_EVENTS_INTERVAL, "(iterationNumber % createLinkEventsInterval) == 0 defines in which iterations linkEnter- and linkLeave-events are created,\n" +
                "\t\t\t\t\"useful for visualization or analysis purposes. Defaults to 0. `0' disables the creation of events completely.");
        return comments;
    }
}
