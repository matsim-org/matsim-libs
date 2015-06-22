package playground.dhosse.cl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.cl.population.Etapa;
import playground.dhosse.cl.population.Persona;
import playground.dhosse.cl.population.Viaje;

import com.vividsolutions.jts.geom.Geometry;

public class CSVToPlans {
	
	private final String carAvail = "carAvail";
	
	private final String shapefile;
	private final String ouputPlansFile;
	
	private Scenario scenario;
	private ObjectAttributes agentAttributes;
	
	private int legCounter = 0;
	
	CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:32719");
	
	private Map<String,Persona> personas = new HashMap<>();
	private Map<String, Integer> hogarId2NVehicles = new HashMap<>();
	private Map<String, Coord> hogarId2Coord = new HashMap<>();
	
	public CSVToPlans(String outputFile){
		
		this(null, outputFile);
		
	}
	
	public CSVToPlans(String outputFile, String shapefileName){
		
		this.shapefile = shapefileName;
		this.ouputPlansFile = outputFile;
		
	}
	
	public void run(String hogaresFile, String personasFile, String viajesFile, String etapasFile){
		
		this.readHogares(hogaresFile);
		this.readPersonas(personasFile);
		this.readViajes(viajesFile);
		this.readEtapas(etapasFile);
		this.createPersons();
		this.write();
		
		System.out.println("created " + Integer.toString(this.legCounter) + " legs!");
		
	}
	
	private void readHogares(String hogaresFile){
		
		final int idxHogarId = 0;
		final int idxCoordX = 4;
		final int idxCoordY = 5;
		final int idxNVeh = 11;
		
		BufferedReader reader = IOUtils.getBufferedReader(hogaresFile);
		
		try {
			
			String line = reader.readLine();
			
			while( (line = reader.readLine()) != null ){
				
				String[] splittedLine = line.split(";");
				
				String id = splittedLine[idxHogarId];
				int nVehicles = Integer.parseInt(splittedLine[idxNVeh]);
				
				this.hogarId2NVehicles.put(id, nVehicles);
				
				String x = splittedLine[idxCoordX].replace("," , ".");
				String y = splittedLine[idxCoordY].replace("," , ".");
				this.hogarId2Coord.put(id, new CoordImpl(x, y));
				
			}
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	private void readPersonas(String personasFile){
		
		final int idxHogarId = 0;
		final int idxPersonId = 1;
		final int idxAge = 2;
		final int idxSex = 3;
		final int idxNViajes = 5;
		final int idxLicence = 6;
		final int idxCoordX = 16;
		final int idxCoordY = 17;
		
		BufferedReader reader = IOUtils.getBufferedReader(personasFile);
		
		try {
			
			String line = reader.readLine();
			
			while( (line = reader.readLine()) != null ){
				
				String[] splittedLine = line.split(";");

				String hogarId = splittedLine[idxHogarId];
				String id = splittedLine[idxPersonId];
				int age = 2012 - Integer.valueOf(splittedLine[idxAge]);
				String sex = splittedLine[idxSex];
				String drivingLicence = splittedLine[idxLicence];
				int nCars = this.hogarId2NVehicles.get(hogarId);
				String nViajes = splittedLine[idxNViajes];
				
				Persona persona = new Persona(id, age, sex, drivingLicence, nCars, nViajes);
				persona.setHomeCoord(this.hogarId2Coord.get(hogarId));
				
				String x = splittedLine[idxCoordX].replace("," , ".");
				String y = splittedLine[idxCoordY].replace("," , ".");
				if(!x.equals("") && !y.equals("")){
					persona.setWorkCoord(new CoordImpl(x, y));
				}
				
				this.personas.put(id,persona);
				
			}
			
			reader.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	double latestStart = Double.NEGATIVE_INFINITY;
	double latestEnd = Double.NEGATIVE_INFINITY;
	
	private void readViajes(String viajesFile){
		
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
		
		BufferedReader reader = IOUtils.getBufferedReader(viajesFile);
		
		try {
			
			int counter = 0;
			
			String line = reader.readLine();
			
			while( (line = reader.readLine()) != null ){
				
				String[] splittedLine = line.split(";");
				
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
			
			System.out.println(counter);
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	private void readEtapas(String etapasFile){
		
		final int idxPersonId = 1;
		final int idxViajeId = 2;
		final int idxComunaOrigen = 6;
		final int idxComunaDestino = 7;
		final int idxOriginX = 8;
		final int idxOriginY = 9;
		final int idxDestX = 10;
		final int idxDestY = 11;
		final int idxMode = 12;
		
		BufferedReader reader = IOUtils.getBufferedReader(etapasFile);
		
		try {
			
			String line = reader.readLine();
			
			while( (line = reader.readLine()) != null ){
				
				String[] splittedLine = line.split(";");
				
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
					
				}
				
			}
			
			reader.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	private void createPersons(){
		
		Map<String, Geometry> geometries = createComunaGeometries();
		
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Population population = this.scenario.getPopulation();
		PopulationFactoryImpl popFactory = (PopulationFactoryImpl) population.getFactory();
		
		this.agentAttributes = new ObjectAttributes();
		
		for(Persona persona : this.personas.values()){

			boolean toAdd = true;
			
//			if(persona.getId().equals("10430102") || persona.getId().equals("12220001") || persona.getId().equals("14676102") || persona.getId().equals("15520101") ||
//					persona.getId().equals("18982103") || persona.getId().equals("22078102") || persona.getId().equals("23832104") || persona.getId().equals("14510001") ||
//					persona.getId().equals("32430002") || persona.getId().equals("24903102") || persona.getId().equals("11390103") || persona.getId().equals("17135101") ||
//					persona.getId().equals("16709103")){
//				continue;
//			}
			
			Person person = popFactory.createPerson(Id.createPersonId(persona.getId()));
			
			Plan plan = popFactory.createPlan();
			LinkedList<PlanElement> planElements = new LinkedList<PlanElement>();
			
			int idx = 0;
			Activity lastActivity = null;
			
			for(Viaje viaje : persona.getViajes().values()){
				
				int idxEtapa = 0;
				
				for(Etapa etapa : viaje.getEtapas()){
					
					Coord origin = etapa.getOrigin();
					Coord destination = etapa.getDestination();
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
								
								toAdd = false;
								break;
								
							} else{
							
								origin = ct.transform(shoot(gOrigin));
								destination = this.ct.transform(shoot(gDest));
							
							}
							
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
												
						if(!anterior.getType().equals("pt interaction")){
						
							double endTime = viaje.getStartTime();
							
							if(endTime < 0){
								toAdd = false;
								break;
							}
							
							if(endTime < anterior.getStartTime()){
								endTime += 24*3600;
							}
							
							anterior.setEndTime(endTime);
						
						}
						
					}
					
					Activity posterior;
					
					if(idxEtapa >= viaje.getEtapas().size() - 1){
						
						String proposito = viaje.getProposito();
						if(proposito.equals("home")){
							destination = persona.getHomeCoord();
						} else if(persona.getWorkCoord() != null){
							if(proposito.equals("work")){
								destination = persona.getWorkCoord();
							}
						}
						
						posterior = popFactory.createActivityFromCoord(proposito, destination);
						
						double startTime = viaje.getEndTime();
						
						if(startTime < 0){
							toAdd = false;
							break;
						}

						lastActivity = lastActivity == null ? anterior : lastActivity;
						
						if(startTime < lastActivity.getEndTime()){
							
							startTime += 24*3600;
							
						}
							
						posterior.setStartTime(startTime);
						lastActivity = posterior;
						
					} else{
						
						posterior = popFactory.createActivityFromCoord("pt interaction", destination);
						posterior.setMaximumDuration(0.);
						
					}
						
					String legMode = this.getLegMode(Integer.valueOf(etapa.getMode()));
					
					if(persona.hasCar() && persona.hasDrivingLicence() && isCarOrPTUser(legMode)){
						agentAttributes.putAttribute(person.getId().toString(), this.carAvail, this.carAvail);
					}
					
					if(legMode.equals(TransportMode.walk) && (posterior.getType().equals("pt interaction") || posterior.getType().equals("pt interaction"))){
						legMode = TransportMode.transit_walk;
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
				
				if(!toAdd){
					break;
				}
				
				idx++;
				
			}
			
//			if(!toAdd) continue;
			
			int nLegs = 0;
			for(PlanElement pE : planElements){
				if(pE instanceof Activity){
					plan.addActivity((Activity)pE);
				} else{
					plan.addLeg((Leg)pE);
					nLegs++;
				}
			}
			
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			
			//a plan needs at least 3 plan elements (act - leg - act)
			int add = 0;
			if(plan.getPlanElements().size() > 2){
				if(!((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getType().equals("pt interaction")){
					population.addPerson(person);
					add++;
				}
			}
			this.legCounter += add * nLegs;
			
		}
		
	}
	
	private Map<String, Geometry> createComunaGeometries() {
		
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(this.shapefile);
		Map<String, Geometry> geometries = new HashMap<String, Geometry>();
		
		for(SimpleFeature feature : features){
			
			geometries.put((String) feature.getAttribute("NAME"),(Geometry) feature.getDefaultGeometry());
			
		}
		return geometries;
		
	}
	
	private void write(){
		
		new ObjectAttributesXmlWriter(agentAttributes).writeFile("C:/Users/dhosse/workspace/shared-svn/studies/countries/cl/Kai_und_Daniel/inputFiles/agentAttributes.xml");
		new PopulationWriter(this.scenario.getPopulation()).write(this.ouputPlansFile);
		
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
//			case 1: return TransportMode.car;
//			case 2: return TransportMode.pt;
//			case 3: return TransportMode.pt;
//			case 4: return TransportMode.pt;
//			case 5: return "collective taxi";
//			case 6: return TransportMode.pt;
//			case 7: return "taxi";
//			case 8: return TransportMode.walk;
//			case 9: return TransportMode.bike;
//			case 10: return "motorcycle";
//			case 11: return TransportMode.pt;
//			case 12: return TransportMode.pt;
//			case 13: return TransportMode.pt;
//			case 14: return TransportMode.pt;
//			case 15: return "other";
//			case 16: return TransportMode.pt;
//			case 17: return TransportMode.ride;
//			case 18: return TransportMode.ride;
//			default: return TransportMode.other;
			case 1: return TransportMode.car;
			case 2: return "feeder bus";
			case 3: return "main bus";
			case 4: return "subway";
			case 5: return "collective taxi";
			case 6: return "school bus";
			case 7: return "taxi";
			case 8: return TransportMode.walk;
			case 9: return TransportMode.bike;
			case 10: return "motorcycle";
			case 11: return "institutional bus";
			case 12: return "rural bus";
			case 13: return "school bus";
			case 14: return "urban bus";
			case 15: return "other";
			case 16: return "train";
			case 17: return TransportMode.ride;
			case 18: return TransportMode.ride;
			default: return TransportMode.other;
		}
		
	}
	
	private boolean isCarOrPTUser(String legMode){
		
//		return legMode.equals(TransportMode.car) || legMode.equals(TransportMode.pt);
		return legMode.equals(TransportMode.car) || legMode.equals("feeder bus") || legMode.equals("main bus") || legMode.equals("subway")
				|| legMode.equals("institutional bus") || legMode.equals("rural bus") || legMode.equals("urban bus") || legMode.equals("train");
		
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
