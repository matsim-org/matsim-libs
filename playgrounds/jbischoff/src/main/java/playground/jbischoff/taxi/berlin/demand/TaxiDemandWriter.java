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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
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
	private Population population;
	private NetworkImpl network;
	private Scenario scenario;
	private CoordinateTransformation ct = TransformationFactory
			.getCoordinateTransformation(
					"EPSG:25833",
					TransformationFactory.DHDN_GK4);

	private Random rnd = new Random(17);
	private final static String DATADIR = "C:/local_jb/data/";
	private final static String NETWORKFILE = DATADIR+"network/berlin_brb.xml.gz";
	private final static Id TXLLORID = new IdImpl("12214125");
	private final static double SCALEFACTOR = 1.0;
	static int fromTXL = 0;
	static int toTXL = 0;
	
	private Map<Id,Integer> oMap = new HashMap<Id, Integer>();
	private Map<Id,Integer> dMap = new HashMap<Id, Integer>();



	public static void main(String[] args) {
		LorShapeReader lsr = new LorShapeReader();
		lsr.readShapeFile(DATADIR+"OD/shp_merged/Planungsraum.shp", "SCHLUESSEL");
		lsr.readShapeFile(DATADIR+"OD/shp_merged/gemeinden.shp", "NR");
		for (int i = 16; i <17 ; i++){
			TaxiDemandWriter tdw = new TaxiDemandWriter();
			tdw.setMunicipalityMap(lsr.getShapeMap());
			tdw.writeDemand(DATADIR+"/OD/201304"+i+"/", "OD_201304"+i);
//			tdw.writeODbyZone(DATADIR+"OD/201304"+i+"/od.csv");
		}
		System.out.println("trips from TXL "+TaxiDemandWriter.fromTXL);
		System.out.println("trips to TXL "+TaxiDemandWriter.toTXL);
	}
	
	

	private void writeODbyZone(String outputFileName) {
		Set<Id> allZones = new TreeSet<Id>();
		allZones.addAll(this.dMap.keySet());
		allZones.addAll(this.oMap.keySet());
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFileName);
		try {
		for (Id zoneId : allZones){
			int o = getFromMapOrReturnZero(this.oMap,zoneId);
			int d = getFromMapOrReturnZero(dMap, zoneId);
			String s = zoneId.toString() + "\t" + o + "\t" +d+ "\n";
				bw.append(s);
		}

		bw.flush();
		bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	


	private int getFromMapOrReturnZero(Map<Id, Integer> odMap, Id zoneId) {
		int rv = 0;
		if (odMap.containsKey(zoneId)) rv = odMap.get(zoneId); 
		return rv;
	}



	private void setMunicipalityMap(Map<String, Geometry> municipalityMap) {
		this.municipalityMap = municipalityMap;
	}
	TaxiDemandWriter(){
	 scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());;
	 new MatsimNetworkReader(scenario).readFile(NETWORKFILE);	
	 this.network = (NetworkImpl) scenario.getNetwork();
	}



	private void writeDemand(String dirname, String fileNamePrefix) {
		
		
		population = scenario.getPopulation();
//		generatePopulation(dirname, fileNamePrefix);
		generatePopulation4to4();
		log.info("Population size: " +population.getPersons().size());
		PopulationWriter populationWriter = new PopulationWriter(
				scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(dirname+fileNamePrefix+"_SCALE_"+SCALEFACTOR+"_"+"plans4to4.xml.gz");
	}

	private void generatePopulation(String dirname, String fileNamePrefix) {
		
		for (int i = 0; i<24; i++){
			String hrstring = String.format("%02d", i);
			DemandParser dp = new DemandParser();
			String currentFileName = dirname+fileNamePrefix+hrstring+"0000.dat";
			read(currentFileName, dp);
			generatePlansForZones(i, dp.getDemand());
			writeODbyZone( dirname+fileNamePrefix+hrstring+"_OD.csv");
			this.oMap.clear();
			this.dMap.clear();
			
		}

	}
	private void generatePopulation4to4() {
		
		for (int i = 4; i<24; i++){
			String hrstring = String.format("%02d", i);
			DemandParser dp = new DemandParser();
			String currentFileName = DATADIR +"/OD/20130416/OD_20130416"+hrstring+"0000.dat";
			read(currentFileName, dp);
			generatePlansForZones(i, dp.getDemand());
			this.oMap.clear();
			this.dMap.clear();
			
		}
		for (int i = 0; i<4; i++){
			String hrstring = String.format("%02d", i);
			DemandParser dp = new DemandParser();
			String currentFileName = DATADIR +"/OD/20130417/OD_20130417"+hrstring+"0000.dat";
			read(currentFileName, dp);
			generatePlansForZones(i+24, dp.getDemand());
			this.oMap.clear();
			this.dMap.clear();
			
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
			double amount = tde.getAmount() * SCALEFACTOR;
		for (int i = 0; i< amount; i++){
			Person p;
			Id pId = scenario.createId("p"+tde.fromId+"_"+tde.toId+"_hr_"+hr+"_nr_"+i);
			p = generatePerson(tde.fromId, tde.toId,pId, hr);
			if (p == null ) continue;
			population.addPerson(p);
			incMap(oMap,tde.fromId);
			incMap(dMap,tde.toId);
		}
		}
	}

	private void incMap(Map<Id, Integer> odMap, Id fromId) {
		Integer val;
		if (odMap.containsKey(fromId)) val = odMap.get(fromId) ;
		else val = 0;
		val++;
		odMap.put(fromId, val);
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
		Coord fromCoord;
		Coord toCoord; 
		if (from.equals(TXLLORID)){
			fromCoord = new CoordImpl(4588171.603474933,5825260.734177444);
			TaxiDemandWriter.fromTXL++;
		} else {
		 fromCoord = this.shoot(from);
		}
		if (to.equals(TXLLORID)){
			toCoord = new CoordImpl(4588171.603474933,5825260.734177444);
			TaxiDemandWriter.toTXL++;
		} else {
			toCoord = this.shoot(to);
		}
		
		
		
		
		if (fromCoord == null) return null;
		if (toCoord == null) return null;
		
		Link fromLink = network.getNearestLinkExactly(fromCoord);
		Link toLink = network.getNearestLinkExactly(toCoord);

		
		double activityStart = Math.round(hr * 3600. + rnd.nextDouble() * 3600.);

		plan.addActivity(this.addActivity("home", 0.0, activityStart, fromLink));
		plan.addLeg(this.addLeg(activityStart , "taxi", fromLink, toLink));
		plan.addActivity(this.addActivity("work", toLink));

		return plan;
	}

	private Activity addActivity(String type, Double start, Double end,
			Link link) {

		Activity activity = population.getFactory().createActivityFromLinkId(type, link.getId());
		activity.setStartTime(start);
		activity.setEndTime(end);
		return activity;
	}
	
	private Activity addActivity(String type, Link link) {

		Activity activity = population.getFactory().createActivityFromLinkId(type, link.getId());
		
		return activity;
	}

	private Leg addLeg(double departure, String mode, Link fromLink, Link toLink) {
		Leg leg = population.getFactory().createLeg(mode);
		leg.setDepartureTime(departure );
		leg.setRoute(new  LinkNetworkRouteImpl(fromLink.getId(),toLink.getId()));
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

	public static Point getRandomPointInFeature(Random rnd, Geometry g) {
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
