package playground.dhosse.prt.launch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.prt.passenger.PrtRequestCreator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class PrtCottbusLauncher {
	
	public static void main(String args[]){
		
		String configFileName = "C:/Users/Daniel/Desktop/dvrp/"+
				"cottbus_scenario/config.xml";
		
//		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFileName));
//		
//		for(Link link : scenario.getNetwork().getLinks().values()){
//			if(link.getFreespeed() <= 50 / 3.6){
//			Set<String> allowedModes = new HashSet<String>();
//			for(String s : link.getAllowedModes()){
//				allowedModes.add(s);
//			}
//    		allowedModes.add(TransportMode.walk);
//    		link.setAllowedModes(allowedModes);
//			}
//		}
//		
//		new NetworkWriter(scenario.getNetwork()).write("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/network_prt.xml");
		
//		ArrayList<TaxiRank> taxiRanks = new ArrayList<TaxiRank>();
//		
//		for(TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()){
//			
//			TaxiRank rank = new TaxiRank(Id.create(stop.getId().toString(), TaxiRank.class),
//					stop.getName(), scenario.getNetwork().getLinks().get(stop.getLinkId()));
//			taxiRanks.add(rank);
//			
//		}
//		
//		BufferedWriter writer = IOUtils.getBufferedWriter("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/taxiRanks.xml");
//		
//		try {
//			
//			writer.write("<ranks>\n");
//			
//			for(TaxiRank rank : taxiRanks){
//				
//					writer.write("<rank id=\"" + rank.getId().toString() + "\" name=\"" + rank.getName() + "\" link=\"" +
//					rank.getLink().getId().toString() + "\"/>\n");
//				
//			}
//			
//			writer.write("</ranks>");
//			
//			writer.flush();
//			writer.close();
//		
//		} catch (IOException e) {
//			
//			e.printStackTrace();
//			
//		}
		
//		OTFVis.playMVI("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/out/ITERS/it.0/pt.0.otfvis.mvi");
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, configFileName);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			if(person.getId().toString().contains("pt")){
				
				Plan plan = factory.createPlan();
				
				for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
					if(pe instanceof Leg){
						Leg leg = factory.createLeg(PrtRequestCreator.MODE);
						plan.addLeg(leg);
						continue;
					}
					plan.addActivity((Activity)pe);
				}
				
				List<Plan> plans = new ArrayList<Plan>();
				
				for(Plan p : person.getPlans()) plans.add(p);
				
				for(Plan p : plans) person.removePlan(p);
				
				person.addPlan(plan);
				
			}
		}
		
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		writer.write("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/population_miv_prt.xml");
		
//		Controler controler = new Controler(scenario);
//		controler.setOverwriteFiles(true);
//		controler.run();
		
//		ShapeFileReader reader = new ShapeFileReader();
//		Collection<SimpleFeature> features = reader.readFileAndInitialize("C:/Users/Daniel/Documents/Masterarbeit/inputFiles/buffer/convHull.shp");
//		CoordinateTransformation ct2 = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM33N,
//				TransformationFactory.WGS84);
//		Geometry cottbus = null;
//		for(SimpleFeature feat : features){
//			cottbus = (Geometry) feat.getDefaultGeometry();
//		}
//		
//		List<Person> filteredPopulation = new ArrayList<Person>();
//		
//		for(Person person : scenario.getPopulation().getPersons().values()){
//			
//			Activity home = (Activity) person.getSelectedPlan().getPlanElements().get(0);
//			Activity work = (Activity) person.getSelectedPlan().getPlanElements().get(2);
//			
//			if(cottbus.contains(new GeometryFactory().createPoint(MGC.coord2Coordinate(home.getCoord())))&&
//					cottbus.contains(new GeometryFactory().createPoint(MGC.coord2Coordinate(work.getCoord())))){
//				filteredPopulation.add(person);
//			}
//			
//		}
//		
//		Random random = MatsimRandom.getRandom();
//		
//		int cnt = 0;
//		
//		PopulationFactory factory = scenario.getPopulation().getFactory();
//		
//		while(cnt < 15358){
//			Person template = filteredPopulation.get((int)(random.nextDouble()*filteredPopulation.size()));
//			Person person = factory.createPerson(Id.createPersonId("pt_" + cnt + "_" + template.getId().toString()));
//			Plan plan = factory.createPlan();
//			for(PlanElement pe : template.getSelectedPlan().getPlanElements()){
//				
//				if(pe instanceof Leg){
//					Leg leg = factory.createLeg(TransportMode.pt);
//					plan.addLeg(leg);
//					continue;
//				}
//				plan.addActivity((Activity)pe);
//				
//			}
//			
//			person.addPlan(plan);
//			scenario.getPopulation().addPerson(person);
//			scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", "pt");
//			cnt++;
//			
//		}
//		
//		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
//		writer.write("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/population_miv_pt.xml");
//		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile("C:/Users/Daniel/Desktop/dvrp/"
//				+ "cottbus_scenario/personAttributes.xml");
		
//		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
//		typeBuilder.setName("shape");
//		typeBuilder.add("stop",Point.class);
//		typeBuilder.add("ID",String.class);
//		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
//		
//		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
//		
//		for(TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()){
//			
//			SimpleFeature feature = builder.buildFeature(null, new Object[]{
//				new GeometryFactory().createPoint(MGC.coord2Coordinate(facility.getCoord())),
//				facility.getId()				
//			});
//			
//			features.add(feature);
//			
//		}
//		
//		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Documents/Masterarbeit/inputFiles/stops.shp");
		
//		System.out.println(scenario.getPopulation().getPersons().size());
		
	}
	
}
