/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 */
public class LegStatsPerZone implements AnalyzerTask<Collection<? extends Person>> {

    private final ZoneCollection zones;

    private final ActivityFacilities facilities;

    private Predicate<Segment> predicate;

    private final FileIOContext ioContext;

    public LegStatsPerZone(ZoneCollection zones, ActivityFacilities facilities, FileIOContext ioContext) {
        this.zones = zones;
        this.facilities = facilities;
        this.ioContext = ioContext;
    }

    public void setPredicate(Predicate<Segment> predicate) {
        this.predicate = predicate;
    }

    @Override
    public void analyze(Collection<? extends Person> object, List<StatsContainer> containers) {
        Map<Person, Zone> personZoneMapping = new HashMap<>();
        for(Person p : object) {
            ActivityFacility home = getHomeLocation(p);
            if(home != null) {
                Zone zone = zones.get(new Coordinate(home.getCoord().getX(), home.getCoord().getY()));
                if(zone != null) {
                    personZoneMapping.put(p, zone);
                }
            }
        }

        ConcurrentAnalyzerTask<Collection<? extends Person>> tasks = new ConcurrentAnalyzerTask<>();
        for(Zone zone : zones.getZones()) {
            Predicate<Segment> pred = predicate;
            if(pred != null) {
                pred = PredicateAndComposite.create(predicate, new ZonePredicate(personZoneMapping, zone));
            } else {
                pred = new ZonePredicate(personZoneMapping, zone);
            }

            NumericAnalyzer analyzer = NumericLegAnalyzer.create(
                    CommonKeys.LEG_GEO_DISTANCE,
                    true,
                    pred,
                    zone.getAttribute(zones.getPrimaryKey()),
                    null);
            tasks.addComponent(analyzer);
        }

        AnalyzerTaskRunner.run(object, tasks, String.format("%s/legStatsPerZone.txt", ioContext.getPath()));
    }

    private ActivityFacility getHomeLocation(Person person) {
        for(Episode e : person.getEpisodes()) {
            for(Segment act : e.getActivities()) {
                if(ActivityTypes.HOME.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
                    String id = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
                    ActivityFacility f = facilities.getFacilities().get(Id.create(id, ActivityFacility.class));
                    return f;
                }
            }
        }

        return null;
    }

    private static class ZonePredicate implements Predicate<Segment> {

        private final Map<Person, Zone> personZoneMapping;

        private final Zone zone;

        public ZonePredicate(Map<Person, Zone> personZoneMapping, Zone zone) {
            this.personZoneMapping = personZoneMapping;
            this.zone = zone;
        }

        @Override
        public boolean test(Segment segment) {
            Zone z = personZoneMapping.get(segment.getEpisode().getPerson());
            return zone.equals(z);
        }
    }
}
