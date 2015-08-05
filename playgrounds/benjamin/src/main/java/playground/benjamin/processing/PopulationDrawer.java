///* *********************************************************************** *
// * project: org.matsim.*
// * PopulationAnalysis.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2009 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.benjamin.processing;
//
//import java.util.ArrayList;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.network.MatsimNetworkReader;
//import org.matsim.core.population.MatsimPopulationReader;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.geometry.transformations.GK4toWGS84;
//
//import playground.benjamin.scenarios.munich.analysis.filter.LocationFilter;
//import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
//import processing.core.PApplet;
//import processing.core.PVector;
//
///**
// * @author benjamin
// *
// */
//public class PopulationDrawer extends PApplet {
//	private static final Logger logger = Logger.getLogger(PopulationDrawer.class);
//
//	LocationFilter locFilter;
//	PersonFilter personFilter;
//	Population population;
//
//	ArrayList<PVector> coords;
//	GK4toWGS84 proj;
//
//	PVector tlCorner;
//	PVector brCorner;
//
//	@Override
//	public void setup(){
//
//		String popFile = "/media/data/2_Workspaces/repos/runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/output_plans.xml.gz";
//		String netFile = "/media/data/2_Workspaces/repos/runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/output_network.xml.gz";
//
//		locFilter = new LocationFilter();
//		personFilter = new PersonFilter();
//		population = loadPopulation(popFile, netFile);
//
//		coords = new ArrayList<PVector>();
//		proj = new GK4toWGS84();
//		
//		for(Person person : population.getPersons().values()){
//			Coord homeActCoordGK4 = locFilter.getHomeActivityCoord(person);
//			Coord homeActCoordWGS84 = proj.transform(homeActCoordGK4);
//			
//			PVector homeActCoordVector = new PVector((float) homeActCoordWGS84.getX(), (float) homeActCoordWGS84.getY());
//			coords.add(homeActCoordVector);
//			updateCorners(homeActCoordVector);
//		}
//
//		size(1000, 740);
//		background(0, 0, 0);
//	}
//
//	@Override
//	public void draw(){
//		noFill();
//		stroke(150,50,50,150);
//		strokeWeight(6);
//		beginShape();
//		for(PVector coord : coords) {
//			PVector scrCoord = geoToScreen(coord);
//			point(scrCoord.x, scrCoord.y);
//		}
//		endShape();
//	}
//
//	// This could probably be done a lot easier...
//	private void updateCorners(PVector homeActCoordVector) {
//		float actX = homeActCoordVector.x;
//		float actY = homeActCoordVector.y;
//		if(brCorner == null){
//			brCorner = new PVector(actX, actY);
//			tlCorner = new PVector(actX, actY);
//		} else{
//			if(actX < brCorner.x){
//				if(actX < tlCorner.x){
//					tlCorner.x = actX;
//				}
//				if(actY >= tlCorner.y){
//					tlCorner.y = actY;
//				}
//				if(actY <= brCorner.y){
//					brCorner.y = actY;
//				}
//			} else if(actX >= brCorner.x){
//				brCorner.x = actX;
//				
//				if(actY <= brCorner.y){
//					brCorner.y = actY;
//				}
//				if(actY >= tlCorner.y){
//					tlCorner.y = actY;
//				}
//			} else {
//				logger.warn("Invalid activity coordinate: " + actX + "; aborting...");
//				throw new RuntimeException();
//			}
//		}
//	}
//	
//	private PVector geoToScreen(PVector coord) {
//		float leftBoarder = (float) (0 + (width * 0.05));
//		float rightBoarder = (float) (width - (width * 0.05));
//		float topBoarder = (float) (0 + (height * 0.05));
//		float bottomBoarder = (float) (height - (height * 0.05));
//		return new PVector(map(coord.x, tlCorner.x, brCorner.x, leftBoarder, rightBoarder),
//                		   map(coord.y, tlCorner.y, brCorner.y, topBoarder, bottomBoarder));
//	}
//
//	private Population loadPopulation(String popFile, String netFile) {
//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(sc).readFile(netFile);
//		new MatsimPopulationReader(sc).readFile(popFile);
//
////		Population freightPop = personFilter.getFreightPopulation(sc.getPopulation());
////		return freightPop;
//		
//		Population urbanPop = personFilter.getMiDPopulation(sc.getPopulation());
//		return urbanPop;
//
////		return sc.getPopulation();
//	}
//
//	public static void main(String[] args) {
//		PApplet.main(new String[] {"--present", "playground.benjamin.processing.PopulationAnalysis"});
//	}
//}
