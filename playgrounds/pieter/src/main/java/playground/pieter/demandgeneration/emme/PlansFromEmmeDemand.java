/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.pieter.demandgeneration.emme;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;






import playground.pieter.balmermi.world.World;
import playground.pieter.balmermi.world.WorldUtils;
import playground.pieter.balmermi.world.Zone;
import playground.pieter.balmermi.world.ZoneLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

class PlansFromEmmeDemand {

	/**
	 * @param args
	 */
	private final Logger log = Logger.getLogger(PlansFromEmmeDemand.class);
    private final CoordinateReferenceSystem outputCRS;
    private final String zoneCoordsFileName;
	private final String departureTime;
	private HashMatrix demand;
	private String outputPath;
	private Collection<SimpleFeature> homeLocationCollection;
	private Collection<SimpleFeature> workLocationCollection;
	private Map<Id,Polygon> zonePolygons;
	private ZoneLayer zones;
	private GeometryFactory geofac;
	private PointFeatureFactory factory;
	
	private Map<String, ZoneXY> zoneXYs;

	private Random random;

	public PlansFromEmmeDemand (
			String matrixFileName, String zoneCoordsFileName,
			String departureTime, String outPath, CoordinateReferenceSystem CRS) throws Exception{
		
		this.outputCRS = CRS;
		this.zoneCoordsFileName = zoneCoordsFileName;
		this.departureTime = departureTime;
		this.demand = new HashMatrix(matrixFileName);
		this.outputPath = outPath;
		this.geofac = new GeometryFactory();
		this.random = new Random();
	}
	
	public void processInput() throws Exception{
		initFeatures();
		readZones();
		createHomeLocations();
		ShapeFileWriter.writeGeometries(this.homeLocationCollection, (this.outputPath + "homeLocations.shp"));
		createWorkLocations();
		ShapeFileWriter.writeGeometries(this.workLocationCollection, (this.outputPath + "workLocations.shp"));
	}
	
	public void createPlansXML() throws IOException {
		BufferedWriter output = IOUtils.getBufferedWriter((this.outputPath+"plans.xml.gz"));
		output.write("<?xml version=\"1.0\" ?>\n");
		output.write("<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">\n");
		output.write("<plans>\n");
		for (SimpleFeature person : this.workLocationCollection){
			int ID = (Integer)person.getAttribute(1);
			double homeX = (Double)person.getAttribute(4);
			double homeY = (Double)person.getAttribute(5);
			double workX = (Double)person.getAttribute(6);
			double workY = (Double)person.getAttribute(7);
			String xmlEntry = String.format("\t<person id = \"%d\">\n",ID);
			xmlEntry += "\t\t<plan>\n";
			String endTime = this.departureTime;
			xmlEntry += String.format("\t\t\t<act type=\"home\" x=\"%f\" y=\"%f\" end_time=\"%s\"/>\n",homeX,homeY,endTime  );
			xmlEntry += "\t\t\t<leg mode=\"car\"/>\n";
			xmlEntry += String.format("\t\t\t<act type=\"work\" x=\"%f\" y=\"%f\" dur=\"09:00:00\"/>\n",workX,workY );
			xmlEntry += "\t\t\t<leg mode=\"car\"/>\n";
			xmlEntry += String.format("\t\t\t<act type=\"home\" x=\"%f\" y=\"%f\"/>\n",homeX,homeY );
			xmlEntry += "\t\t</plan>\n";
			xmlEntry += "\t</person>\n";
			output.write(xmlEntry);
		}
		output.write("</plans>");
		output.close();
	}

	///////////////////////////////////////	
	//Public methods for zones
	///////////////////////////////////////


	Map<String, ZoneXY> getZoneXYs() {
		return this.zoneXYs;
	}


	private void initFeatures() {
		//Define the point collection with its attributes
		this.factory = new PointFeatureFactory.Builder().
				setCrs(this.outputCRS).
				setName("person").
				addAttribute("ID", Integer.class).
				addAttribute("homeZone", Integer.class).
				addAttribute("workZone", Integer.class).
				addAttribute("homeX", Double.class).
				addAttribute("homeY", Double.class).
				addAttribute("workX", Double.class).
				addAttribute("workY", Double.class).
				create();
	}


	void readZones(){
		World w = new World();
		this.zones = (ZoneLayer) w.createLayer(Id.create("zones",ZoneLayer.class));
		this.zoneXYs = new HashMap<>();
		BufferedReader zoneReader;
		try {
			zoneReader = IOUtils.getBufferedReader(this.zoneCoordsFileName);
			String zoneLine = "";
			do {
				zoneLine = zoneReader.readLine();
				if (zoneLine != null) {
					String[] zoneLines = zoneLine.split(",");
					this.getZoneXYs().put(zoneLines[0],
							new ZoneXY(Id.create(zoneLines[0],ZoneXY.class), zoneLines[1], zoneLines[2]));
				}
			} while (zoneLine != null);
			zoneReader.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		/*
		 * Now create the zones.
		 */
		for (ZoneXY zxy : this.zoneXYs.values()) {
			this.zones.createZone(zxy.getZoneId(), zxy.getX(), zxy.getY(), null, null, null, null);
		}
		this.zoneXYs.clear();
	}


	private Point getRandomizedCoordInZone(Id zoneId) {
		Coord zoneCoord = WorldUtils.getRandomCoordInZone(
				(Zone) this.zones.getLocation(zoneId), this.zones);
		return this.geofac.createPoint(new Coordinate(zoneCoord.getX(),zoneCoord.getY()));
	}

	private void createHomeLocations() throws Exception {
		log.info("Creating home locations:");
		this.homeLocationCollection = new ArrayList<>();
		
		/*
		 * Get the HashMatrix's set of headers, generate the row total number of people.
		 */
		int persId = 0;
		int personMultiplier = 1;
        for (Integer zoneNumber : this.demand.getHeaderSet()) {
            /*
			 * Read the SP_CODE as string, same as HashMap keys.
			 */
            int PERSON_SCALER = 10;
            long numberOfPeopleInZone = Math.round(this.demand.getRowTotal(zoneNumber)) / PERSON_SCALER;
            for (long i = 0; i < numberOfPeopleInZone; i++) {
                Point point = getRandomizedCoordInZone(Id.create(zoneNumber, Point.class));
                Object[] fta = {persId++, zoneNumber, 9999,
                        point.getCoordinate().x, point.getCoordinate().y, 0, 0};
                SimpleFeature ft = this.factory.createPoint(point.getCoordinate(), fta, null);
                this.homeLocationCollection.add(ft);
                // Report progress.
                if (persId == personMultiplier) {
                    log.info("   home locations created: " + persId);
                    personMultiplier *= 2;
                }
            }
        }
		log.info("   home locations created: " + persId + " (Done)");
	}

	private void createWorkLocations() throws ArrayIndexOutOfBoundsException{
		//creates a work location for each person in personCollection
		System.out.println("Creating work locations:");
		this.workLocationCollection = new ArrayList<>();
		int persId = 0;
		int personMultiplier = 1;
		for(SimpleFeature person : this.homeLocationCollection) {
			int homeTAZ = (Integer)person.getAttribute(2);
			int workTAZ = getWorkTAZ(homeTAZ);
			Point workPoint = getRandomizedCoordInZone(Id.create(workTAZ,Point.class));
			//update home locations with new info
			person.setAttribute(3, workTAZ);
			person.setAttribute(6, workPoint.getCoordinate().x);
			person.setAttribute(7, workPoint.getCoordinate().y);
			//create work location point
			Object [] workFeature = {person.getAttribute(1),
					homeTAZ, workTAZ,
					person.getAttribute(4), person.getAttribute(5),
					workPoint.getCoordinate().x, workPoint.getCoordinate().y};
			persId = (Integer)person.getAttribute(1);
			// Report progress.
			if(persId == personMultiplier){
				log.info("   work locations created: " + persId);
				personMultiplier *= 2;	
			}
			SimpleFeature personFeature = this.factory.createPoint(workPoint.getCoordinate(), workFeature, null);
			this.workLocationCollection.add(personFeature);
		}
		log.info("   work locations created: " + persId + " (Done)");
	}

	private int getWorkTAZ(int homeTAZ) {
		//generate random number, find the corresponding interval in the cumulative OD matrix
		double randomNumber = this.random.nextDouble();
		Set<Entry<Integer,Double>> rowProbs = this.demand.getRowProbabilitySet(homeTAZ);
		double threshold =0.0;
		Iterator<Entry<Integer,Double>> rowEntries = rowProbs.iterator();
		while(randomNumber > threshold){
			Entry<Integer,Double> currEntry = rowEntries.next();
			threshold += currEntry.getValue();
			if(randomNumber <= threshold){
				return currEntry.getKey();
			}
		}
		return 0;
	}
}
