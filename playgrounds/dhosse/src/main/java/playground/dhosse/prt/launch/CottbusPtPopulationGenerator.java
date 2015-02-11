package playground.dhosse.prt.launch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.dvrp.router.DistanceAsTravelDisutility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImplTest;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.old.PseudoTransitLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class CottbusPtPopulationGenerator {
	
//	private Config config;
//	private TransitSchedule transitSchedule;
//	private NetworkImpl network;
//	private Population population;
//	private Geometry munBounds;
//	private List<Geometry> residentials = new ArrayList<Geometry>();
//	private List<Geometry> commercials = new ArrayList<Geometry>();
//	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
//			TransformationFactory.WGS84_UTM33N);
//	private CoordinateTransformation ct2 = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM33N,
//			TransformationFactory.WGS84);
//	private List<Geometry> buffers = new ArrayList<Geometry>();
//	
//	public static void main(String args[]){
//		
//		Config config = ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(config, "C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/config.xml");
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		
//		CottbusPtPopulationGenerator gen = new CottbusPtPopulationGenerator(config, scenario);
//		
//		ShapeFileReader reader = new ShapeFileReader();
//		Collection<SimpleFeature> features = reader.readFileAndInitialize("C:/Users/Daniel/Documents/Masterarbeit/inputFiles/admin_level_6/admin_level_6.shp");
//		SimpleFeature feature = null;
//		Geometry spreeneisse = null;
//		for(SimpleFeature feat : features){
//			if(feat.getAttribute("name").equals("Cottbus, Stadt")){
//				feature = feat;
//				gen.munBounds = (Geometry) feat.getDefaultGeometry();
//			} else if(feat.getAttribute("name").equals("Spree-Neisse")){
//				spreeneisse = (Geometry) feat.getDefaultGeometry();
//			}
//		}
//		
//		features = reader.readFileAndInitialize("C:/Users/Daniel/Documents/Masterarbeit/inputFiles/buffer/regions2.shp");
//		for(SimpleFeature feat : features){
//			gen.buffers.add((Geometry)feat.getDefaultGeometry());
//		}
//		
////		gen.munBounds = (Geometry) features.iterator().next().getDefaultGeometry();
//		
//		Geometry cottbus = (Geometry) feature.getDefaultGeometry();
//		
//		features = reader.readFileAndInitialize("C:/Users/Daniel/Documents/Masterarbeit/inputFiles/osm/landuse.shp");
//		for(SimpleFeature feat : features){
//			Geometry g = (Geometry) feat.getDefaultGeometry();
//			if(cottbus.contains(g)&&feat.getAttribute("type").equals("residential")){
//				gen.residentials.add(g);
//			}
//			if(cottbus.contains(g)&&feat.getAttribute("type").equals("commercial")){
//				gen.commercials.add(g);
//			}
//		}
//		
//		gen.run();
////		gen.filterPopulation();
//		
//	}
//	
//	public CottbusPtPopulationGenerator(Config config, Scenario scenario){
//		this.network = (NetworkImpl) scenario.getNetwork();
//		this.population = scenario.getPopulation();
//		this.config = config;
//		this.transitSchedule = scenario.getTransitSchedule();
//	}
//	
//	public void filterPopulation(){
//		
//		Population filteredPlans = PopulationUtils.createPopulation(ConfigUtils.createConfig());
//		GeometryFactory factory = new GeometryFactory();
//
//		Dijkstra d = new Dijkstra(network, new DistanceAsTravelDisutility(), new FreeSpeedTravelTime());
//		
//		for(Person person : this.population.getPersons().values()){
//			
//			Activity h = (Activity) person.getSelectedPlan().getPlanElements().get(0);
//			Activity w = null;
//				w = (Activity) person.getSelectedPlan().getPlanElements().get(2);
//			Coord home = h.getCoord();
//			Coord work = w.getCoord();
//			Node fromF = this.network.getLinks().get(h.getLinkId()).getFromNode();
//			Node toF = this.network.getLinks().get(h.getLinkId()).getToNode();
//			Node fromT = this.network.getLinks().get(w.getLinkId()).getFromNode();
//			Node toT = this.network.getLinks().get(w.getLinkId()).getToNode();
//			
//			Link from = this.network.getLinks().get(h.getLinkId());
//			Link to = this.network.getLinks().get(w.getLinkId());
//			
//			if(this.munBounds.contains(factory.createPoint(MGC.coord2Coordinate(ct2.transform(home))))&&
//					this.munBounds.contains(factory.createPoint(MGC.coord2Coordinate(ct2.transform(work))))){
//				if(!from.getId().toString().contains("pt")&&!to.getId().toString().contains("pt")){
////				if(!fromF.getId().toString().contains("pt")||!toF.getId().toString().contains("pt")||
////						!fromT.getId().toString().contains("pt")||!toT.getId().toString().contains("pt")){
//					filteredPlans.addPerson(person);
//				}
//			}
//		}
//		
//		PopulationWriter writer = new PopulationWriter(filteredPlans, network);
//		writer.write("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/filtered_population_with_pt.xml");
//		
//	}
//	
//	public void run(){
//		Set<Id<Link>> remove = new HashSet<Id<Link>>();
//		for(Link link : this.network.getLinks().values()){
//			if(link.getFromNode().getId().toString().contains("pt")||link.getToNode().getId().toString().contains("pt")){
//				remove.add(link.getId());
//			}
//		}
//		
//		for(Id<Link> linkId : remove){
//			this.network.removeLink(linkId);
//		}
//		
//		PopulationFactory factory = this.population.getFactory();
//		Population onlyPt = PopulationUtils.createPopulation(ConfigUtils.createConfig());
////		TransitRouterConfig rConfig = new TransitRouterConfig(config);
////		TransitRouterImpl router = new TransitRouterImpl(rConfig, this.transitSchedule);
////		LeastCostPathCalculator router = new Dijkstra(network, new DistanceAsTravelDisutility(), new FreeSpeedTravelTime());
//		
////		int q = 16652;
////		double ttime = 0;
//		int cnt = 0;
//
//		for(int j = 0; j < this.buffers.size(); j++){
//				
//			for(int k = 0; k < this.buffers.size(); k++){
//			
//				for(int i = cnt; i < cnt+666; i++){
//				
//					Person person = factory.createPerson(Id.createPersonId("prt_"+i));
//					Plan plan = factory.createPlan();
//	
//					Activity home = null;
//					Link nearest = null;
//				
//					Random rnd = MatsimRandom.getRandom();
//					
//					List<Geometry> filtered = new ArrayList<Geometry>();
//					
//					for(Geometry g : this.residentials){
//						if(this.buffers.get(j).contains(g)) filtered.add(g);
//					}
//					
//					Geometry h = filtered.get((int) (rnd.nextDouble()*filtered.size()));
//					Coord homeCoord = ct.transform(drawRandomPointFromGeometry(h));
//					home = factory.createActivityFromCoord("home", homeCoord);
//					home.setStartTime(0.);
//					home.setEndTime(setDepartureTime(6*3600, 8d*3600));
//					nearest = this.network.getNearestLinkExactly(homeCoord);
//					((ActivityImpl)home).setLinkId(nearest.getId());
//					
//					Activity work = null;
//					nearest = null;
//					filtered = new ArrayList<Geometry>();
//					for(Geometry g : this.commercials){
//						if(this.buffers.get(k).contains(g)) filtered.add(g);
//					}
//					
//					Coord workCoord = ct.transform(drawRandomPointFromGeometry(filtered.get((int) (rnd.nextDouble()*filtered.size()))));
//					work = factory.createActivityFromCoord("work", workCoord);
//					work.setStartTime(home.getEndTime());
//					work.setEndTime(home.getEndTime() + 8*3600 + 30*60);
//					nearest = this.network.getNearestLinkExactly(workCoord);
//					((ActivityImpl)work).setLinkId(nearest.getId());
//					
//					Activity home2 = factory.createActivityFromCoord("home", home.getCoord());
//					home2.setStartTime(work.getEndTime());
//					home2.setEndTime(24*3600);
//					((ActivityImpl)home2).setLinkId(home.getLinkId());
//					
//					plan.addActivity(home);
//					plan.addLeg(new LegImpl(TransportMode.pt));
//					plan.addActivity(work);
//					plan.addLeg(new LegImpl(TransportMode.pt));
//					plan.addActivity(home2);
//					person.addPlan(plan);
//					population.addPerson(person);
//					onlyPt.addPerson(person);
//					population.getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", "prt");
//					
//				}
//				
//				cnt += 666;
//				
//			}
//			
//		}
////			do{
////				Random rnd = MatsimRandom.getRandom();
////				Geometry h = this.residentials.get((int) (rnd.nextDouble()*this.residentials.size()));
////				Coord homeCoord = ct.transform(drawRandomPointFromGeometry(h));
////				home = factory.createActivityFromCoord("home", homeCoord);
////				home.setStartTime(0.);
////				home.setEndTime(setDepartureTime(6*3600, 8d*3600));
////				nearest = this.network.getNearestLinkExactly(homeCoord);
////				((ActivityImpl)home).setLinkId(nearest.getId());
////			} while(!nearest.getAllowedModes().contains(TransportMode.walk));
//			
////			Activity work = null;
////			nearest = null;
////			List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
////			do{
////				Random rnd = MatsimRandom.getRandom();
////			Geometry buffer = h.buffer(2000);
////			List<Geometry> filtered = new ArrayList<Geometry>();
////			for(Geometry g : this.commercials){
////				if(buffer.contains(g)) filtered.add(g);
////			}
////				Coord workCoord = ct.transform(drawRandomPointFromGeometry(filtered.get((int) (rnd.nextDouble()*filtered.size()))));
////				work = factory.createActivityFromCoord("work", workCoord);
////				work.setStartTime(home.getEndTime());
////				work.setEndTime(home.getEndTime() + 8*3600 + 30*60);
////				nearest = this.network.getNearestLinkExactly(workCoord);
////				((ActivityImpl)work).setLinkId(nearest.getId());
////				Path p = router.calcLeastCostPath(this.network.getLinks().get(home.getLinkId()).getToNode(), nearest.getToNode(), home.getEndTime(), person, null);
////				ttime += p.travelTime;
////				System.out.println(p.travelTime);
////				if(p != null){
////				for(Link link : p.links) linkIds.add(link.getId());
////				}
////			}while(RouteUtils.calcDistance(RouteUtils.createNetworkRoute(linkIds, network), network) > 8000);
//						
////			Activity home2 = factory.createActivityFromCoord("home", home.getCoord());
////			home2.setStartTime(work.getEndTime());
////			home2.setEndTime(24*3600);
////			((ActivityImpl)home2).setLinkId(home.getLinkId());
////			
////			plan.addActivity(home);
////			plan.addLeg(new LegImpl(TransportMode.pt));
////			plan.addActivity(work);
////			plan.addLeg(new LegImpl(TransportMode.pt));
////			plan.addActivity(home2);
////			person.addPlan(plan);
////			population.addPerson(person);
////			onlyPt.addPerson(person);
////			population.getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", "prt");
////			
////		}
////		
////		System.out.println(ttime / (q-1));
//		
//		new ObjectAttributesXmlWriter(population.getPersonAttributes()).writeFile("C:/Users/Daniel/Desktop/dvrp/"
//				+ "cottbus_scenario/personAttributes.xml");
//		
//		PopulationWriter writer = new PopulationWriter(population, network);
//		writer.write("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/population_with_pt.xml");
//		writer = new PopulationWriter(onlyPt, network);
//		writer.write("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/population_only_pt.xml");
//		
//	}
//	
//	private static Coord drawRandomPointFromGeometry(Geometry g) {
//		Random rnd = MatsimRandom.getRandom();
//		Point p;
//		double x, y;
//		boolean in = false;
//		do {
//			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
//			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
//			p = MGC.xy2Point(x, y);
////			for(Geometry ge : buffers){
////				if(ge.contains(p)){
////					in = true;
////					break;
////				}
////			}
//		} while (!g.contains(p));
//		Coord coord = new CoordImpl(p.getX(), p.getY());
//		return coord;
//	}
//	
//	private static double setDepartureTime(double tmin, double tmax){
//		Random rnd = MatsimRandom.getRandom();
//		double t = tmin + rnd.nextDouble() * (tmax - tmin);
//		return t;
//	}

}
