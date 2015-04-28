package playground.dhosse.cl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

public class CSVToPlans {
	
	private final String shapefile;
	private final String ouputPlansFile;
	
	private Scenario scenario;
	private String comunasFile;
	
	private final int idxPersonId = 1;
	private final int idxComunaOrigen = 4;
	private final int idxComunaDestino = 5;
	private final int idxOriginX = 10;
	private final int idxOriginY = 11;
	private final int idxDestinationX = 12;
	private final int idxDestinationY = 13;
	private final int idxActType = 14;
	private final int idxModes = 17;
	private final int idxStartTime = 21;
	private final int idxEndTime = 22;

	/*
	 * proposito
	 * 1 al trabajo = zur Arbeit
	 * 2 por trabajo = geschäftlich
	 * 3 al estudio = zur Ausbildung
	 * 4 por estudio = wg. d. Ausbildung
	 * 5 de salud = Gesundheit (evtl. Arzt)
	 * 6 visitar al alguien = jmd. besuchen
	 * 7 volver a casa = nach Hause
	 * 8 buscar o dejar a alguien
	 * 9 comer o tomar algo = essen & trinken
	 * 10 buscar o dejar algo
	 * 11 de compras = Einkaufen
	 * 12 tramites = Formalitäten
	 * 13 recreacion = Erholung
	 * 14 otra actividados = andere...
	 */
	
	//modes
	//network modes
	//car
	private final int modeAutoChofer = 1; //Auto als Fahrer
	private final int modeAutoAcompanante = 17; //Auto Beifahrer
	
	//teleported modes
	//walk
	private final int modeEntramenteAPie = 8; //Fuß
	//pt
	private final int modeBusAlimentador = 2; //Feederbus
	private final int modeBusTroncal = 3; //Bus (Stammstrecke)
	private final int modeMetro = 4; //U-Bahn
	private final int modeFurgonEscolarComoPasajero = 6; //Schulbus als Passagier
	private final int modeBusInstitucional = 11; //
	private final int modeBusInterurbanoORural = 12; //Überlandbus
	private final int modeFurgonEscolarComoChoferOAcompanante = 13; //Schulbus als Fahrer oder Begleiter
	private final int modeBusUrbanoConPagoAlConductor = 14; //Metrobus
	private final int modeTren = 16; //Zug
	//taxi
	private final int modeTaxiColectivo = 5; //Sammeltaxi
	private final int modeTaxiORadioTaxi = 7; //Taxi oder Ruftaxi
	//bike
	private final int modeBicicleta = 9; //Fahrrad
	//motorcycle
	private final int modeMotocicleta = 10; //Motorrad
	private final int modeMotocicletaAcompanante = 18; //Motorrad Beifahrer
	//other
	private final int modeServicioInformal = 15; //
	

	public CSVToPlans(String outputFile){
		
		this.shapefile = null;
		this.ouputPlansFile = outputFile;
		
	}
	
	public CSVToPlans(String outputFile, String shapefileName){
		
		this.shapefile = shapefileName;
		this.ouputPlansFile = outputFile;
		
	}
	
	public void run(String travelDataFilename,String comunasFile){
		
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(this.shapefile);
		Map<String, Geometry> geometries = new HashMap<String, Geometry>();
		
		for(SimpleFeature feature : features){
			
			geometries.put((String) feature.getAttribute("NAME"),(Geometry) feature.getDefaultGeometry());
			
		}
		
		BufferedReader r = IOUtils.getBufferedReader(comunasFile);
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:32719");
		
		Map<Integer,Geometry> comunaId2Geometry = new HashMap<Integer, Geometry>();
		
		try{
			
			String line = r.readLine();
			
			while((line = r.readLine()) != null){
				
				String[] splittedLine = line.split(";");
				int id = Integer.parseInt(splittedLine[0]);
				String name = splittedLine[1];
				
				for(String s : geometries.keySet()){
					if(s.equalsIgnoreCase(name)){
						comunaId2Geometry.put(id, geometries.get(s));
					}
				}
				
			}
			
		} catch(IOException e){
			
			e.printStackTrace();
			
		}
		
		if(this.scenario == null){
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		}

		Population population = scenario.getPopulation();
		PopulationFactoryImpl popFactory = (PopulationFactoryImpl) population.getFactory();
		
		Map<Id<Person>, Person> personsMap = new TreeMap<Id<Person>, Person>();
		
		BufferedReader reader = IOUtils.getBufferedReader(travelDataFilename);
		
		try {
			
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null){
				
				String[] splittedLine = line.split(";");
				
				Id<Person> personId = Id.createPersonId(splittedLine[this.idxPersonId]);
				
				if(!personsMap.containsKey(personId)){
					
					Person person = popFactory.createPerson(personId);
					Plan plan = popFactory.createPlan();
					
					String xOrigin = splittedLine[this.idxOriginX];
					String yOrigin = splittedLine[this.idxOriginY];
					
					Coord coord = null;
					if(xOrigin.equals("")||yOrigin.equals("")){
						
						String idString = splittedLine[this.idxComunaDestino];
						
						if(idString.equals("")) continue;
						
						int id = Integer.parseInt(idString);
						
						Geometry comuna = comunaId2Geometry.get(id);
						
						if(comuna == null){
							continue;
						}
						
						Coord tempCoord = shoot(comuna);

						coord = ct.transform(tempCoord);

					} else{
						
						coord = new CoordImpl(splittedLine[this.idxOriginX], splittedLine[this.idxOriginY]);
						
					}
					
					Activity act1 = popFactory.createActivityFromCoord("h", coord);
					
					double endTime = Time.parseTime(splittedLine[this.idxStartTime]);
					act1.setEndTime(endTime);
					
					String xDest = splittedLine[this.idxDestinationX];
					String yDest = splittedLine[this.idxDestinationY];
					
					Coord coord2 = null;
					
					if(xDest.equals("")||yDest.equals("")){
						
						String idString = splittedLine[this.idxComunaDestino];
						
						if(idString.equals("")) continue;
						
						int id = Integer.parseInt(idString);
						
						Geometry comuna = comunaId2Geometry.get(id);
						
						if(comuna == null){
							continue;
						}
						
						Coord tempCoord = shoot(comuna);

						coord2 = ct.transform(tempCoord);
						
					} else{
						
						coord2 = new CoordImpl(splittedLine[this.idxDestinationX], splittedLine[this.idxDestinationY]);
						
					}
					
					String actTypeIdx = splittedLine[this.idxActType];
					String actType = !actTypeIdx.equals("") ? this.getActType(Integer.parseInt(actTypeIdx)) : "o";
					Activity act2 = popFactory.createActivityFromCoord(actType, coord2);
					
					String startTimeString = splittedLine[this.idxEndTime];
					double startTimeAct2 = 0.;
					if(startTimeString.equals("")){
						startTimeAct2 = act1.getEndTime();
					} else{
						startTimeAct2 = Time.parseTime(startTimeString);
					}
					act2.setStartTime(startTimeAct2);
					
					Set<Leg> legs = new HashSet<Leg>();
					String[] modes = splittedLine[this.idxModes].split("_");
					String lastMode = null;
					
					for(int i = 0; i < modes.length; i++){
						
						Leg leg = popFactory.createLeg(this.getLegMode(Integer.parseInt(modes[i])));
						
						if(lastMode != null){
							
							if(lastMode.equals(leg.getMode())){
								
								continue;
								
							}
							
						}
						
						legs.add(leg);
						lastMode = leg.getMode();
						
					}
					
					plan.addActivity(act1);
					
					for(Leg leg : legs){
						plan.addLeg(leg);
					}
					plan.addActivity(act2);
					
					person.addPlan(plan);
					
					person.setSelectedPlan(plan);
					
					personsMap.put(personId, person);
					
				} else {
					
					Person person = personsMap.get(personId);
					Plan selectedPlan = person.getSelectedPlan();
					
					Activity lastActivity = (Activity) selectedPlan.getPlanElements().get(selectedPlan.getPlanElements().size()-1);
					
					String endTimeString = splittedLine[this.idxStartTime];
					double endTime = 0.;
					if(endTimeString.equals("")){
						endTime = Time.parseTime(splittedLine[this.idxEndTime]);
					}else{
						endTime = Time.parseTime(endTimeString);
					}
					lastActivity.setEndTime(endTime);
					
					String xDest = splittedLine[this.idxDestinationX];
					String yDest = splittedLine[this.idxDestinationY];
					
					Coord coord = null;
					
					if(xDest.equals("")||yDest.equals("")){
						
						int id = Integer.parseInt(splittedLine[this.idxComunaDestino]);
						
						Geometry comuna = comunaId2Geometry.get(id);
						
						if(comuna == null){
							continue;
						}
						
						Coord tempCoord = shoot(comuna);

						coord = ct.transform(tempCoord);

						
					} else{
					
						coord = new CoordImpl(splittedLine[this.idxDestinationX], splittedLine[this.idxDestinationY]);
						
					}
					
					String actTypeIdx = splittedLine[this.idxActType];
					String actType = !actTypeIdx.equals("") ? this.getActType(Integer.parseInt(actTypeIdx)) : "o";
					Activity act2 = popFactory.createActivityFromCoord(actType, coord);
					
					double startTime = Time.parseTime(splittedLine[this.idxEndTime]);
					act2.setStartTime(startTime);
					
					Set<Leg> legs = new HashSet<Leg>();
					String[] modes = splittedLine[this.idxModes].split("_");
					for(int i = 0; i < modes.length; i++){
						Leg leg = popFactory.createLeg(this.getLegMode(Integer.parseInt(modes[i])));
						legs.add(leg);
					}
					
					String lastMode = null;
					for(Leg leg : legs){
						
						if(lastMode != null){
							if(lastMode.equalsIgnoreCase(leg.getMode())){
								continue;
							}
						}
						lastMode = leg.getMode();
						selectedPlan.addLeg(leg);
						
					}
					
					selectedPlan.addActivity(act2);
					
				}
			
			}
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		for(Person person : personsMap.values()){
			population.addPerson(person);
		}
		
	}
	
	public void finalize(){
		
		new PopulationWriter(this.scenario.getPopulation()).write(this.ouputPlansFile);
		
	}
	
	private String getActType(int index){
		
		switch(index){
			case 1:
			case 2: return "w";
			case 3:
			case 4: return "e";
			case 7: return "h";
			case 11: return "s";
			case 13: return "l";
			case 5:
			case 6: 
			case 8:
			case 9:
			case 10:
			case 12:
			case 14:
			default: return "o"; //not specified
		}
		
	}
	
	private String getLegMode(int index){
		
		switch(index){
			case 1:
			case 17: return TransportMode.car;
			case 8: return TransportMode.walk;
			case 2:
			case 3:
			case 4:
			case 6:
			case 11:
			case 12:
			case 13:
			case 14:
			case 16: return TransportMode.pt;
			case 5:
			case 7: return "taxi";
			case 9: return TransportMode.bike;
			case 10:
			case 18: return "motorcycle";
			case 15:
			default: return TransportMode.other;
		}
		
	}
	
	private Coord shoot(Geometry comuna){
		
		Random random = MatsimRandom.getRandom();
	
  	   com.vividsolutions.jts.geom.Point p;
  	   double x, y;
  	   do {
  	      x = comuna.getEnvelopeInternal().getMinX() + random.nextDouble() * (comuna.getEnvelopeInternal().getMaxX() - comuna.getEnvelopeInternal().getMinX());
  	      y = comuna.getEnvelopeInternal().getMinY() + random.nextDouble() * (comuna.getEnvelopeInternal().getMaxY() - comuna.getEnvelopeInternal().getMinY());
  	      p = MGC.xy2Point(x, y);
  	   } while (!comuna.contains(p));
  	   Coord coord = new CoordImpl(p.getX(), p.getY());
		
		return coord;
		
	}
	
}
