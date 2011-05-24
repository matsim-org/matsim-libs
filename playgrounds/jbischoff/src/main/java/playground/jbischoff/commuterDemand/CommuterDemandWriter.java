/* *********************************************************************** *
 * project: org.matsim.*
 * CommuterDemandWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.jbischoff.commuterDemand;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;


import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
/**
 * @author jbischoff
 *
 */
public class CommuterDemandWriter {
	private static final Logger log = Logger.getLogger(CommuterDemandWriter.class);
	private Map<String,Geometry> municipalityMap;
	private List<CommuterDataElement> demand;
	private Scenario scenario;
	private Population population;
	private double scalefactor;
	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]", TransformationFactory.WGS84_UTM33N);
	//adjust Coordinate System to your needs
	private double offset;
	private double start;
	private double duration;

	
	public  CommuterDemandWriter(Map<String,Geometry> municipalityMap,List<CommuterDataElement> demand){
			this.demand=demand;
			this.municipalityMap=municipalityMap;
			this.scalefactor = 1.0;
			this.offset = 4;
			this.start = 6;
			this.duration = 8.5;
	}
	
	public void writeDemand(String filename){
		log.error("Watch out! This version of the demand generator turns the whole BA data into car commuters which isn't exactly accurate. Adjusting the CommuterDemandWriter --> Scalefactor to your needs might make sense.");
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		generatePopulation();
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(filename);
	}
	private void generatePopulation(){
		int pnr = 0;
		
		for (CommuterDataElement commuterDataElement : demand){
			int car = 0;
			int pt = 0;
			for (int i = 0; i<commuterDataElement.getCommuters() * scalefactor; i++){
				
				if (i <0.67*commuterDataElement.getCommuters() * scalefactor) {generatePlanForZones(pnr,commuterDataElement.getFromId(),commuterDataElement.getToId(),"car");
				car++;
				}
				else {generatePlanForZones(pnr,commuterDataElement.getFromId(),commuterDataElement.getToId(),"pt");
				pt++;
				}pnr++;
			}
			
		log.info("Created "+ car+ "car and "+pt+ " pt commuters from "+commuterDataElement.getFromName()+" ("+commuterDataElement.getFromId()+") to "+commuterDataElement.getToName()+" ("+commuterDataElement.getToId()+")");
		}
		log.info("Created "+pnr+" commuters in total." );
	
	}
		
	
	
	
	private void generatePlanForZones(int pnr, String home, String work, String mode ){
		double poffset = this.offset * 3600 * Math.random();
		double pstart = this.start * 3600 + poffset;
		double pend = (this.start+this.duration)*3600+poffset;
		
		Person p;
		p = generatePerson(home, work, pstart, pend, pnr, mode);
		population.addPerson(p);
		
		
	}
	
	private Person generatePerson(String home, String work, double workStart, double workEnd, int pnr, String mode){
		Person p;
		Id id;
		Plan plan;
		id = scenario.createId(pnr+"_"+home.toString()+"_"+work.toString());
		p = population.getFactory().createPerson(id);
		plan = generatePlan(home,work,workStart,workEnd,mode);
		p.addPlan(plan);
		return p;
	}
	
	
	private Plan generatePlan(String home, String work, double workStart, double workEnd, String mode){
		Plan plan= population.getFactory().createPlan(); 
		
		Coord homeCoord = this.shoot(home); 
		Coord workCoord = this.shoot(work);
	

		plan.addActivity(this.addActivity( "home", 0.0, workStart-1.0, homeCoord));
		plan.addLeg(this.addLeg(workStart -1, mode)); 
		plan.addActivity(this.addActivity( "work",	workStart, workEnd, workCoord)); 
		plan.addLeg(this.addLeg(workEnd, mode));
		plan.addActivity(this.addActivity( "home", workEnd + 1.0, 24.0 * 3600, homeCoord));
		return plan;
		
}
		private Activity addActivity(String type, Double start, Double end, Coord coord){
			
			Activity activity = population.getFactory().createActivityFromCoord(type, coord);
			activity.setStartTime(start);
			activity.setEndTime(end);
			return activity;
		}
			
			private Leg addLeg (double departure,String mode){ 
				Leg leg = population.getFactory().createLeg(mode);
				leg.setDepartureTime(departure+1);
			return leg;
			}
			
			private Coord shoot(String home) {
				Random r = new Random();
				Point point;
				point = getRandomPointInFeature(r, this.municipalityMap.get(home));
				CoordImpl coordImpl = new CoordImpl(point.getX(), point.getY());
				return ct.transform(coordImpl);
			}

			private static Point getRandomPointInFeature(Random rnd, Geometry g) {
				Point p = null;
				double x, y;
				do {
					x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
					y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
					p = MGC.xy2Point(x, y);
				} while (!g.contains(p));
				return p;
			}

		

	public void setScalefactor(double scalefactor) {
		this.scalefactor = scalefactor;
	}

	public void setOffset(double offset) {
		this.offset = offset;
	}

	public void setStart(double start) {
		this.start = start;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}
	
	
}
