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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import playground.santiago.SantiagoScenarioConstants;

public class CSVToPlans {
	private static final Logger log = Logger.getLogger(CSVToPlans.class);
	
	private final String carUsers = "carUsers"; 	//Attribute name for subpopulation
	private final String carAvail = "carAvail";		//Attribute value for subpopulation -> car is available

	private Scenario scenario;
	private ObjectAttributes agentAttributes;
	private LinkedList<String> agentsWithCar;
	CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:4326", SantiagoScenarioConstants.toCRS);
	
	private Map<String,Persona> personas = new HashMap<>();
	private Map<String, Integer> hogarId2NVehicles = new HashMap<>();
	private Map<String, Coord> hogarId2Coord = new HashMap<>();
	private Map<String,String> comunaName2Id = new HashMap<>();
	
	private final Config config;
	private final String shapefile;
	private final String outputDirectory;
	
	private Map<String,Integer> legMode2NumberOfShotLegs = new HashMap<>();
	private int legCounter = 0;
	private int carAvailCounter;
	private int implausibleCarUseCounter = 0;
	
	public CSVToPlans(Config config, String outputDirectory, String shapefileName){
		this.config = config;
		this.shapefile = shapefileName;
		this.outputDirectory = outputDirectory;
	}
	
	public void run(String hogaresFile, String personasFile, String viajesFile, String etapasFile){
		this.readHogares(hogaresFile);
		this.readPersonas(personasFile);
		this.readViajes(viajesFile);
		this.readEtapas(etapasFile);
		this.createPersons();
		this.write();
	}
	
	public void run(String hogaresFile, String personasFile, String viajesFile, String etapasFile, String comunasFile){
		this.readHogares(hogaresFile);
		this.readPersonas(personasFile);
		this.readViajes(viajesFile);
		this.readEtapas(etapasFile);
		this.readComunas(comunasFile);
		this.createPersons();
		this.write();
	}
	
	private void readComunas(String comunasFile){
		BufferedReader reader = IOUtils.getBufferedReader(comunasFile);	
		try {
			String line = reader.readLine();
			while( (line = reader.readLine()) != null ){
				String[] tokens = line.split(";");
				this.comunaName2Id.put(tokens[1], tokens[0]);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void readHogares(String hogaresFile){
		
		log.info("Reading households from file " + hogaresFile + "...");
		
		final int idxHogarId = 0;
		final int idxCoordX = 4;
		final int idxCoordY = 5;
		final int idxNVeh = 11;
		
		BufferedReader reader = IOUtils.getBufferedReader(hogaresFile);
		int counter = 0;
		
		try {
			String line = reader.readLine();
			while( (line = reader.readLine()) != null ){
				String[] splittedLine = line.split(",");
				String id = splittedLine[idxHogarId];
				int nVehicles = Integer.parseInt(splittedLine[idxNVeh]);
				this.hogarId2NVehicles.put(id, nVehicles);
				String x = splittedLine[idxCoordX]/*.replace("," , ".")*/;
				String y = splittedLine[idxCoordY]/*.replace("," , ".")*/;
				this.hogarId2Coord.put(id, new Coord(Double.parseDouble(x), Double.parseDouble(y)));
				counter++;
			}
			reader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("Read data of " + counter + " households...");
	}
	
	private void readPersonas(String personasFile){
		log.info("Reading persons from file " + personasFile + "...");
		final int idxHogarId = 0;
		final int idxPersonId = 1;
		final int idxAge = 2;
		final int idxSex = 3;
		final int idxNViajes = 5;
		final int idxLicence = 6;
		final int idxCoordX = 16;
		final int idxCoordY = 17;
		
		int counter = 0;
		
		BufferedReader reader = IOUtils.getBufferedReader(personasFile);
		
		try {
			String line = reader.readLine();
			while( (line = reader.readLine()) != null ){
				String[] splittedLine = line.split(",");
				String hogarId = splittedLine[idxHogarId];
				String id = splittedLine[idxPersonId];
				int age = 2012 - Integer.valueOf(splittedLine[idxAge]);
				String sex = splittedLine[idxSex];
				String drivingLicence = splittedLine[idxLicence];
				int nCars = this.hogarId2NVehicles.get(hogarId);
				String nViajes = splittedLine[idxNViajes];
				
				Persona persona = new Persona(id, age, sex, drivingLicence, nCars, nViajes);
				persona.setHomeCoord(this.hogarId2Coord.get(hogarId));
				
				String x = splittedLine[idxCoordX]/*.replace("," , ".")*/;
				String y = splittedLine[idxCoordY]/*.replace("," , ".")*/;
				if(!x.equals("") && !y.equals("") && !x.equals("0") && !y.equals("0")){
					persona.setWorkCoord(new Coord(Double.parseDouble(x), Double.parseDouble(y)));
				}
				
				this.personas.put(id,persona);
				counter++;
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Read data of " + counter + " persons...");
	}
	
	double latestStart = Double.NEGATIVE_INFINITY;
	double latestEnd = Double.NEGATIVE_INFINITY;
	
	private void readViajes(String viajesFile){
		
		log.info("Reading trips from file " + viajesFile + "...");
		
		final int idxPersonId = 1;
		final int idxId = 2;
		final int idxComunaOrigin = 4;
		final int idxComunaDestino = 5;
		final int idxOriginX = 10;
		final int idxOriginY = 11;
		final int idxDestX = 12;
		final int idxDestY = 13;
		final int idxProposito = 14;
		final int idxStartTime = 21;
		final int idxEndTime = 22;
		
		int counter = 0;
		
		BufferedReader reader = IOUtils.getBufferedReader(viajesFile);
		
		try {
			String line = reader.readLine();
			while( (line = reader.readLine()) != null ){
				String[] splittedLine = line.split(",");
				
				String personId = splittedLine[idxPersonId];
				Persona persona = this.personas.get(personId);
				
				String id = splittedLine[idxId];
				String comunaOrigen = splittedLine[idxComunaOrigin];
				String comunaDestino = splittedLine[idxComunaDestino];
				String originX = splittedLine[idxOriginX];
				String originY = splittedLine[idxOriginY];
				String destinationX = splittedLine[idxDestX];
				String destinationY = splittedLine[idxDestY];
				String propString = splittedLine[idxProposito];
				String proposito = propString.equals("") ? "other" : this.getActType(Integer.parseInt(propString));
				String start = splittedLine[idxStartTime];
				String end = splittedLine[idxEndTime];
				
				if(Time.parseTime(end) > latestEnd) latestEnd = Time.parseTime(end);
				if(Time.parseTime(start) > latestStart) latestStart = Time.parseTime(start);
				
				Viaje viaje = new Viaje(id, comunaOrigen, comunaDestino, originX, originY, destinationX, destinationY, proposito, start, end);

				persona.addViaje(viaje);
				counter++;
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Read data of " + counter + " trips...");
	}
	
	private void readEtapas(String etapasFile){
		log.info("Reading stages from file " + etapasFile + "...");
		final int idxPersonId = 1;
		final int idxViajeId = 2;
		final int idxComunaOrigen = 6;
		final int idxComunaDestino = 7;
		final int idxOriginX = 8;
		final int idxOriginY = 9;
		final int idxDestX = 10;
		final int idxDestY = 11;
		final int idxMode = 12;
		
		int counter = 0;
		
		BufferedReader reader = IOUtils.getBufferedReader(etapasFile);
		
		try {
			String line = reader.readLine();
			while( (line = reader.readLine()) != null ){
				String[] splittedLine = line.split(",");
				String personId = splittedLine[idxPersonId];
				String viajeId = splittedLine[idxViajeId];
				
				Persona persona = this.personas.get(personId);
				Viaje viaje = persona.getViajes().get(viajeId);
				
				if(viaje != null){
				
					String mode = splittedLine[idxMode];
					String comunaOrigen = splittedLine[idxComunaOrigen];
					String comunaDestino = splittedLine[idxComunaDestino];
					String originX = splittedLine[idxOriginX];
					String originY = splittedLine[idxOriginY];
					String destinationX = splittedLine[idxDestX];
					String destinationY = splittedLine[idxDestY];
					
					Etapa etapa = new Etapa(mode, comunaOrigen, comunaDestino, originX, originY, destinationX, destinationY);
					
					viaje.addEtapa(etapa);
					counter++;
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Read data of " + counter + " stages...");
	}
	
	private void createPersons(){
		Map<String, Geometry> geometries = createComunaGeometries();
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = this.scenario.getPopulation();
		PopulationFactory popFactory = (PopulationFactory) population.getFactory();
		this.agentAttributes = new ObjectAttributes();
		this.agentsWithCar = new LinkedList<>(); 

		for(Persona persona : this.personas.values()){
			Person person = popFactory.createPerson(Id.createPersonId(persona.getId()));
			if(persona.hasCar() && persona.hasDrivingLicence()){
				String id = persona.getId();
				agentsWithCar.add(id);
				agentAttributes.putAttribute(person.getId().toString(), carUsers, carAvail);
				carAvailCounter++;
			}
			Plan plan = popFactory.createPlan();
			LinkedList<PlanElement> planElements = new LinkedList<PlanElement>();
			
			int idx = 0;
			Activity lastActivity = null;
			
			for(Viaje viaje : persona.getViajes().values()){
				int idxEtapa = 0;
				for(Etapa etapa : viaje.getEtapas()){
					Coord origin = etapa.getOrigin();
					Coord destination = etapa.getDestination();
					double reportedTravelTime = 0;
					if(viaje.getEtapas().size() < 2){
						reportedTravelTime = viaje.getEndTime() - viaje.getStartTime();
					}
//					if(persona.getId().equals("13768103")){
//						System.out.println();
//					}
					String proposito = viaje.getProposito();
					if(proposito.equals("home")){
						destination = persona.getHomeCoord();
					} else if(persona.getWorkCoord() != null){
						if(proposito.equals("work")){
//							destination = persona.getWorkCoord();
						}
					}
					if(viaje.getEtapas().indexOf(etapa) >= viaje.getEtapas().size() - 1){
						destination = viaje.getDestination() != null ? viaje.getDestination() : etapa.getDestination();
					}
					if((origin == null || destination == null) || origin.getX() == 0 || origin.getY() == 0 || destination.getX() == 0 || destination.getY() == 0){
						String comunaOrigin = etapa.getComunaOrigen();
						String comunaDestino = etapa.getComunaDestino();
						if((!comunaOrigin.equals("") && !comunaDestino.equals("")) || (!comunaOrigin.equals("0") && !comunaDestino.equalsIgnoreCase("0"))){
							Geometry gOrigin = geometries.get(comunaOrigin);
							Geometry gDest = geometries.get(comunaDestino);
							if(gOrigin == null || gDest == null){
								origin = origin == null ? new Coord(0.0, 0.0) : origin;
								destination = etapa.getDestination() == null ? new Coord(0.0, 0.0) : etapa.getDestination();
							} else{
								String legMode = this.getLegMode(Integer.parseInt(etapa.getMode()));
								origin = lastActivity == null ? ct.transform(shoot(legMode, gOrigin)) : lastActivity.getCoord();
								if(viaje.getEndTime() != 0 && viaje.getStartTime() != 0 && !legMode.equals(TransportMode.car)){
									destination = destination == null ? shootLegDestination(legMode, origin, gDest, reportedTravelTime) : destination;
								} else{
									if(legMode.equals(TransportMode.walk)){
										destination = destination == null ? shootWalkTrip(origin, gDest) : destination;
									} else{
										destination = destination == null ? this.ct.transform(shoot(legMode, gDest)) : destination;
									}
								}
							}
							
						} else{
							origin = origin == null ? new Coord(0.0, 0.0) : origin;
							destination = destination == null ? new Coord(0.0, 0.0) : destination;
						}
					}
					Activity anterior = null;
					
					if(idx <= 0){
						String actType = "other";
						if(Math.abs(origin.getX() - persona.getHomeCoord().getX()) <= 1.0 && Math.abs(origin.getY() - persona.getHomeCoord().getY()) <= 1){
							actType = "home";
						} else if(persona.getWorkCoord() != null){
							if(Math.abs(origin.getX() - persona.getWorkCoord().getX()) <= 1.0 && Math.abs(origin.getY() - persona.getWorkCoord().getY()) <= 1){
								actType = "work";
							}
						}
						anterior = popFactory.createActivityFromCoord(actType, origin);
						anterior.setEndTime(viaje.getStartTime());
					} else{
						anterior = (Activity) planElements.getLast();
						if(!anterior.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
							double endTime = viaje.getStartTime();
							anterior.setEndTime(endTime);
						}
					}
					Activity posterior;
					if(idxEtapa >= viaje.getEtapas().size() - 1){
						posterior = popFactory.createActivityFromCoord(proposito, destination);
						double startTime = viaje.getEndTime();
						lastActivity = lastActivity == null ? anterior : lastActivity;
						if(startTime < lastActivity.getEndTime()){
							startTime += 24*3600;
						}
						posterior.setStartTime(startTime);
						lastActivity = posterior;
					} else{
						posterior = popFactory.createActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, destination);
						posterior.setMaximumDuration(0.);
					}
					String legMode = this.getLegMode(Integer.valueOf(etapa.getMode()));
					
					if(legMode.equals(TransportMode.car)){
						// TODO: maybe interpret this as ride?
						if(!persona.hasCar()){
//							log.warn("A person is using car despite having one.");
							implausibleCarUseCounter ++;
						} else if(!persona.hasDrivingLicence()){
//							log.warn("A person is using a car despite having a driver's license.");
							implausibleCarUseCounter ++;
						} else {
							// everything fine
						}
					}

					Leg leg = popFactory.createLeg(legMode);
					if(anterior != null){
						if(!planElements.isEmpty()){
							if(planElements.getLast() instanceof Leg){
								planElements.addLast(anterior);
							}
						} else{
							if(anterior != null){
								planElements.addLast(anterior);
							}
						}
					}
					if(anterior != null)
						planElements.addLast(leg);
					planElements.addLast(posterior);
					
					idxEtapa++;
				}
				idx++;
			}
			int nLegs = 0;
			for(PlanElement pe : planElements){
				if(pe instanceof Activity){
					plan.addActivity((Activity)pe);
				}else{
					plan.addLeg((Leg)pe);
					nLegs++;
				}
			}
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			
			int add = 0;
			//a plan needs at least 3 plan elements (act - leg - act)
			if(plan.getPlanElements().size() > 2){
				population.addPerson(person);
				add = 1;
			}
			this.legCounter += add * nLegs;
		}
		
		log.info("Created " + population.getPersons().size() + " persons and " + this.legCounter + " legs.");
		log.info("Dumping information about legs that were shot...");
		for(String mode : this.legMode2NumberOfShotLegs.keySet()){
			log.info("Shot " + this.legMode2NumberOfShotLegs.get(mode) + " " + mode + " legs.");
		}
		log.info("There are " + carAvailCounter + " persons in the population with a car available.");
		log.info("There were " + implausibleCarUseCounter + " legs with implausible car use (person not having a car or a diver's license).");
	}
	
	private Map<String, Geometry> createComunaGeometries() {
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(this.shapefile);
		Map<String, Geometry> geometries = new HashMap<String, Geometry>();
		
		for(SimpleFeature feature : features){
			String comuna = (String)feature.getAttribute("NAME");
			String key = null;
			for(String c : comunaName2Id.keySet()){
				if(c.equalsIgnoreCase(comuna)){
					key = comunaName2Id.get(c);
				}
			}
			geometries.put( key,(Geometry) feature.getDefaultGeometry());
		}
		return geometries;
	}
	
	private void write(){
//		createDir(new File(this.outputDirectory));
		new ObjectAttributesXmlWriter(agentAttributes).writeFile(this.outputDirectory + "agentAttributes.xml");
		new PopulationWriter(this.scenario.getPopulation()).write(this.outputDirectory + "plans_eod.xml.gz");
		
		
		try {
			
			PrintWriter pw = new PrintWriter (new FileWriter ( this.outputDirectory + "agentsWithCar.txt" ));
			for (String agent : agentsWithCar) {
				pw.println(agent);

			}
			
			pw.close();
		} catch (IOException e) {
			log.error(new Exception(e));
		}
		
	}
	
	private String getActType(int index){
		switch(index){
			case 1: return "work";
			case 2: return "business";
			case 3: return "education";
			case 4: return "education";
			case 5: return "health";
			case 6: return "visit";
			case 7: return "home";
			case 8: return "other";
			case 9: return "other";
			case 10: return "other";
			case 11: return "shop";
			case 12: return "other";
			case 13: return "leisure";
			case 14: return "other";
			default: return "other"; //not specified
		}
	}
	
	private String getLegMode(int index){
		switch(index){
			case 1: return TransportMode.car;
			case 2: return SantiagoScenarioConstants.Modes.bus.toString();
			case 3: return SantiagoScenarioConstants.Modes.bus.toString();
			case 4: return SantiagoScenarioConstants.Modes.metro.toString();
			case 5: return SantiagoScenarioConstants.Modes.colectivo.toString();
			case 6: return SantiagoScenarioConstants.Modes.other.toString();
			case 7: return SantiagoScenarioConstants.Modes.taxi.toString();
			case 8: return TransportMode.walk;
			case 9: return TransportMode.bike;
			case 10: return SantiagoScenarioConstants.Modes.other.toString();
			case 11: return SantiagoScenarioConstants.Modes.other.toString();
			case 12: return SantiagoScenarioConstants.Modes.other.toString();
			case 13: return SantiagoScenarioConstants.Modes.other.toString();
			case 14: return SantiagoScenarioConstants.Modes.other.toString();
			case 15: return SantiagoScenarioConstants.Modes.other.toString();
			case 16: return SantiagoScenarioConstants.Modes.train.toString();
			case 17: return TransportMode.ride;
			case 18: return SantiagoScenarioConstants.Modes.other.toString();
			default: return SantiagoScenarioConstants.Modes.other.toString();
		}
	}
	
	// TODO: Check if this is correct
	private boolean isCarOrPTUser(String legMode){
		return legMode.equals(TransportMode.car) || legMode.equals(SantiagoScenarioConstants.Modes.bus.toString()) || legMode.equals(TransportMode.walk);
	}
	
	private Coord shoot(String legMode, Geometry comuna){
		Random random = MatsimRandom.getRandom();
  	   com.vividsolutions.jts.geom.Point p;
  	   double x, y;
  	   do {
  	      x = comuna.getEnvelopeInternal().getMinX() + random.nextDouble() * (comuna.getEnvelopeInternal().getMaxX() - comuna.getEnvelopeInternal().getMinX());
  	      y = comuna.getEnvelopeInternal().getMinY() + random.nextDouble() * (comuna.getEnvelopeInternal().getMaxY() - comuna.getEnvelopeInternal().getMinY());
  	      p = MGC.xy2Point(x, y);
  	   } while (!comuna.contains(p));
		Coord coord = new Coord(p.getX(), p.getY());
		
  	 if(!this.legMode2NumberOfShotLegs.containsKey(legMode + " w/ travel time")){
			this.legMode2NumberOfShotLegs.put(legMode + " w/ travel time", 0);
		}
		int n = this.legMode2NumberOfShotLegs.get(legMode + " w/ travel time");
		this.legMode2NumberOfShotLegs.put(legMode + " w/ travel time", n+1);
  	   
		return coord;
	}
	
	private final double lambda = 0.00160256410256410256410256410256;
	private Coord shootWalkTrip(Coord origin, Geometry comuna){
		if(origin.getX() < 0 && origin.getY() < 0){
			origin = ct.transform(origin);
		}
		Random random = MatsimRandom.getRandom();
		double d = 0.;
		double p = 0.;
		double pDistribution = 0.;
		do{
			p = random.nextDouble();
			d = (-Math.log(lambda/p)/p)/this.config.plansCalcRoute().getBeelineDistanceFactors().get(TransportMode.walk);
			pDistribution = lambda * Math.exp(- lambda * d);
		} while(p > pDistribution && d > 5000);
		
		int signX = 0;
		double proba = random.nextDouble();
		if(proba < 0.5){
			signX = 1;
		} else{
			signX = -1;
		}
		int signY = 0;
		proba = random.nextDouble();
		if(proba < 0.5){
			signY = 1;
		} else{
			signY = -1;
		}
		double x = signX * random.nextDouble() * d;
		double y = signY * Math.sqrt(d*d - x*x);
		
		double resX = origin.getX() + x;
		double resY = origin.getY() + y;

		return new Coord(resX, resY);
	}
	
	private Coord shootLegDestination(String legMode, Coord origin, Geometry comuna, double reportedTravelTime){
		if(origin.getX() < 0 || origin.getY() < 0){
			origin = this.ct.transform(origin);
		}
		Random random = MatsimRandom.getRandom();
		double range = reportedTravelTime * this.config.plansCalcRoute().getTeleportedModeSpeeds().get(legMode) / this.config.plansCalcRoute().getBeelineDistanceFactors().get(legMode);
		
		Coord res = null;
//		do{
			int signX = 0;
			double proba = random.nextDouble();
			if(proba < 0.5){
				signX = 1;
			} else{
				signX = -1;
			}
			
			int signY = 0;
			proba = random.nextDouble();
			if(proba < 0.5){
				signY = 1;
			} else{
				signY = -1;
			}
			
			double x = signX * random.nextDouble() * range;
			double y = signY * Math.sqrt(range*range - x*x);
			
			double resX = origin.getX() + x;
			double resY = origin.getY() + y;

		res = new Coord(resX, resY);
		
//		} while(!comuna.contains(MGC.coord2Point(res)));
			
		if(!this.legMode2NumberOfShotLegs.containsKey(legMode + " w/o travel time")){
			this.legMode2NumberOfShotLegs.put(legMode + " w/o travel time", 0);
		}
		int n = this.legMode2NumberOfShotLegs.get(legMode + " w/o travel time");
		this.legMode2NumberOfShotLegs.put(legMode + " w/o travel time", n+1);
		
		return res;
	}
}