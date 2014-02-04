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
package playground.jbischoff.taxi.berlin.demand;

import java.util.ArrayList;
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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author jbischoff
 * 
 */



public class TaxiDemandWriter {
	private static final Logger log = Logger.getLogger(TaxiDemandWriter.class);
	private Map<String, Geometry> municipalityMap;
	private Scenario scenario;
	private Population population;
	private CoordinateTransformation ct = TransformationFactory
			.getCoordinateTransformation(
					"PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]",
					TransformationFactory.WGS84_UTM33N);

	private Random rnd = new Random(17);

	public static void main(String[] args) {
		TaxiDemandWriter tdw = new TaxiDemandWriter();
		LorShapeReader lsr = new LorShapeReader();
		lsr.readShapeFile("/Users/jb/tucloud/taxi/OD/LOR_SHP_EPSG_25833/Planungsraum.shp", "SCHLUESSEL");
		lsr.readShapeFile("/Users/jb/shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp", "Nr");
		tdw.setMunicipalityMap(lsr.getShapeMap());
		for (int i = 15; i <22 ; i++){
			tdw.writeDemand("/Users/jb/tucloud/taxi/OD/201304"+i+"/", "OD_201304"+i);
			
		}
	}
	
	

	public void setMunicipalityMap(Map<String, Geometry> municipalityMap) {
		this.municipalityMap = municipalityMap;
	}



	public void writeDemand(String dirname, String fileNamePrefix) {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		generatePopulation(dirname, fileNamePrefix);
		log.info("Population size: " +population.getPersons().size());
		PopulationWriter populationWriter = new PopulationWriter(
				scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(dirname+"plans.xml");
	}

	private void generatePopulation(String dirname, String fileNamePrefix) {
		
		for (int i = 0; i<24; i++){
			String hrstring = String.format("%02d", i);
			DemandParser dp = new DemandParser();
			String currentFileName = dirname+fileNamePrefix+hrstring+"0000.dat";
			read(currentFileName, dp);
			generatePlansForZones(i, dp.getDemand());

		}

	}

	private void read(String file, TabularFileHandler handler) {
		TabularFileParserConfig config = new TabularFileParserConfig();
		log.info("parsing " + file);
		config.setDelimiterTags(new String[]{"\t"," "});
		config.setFileName(file);
		new TabularFileParser().parse(config, handler);
		log.info("done. (parsing " + file + ")");
	}
	
	private void generatePlansForZones(int hr, List<TaxiDemandElement> hourlyTaxiDemand) {

		for (TaxiDemandElement tde : hourlyTaxiDemand){
		for (int i = 0; i< tde.getAmount(); i++){
			Person p;
			Id pId = scenario.createId("p"+tde.fromId+"_"+tde.toId+"_hr_"+hr+"_nr_"+i);
			p = generatePerson(tde.fromId, tde.toId,pId, hr);
			if (p == null ) continue;
			population.addPerson(p);
		}
		}
	}

	private Person generatePerson(Id from, Id to, Id pId, int hr) {
		Person p;
		Plan plan;
		p = population.getFactory().createPerson(pId);
		plan = generatePlan(from, to, hr);
		if (plan == null ) return null;
		p.addPlan(plan);
		return p;
	}

	private Plan generatePlan(Id from, Id to, int hr) {
		Plan plan = population.getFactory().createPlan();

		Coord fromCoord = this.shoot(from);
		Coord toCoord = this.shoot(to);
		double activityStart = Math.round(hr * 3600. + rnd.nextDouble() * 3600.);

		plan.addActivity(this.addActivity("home", 0.0, activityStart - 1.0, fromCoord));
		plan.addLeg(this.addLeg(activityStart - 1., "taxi"));
		plan.addActivity(this.addActivity("work", toCoord));
		if (fromCoord == null) return null;
		if (toCoord == null) return null;

		return plan;
	}

	private Activity addActivity(String type, Double start, Double end,
			Coord coord) {

		Activity activity = population.getFactory().createActivityFromCoord(
				type, coord);
		activity.setStartTime(start);
		activity.setEndTime(end);
		return activity;
	}
	
	private Activity addActivity(String type, Coord coord) {

		Activity activity = population.getFactory().createActivityFromCoord(
				type, coord);

		return activity;
	}

	private Leg addLeg(double departure, String mode) {
		Leg leg = population.getFactory().createLeg(mode);
		leg.setDepartureTime(departure + 1);
		return leg;
	}

	private Coord shoot(Id zoneId) {
		Point point;
//		log.info("Zone" + zoneId);
			if (this.municipalityMap.containsKey(zoneId.toString())){
		point = getRandomPointInFeature(this.rnd, this.municipalityMap.get(zoneId.toString()));
		CoordImpl coordImpl = new CoordImpl(point.getX(), point.getY());
		return ct.transform(coordImpl);}
			else {
				log.error(zoneId.toString() +"does not exist in shapedata");
				return null;
			}
	}

	private static Point getRandomPointInFeature(Random rnd, Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX()
					+ rnd.nextDouble()
					* (g.getEnvelopeInternal().getMaxX() - g
							.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY()
					+ rnd.nextDouble()
					* (g.getEnvelopeInternal().getMaxY() - g
							.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		return p;
	}



}

class TaxiDemandElement {
	Id fromId;
	Id toId;
	int amount;

	TaxiDemandElement(Id from, Id to, int amount) {
		this.fromId = from;
		this.toId = to;
		this.amount = amount;
	}

	public Id getFromId() {
		return fromId;
	}

	public Id getToId() {
		return toId;
	}

	public int getAmount() {
		return amount;
	}

}
class DemandParser implements TabularFileHandler{

	List<TaxiDemandElement> demand = new ArrayList<TaxiDemandElement>();
	
	@Override
	public void startRow(String[] row) {
		demand.add(new TaxiDemandElement(new IdImpl(row[0]), new IdImpl(row[1]), Integer.parseInt(row[2])));
	}

	public List<TaxiDemandElement> getDemand() {
		return demand;
	}
	
	}
