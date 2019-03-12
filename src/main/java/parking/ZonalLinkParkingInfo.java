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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.google.inject.Inject;

import parking.capacityCalculation.LinkParkingCapacityCalculator;

/**
 * Created by amit on 09.02.18.
 */

public class ZonalLinkParkingInfo {

    private final Network network;
    private final Population population;
    private final Map<Id<ParkingZone>, ParkingZone> parkingZones = new HashMap<>();

    private final boolean useCarLinksOnly = true;

    private final LinkParkingCapacityCalculator linkParkingCapacityCalculator;

    private final QSimConfigGroup qSimConfigGroup;
    private final Random r = MatsimRandom.getRandom();
    

    @Inject
    public ZonalLinkParkingInfo(Config config,Network network, LinkParkingCapacityCalculator linkParkingCapacityCalculator, Population population) {
        this.network = network;
        this.linkParkingCapacityCalculator = linkParkingCapacityCalculator;
        this.qSimConfigGroup = config.qsim();
        this.population=population;
        ParkingRouterConfigGroup prc = ParkingRouterConfigGroup.get(config);
        init(prc.getShapeFileUrl(config.getContext()).getFile(), prc.getShapeKey());
        initializeInitialParkingOccupancy();

    }
    

	public ZonalLinkParkingInfo(String shapeFile, String shapeKey, double storageCapacityFactor, Network network, LinkParkingCapacityCalculator linkParkingCapacityCalculator, Population population) {
        this.network = network;
        this.linkParkingCapacityCalculator = linkParkingCapacityCalculator;
        this.qSimConfigGroup = new QSimConfigGroup();
        this.population=population;
        qSimConfigGroup.setStorageCapFactor(storageCapacityFactor);
        ParkingRouterConfigGroup prc = null;
        init(shapeFile, shapeKey);
        initializeInitialParkingOccupancy();
    }

    public void initializeInitialParkingOccupancy() {
//        this.parkingZones.values().forEach(parkingZone -> parkingZone.resetParkingCapacities());
		Network net = this.network;
		if (NetworkUtils.isMultimodal(network)) {
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
			net = NetworkUtils.createNetwork();
			HashSet<String> modes = new HashSet<String>();
			modes.add(TransportMode.car);
			filter.filter(net, modes);
		}
		for (Person p : population.getPersons().values()) {
			MutableBoolean usesCar = new MutableBoolean(false);
			p.getSelectedPlan().getPlanElements().stream().filter(Leg.class::isInstance).forEach(l->{
				Leg leg = (Leg) l;
				if (leg.getMode().equals(TransportMode.car)) {
					usesCar.setTrue();
				}
			});
			if(usesCar.isTrue()) {
				Coord c = ((Activity)p.getSelectedPlan().getPlanElements().get(0)).getCoord();
				Link l = NetworkUtils.getNearestLink(net, c);
				ParkingZone z = getParkingZone(l);
				if (z!=null) {
					z.updateLinkParkingCapacity(l, -1);

                }
			}
		}
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
            String key = String.valueOf(feature.getAttribute(featureKey));
            Id<ParkingZone> parkingZoneId = Id.create(key, ParkingZone.class);
            double garageProbability = Double.parseDouble(String.valueOf(feature.getAttribute("pripark_sh")));
            this.parkingZones.getOrDefault(parkingZoneId,new ParkingZone(key)).setGarageProbability(garageProbability);;
            
        }
    }

    public ParkingZone getParkingZone(Link link) {
        Optional<Map.Entry<Id<ParkingZone>, ParkingZone>> first
                = this.parkingZones.entrySet().stream().filter(e -> e.getValue().isLinkInsideZone(link)).findFirst();
        if (first.isPresent()) return first.get().getValue();
        else return null;
    }
    
    public Map<Id<ParkingZone>, ParkingZone> getParkingZones() {
		return parkingZones;
	}

   
}
