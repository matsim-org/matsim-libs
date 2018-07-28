package org.matsim.contrib.emissions.types;

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
public class OsmHbefaMapping implements HbefaRoadTypeMapping {
    private static final int MAX_SPEED = 130;
    Map<String, Hbefa> hbfeaMap = new HashMap<>();

    public static class Hbefa {
        String name;
        int min;
        int max;

        public Hbefa(String name, int min, int max) {
            this.name = name;
            this.min = min;
            this.max = max;
        }
    }


    public static OsmHbefaMapping build() {
        OsmHbefaMapping mapping = new OsmHbefaMapping();
        mapping.hbfeaMap.put("motorway-Nat.", new Hbefa("MW-Nat.",80,130));
        mapping.hbfeaMap.put("motorway", new Hbefa("MW-City",60,90));
        mapping.hbfeaMap.put("primary-Nat.", new Hbefa("Trunk-Nat.",80,110));
        mapping.hbfeaMap.put("primary", new Hbefa("Trunk-City",50,80));
        mapping.hbfeaMap.put("secondary", new Hbefa("Distr",50,80));
        mapping.hbfeaMap.put("tertiary", new Hbefa("Local",50,60));
        mapping.hbfeaMap.put("residential", new Hbefa("Access",30,50));
        mapping.hbfeaMap.put("service", new Hbefa("Access",30,50));
        mapping.hbfeaMap.put("living", new Hbefa("Access",30,50));

        return mapping;
    }

    @Override
    public String get(String roadType, double freeVelocity) {
        
        return getHEBFAtype(roadType,freeVelocity);


    }

    private int getSpeedCat(double speed) {

            int speedCat = Double.isInfinite(speed) ? MAX_SPEED : (int) Math.round(speed * 3.6);
            speedCat = ((speedCat + 5) / 10) * 10;
            speedCat = Math.min(MAX_SPEED, speedCat);
            return speedCat;

    }

    private String getHEBFAtype(String roadType, double freeVelocity) {


        String[] ss = roadType.split("_"); //want to remove
        String type = ss[0];

        //TODO: could make distinction between national and city, based on shapefile, or regions.

        if (type.equals("unclassified") || type.equals("road")) {
            if (freeVelocity <= 50) type = "living";
            else if (freeVelocity == 60) type = "tertiary";
            else if (freeVelocity == 70) type = "secondary";
            else if (freeVelocity <= 90) type = "primary";
            else type = "motorway";
        }

        //specify that if speed > 90 and primary or motorway, then Nat.
        if (type.equals("motorway") || type.equals("primary") && freeVelocity >= 90) {
            type += "-Nat.";
        }
        if (hbfeaMap.get(type) == null) {
            throw new RuntimeException("'"+ type +"' not in hbefa map");
        }
        int min_speed = hbfeaMap.get(type).min;
        int max_speed = hbfeaMap.get(type).max;
        int capped_speed = (int) Math.min(Math.max(min_speed, freeVelocity), max_speed);

        return "URB/" + hbfeaMap.get(type).name + "/" + capped_speed;


    }


}
