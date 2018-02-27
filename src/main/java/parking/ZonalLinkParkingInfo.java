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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.SortedMap;
import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Created by amit on 09.02.18.
 */

public class ZonalLinkParkingInfo {

    private final Network network;
    private final Map<Id<ParkingZone>, ParkingZone> parkingZones = new HashMap<>();

    private final boolean useCarLinksOnly = true;

    private static final double PARKING_SPACE_PER_CAR = 7.5;

    private final LinkParkingCapacityCalculator linkParkingCapacityCalculator;

    private final QSimConfigGroup qSimConfigGroup;

    public ZonalLinkParkingInfo(String shapeFile, String featureKey, Network network, LinkParkingCapacityCalculator linkParkingCapacityCalculator, QSimConfigGroup qSimConfigGroup) {
        this.network = network;
        this.linkParkingCapacityCalculator = linkParkingCapacityCalculator;
        this.qSimConfigGroup = qSimConfigGroup;
        init(shapeFile, featureKey);
    }

    public ZonalLinkParkingInfo(String shapeFile, String featureKey, Network network, QSimConfigGroup qSimConfigGroup) {
        this(shapeFile, featureKey, network, new LinkParkingCapacityCalculator() {
            @Override
            public double getLinkCapacity(Link link) {
                return link.getLength() / PARKING_SPACE_PER_CAR ;
            }
        }, qSimConfigGroup);
    }

    private void init(String shapeFile, String featureKey) {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);

        for (SimpleFeature feature : features) {
            for (Link l : this.network.getLinks().values()) {
                if (useCarLinksOnly && l.getAllowedModes().contains(TransportMode.car)) {
                    if (((Geometry) feature.getDefaultGeometry()).contains(MGC.coord2Point(l.getCoord()))) {

                        String key = String.valueOf(feature.getAttribute(featureKey));
                        Id<ParkingZone> parkingZoneId = Id.create(key, ParkingZone.class);
                        ParkingZone parkingZone = this.parkingZones.getOrDefault(parkingZoneId, new ParkingZone(key));
                        parkingZone.updateLinkParkingCapacity(l, this.linkParkingCapacityCalculator.getLinkCapacity(l) * qSimConfigGroup.getStorageCapFactor() );
                        this.parkingZones.put(parkingZoneId, parkingZone);

                    }
                } else {
                    //exclude links such as -> bike, pt
                }
            }
        }
    }

    public ParkingZone getParkingZone(Link link) {
        Optional<Map.Entry<Id<ParkingZone>, ParkingZone>> first
                = this.parkingZones.entrySet().stream().filter(e -> e.getValue().isLinkInsideZone(link)).findFirst();
        if (first.isPresent()) return first.get().getValue();
        else return null;
    }

    // an example:
    public static void main(String[] args) {
        String shapeFile = "../../repos/shared-svn/projects/vw_rufbus/projekt2/parking/shp/parking-zones.shp";
        String networkFile = "../../repos/shared-svn/projects/vw_rufbus/projekt2/parking/example_scenario/vw202.0.01/vw202.0.01.output_network.xml.gz";
        String configFile = "../../repos/shared-svn/projects/vw_rufbus/projekt2/parking/example_scenario/vw202.0.01/vw202.0.01.output_config.xml";

        Config config = ConfigUtils.loadConfig(configFile);
        config.plans().setInputFile(null);
        config.transit().setUseTransit(false);
        config.transit().setVehiclesFile(null);
        config.transit().setTransitScheduleFile(null);
        config.vehicles().setVehiclesFile(null);
        config.network().setInputFile(networkFile);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();


        Link link = network.getLinks().get(Id.createLinkId(36804));
        ParkingZone parkingZone = new ZonalLinkParkingInfo(shapeFile, "NO", network, scenario.getConfig().qsim() ).getParkingZone(link);
        if (parkingZone != null) {
            SortedMap<Id<Link>,Double> parkingProbs = parkingZone.getLinkParkingProbabilities();
            parkingProbs.entrySet()
                        .forEach(e -> System.out.println("Parking probability for link " + e.getKey() + " in parking zone id " + parkingZone
                                .getId() + " is " + e.getValue()));
        }
        //
        double randNr = new Random().nextDouble(); //MatsimRandom.getRandom().nextDouble();

        double cumSum =0.;
        for(Map.Entry<Id<Link>,Double> prob : parkingZone.getLinkParkingProbabilities().entrySet()) {
            cumSum += prob.getValue();
            if (randNr <= cumSum) {
                System.out.println("\n\nFor the given probability of "+randNr+", the chosen parking link is "+prob.getKey());
                break;
            }
            System.out.println("Cumulative sum of the probabilities: "+cumSum);
        }
    }
}
