/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.analysis;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.studies.matrix2014.data.PersonAttributeUtils;
import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.util.Collection;
import java.util.List;

/**
 * @author jillenberger
 */
public class ValidateLAU2Attribute implements AnalyzerTask<Collection<? extends Person>> {

    private static final Logger logger = Logger.getLogger(ValidateLAU2Attribute.class);

    private final ZoneCollection zones;

    private final ActivityFacilities facilities;

    public ValidateLAU2Attribute(DataPool dataPool) {
        zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");
        facilities = ((FacilityData) dataPool.get(FacilityDataLoader.KEY)).getAll();
    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        int cnt = 0;
        int fail = 0;

        for(Person p : persons) {
            ActivityFacility f = PersonAttributeUtils.getHomeFacility(p, facilities);
            if(f != null) {
                Zone z = zones.get(new Coordinate(f.getCoord().getX(), f.getCoord().getY()));
                if(z != null) {
                   if(!p.getAttribute(MiDKeys.PERSON_LAU2_CLASS).equalsIgnoreCase(z.getAttribute(MiDKeys.PERSON_LAU2_CLASS))) {
                       cnt++;
                   }
                } else {
                    fail++;
                }
            } else {
                fail++;
            }
        }

        if(cnt > 0) logger.warn(String.format("%s persons located in wrong LAU2 zone", cnt));
        if(fail > 0) logger.warn(String.format("Failed to obtain home zone for %s persons", fail));
    }
}
