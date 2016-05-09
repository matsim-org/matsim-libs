/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

import playground.ikaddoura.incidents.DateTime;
import playground.ikaddoura.incidents.TMCAlerts;
import playground.ikaddoura.incidents.data.NetworkIncident;
import playground.ikaddoura.incidents.data.TrafficItem;

/**
* Writes traffic incidents to a shape file.
*  
* @author ikaddoura
*/

public class Incident2SHPWriter {
	private static final Logger log = Logger.getLogger(Incident2SHPWriter.class);

	TMCAlerts tmc = null;
	Map<String, TrafficItem> trafficItems = null;
	Map<String, Path> trafficItemId2path = null;
	
	public Incident2SHPWriter(TMCAlerts tmc, Map<String, TrafficItem> trafficItems, Map<String, Path> trafficItemId2path) {
		this.tmc = tmc;
		this.trafficItems = trafficItems;
		this.trafficItemId2path = trafficItemId2path;
	}

	public static void writeDailyIncidentLinksToShapeFile(Map<Id<Link>, List<NetworkIncident>> linkId2processedIncidentsCurrentDay, String outputDirectory, double dateInSec, String crs) {
		
		String outputShpFile = outputDirectory + "processedNetworkIncidents_" + DateTime.secToDateString(dateInSec) + ".shp";
		
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
				.setCrs(MGC.getCRS(crs))
				.setName("Link")
				.addAttribute("LinkId", String.class)
				.addAttribute("IncidentId", String.class)
				
				.addAttribute("Capacity", Double.class)
				.addAttribute("Freespeed", Double.class)
				.addAttribute("Lanes", Double.class)
				.addAttribute("Modes", String.class)
				
				.addAttribute("IncCap", Double.class)
				.addAttribute("IncSpeed", Double.class)
				.addAttribute("IncLanes", Double.class)
				.addAttribute("IncModes", String.class)
				
				.addAttribute("IncStart", String.class)
				.addAttribute("IncEnd", String.class)
				.create();
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for (Id<Link> linkId : linkId2processedIncidentsCurrentDay.keySet()) {
			for (NetworkIncident incident : linkId2processedIncidentsCurrentDay.get(linkId)) {

				Link link = incident.getLink();
				Link incidentLink = incident.getIncidentLink();
				
				double endTime = incident.getEndTime();
				if ((int) endTime == 24 * 3600) {
					endTime = endTime - 1.;
					// in order to have the TimeManager QGIS Plugin running...
				}
				
				Object[] incidentObject = new Object[] {
						link.getId().toString(),
						incident.getId(),
						
						// the parameters under normal conditions
						link.getCapacity(),
						link.getFreespeed(),
						link.getNumberOfLanes(),
						link.getAllowedModes(),
	
						// incident specific values
						incidentLink.getCapacity(),
						incidentLink.getFreespeed(),
						incidentLink.getNumberOfLanes(),
						incidentLink.getAllowedModes(),
	
						// start and end time
						DateTime.secToDateString(dateInSec) + " " + Time.writeTime(incident.getStartTime()),
						DateTime.secToDateString(dateInSec) + " " + Time.writeTime(endTime)
				};
				
				SimpleFeature feature = factory.createPolyline(
						new Coordinate[] {
								new Coordinate(MGC.coord2Coordinate(incident.getLink().getFromNode().getCoord())),
								new Coordinate(MGC.coord2Coordinate(incident.getLink().getToNode().getCoord())) }
						, incidentObject
						, null);
				features.add(feature);
			}
		}
		
		if (features.isEmpty()) {
			log.warn("No traffic incidents. Nothing to write into a shape file.");
		} else {
			log.info("Writing out incident shapefile... ");
			ShapeFileWriter.writeGeometries(features, outputShpFile);
			log.info("Writing out incident shapefile... Done.");
		}
	}
	
	public void writeTrafficItemLinksToShapeFile(String outputShpFile, Set<String> itemIdsToPrint, String crs) {
		
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
				.setCrs(MGC.getCRS(crs))
				.setName("Link")
				.addAttribute("LinkId", String.class)
				.addAttribute("IncidentId", String.class)
				.addAttribute("Street", String.class)
				.addAttribute("Alert", String.class)
				.addAttribute("Message", String.class)
				.addAttribute("Length", Double.class)
				.addAttribute("Modes", String.class)
				.addAttribute("Capacity", Double.class)
				.addAttribute("Lanes", Double.class)
				.addAttribute("Freespeed", Double.class)
				.addAttribute("IncModes", String.class)
				.addAttribute("IncCap", Double.class)
				.addAttribute("IncLanes", Double.class)
				.addAttribute("IncSpeed", Double.class)
				.addAttribute("IncStart", String.class)
				.addAttribute("IncEnd", String.class)
				.create();
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for (String id : itemIdsToPrint) {		
			if (trafficItemId2path.get(id) == null) {
				// no path identified
				log.warn("Skipping traffic item " + id + " because there is no path.");
				
			} else {
				for (Link link : trafficItemId2path.get(id).links) {
					if (getIncidentObject(link, trafficItems.get(id)) != null) {
						SimpleFeature feature = factory.createPolyline(
								new Coordinate[] {
										new Coordinate(MGC.coord2Coordinate(link.getFromNode().getCoord())),
										new Coordinate(MGC.coord2Coordinate(link.getToNode().getCoord())) }
								, getIncidentObject(link, trafficItems.get(id))
								, null);
						features.add(feature);
					}
				}
			}
		}
		
		if (features.isEmpty()) {
			log.warn("No traffic incidents. Nothing to write into a shape file.");
		} else {
			log.info("Writing out incident shapefile... ");
			ShapeFileWriter.writeGeometries(features, outputShpFile);
			log.info("Writing out incident shapefile... Done.");
		}
	}
	
	private Object[] getIncidentObject(Link link, TrafficItem trafficItem) {
		
		Link incidentLink = tmc.getTrafficIncidentLink(link, trafficItem);

		if (incidentLink == null) {
			return null;
		} else {
			Object[] incidentObject = createIncidentObject(trafficItem, link, incidentLink);
			return incidentObject;
		}
	}
	
	private Object[] createIncidentObject(TrafficItem trafficItem, Link link, Link incidentLink) {
		
		Object[] incidentObject = new Object[] {
				link.getId(),
				trafficItem.getId(),
				trafficItem.getOrigin().getDescription() + " --> " + trafficItem.getTo().getDescription(),
				trafficItem.getTMCAlert().getPhraseCode(),
				trafficItem.getTMCAlert().getDescription(),
				link.getLength(),
				
				// the parameters under normal conditions
				link.getAllowedModes(),
				link.getCapacity(),
				link.getNumberOfLanes(),
				link.getFreespeed(),
				
				// incident specific values
				incidentLink.getAllowedModes(),
				incidentLink.getCapacity(),
				incidentLink.getNumberOfLanes(),
				incidentLink.getFreespeed(),
					
				// start and end time
				trafficItem.getStartDateTime(),
				trafficItem.getEndDateTime()
		};
		return incidentObject;
	}

	public void writeCongestionInfo2ShapeFile(String outputShpFile, Set<String> itemIdsToPrint, String crs) {
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
				.setCrs(MGC.getCRS(crs))
				.setName("Link")
				.addAttribute("LinkId", String.class)
				.addAttribute("IncidentId", String.class)
				.addAttribute("Street", String.class)
				.addAttribute("Alert", String.class)
				.addAttribute("Message", String.class)
				.addAttribute("Length", Double.class)
				.addAttribute("Modes", String.class)
				.addAttribute("Capacity", Double.class)
				.addAttribute("Lanes", Double.class)
				.addAttribute("Freespeed", Double.class)
				.addAttribute("IncAddTime", Double.class)
				.addAttribute("IncStart", String.class)
				.addAttribute("IncEnd", String.class)
				.create();
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for (String id : itemIdsToPrint) {		
			if (trafficItemId2path.get(id) == null) {
				// no path identified
				log.warn("Skipping traffic item " + id + " because there is no path.");
				
			} else {
				
				double additionalTravelTimePath = tmc.getAdditionalTravelTime(trafficItems.get(id));
				if ((int) additionalTravelTimePath == 0) {
					// no congestion info
				} else {					
					double pathTravelTime = trafficItemId2path.get(id).travelTime;
					
					log.info("Path travel time: " + pathTravelTime);
					log.info("Additional path travel time: " + additionalTravelTimePath);
					
					for (Link link : trafficItemId2path.get(id).links) {
						double freespeedTravelTime = (link.getLength() / link.getFreespeed());
						double additionalTravelTimeThisLink = ( freespeedTravelTime / pathTravelTime ) * additionalTravelTimePath;
						
						log.info("	-> Link: " + link.getId());				
						log.info("		-> freespeed travel time: " + freespeedTravelTime);
						log.info("		-> travel time share: " + freespeedTravelTime / pathTravelTime);
						log.info("		-> additional travel time link: " + additionalTravelTimeThisLink);
						
						SimpleFeature feature = factory.createPolyline(
								new Coordinate[] {
										new Coordinate(MGC.coord2Coordinate(link.getFromNode().getCoord())),
										new Coordinate(MGC.coord2Coordinate(link.getToNode().getCoord())) }
								, createCongestionInfoObject(trafficItems.get(id), link, additionalTravelTimeThisLink)
								, null);
						features.add(feature);
					}
				}
			}
		}
		
		if (features.isEmpty()) {
			log.warn("No traffic incidents. Nothing to write into a shape file.");
		} else {
			log.info("Writing out incident shapefile... ");
			ShapeFileWriter.writeGeometries(features, outputShpFile);
			log.info("Writing out incident shapefile... Done.");
		}
	}
	
	private Object[] createCongestionInfoObject(TrafficItem trafficItem, Link link, double additionalTravelTime) {
		
		Object[] incidentObject = new Object[] {
				link.getId(),
				trafficItem.getId(),
				trafficItem.getOrigin().getDescription() + " --> " + trafficItem.getTo().getDescription(),
				trafficItem.getTMCAlert().getPhraseCode(),
				trafficItem.getTMCAlert().getDescription(),
				link.getLength(),
				
				// the parameters under normal conditions
				link.getAllowedModes(),
				link.getCapacity(),
				link.getNumberOfLanes(),
				link.getFreespeed(),
				
				// incident additional travel time
				additionalTravelTime,
					
				// start and end time
				trafficItem.getStartDateTime(),
				trafficItem.getEndDateTime()
		};
		return incidentObject;
	}
	
}

