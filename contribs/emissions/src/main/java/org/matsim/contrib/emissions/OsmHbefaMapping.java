/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions;

import com.google.inject.Provides;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by molloyj on 01.12.2017.
 * <p>
 *
 * handled OSM road types:
 *    motorway,trunk,primary,secondary, tertiary, unclassified,residential,service
 *    motorway_link, trunk_link,primary_link, secondary_link
 *    tertiary_link, living_street, pedestrian,track,road
 * <p>
 * Hbefa categories and respective speeds
 *    URB/MW-Nat./80 - 130
 *    URB/MW-City/60 - 110
 *    URB/Trunk-Nat./70 - 110
 *    URB/Trunk-City/50 - 90
 *    URB/Distr/50 - 80
 *    URB/Local/50 - 60
 *    URB/Access/30 - 50
 * <p>
 * Conversions from OSM to hbefa types
 *    motorway;MW
 *    primary;Trunk
 *    secondary;Distr
 *    tertiary;Local
 *    residential;Access
 *    living;Access

 */
public class OsmHbefaMapping extends HbefaRoadTypeMapping {

    private final Map<String, Hbefa> hbfeaMap = new HashMap<>();

    static class Hbefa {
        final String name;
        final int min_speed;
        final int max_speed;

        Hbefa(String name, int min_speed, int max) {
            this.name = name;
            this.min_speed = min_speed;
            this.max_speed = max;
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
        mapping.put("cycleway", new Hbefa("Access",30,50));
        mapping.put("path", new Hbefa("Access",30,50));

        return mapping;
    }

    private void put(String s, Hbefa hbefa) {
        hbfeaMap.put(s, hbefa);
    }

    @Override
    public String determineHbefaType(Link link) {
        String roadType = NetworkUtils.getHighwayType(link);
        double allowedSpeed = NetworkUtils.getAllowedSpeed(link);

        return getHEBFAtype(roadType, allowedSpeed);
    }

    private String getHEBFAtype(String type, double speed) {

        //TODO: could make distinction between national and city, based on shapefile, or regions.
        double freeVelocity_kmh = speed * 3.6;

        /*
         * speed attributes are sometimes rounded beforehand.
         * make sure the speed is a multiple of 10, as the HBEFA emission key factors are, and otherwise we get problems later...
         */
		freeVelocity_kmh = Math.round(freeVelocity_kmh/10.0) * 10;

        if (type.equals("unclassified") || type.equals("road")) {
            if (freeVelocity_kmh <= 50) type = "living";
            else if (freeVelocity_kmh == 60) type = "tertiary";
            else if (freeVelocity_kmh == 70) type = "secondary";
            else if (freeVelocity_kmh <= 90) type = "primary";
            else type = "motorway";
        }



        //sometimes link type is smth like 'motorway_link' or 'living_street' or 'primary|railway.tram'. We only care about the first part, here
		int idx = Stream.of(type.indexOf('.'), type.indexOf('|'), type.indexOf('_'), type.indexOf(','))
				.filter(i -> i>= 0) //if chrc is not in String indexOf returns -1
				.min(Integer::compare)
				.orElse(type.length());
        type = type.substring(0, idx);

        //specify that if speed > 90 and primary or motorway, then Nat.
        if (type.equals("motorway") || type.equals("primary") && freeVelocity_kmh >= 90) {
            type += "-Nat.";
        }
        if (!hbfeaMap.containsKey(type)) {
            throw new RuntimeException("'" + type + "' not in hbefa map");
        }
        int min_speed = hbfeaMap.get(type).min_speed;
        int max_speed = hbfeaMap.get(type).max_speed;
        int clamped_speed = (int) Math.min(Math.max(min_speed, freeVelocity_kmh), max_speed);

        return "URB/" + hbfeaMap.get(type).name + "/" + clamped_speed;
    }
}
