package playground.dhosse.prt.launch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.prt.passenger.PrtRequestCreator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class PrtCottbusLauncher {
	
	public static void main(String args[]){
		
		String configFileName = "C:/Users/Daniel/Desktop/dvrp/"+
				"cottbus_scenario/config.xml";
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, configFileName);
		Scenario scenario = ScenarioUtils.loadScenario(config);

//		createPtPopulation(scenario);
//		createPrtPopulation(config, scenario);
//		createShapes(scenario);
		
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.run();
		
	}

	private static void createShapes(Scenario scenario) {
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("link",Point.class);
		typeBuilder.add("id",String.class);
		typeBuilder.add("type",String.class);
		typeBuilder.add("acttype",String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for(Person p : scenario.getPopulation().getPersons().values()){
			
			String type = p.getId().toString().contains("prt") ? "pt" : "iv";
			
			Activity home = (Activity) p.getSelectedPlan().getPlanElements().get(0);
			
			SimpleFeature feature = builder.buildFeature(null, new Object[]{
				new GeometryFactory().createPoint(MGC.coord2Coordinate(home.getCoord())),
				p.getId().toString(),
				type,
				home.getType()
			});
			features.add(feature);
			
			Activity work = (Activity) p.getSelectedPlan().getPlanElements().get(2);
			
			feature = builder.buildFeature(null, new Object[]{
					new GeometryFactory().createPoint(MGC.coord2Coordinate(work.getCoord())),
					p.getId().toString(),
					type,
					work.getType()
				});
				features.add(feature);
			
		}
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Documents/Masterarbeit/outputFiles/activities.shp");
		
		typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("link",LineString.class);
		typeBuilder.add("line",String.class);
		typeBuilder.add("type", String.class);
		builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		features = new ArrayList<SimpleFeature>();
		
		for(TransitLine tl : scenario.getTransitSchedule().getTransitLines().values()){
			
			String mode = "";

			List<Link> links = new ArrayList<Link>();
			
			for(TransitRoute tr : tl.getRoutes().values()){
				
				mode = tr.getTransportMode();
				
				for(Id<Link> linkId : tr.getRoute().getLinkIds()){
					
					links.add(scenario.getNetwork().getLinks().get(linkId));
					
				}
				
			}
			
			for(Link link : links){
				Coordinate[] c = {MGC.coord2Coordinate(link.getFromNode().getCoord()),MGC.coord2Coordinate(link.getToNode().getCoord())};
				SimpleFeature feature = builder.buildFeature(null, new Object[]{
					new GeometryFactory().createLineString(c),
					tl.getId().toString(),
					mode
				});
				features.add(feature);
			}
			
		}
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Documents/Masterarbeit/outputFiles/transitLines.shp");
		
		typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("link",Point.class);
		typeBuilder.add("ID",String.class);
		builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		features = new ArrayList<SimpleFeature>();
		
		for(TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()){
			SimpleFeature feature = builder.buildFeature(null, new Object[]{
				new GeometryFactory().createPoint(MGC.coord2Coordinate(stop.getCoord())),
				stop.getId().toString()
			});
			features.add(feature);
		}
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Documents/Masterarbeit/outputFiles/stops.shp");
		
		typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("link",LineString.class);
		typeBuilder.add("ID",String.class);
		typeBuilder.add("length",Double.class);
		typeBuilder.add("freespeed",Double.class);
		typeBuilder.add("capacity",Double.class);
		typeBuilder.add("nlanes", String.class);
		typeBuilder.add("allowed_modes", String.class);
		builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		features = new ArrayList<SimpleFeature>();
		
		for(Link link : scenario.getNetwork().getLinks().values()){
			
			String allowedModes = "";
			for(String mode : link.getAllowedModes())
				allowedModes += mode + ",";
			
			SimpleFeature feature = builder.buildFeature(null, new Object[]{
				new GeometryFactory().createLineString(new Coordinate[]{
							new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY()),
							new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY())
				}),
				link.getId(),
				link.getLength(),
				link.getFreespeed(),
				link.getCapacity(),
				link.getNumberOfLanes(),
				allowedModes
				
			});
			
			features.add(feature);
			
		}
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Documents/Masterarbeit/outputFiles/network.shp");
		

		typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("stop",Point.class);
		typeBuilder.add("ID",String.class);
		builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		features = new ArrayList<SimpleFeature>();
		
		for(TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()){
			
			SimpleFeature feature = builder.buildFeature(null, new Object[]{
				new GeometryFactory().createPoint(MGC.coord2Coordinate(facility.getCoord())),
				facility.getId()				
			});
			
			features.add(feature);
			
		}
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Documents/Masterarbeit/inputFiles/stops.shp");
	}

	private static void createPtPopulation(Scenario scenario) {
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize("C:/Users/Daniel/Documents/Masterarbeit/outputFiles/buffer/buffer.shp");
		Geometry g = (Geometry) features.iterator().next().getDefaultGeometry();
		
		List<Person> filteredPopulation = new ArrayList<Person>();
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			Activity home = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			Activity work = (Activity) person.getSelectedPlan().getPlanElements().get(2);
			double d = ((Leg)person.getSelectedPlan().getPlanElements().get(1)).getRoute().getDistance();
			
			if(g.contains(new GeometryFactory().createPoint(MGC.coord2Coordinate(home.getCoord())))&&
					g.contains(new GeometryFactory().createPoint(MGC.coord2Coordinate(work.getCoord())))&&
					d <= 4500){
				
				filteredPopulation.add(person);
				
			}
		}
		
		int cnt = 0;
		
		Random rnd = MatsimRandom.getRandom();
		
		while(cnt < 16652){
			
			Person template = filteredPopulation.get((int)(rnd.nextDouble() * filteredPopulation.size()));
			
			Person person = factory.createPerson(Id.createPersonId(cnt + "_" + template.getId() + "_prt"));
			Plan plan = factory.createPlan();
			Activity home = (Activity) template.getSelectedPlan().getPlanElements().get(0);
			Activity work = (Activity) template.getSelectedPlan().getPlanElements().get(2);
			Activity home2 = (Activity) template.getSelectedPlan().getPlanElements().get(4);
			plan.addActivity(home);
			plan.addLeg(factory.createLeg(TransportMode.pt));
			plan.addActivity(work);
			plan.addLeg(factory.createLeg(TransportMode.pt));
			plan.addActivity(home2);
			
			person.addPlan(plan);
			
			scenario.getPopulation().addPerson(person);
			scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", "prt");
			
			cnt++;
			
		}
		
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		writer.write("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/population_miv_pt2.xml");
	}

	private static void createPrtPopulation(Config config, Scenario scenario) {
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		Population prt = PopulationUtils.createPopulation(config, scenario.getNetwork());
		
		for(Person p : scenario.getPopulation().getPersons().values()){
			if(p.getId().toString().contains("prt")){
				Person person = factory.createPerson(p.getId());
				Plan newPlan = factory.createPlan();
				Plan plan = p.getSelectedPlan();
				Activity home = (Activity) plan.getPlanElements().get(0);
				Activity work = (Activity) plan.getPlanElements().get(2);
				Activity home2 = (Activity) plan.getPlanElements().get(4);
				
				newPlan.addActivity(home);
				newPlan.addLeg(factory.createLeg(PrtRequestCreator.MODE));
				newPlan.addActivity(work);
				newPlan.addLeg(factory.createLeg(PrtRequestCreator.MODE));
				newPlan.addActivity(home2);
				person.addPlan(newPlan);
				prt.addPerson(person);
			} else{
				prt.addPerson(p);
			}
		}
		
		PopulationWriter writer = new PopulationWriter(prt, scenario.getNetwork());
		writer.write("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/prt_population_final.xml");
	}
	
}
