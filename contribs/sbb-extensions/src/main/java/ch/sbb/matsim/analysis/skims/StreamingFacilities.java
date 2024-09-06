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
package ch.sbb.matsim.analysis.skims;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.FailingObjectAttributes;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * A special facilities container that does not store facilities, but passes them on to a consumer to process otherwise.
 *
 * @author mrieser / SBB
 */
public class StreamingFacilities implements ActivityFacilities {

    private final Consumer<ActivityFacility> consumer;
    private final ActivityFacilitiesFactory factory = new ActivityFacilitiesFactoryImpl();

    public StreamingFacilities(Consumer<ActivityFacility> consumer) {
        this.consumer = consumer;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public ActivityFacilitiesFactory getFactory() {
        return this.factory;
    }

    @Override
    public Map<Id<ActivityFacility>, ? extends ActivityFacility> getFacilities() {
        return null;
    }

    @Override
    public void addActivityFacility(ActivityFacility facility) {
        this.consumer.accept(facility);
    }

    @Override
    @Deprecated
    public FailingObjectAttributes getFacilityAttributes() {
        return null;
    }

    @Override
    public TreeMap<Id<ActivityFacility>, ActivityFacility> getFacilitiesForActivityType(String actType) {
        return null;
    }

    @Override
    public Attributes getAttributes() {
        return new AttributesImpl();
    }
}
