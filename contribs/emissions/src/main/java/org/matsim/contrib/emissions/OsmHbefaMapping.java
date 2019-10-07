package org.matsim.contrib.emissions;

import com.google.inject.Provides;
import org.matsim.api.core.v01.network.Link;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by molloyj on 01.12.2017.
 *
 *
 * handled OSM road types:
 *    motorway,trunk,primary,secondary, tertiary, unclassified,residential,service
 *    motorway_link, trunk_link,primary_link, secondary_link
 *    tertiary_link, living_street, pedestrian,track,road
 *
 * Hbefa categories and respective speeds
 *    URB/MW-Nat./80 - 130
 *    URB/MW-City/60 - 110
 *    URB/Trunk-Nat./70 - 110
 *    URB/Trunk-City/50 - 90
 *    URB/Distr/50 - 80
 *    URB/Local/50 - 60
 *    URB/Access/30 - 50
 *
 * Conversions from OSM to hbefa types
 *    motorway;MW
 *    primary;Trunk
 *    secondary;Distr
 *    tertiary;Local
 *    residential;Access
 *    living;Access

 */
class OsmHbefaMapping extends HbefaRoadTypeMapping {
    private static final int MAX_SPEED = 130;
    private static final String OSM_HIGHWAY_TAG = "osm:way:highway";
    private Map<String, Hbefa> hbfeaMap = new HashMap<>();

    public static class Hbefa {
        String name;
        int min;
        int max;

        Hbefa(String name, int min, int max) {
            this.name = name;
            this.min = min;
            this.max = max;
        }
    }

    @Provides
    public static OsmHbefaMapping build() {
        OsmHbefaMapping mapping = new OsmHbefaMapping();
        mapping.put("motorway-Nat.", new Hbefa("MW-Nat.",80,130));
        mapping.put("motorway", new Hbefa("MW-City",60,90));
        mapping.put("primary-Nat.", new Hbefa("Trunk-Nat.",80,110));
        mapping.put("primary", new Hbefa("Trunk-City",50,80));
        mapping.put("trunk", new Hbefa("Trunk-City",50,80));
        mapping.put("secondary", new Hbefa("Distr",50,80));
        mapping.put("tertiary", new Hbefa("Local",50,60));
        mapping.put("residential", new Hbefa("Access",30,50));
        mapping.put("service", new Hbefa("Access",30,50));
        mapping.put("living", new Hbefa("Access",30,50));

        return mapping;
    }

    private void put(String s, Hbefa hbefa) {
        hbfeaMap.put(s, hbefa);
    }

    @Override
    public String determineHebfaType(Link link) {
        String roadType = (String) link.getAttributes().getAttribute(OSM_HIGHWAY_TAG);
        String hbefaType = null;
        if (roadType != null) {
            hbefaType = getHEBFAtype(roadType,link.getFreespeed());
        }
        return hbefaType;
    }

    private String getHEBFAtype(String roadType, double freeVelocity) {
        String[] ss = roadType.split("_"); //want to remove
        String type = ss[0];

        //TODO: could make distinction between national and city, based on shapefile, or regions.
        double freeVelocity_kmh = freeVelocity * 3.6;

        if (type.equals("unclassified") || type.equals("road")) {
            if (freeVelocity_kmh <= 50) type = "living";
            else if (freeVelocity_kmh == 60) type = "tertiary";
            else if (freeVelocity_kmh == 70) type = "secondary";
            else if (freeVelocity_kmh <= 90) type = "primary";
            else type = "motorway";
        }

        //specify that if speed > 90 and primary or motorway, then Nat.
        if (type.equals("motorway") || type.equals("primary") && freeVelocity_kmh >= 90) {
            type += "-Nat.";
        }
        if (hbfeaMap.get(type) == null) {
            throw new RuntimeException("'"+ type +"' not in hbefa map");
        }
        int min_speed = hbfeaMap.get(type).min;
        int max_speed = hbfeaMap.get(type).max;
        int capped_speed = (int) Math.min(Math.max(min_speed, freeVelocity_kmh), max_speed);

        return "URB/" + hbfeaMap.get(type).name + "/" + capped_speed;
    }

}
