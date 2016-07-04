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

package playground.santiago.population;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

public class FreightBuilder {

	private static final String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/"; 	//Path: KT (SVN-checkout)
	private static final String workingDirInputFiles = svnWorkingDir + "inputFromElsewhere/exportedFilesFromDatabase/" ;
	private static final String outputDir = svnWorkingDir + "inputForMATSim/plans/" ; //outputDir of this class -> input for Matsim (KT)

	private static Map<String, Coord> zonaId2Coord = new HashMap<String, Coord>();
	private static Map<String, FreightTrip> tripId2FreightTrip = new HashMap<String, FreightTrip>(); 

	private static final Logger log = Logger.getLogger(FreightBuilder.class);

	//TODO: Pläne aus den Informationen erstellen (Haben nur 2 Aktivitäten: Start und Zielort. Muss aber getrennt erfolgen, 
	// da für Outgoing Verkehr die Startzeit unbekannt ist.) -> Alternativ: Rundtrip draus machen
	//TODO: Hochrechnen auf andere Zeiten, die in der Umfrage nicht erfasst sind
	//TODO: LKW-Verkehr innerhalb des Großraums Santiago erstellen.
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		createDir(new File(outputDir));

		String crs = "EPSG:32719";
		//		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs);

		createObservationPoints();
		readZonas(workingDirInputFiles + "freight_Centroides.csv");
		convertZonas2Shape(crs, outputDir + "shapes/");
		readEODData(workingDirInputFiles + "freight_EODcam.csv");
		Population population = generatePlans();
		write(population, outputDir);
		log.info("### Done. ###");

	}

	private static void write(Population population, String outputDir){
		createDir(new File(outputDir));
		new PopulationWriter(population).write(outputDir + "freight_plans.xml.gz");

		//write list of all AgentIds to file. Can be used for selecting all freightVehicleAgents in visualization with Senozon Via   
		try {
			FileWriter writer = new FileWriter(outputDir + "freightAgentIds.txt");
			for (Person person: population.getPersons().values()){
				writer.write(person.getId().toString() + System.getProperty("line.separator"));
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private static Population generatePlans() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Population population = scenario.getPopulation();
		PopulationFactory popFactory = (PopulationFactory) population.getFactory();

		List<FreightTrip> inboundTrips = new ArrayList<FreightTrip>();
		List<FreightTrip> outboundTrips = new ArrayList<FreightTrip>();

		for(FreightTrip freightTrip : tripId2FreightTrip.values()){
			if (freightTrip.getOriginZone().startsWith("CE")){ //surveyed at arrival
				inboundTrips.add(freightTrip);
			} else
				outboundTrips.add(freightTrip);
		}

		for (FreightTrip freightTrip : tripId2FreightTrip.values()){

			Person person = popFactory.createPerson(Id.createPersonId("f_" + freightTrip.getId()));

			Plan plan = popFactory.createPlan();
			LinkedList<PlanElement> planElements = new LinkedList<PlanElement>();

			Activity firstActivity = null;
			Activity lastActivity = null;

			//TODO: correct assignment of start /end to the activities... or user "other" or anything else
			String actTypeStart = "home";
			String actTypeEnd = "work";

			String originZone = freightTrip.getOriginZone() ;
			String destinationZone = freightTrip.getDestinationZone();
			String prefix = "CE";

			Coord originCoord = null;
			Coord destinationCoord = null;

			//			System.out.println(freightTrip.getId()+ " OriginZone: "+ originZone + " "+ originZone.startsWith(prefix) );
			if (originZone.startsWith(prefix)){ //surveyed at arriving 
				String zoneName = originZone.concat("in");
				if (zonaId2Coord.containsKey(zoneName)){
					originCoord = zonaId2Coord.get(zoneName);
					//				System.out.println(freightTrip.getId()+ " origin: " + originCoord.toString());
				} else {
					log.warn("Zone can not be resolved: " + originZone);
				}
			} else {
				String zoneName = originZone;
				if (zonaId2Coord.containsKey(zoneName)){
					originCoord = zonaId2Coord.get(zoneName);
					//				System.out.println(freightTrip.getId()+ " origin: " + originCoord.toString());
				} else {
					log.warn("Zone can not be resolved: " + originZone);

				}
			}

			//			System.out.println(freightTrip.getId()+ " DestZone: "+ destinationZone + " "+ destinationZone.startsWith(prefix) );
			if (destinationZone.startsWith(prefix)){	//surveyed at leaving 
				String zoneName = destinationZone.concat("out");
				if (zonaId2Coord.containsKey(zoneName)){
					destinationCoord = zonaId2Coord.get(zoneName);
					//				System.out.println(freightTrip.getId()+ " dest: " + destinationCoord.toString());
				}else {
					log.warn("Zone can not be resolved: " + originZone);
				}
			} else {
				String zoneName = destinationZone;
				if (zonaId2Coord.containsKey(zoneName)){
					destinationCoord = zonaId2Coord.get(zoneName);
					//				System.out.println(freightTrip.getId()+ " dest: " + destinationCoord.toString());
				}else {
					log.warn("Zone can not be resolved: " + originZone);
				}
				//				System.out.println(freightTrip.getId()+ " dest: " + destinationCoord.toString());
			}						


			if ( originCoord != null && destinationCoord != null && actTypeStart != null && actTypeEnd != null) {
				double reportedTravelTime = 3600 ;		//TODO: Use individual value (from survey or estimate one) 
				firstActivity = popFactory.createActivityFromCoord(actTypeStart, originCoord);
				if (inboundTrips.contains(freightTrip)){
					firstActivity.setEndTime(freightTrip.getTimeOfSurvey());
				} else if (outboundTrips.contains(freightTrip)){
					firstActivity.setEndTime(freightTrip.getTimeOfSurvey() - reportedTravelTime);  
				}


				lastActivity = popFactory.createActivityFromCoord(actTypeEnd, destinationCoord);

				if (inboundTrips.contains(freightTrip)){
					lastActivity.setStartTime(freightTrip.getTimeOfSurvey() + reportedTravelTime);
				} else if (outboundTrips.contains(freightTrip)){
					lastActivity.setStartTime(freightTrip.getTimeOfSurvey() );  
				}

				//			firstActivity.setMaximumDuration(0);
				//			lastActivity.setMaximumDuration(0);


				//			String legMode = Constants.Modes.truck.toString(); //TODO doesn't work by now in matsim.run()
				String legMode = TransportMode.car; 

				Leg leg = popFactory.createLeg(legMode);

				planElements.add(firstActivity);
				planElements.addLast(leg);
				planElements.addLast(lastActivity);

				for(PlanElement pe : planElements){
					if(pe instanceof Activity){
						plan.addActivity((Activity)pe);
					}else{
						plan.addLeg((Leg)pe);
					}
				}

				person.addPlan(plan);
				person.setSelectedPlan(plan);

				//a plan needs at least 3 plan elements (act - leg - act)
				if(plan.getPlanElements().size() > 2){
					population.addPerson(person);
				}
			}


		}
		log.info("Created " + population.getPersons().size() + " persons ");
		return scenario.getPopulation();

	}


	/**
	 * Create locations for the ObservationPoints of the survey as origin or destination of freight tour.
	 * Origin and destination are separated so there the incoming traffic start on an inbound link to Santiago and 
	 * outgoing traffic ends on an outbound link from Santigo
	 *  TODO: (how to= make sure, that the right link will be used(?)
	 */
	private static void createObservationPoints() {

		//CE01	CAMINO A MELIPILLA
		zonaId2Coord.put("CE01in", new Coord(296025.0, 6271935.0));
		zonaId2Coord.put("CE01out", new Coord(296025.0, 6271905.0)); 
		//CE02	AUTOPISTA DEL SOL
		zonaId2Coord.put("CE02in", new Coord(292517.0, 6271720.0));
		zonaId2Coord.put("CE02out", new Coord(292615.0, 6271840.0)); 
		//CE03	RUTA 68 (A VALPARAISO); moved more to city of Santiago (better link matching).
		//		zonaId2Coord.put("CE03in", new Coord(323540.0, 6296520.0));
		//		zonaId2Coord.put("CE03out", new Coord(323560.0, 6296520.0)); 
		zonaId2Coord.put("CE03in", new Coord(327000.0, 6297700.0));
		zonaId2Coord.put("CE03out", new Coord(327000.0, 6297620.0)); 
		//CE04	RUTA 5 SUR (ANGOSTURA) ; out of box -> moved northbound on Ruta 5
		zonaId2Coord.put("CE04in", new Coord(338322.0, 6252325.0));
		zonaId2Coord.put("CE04out", new Coord(338185.0, 6253060.0)); 
		//CE05	RUTA 5 NORTE (LAMPA)
		zonaId2Coord.put("CE05in", new Coord(336100.0 , 6321210.0));
		zonaId2Coord.put("CE05out", new Coord(336200.0 , 6321210.0)); 
		//CE06	CAMINO PADRE HURTADO
		zonaId2Coord.put("CE06in", new Coord(343990.0, 6266370.0));
		zonaId2Coord.put("CE06out", new Coord(343955.0, 6266370.0)); 
		//CE07	AUTOPISTA LOS LIBERTADORES; toll Station Chacabuco -> not found on google earth.
		zonaId2Coord.put("CE07in", new Coord(346515.0, 6327220.0));
		zonaId2Coord.put("CE07out", new Coord(346560.0, 6327220.0)); 
		//CE08	CAMINO SAN JOSE DE MAIPO
		zonaId2Coord.put("CE08in", new Coord(358240.0, 6281350.0));
		zonaId2Coord.put("CE08out", new Coord(358240.0, 6281280.0)); 
		//CE09	CAMINO A FARELLONES
		zonaId2Coord.put("CE09in", new Coord(360730.0, 6307035.0));
		zonaId2Coord.put("CE09out", new Coord(360730.0, 6306980.0)); 
		//CE10	CAMINO LO ECHEVERS (Lampa) Road Isabel Riquelme, north of Antonio Varas
		zonaId2Coord.put("CE10in", new Coord(323950.0, 6316720.0));
		zonaId2Coord.put("CE10out", new Coord(324000.0, 6316720.0)); 		
	}

	/**
	 * Read Coordinates for OS-Zonas from file
	 * @param ZonasFile
	 */
	private static void readZonas(String ZonasFile){

		log.info("Reading zonas from file " + ZonasFile + "...");

		final int idxZonaId = 0;
		final int idxCoordX = 1;
		final int idxCoordY = 2;

		BufferedReader reader = IOUtils.getBufferedReader(ZonasFile);

		int counter = 0;

		try {

			String line = reader.readLine();

			while( (line = reader.readLine()) != null ){

				String[] splittedLine = line.split(";");

				String id = splittedLine[idxZonaId];
				String x = splittedLine[idxCoordX].replace("," , ".");
				String y = splittedLine[idxCoordY].replace("," , ".");
				//				this.ZonaId2Coord.put(id, new Coord(Double.parseDouble(x), Double.parseDouble(y)));
				zonaId2Coord.put(id, new Coord(Double.parseDouble(x), Double.parseDouble(y)));
				System.out.println("coordinates of zone: "+ id + ": "+ zonaId2Coord.get(id));
				counter++;

			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();

		}

		log.info("Read data of " + counter + " zonas...");

	}

	private static void convertZonas2Shape(String crs, String outputDir){

		createDir(new File(outputDir));

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				setName("zonas").
				addAttribute("ID", String.class).
				create();

		for (String zonaId : zonaId2Coord.keySet()) {
			//			SimpleFeature ft = nodeFactory.createPoint(zonaId2Coord.get(zonaId), null, zonaId);
			SimpleFeature ft = nodeFactory.createPoint(zonaId2Coord.get(zonaId), new Object[] {zonaId}, null);
			features.add(ft);
		}
		ShapeFileWriter.writeGeometries(features, outputDir + "Zonas_Points.shp");
	}

	/**
	 * Read trip data from EOD file.
	 * @param string
	 */
	private static void readEODData(String EODFile) {
		log.info("Reading trips from file " + EODFile + "...");

		final int idxTripId = 0;
		final int idxPCon = 3;		//Point of survey
		final int idxHour = 7;		//Time of recorded 
		final int idxMinute = 8;	//Time of recorded 
		final int idxAxis = 10;		//Number of axis of truck
		final int idxZOrig = 11;	//Zone of tour start
		final int idxZDest = 12;	//Zone of tour destination
		final int idxCarga = 13; //Type of goods loaded

		BufferedReader reader = IOUtils.getBufferedReader(EODFile);

		int counter = 0;

		try {

			String line = reader.readLine();
			while( (line = reader.readLine()) != null ){

				String[] splittedLine = line.split(";");

				String id = splittedLine[idxTripId].split(",")[0];
				String originZone = splittedLine[idxZOrig].replace("\"", "");
				String destinationZone = splittedLine[idxZDest].replace("\"", "");
				String pCon = splittedLine[idxPCon].replace("\"", ""); //Point of survey
				int numberOfAxis =  new Double(splittedLine[idxAxis].split(",")[0]).intValue();
				double timeOfSurvey = new Double(splittedLine[idxHour].replace("," , "."))*3600 + new Double(splittedLine[idxMinute].replace("," , "."))*60;
				String typeOfGoods = splittedLine[idxCarga].split(",")[0]; 

				//Vehicle was recorded on the way TO Santiago (inbound)
				if ( originZone == pCon) {
					originZone = originZone.concat("in");
				}

				//Vehicle was recorded on the way FROM Santiago (outbound)
				if ( destinationZone == pCon) {
					destinationZone = destinationZone.concat("out");
				}

				FreightTrip trip = new FreightTrip(id, originZone, destinationZone, pCon, numberOfAxis, timeOfSurvey, typeOfGoods);
				System.out.println(trip.toString());
				tripId2FreightTrip.put(id, trip);
				counter++;
			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();

		}

		log.info("Read data of " + counter + " trips...");

	}

	private static void createDir(File file) {
		System.out.println("Directory " + file + " created: "+ file.mkdirs());	
	}
}
