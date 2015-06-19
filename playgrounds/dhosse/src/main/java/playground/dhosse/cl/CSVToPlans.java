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

import playground.dhosse.cl.population.Etapa;
import playground.dhosse.cl.population.Persona;
import playground.dhosse.cl.population.Viaje;

import com.vividsolutions.jts.geom.Geometry;

public class CSVToPlans {
	
	private final String shapefile;
	private final String ouputPlansFile;
	
	private Scenario scenario;
	
	private int legCounter = 0;
	
	CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:32719");
	
	private Map<String,Persona> personas = new HashMap<>();
	
	public CSVToPlans(String outputFile){
		
		this(null, outputFile);
		
	}
	
	public CSVToPlans(String outputFile, String shapefileName){
		
		this.shapefile = shapefileName;
		this.ouputPlansFile = outputFile;
		
	}
	
	public void run(String personasFile, String viajesFile, String etapasFile){
		
		this.readPersonas(personasFile);
		this.readViajes(viajesFile);
		this.readEtapas(etapasFile);
		this.createPersons();
		this.write();
		
		System.out.println("created " + Integer.toString(this.legCounter) + " legs!");
		
	}
	
	private void readPersonas(String personasFile){
		
		final int idxPersonId = 1;
		final int idxAge = 2;
		final int idxSex = 3;
		final int idxNViajes = 5;
		final int idxLicence = 6;
		
		BufferedReader reader = IOUtils.getBufferedReader(personasFile);
		
		try {
			
			String line = reader.readLine();
			
			while( (line = reader.readLine()) != null ){
				
				String[] splittedLine = line.split(";");

				String id = splittedLine[idxPersonId];
				int age = 2012 - Integer.valueOf(splittedLine[idxAge]);
				String sex = splittedLine[idxSex];
				String drivingLicence = splittedLine[idxLicence];
				String nViajes = splittedLine[idxNViajes];
				
				Persona persona = new Persona(id, age, sex, drivingLicence, nViajes);
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
		
		for(Persona persona : this.personas.values()){

			boolean toAdd = true;
			
			if(persona.getId().equals("10430102") || persona.getId().equals("12220001") || persona.getId().equals("14676102") || persona.getId().equals("15520101") ||
					persona.getId().equals("18982103") || persona.getId().equals("22078102") || persona.getId().equals("23832104") || persona.getId().equals("14510001") ||
					persona.getId().equals("32430002") || persona.getId().equals("24903102") || persona.getId().equals("11390103")){
				continue;
			}
			
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
					
					if((origin == null || destination == null) || origin.getX() == 0 || origin.getY() == 0 || destination.getX() == 0 || destination.getY() == 0){
						
						String comunaOrigin = etapa.getComunaOrigen();
						String comunaDestino = etapa.getComunaDestino();
						
						if((!comunaOrigin.equals("") && !comunaDestino.equals("")) || (!comunaOrigin.equals("0") && !comunaDestino.equalsIgnoreCase("0"))){
							
							Geometry gOrigin = geometries.get(comunaOrigin);
							Geometry gDest = geometries.get(comunaDestino);
							
							if(gOrigin == null || gDest == null){
								
//								origin = new CoordImpl(0,0);
//								destination = new CoordImpl(0,0);
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
						
						anterior = popFactory.createActivityFromCoord("home", origin);
						anterior.setStartTime(0.);
						anterior.setEndTime(viaje.getStartTime());
						
					} else{
						
						if(planElements.isEmpty()){
							anterior = popFactory.createActivityFromCoord("home", origin);
							anterior.setStartTime(0.);
						} else{
							anterior = (Activity) planElements.getLast();
						}
						
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
						
						posterior = popFactory.createActivityFromCoord(viaje.getProposito(), destination);
						
						double startTime = viaje.getEndTime();
						
						if(startTime < 0){
							toAdd = false;
							break;
						}

						lastActivity = lastActivity == null ? anterior : lastActivity;
						
//						if(lastActivity != null){
							
							if(startTime < lastActivity.getEndTime()){
								
								startTime += 24*3600;
								
							}
							
//						} else{
//							
//							if(startTime < anterior.getEndTime()){
//								
//								anterior = null;
//								
//							}
//							
//						}
						
						posterior.setStartTime(startTime);
						lastActivity = posterior;
						
					} else{
						
						posterior = popFactory.createActivityFromCoord("pt interaction", destination);
						posterior.setMaximumDuration(0.);
						
					}
						
					String legMode = this.getLegMode(Integer.valueOf(etapa.getMode()));
					if(legMode.equals(TransportMode.walk) && (posterior.getType().equals("pt interaction") || posterior.getType().equals("pt interaction"))){
						legMode = TransportMode.transit_walk;
					}
					Leg leg = popFactory.createLeg(legMode);
//					leg.setTravelTime(posterior.getStartTime() - anterior.getEndTime());
					
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
				population.addPerson(person);
				add++;
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
		
		new PopulationWriter(this.scenario.getPopulation()).write(this.ouputPlansFile);
		
	}
	
	private String getActType(int index){
		
		switch(index){
			case 1:
			case 2: return "work";
			case 3:
			case 4: return "education";
			case 7: return "home";
			case 11: return "shop";
			case 13: return "leisure";
			case 5:
			case 6: 
			case 8:
			case 9:
			case 10:
			case 12:
			case 14:
			default: return "other"; //not specified
		}
		
	}
	
	private String getLegMode(int index){
		
		switch(index){
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
