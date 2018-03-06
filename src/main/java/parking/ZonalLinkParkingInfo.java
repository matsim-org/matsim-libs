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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.google.inject.Inject;
import com.vividsolutions.jts.geom.Geometry;

import parking.capacityCalculation.LinkParkingCapacityCalculator;

/**
 * Created by amit on 09.02.18.
 */

public class ZonalLinkParkingInfo {

    private final Network network;
    private final Map<Id<ParkingZone>, ParkingZone> parkingZones = new HashMap<>();

    private final boolean useCarLinksOnly = true;

    private final LinkParkingCapacityCalculator linkParkingCapacityCalculator;

    private final QSimConfigGroup qSimConfigGroup;

    @Inject
    public ZonalLinkParkingInfo(Config config,Network network, LinkParkingCapacityCalculator linkParkingCapacityCalculator) {
        this.network = network;
        this.linkParkingCapacityCalculator = linkParkingCapacityCalculator;
        this.qSimConfigGroup = config.qsim();
        ParkingRouterConfigGroup prc = ParkingRouterConfigGroup.get(config);
        init(prc.getShapeFileUrl(config.getContext()).getFile(), prc.getShapeKey());
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

   
}
