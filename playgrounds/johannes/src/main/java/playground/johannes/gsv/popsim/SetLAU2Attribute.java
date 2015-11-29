/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.popsim;

import com.vividsolutions.jts.geom.Coordinate;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.processing.PersonTask;
import playground.johannes.synpop.source.mid2008.MiDKeys;

/**
 * @author johannes
 */
public class SetLAU2Attribute implements PersonTask {

    private final ActivityFacilities facilities;

    private final ZoneCollection zones;

    private int errors;

    public SetLAU2Attribute(DataPool dataPool, String layerName) {
        facilities = ((FacilityData)dataPool.get(FacilityDataLoader.KEY)).getAll();
        zones = ((ZoneData)dataPool.get(ZoneDataLoader.KEY)).getLayer(layerName);
        errors = 0;
    }

    public int getErrors() {
        return errors;
    }

    @Override
    public void apply(Person person) {
        ActivityFacility f = null;
        for(Episode episode : person.getEpisodes()) {
            for(Segment act : episode.getActivities()) {
                if(ActivityTypes.HOME.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
                    String id = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
                    if(id != null) {
                        f = facilities.getFacilities().get(Id.create(id, ActivityFacility.class));
                        if(f != null) break;

                    }
                }
            }

            if(f != null) break;
        }

        if(f != null) {
            Zone zone = zones.get(new Coordinate(f.getCoord().getX(), f.getCoord().getY()));
            if(zone != null) {
                String val = zone.getAttribute(ZoneData.POPULATION_KEY);
                if(val != null) {
                    double inhabs = Double.parseDouble(val);
//                    int lau2class = PersonMunicipalityClassHandler.getCategory((int)inhabs);
                    String lau2Class = ZoneSetLAU2Class.inhabitants2Class(inhabs);
                    person.setAttribute(MiDKeys.PERSON_LAU2_CLASS, lau2Class);
                }
            }
        } else errors++;
    }
}
