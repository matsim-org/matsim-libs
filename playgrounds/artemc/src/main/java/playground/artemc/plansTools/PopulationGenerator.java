package playground.artemc.plansTools;

import org.apache.log4j.Logger;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;
import org.opengis.feature.simple.SimpleFeature;
import playground.artemc.networkTools.ShapeArea;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;

public class PopulationGenerator {

	/**
	 * @param args
	 */
	private static final Logger log = Logger.getLogger(ShapeArea.class);

	public static void main(String[] args) throws IOException {
		PopulationGenerator generator = new PopulationGenerator();
		
		
		
		if (args.length != 4) {
			System.out.println("PopulationGenerator usage:");
			System.out.println("java -cp <MATSim release file> PopulationGenerator <path to network.xml> <path to workAreas.shp> <path to output folder> <populationSize>");
			System.exit(-1);
		}
		String network = args[0];
		String areaShape = args[1];
		String outputFolder = args[2];
		Integer populationSize = Integer.parseInt(args[3]);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		log.info("Reading network...");
		new MatsimNetworkReader(scenario).readFile(network);
		NetworkImpl net = (NetworkImpl) scenario.getNetwork();
		
		log.info("Reading work areas...");
		Collection<SimpleFeature> fts = new ShapeFileReader().readFileAndInitialize(areaShape); 
		log.info("Shape file contains "+fts.size()+" zones!");
	
		
		Population population = ((ScenarioImpl)scenario).getPopulation();
		PopulationFactory populationFactory = population.getFactory();
		
		Random rnd = MatsimRandom.getRandom();
		
		Coord[] netDimensions = getDimensions(net);
		
		for(int p=0;p<populationSize;p++){
			Person newPerson = populationFactory.createPerson(Id.create(p, Person.class));
			
			//Get points in residential and business areas
			
			Boolean pointInWorkArea;
			Point homePoint;	
			Point workPoint;	
			
			
			do{
				pointInWorkArea = false;;
				homePoint = getRandomCoordInDimension(rnd, netDimensions);
				for(SimpleFeature ft:fts) {
					Geometry geo = (Geometry) ft.getDefaultGeometry();
					if(geo.contains(homePoint)){
						pointInWorkArea = true;
					}	
				}		
		    }while(pointInWorkArea == true);
			
			do{
				pointInWorkArea = false;
				workPoint = getRandomCoordInDimension(rnd, netDimensions);
				for(SimpleFeature ft:fts) {
					Geometry geo = (Geometry) ft.getDefaultGeometry();
					if(geo.contains(workPoint)){
						pointInWorkArea=true;
					}	
				}		
		    }while(pointInWorkArea == false);
			
			System.out.println("Home: "+homePoint.getX()+","+homePoint.getY()+"   Work: "+workPoint.getX()+","+workPoint.getY());
					
			Plan plan = populationFactory.createPlan();
			Link link = NetworkUtils.getNearestLink(net, new Coord(homePoint.getX(), homePoint.getY()));
			Activity activity = populationFactory.createActivityFromLinkId("home", link.getId());
			activity.setEndTime(3600.0*8);
			plan.addActivity(activity);
			Leg leg = populationFactory.createLeg("car");
			
			plan.addLeg(leg);

			link = NetworkUtils.getNearestLink(net, new Coord(workPoint.getX(), workPoint.getY()));
			activity = populationFactory.createActivityFromLinkId("work", link.getId());
			activity.setStartTime(3600.0*9);
			activity.setEndTime(3600.0*18);
			plan.addActivity(activity);
			
			plan.addLeg(leg);
			
			activity = populationFactory.createActivityFromLinkId("home", link.getId());
			activity.setStartTime(3600.0*19);
			plan.addActivity(activity);
			
			newPerson.addPlan(plan);
			population.addPerson(newPerson);
			
			SelectedPlans2ESRIShape toShape = new  SelectedPlans2ESRIShape(population, net, MGC.getCRS("WGS84_UTM48N"), outputFolder);
		}
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outputFolder+"newPopulation.xml");
		
		
		for(SimpleFeature ft:fts) {
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			Coordinate[] coordinates = geo.getCoordinates();
			System.out.println("Feature:");
			for(int i=0;i<coordinates.length;i++){
				System.out.print(coordinates[i].x+","+coordinates[i].y+"   ");
			}
			System.out.println();
		}

		log.info("Done.");

	}

	
	private static Coord[] getDimensions(NetworkImpl network) {
		
	    Double xMin = 0.0;
	    Double yMin = 0.0;
	    Double xMax = 0.0;
	    Double yMax = 0.0;	   
	    boolean firstNode = true;
	    
		for(Id node:network.getNodes().keySet()){
			
			Coord nodeCoord = network.getNodes().get(node).getCoord();
		
			if(firstNode){
			   xMin = nodeCoord.getX();
			   yMin = nodeCoord.getY();
			   xMax = nodeCoord.getX();
			   yMax = nodeCoord.getY();	
			   firstNode = false;
			}
			
			if(xMin > nodeCoord.getX()){
				xMin = nodeCoord.getX();
			}
			else if(xMax < nodeCoord.getX()){
				xMax = nodeCoord.getX();
			}
			
			if(yMin > nodeCoord.getY()){
				yMin = nodeCoord.getY();
			}
			else if(yMax < nodeCoord.getY()){
				yMax = nodeCoord.getY();
			}
		}
				
		Coord[] dimensions = new Coord[2]; 
				
		System.out.println("Min: "+xMin+","+yMin);
		System.out.println("Max: "+xMax+","+yMax);

		dimensions[0] = new Coord(xMin - 1000, yMin - 1000);
		dimensions[1] = new Coord(xMax + 1000, yMax + 1000);
		
		return dimensions;
	}

	private static Point getRandomCoordInDimension(Random rnd, Coord[] maxMinCoordinates) {
		Point p = null;
		double x, y;
			x = maxMinCoordinates[0].getX() + rnd.nextDouble() * (maxMinCoordinates[1].getX() - maxMinCoordinates[0].getX());
			y = maxMinCoordinates[0].getY() + rnd.nextDouble() * (maxMinCoordinates[1].getY() - maxMinCoordinates[0].getY());
			p = MGC.xy2Point(x, y);
		return p;
	}

}

