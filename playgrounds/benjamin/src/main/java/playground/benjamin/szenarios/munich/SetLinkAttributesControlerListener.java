/* *********************************************************************** *
 * project: org.matsim.*
 * SetLinkAttributesControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.szenarios.munich;

import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.LinkImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author benjamin
 *
 */
public class SetLinkAttributesControlerListener implements StartupListener {

	private Set<Feature> featuresInZone30;

	public SetLinkAttributesControlerListener(Set<Feature> featuresInZone30) {
		this.featuresInZone30 = featuresInZone30;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		Network network = event.getControler().getNetwork();
		for(Link link : network.getLinks().values()){
			Id linkId = link.getId();
			LinkImpl ll = (LinkImpl) network.getLinks().get(linkId);
			if(isLinkInShape(ll)){
				System.out.println("Changing freespeed of link " + ll.getId() + " from " + ll.getFreespeed() + " to 8.3333333334.");
				ll.setFreespeed(30 / 3.6);
				if(ll.getNumberOfLanes() == 1){
					System.out.println("Changing type of link " + ll.getId() + " from " + ll.getType() + " to 75.");
					ll.setType("75");
					System.out.println("Changing capacity of link " + ll.getId() + " from " + ll.getCapacity() + " to 11200.");
					ll.setCapacity(11200);
				}
				else{
					System.out.println("Changing type of link " + ll.getId() + " from " + ll.getType() + " to 83.");
					ll.setType("83");
					System.out.println("Changing capacity of link " + ll.getId() + " from " + ll.getCapacity() + " to 20000.");
					ll.setCapacity(20000);
				}
			}
		}
	}

	private boolean isLinkInShape(Link link) {
		boolean isInShape = false;
		Coord coord = link.getCoord();
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
		for(Feature feature : this.featuresInZone30){
			if(feature.getDefaultGeometry().contains(geo)){
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}
}
