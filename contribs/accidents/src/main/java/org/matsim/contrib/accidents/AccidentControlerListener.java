/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.computation.AccidentCost30vs50;
import org.matsim.contrib.accidents.computation.AccidentCostBVWPtoFrequencyConverter;
import org.matsim.contrib.accidents.computation.AccidentCostComputationBVWP;
import org.matsim.contrib.accidents.computation.AccidentFrequencyComputation;
import org.matsim.contrib.accidents.data.AccidentAreaType;
import org.matsim.contrib.accidents.data.AccidentComputationApproach;
import org.matsim.contrib.accidents.data.AccidentLinkInfo;
import org.matsim.contrib.accidents.data.LinkAccidentsComputationMethod;
import org.matsim.contrib.accidents.data.ParkingType;
import org.matsim.contrib.accidents.data.Planequal_Planfree_Tunnel;
import org.matsim.contrib.accidents.data.TimeBinInfo;
import org.matsim.contrib.accidents.handlers.AnalysisEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.google.inject.Inject;

/**
* @author ikaddoura, mmayobre
*/

public class AccidentControlerListener implements StartupListener, IterationEndsListener, AfterMobsimListener {
	private static final Logger log = Logger.getLogger(AccidentControlerListener.class);

	@Inject
	private AnalysisEventHandler analzyer;
	
	@Inject
	private Scenario scenario;
	
	@Inject
	private AccidentsContext accidentsContext;
	
	private int warnCounter = 0;
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {		
		AccidentWriter accidentWriter = new AccidentWriter();
		accidentWriter.write(this.scenario, event, this.accidentsContext.getLinkId2info(), analzyer);	
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		// Compute accident probability per link and time bin
		
		log.info("Computing accident probabilites and costs per link and time bin...");
		
		double totalAccidentCostsPerDay = 0.;
		
		final TravelTime travelTime = event.getServices().getLinkTravelTimes();
		final double timeBinSize = this.scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();	
		final double numberOfTimeBinsPerDay = (24 * 3600) / timeBinSize;
		
		for (AccidentLinkInfo linkInfo : this.accidentsContext.getLinkId2info().values()) {
					
			Link link = this.scenario.getNetwork().getLinks().get(linkInfo.getLinkId());			

			for (double endTime = timeBinSize ; endTime <= this.scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {
				
				final double time = (endTime - timeBinSize/2.);
				final int timeBinNr = (int) (time / timeBinSize);
				
				final double actualTravelTime = travelTime.getLinkTravelTime(link, time, null, null);
				final double actualSpeed = link.getLength() / actualTravelTime;
				final AccidentsConfigGroup accidentSettings = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);
				final double demand = accidentSettings.getSampleSize() * analzyer.getDemand(linkInfo.getLinkId(), timeBinNr);
				final double estimatedAADT = demand * numberOfTimeBinsPerDay; 
				
				double frequency = 0.;
				double accidentCosts = 0.;
								
				if (linkInfo.getComputationMethod().toString().equals(LinkAccidentsComputationMethod.BVWP.toString())) {
				
					// BVWP
					
					accidentCosts = AccidentCostComputationBVWP.computeAccidentCosts(demand, link.getLength(), linkInfo.getRoadTypeBVWP());
					frequency = AccidentCostBVWPtoFrequencyConverter.convertAccidentFrequencyDependingOnActualSpeed(accidentCosts, actualSpeed);
				
				} else if (linkInfo.getComputationMethod().toString().equals(LinkAccidentsComputationMethod.DenmarkModel.toString())) {

					// DenmarkModel

					if (accidentSettings.getAccidentsComputationApproach().toString().equals(AccidentComputationApproach.BVWPforAllRoads.toString())) {
						throw new RuntimeException("Ther should be no link where the computation method is set to + '" + LinkAccidentsComputationMethod.DenmarkModel.toString() +"'. Aborting...");
					}
					
//					log.info("Link" + linkInfo.getLinkId() + "info: ({ demand: " + demand + ", speedLimit: " + linkInfo.getSpeedLimit() + ", roadWidth: " + linkInfo.getRoadWidth() + ", numberSideRoads: " + linkInfo.getNumberSideRoads() + ", parkingType: " + linkInfo.getParkingType() + " & areaType: " + linkInfo.getAreaType() +" .})") ;
					
					frequency = (AccidentFrequencyComputation.computeAccidentFrequency(
							estimatedAADT, 
							linkInfo.getSpeedLimit(), 
							linkInfo.getRoadWidth() , 
							linkInfo.getNumberSideRoads() , 
							linkInfo.getParkingType(), 
							linkInfo.getAreaType()
							)
								) * (link.getLength() / 1000.) / (365. * 24. * 3600.) * timeBinSize; // frequency per km per year --> frequency per link per timeBin
					accidentCosts = frequency * AccidentCost30vs50.giveAccidentCostDependingOnActualSpeed(actualSpeed);
				
				} else {
					throw new RuntimeException("Unknown accident computation approach or value not set. Aborting...");
				}
								
				TimeBinInfo timeBinInfo = new TimeBinInfo(timeBinNr);
				timeBinInfo.setAccidentFrequency(frequency);
				timeBinInfo.setAccidentCosts(accidentCosts);
				
				linkInfo.getTimeSpecificInfo().put(timeBinNr, timeBinInfo);
				
				totalAccidentCostsPerDay += accidentCosts;
			}
		}
		log.info("Computing accident probabilities per link and time bin... Done.");
		
		log.info("+++ Total accident costs per day [EUR] (upscaled to full population size): " + totalAccidentCostsPerDay);		
	}

	@Override
	public void notifyStartup(StartupEvent arg0) {
		// Initialize all link-specific information
				
		Map<String, SimpleFeature> landUseFeaturesBB = new HashMap<>();
		Map<String, String> landUseDataBB = new HashMap<>();

		Map<String, SimpleFeature> popDensityFeatures = new HashMap<>();
		Map<String, Double> popDensityData = new HashMap<>();
		
		log.info("Initializing all link-specific information...");
				
		AccidentsConfigGroup accidentSettings = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);
		// LAND-USE based on OSM (Berlin and Brandenburg) for AccidentAreaType in the Denmark Method & for built-up/nonbuilt-up area in the BVWP Method
		
		if (accidentSettings.getLanduseOSMInputShapeFile() == null) {
			log.warn("Landuse shape file is null. Using default values...");
		} else {
			SimpleFeatureSource ftsLandUseBB = ShapeFileReader.readDataFile(accidentSettings.getLanduseOSMInputShapeFile());		
			try (SimpleFeatureIterator itLandUseBB = ftsLandUseBB.getFeatures().features()) {
				while (itLandUseBB.hasNext()) {
					SimpleFeature ftLandUseBB = itLandUseBB.next();
					String osmId = ftLandUseBB.getAttribute("osm_id").toString();
					String fclassName = ftLandUseBB.getAttribute("fclass").toString();
					landUseFeaturesBB.put(osmId, ftLandUseBB);
					landUseDataBB.put(osmId, fclassName);
				}
				itLandUseBB.close();
				DataStore ds = (DataStore) ftsLandUseBB.getDataStore();
				ds.dispose();
				log.info("Reading shp file for built-up/nonbuilt-up area & AreaType... Done.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (accidentSettings.getPlacesOSMInputFile() == null) {
			log.warn("Places shape file is null. Using default values...");
		} else {
			// POPULATION DENSITY based on OSM (Berlin) + statistics
			SimpleFeatureSource ftsPlaces = ShapeFileReader.readDataFile(accidentSettings.getPlacesOSMInputFile());
			try (SimpleFeatureIterator itPlaces = ftsPlaces.getFeatures().features()){
				while (itPlaces.hasNext()){
					SimpleFeature ftPlaces = itPlaces.next();
					String osmId = ftPlaces.getAttribute("osm_id").toString();
					double popDensity = Double.parseDouble(ftPlaces.getAttribute("pop_dens").toString());
					popDensityFeatures.put(osmId, ftPlaces);
					popDensityData.put(osmId, popDensity);
				}
				itPlaces.close();
				DataStore ds = (DataStore) ftsPlaces.getDataStore();
				ds.dispose();
				log.info("Reading shp file for population density... Done.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		int linkCounter = 0;
		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			AccidentLinkInfo info = new AccidentLinkInfo(link.getId());
			
			if (linkCounter % 100 == 0) {
				log.info("Link #" + linkCounter + "  (" + (int) ((double) linkCounter / this.scenario.getNetwork().getLinks().size() * 100) + "%)");
			}
			linkCounter++;			
			
			boolean bvwpForAllRoads = false;
			if (accidentSettings.getAccidentsComputationApproach().toString().equals(AccidentComputationApproach.BVWPforAllRoads.toString())) {
				bvwpForAllRoads = true;
			}
			
			if (bvwpForAllRoads || link.getFreespeed() > 14.) {
				// probably a motorway
				
				info.setComputationMethod(LinkAccidentsComputationMethod.BVWP);
				// BVWP	
				
				ArrayList<Integer> list = new ArrayList<>();
				
				//Plan equal, Plan free or Tunnel?
				list.add(0, 1); // Default: Plan equal
				info.setPlanequal_planfree_tunnel(Planequal_Planfree_Tunnel.Planequal);
				
				String[] planfreeLinkIDs = accidentSettings.getPlanFreeLinksArray();
				for(int j=0; j < planfreeLinkIDs.length; j++){
				    if(planfreeLinkIDs[j].equals(String.valueOf(link.getId()))){
				    	list.set(0, 0); // Change to Plan free
				    	log.info(link.getId() + " Changed to Plan free!");
				    	info.setPlanequal_planfree_tunnel(Planequal_Planfree_Tunnel.Planfree);
				    	break;
				    }
				}
								
				String[] tunnelLinkIDs = accidentSettings.getTunnelLinksArray();
				
				for(int i=0; i < tunnelLinkIDs.length; i++){
					if(tunnelLinkIDs[i].equals(String.valueOf(link.getId()))){
						list.set(0, 2); // Change to Tunnel
						log.info(link.getId() + " Changed to Tunnel!");
						info.setPlanequal_planfree_tunnel(Planequal_Planfree_Tunnel.Tunnel);
						break;
					}
				}
								
				// builtup or not builtup area?
				String osmLandUseFeatureBBId = getOSMLandUseFeatureBBId(link, landUseFeaturesBB);
				
				if (osmLandUseFeatureBBId == null) {
					
//					log.info("Link " + link.getId() + " has coordinates X = " + link.getCoord().getX() + " and Y = " + link.getCoord().getY());
//					log.info("Link: "+link.getId()+" is in/near a "+osmLandUseFeatureBBId+" area.");
					
					log.warn("No area type found for link " + link.getId() + ". Using default value: not built-up area.");
					if (link.getFreespeed() > 16.) {
						//probably an Express-Highway
						list.add(1, 0);
					} else list.add(1, 2); // probably not an Express-Highway
				
				} else {
					String landUseTypeBB = landUseDataBB.get(osmLandUseFeatureBBId);
					info.setLandUseType(landUseTypeBB);
					//log.info("ERSTE PROBE, landUseTypeBB = " + landUseDataBB.get(osmLandUseFeatureBBId));
					//String landUseTypeBB = osmLandUseFeatureId;
					if (landUseTypeBB.matches("commercial|industrial|recreation_ground|residential|retail")) { //built-up area
						if (link.getFreespeed() > 16.) {
							//probably an Express-Highway
							list.add(1, 1);
						} else list.add(1, 3); // probably not an Express-Highway
					} else { //probably not built-up area
						if (link.getFreespeed() > 16.) {
							//probably an Express-Highway
							list.add(1, 0);
						} else list.add(1, 2); // probably not an Express-Highway
					}
				}

				int numberOfLanesBVWP;
				if (link.getNumberOfLanes() > 4){
					numberOfLanesBVWP = 4;
				} else numberOfLanesBVWP = (int) link.getNumberOfLanes();
				list.add(2, numberOfLanesBVWP);
				info.setRoadTypeBVWP(list); 
				
				info.setNumberOfLanes(link.getNumberOfLanes()); // for linkinfo-CSV
				info.setSpeedLimit(link.getFreespeed()); // for linkinfo-CSV
				info.setNumberSideRoads(0); // default --> for linkinfo-CSV
				info.setRoadWidth(link.getNumberOfLanes() * 3.75); // from network --> for linkinfo-CSV
					
			} else {
				// freespeed below 14 m/s --> probably not a motorway				
				
				info.setComputationMethod(LinkAccidentsComputationMethod.DenmarkModel);
				
				info.setNumberSideRoads(0); // default
				info.setParkingType(ParkingType.BaysAtKerb); // Default
				info.setRoadWidth(link.getNumberOfLanes() * 3.75); // from network
				info.setSpeedLimit(link.getFreespeed()); // from network
				
				// Accident Area Type:
				
				CoordinateTransformation ctScenarioCRS2osmCRS = TransformationFactory.getCoordinateTransformation(this.scenario.getConfig().global().getCoordinateSystem(), accidentSettings.getOsmInputFileCRS());
				Coord linkCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getCoord());
				Point p = MGC.xy2Point(linkCoordinateTransformedToOSMCRS.getX(), linkCoordinateTransformedToOSMCRS.getY()); 
				
				String osmLandUseFeatureBBId = getOSMLandUseFeatureBBId(link, landUseFeaturesBB);
				
				if (osmLandUseFeatureBBId == null) {
					log.warn("No area type found for link " + link.getId() + ". Using default value: industrial/residential neighbourhood.");
					info.setAreaType(AccidentAreaType.IndustrialResidentialNeighbourhood);
					
				} else {
					String landUseType = landUseDataBB.get(osmLandUseFeatureBBId);
					
					if (landUseType.equals("retail")) {
						info.setAreaType(AccidentAreaType.Shops);
					} else if (landUseType.matches("commercial|industrial|military|park|recreation_ground")) {
						info.setAreaType(AccidentAreaType.IndustrialResidentialNeighbourhood);
					} else if (landUseType.matches("allotments|cemetery|farm|forest|grass|heath|meadow|nature_reserve|orchard|quarry|scrub|vineyard")){
						info.setAreaType(AccidentAreaType.ScatteredHousing);
					} else if (landUseType.equals("residential")) {
						String osmPopulationDensityFeatureId = "";
						for (SimpleFeature feature : popDensityFeatures.values()) {
							if (((Geometry) feature.getDefaultGeometry()).contains(p)) {
								osmPopulationDensityFeatureId = feature.getAttribute("osm_id").toString();
								break;
							}
						}
						double populationDensity = 0. ;
						if (osmPopulationDensityFeatureId == "") {
							
							if (warnCounter <= 5) {
								log.warn("No attribute 'pop_dens' found for link " + link.getId() + ". This link is located in Brandenburg and not in Berlin, setting 'AccidentAreaType' as 'ScatteredHousing'.");
								warnCounter++;
								if (warnCounter == 5) {
									log.warn("Furhter warnings of this type will not be printed out.");
								}
							}
							info.setAreaType(AccidentAreaType.ScatteredHousing);
						} else { 
							populationDensity = popDensityData.get(osmPopulationDensityFeatureId);
							if (populationDensity <= 2000. ){
								info.setAreaType(AccidentAreaType.ScatteredHousing);
							} else {
								info.setAreaType(AccidentAreaType.IndustrialResidentialNeighbourhood);
							}
						}
					} 
				} 
			}		
			this.accidentsContext.getLinkId2info().put(link.getId(), info);
		}
		log.info("Initializing all link-specific information... Done.");

	}

	private String getOSMLandUseFeatureBBId(Link link, Map<String, SimpleFeature> landUseFeaturesBB) {
		
		if (landUseFeaturesBB == null || landUseFeaturesBB.isEmpty()) return null;
		
		AccidentsConfigGroup accidentSettings = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);
		CoordinateTransformation ctScenarioCRS2osmCRS = TransformationFactory.getCoordinateTransformation(this.scenario.getConfig().global().getCoordinateSystem(), accidentSettings.getOsmInputFileCRS());
		
		Coord linkCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getCoord()); // this Method gives the middle point of the link back
		Point pMiddle = MGC.xy2Point(linkCoordinateTransformedToOSMCRS.getX(), linkCoordinateTransformedToOSMCRS.getY());
		
		Coord linkStartCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getFromNode().getCoord());
		Point pStart = MGC.xy2Point(linkStartCoordinateTransformedToOSMCRS.getX(), linkStartCoordinateTransformedToOSMCRS.getY());
		
		Coord linkEndCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getToNode().getCoord());
		Point pEnd = MGC.xy2Point(linkEndCoordinateTransformedToOSMCRS.getX(), linkEndCoordinateTransformedToOSMCRS.getY());
		
		String osmLandUseFeatureBBId = null;
		
		for (SimpleFeature feature : landUseFeaturesBB.values()) {
			if (((Geometry) feature.getDefaultGeometry()).contains(pMiddle)) {
				return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
			}
		}
		
		for (SimpleFeature feature : landUseFeaturesBB.values()) {
			if (((Geometry) feature.getDefaultGeometry()).contains(pStart)) {
				return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
			}
		}
		
		for (SimpleFeature feature : landUseFeaturesBB.values()) {
			if (((Geometry) feature.getDefaultGeometry()).contains(pEnd)) {
				return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
			}
		}
		
		// look around the link
		
		GeometryFactory geoFac = new GeometryFactory();
		CoordinateTransformation cTosmCRSToGK4 = TransformationFactory.getCoordinateTransformation(accidentSettings.getOsmInputFileCRS(), "EPSG:31468");
		
		double distance = 10.0;					
//		log.info("Link ID: " + link.getId());
		
		while (osmLandUseFeatureBBId == null && distance <= 500) {
			Coord coordGK4 = cTosmCRSToGK4.transform(MGC.coordinate2Coord(pMiddle.getCoordinate()));
			Point pGK4 = geoFac.createPoint(MGC.coord2Coordinate(coordGK4));
			
			Point pRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY()));
			Point pRight = transformPointFromGK4ToOSMCRS(pRightGK4);
			
			Point pDownGK4 = geoFac.createPoint(new Coordinate(pGK4.getX(), pGK4.getY() - distance));
			Point pDown = transformPointFromGK4ToOSMCRS(pDownGK4);
			
			Point pLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY()));
			Point pLeft = transformPointFromGK4ToOSMCRS(pLeftGK4);
			
			Point pUpGK4 = geoFac.createPoint(new Coordinate(pGK4.getX(), pGK4.getY() + distance));
			Point pUp = transformPointFromGK4ToOSMCRS(pUpGK4);
			
			Point pUpRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY() + distance));
			Point pUpRight = transformPointFromGK4ToOSMCRS(pUpRightGK4);
			
			Point pDownRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY() - distance));
			Point pDownRight = transformPointFromGK4ToOSMCRS(pDownRightGK4);
			
			Point pDownLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY() - distance));
			Point pDownLeft = transformPointFromGK4ToOSMCRS(pDownLeftGK4);
			
			Point pUpLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY() + distance));
			Point pUpLeft = transformPointFromGK4ToOSMCRS(pUpLeftGK4);
										
			for (SimpleFeature feature : landUseFeaturesBB.values()) {
				
				if (((Geometry) feature.getDefaultGeometry()).contains(pRight)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pDown)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pLeft)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pUp)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pUpRight)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pDownRight)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pDownLeft)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pUpLeft)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				}
			}
			
			distance += 10.0;
		}
		
		log.warn("No area type found. Returning null...");
		return null;
	}

	private Point transformPointFromGK4ToOSMCRS(Point pointGK4) {
		AccidentsConfigGroup accidentSettings = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);
		CoordinateTransformation ctGK4toOSMCRS = TransformationFactory.getCoordinateTransformation("EPSG:31468", accidentSettings.getOsmInputFileCRS());
		
		Coord coordGK4 = MGC.coordinate2Coord(pointGK4.getCoordinate());
		Point pointOSMCRS = new GeometryFactory().createPoint(MGC.coord2Coordinate(ctGK4toOSMCRS.transform(coordGK4)));
		return pointOSMCRS;
	}
}