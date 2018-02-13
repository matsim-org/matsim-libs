/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package parking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Created by amit on 09.02.18.
 */

public class ZoneToLinks {

    private final Network network;
    private final Map<String, List<Id<Link>>> zoneToLinks = new HashMap<>();

    private final boolean useCarLinksOnly = true;

    public ZoneToLinks(String shapeFile, String featureKey, Network network){
        this.network = network;
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);

        for (SimpleFeature feature : features) {
            for (Link l : this.network.getLinks().values()) {
                if (useCarLinksOnly && l.getAllowedModes().contains(TransportMode.car)) {
                    if (((Geometry) feature.getDefaultGeometry()).contains(MGC.coord2Point(l.getCoord()))) {
                        String key = String.valueOf( feature.getAttribute(featureKey));
                        List<Id<Link>> linkIds = this.zoneToLinks.getOrDefault( key, new ArrayList<>());
                        linkIds.add(l.getId());
                        this.zoneToLinks.put(key, linkIds);
                    }
                } else{
                    //exclude links such as -> bike, pt
                }
            }
        }
    }

    public Map<String, List<Id<Link>>> getZoneToLinks() {
        return zoneToLinks;
    }

    public static void main(String[] args) {
        String shapeFile = "../../repos/shared-svn/projects/vw_rufbus/projekt2/parking/shp/parking-zones.shp";
        String networkFile = "../../repos/shared-svn/projects/vw_rufbus/projekt2/parking/example_scenario/vw202.0.01/vw202.0.01.output_network.xml.gz";

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(networkFile);

        Network network = ScenarioUtils.loadScenario(config).getNetwork();

        new ZoneToLinks(shapeFile, "NO", network).getZoneToLinks();
    }
}
