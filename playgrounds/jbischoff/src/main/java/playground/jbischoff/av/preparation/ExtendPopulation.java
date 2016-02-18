package playground.jbischoff.av.preparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class ExtendPopulation {
private Geometry geometry;
private Scenario scenario;	
private Random random = MatsimRandom.getRandom();
private NetworkImpl network;
public static void main(String[] args) {
	ExtendPopulation et = new ExtendPopulation();
	et.run();
}

private void run() {
	this.geometry = readShapeFile("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/shp/Untersuchungsraum.shp");
	Config config = ConfigUtils.createConfig();	
	scenario = ScenarioUtils.createScenario(config);
	new  MatsimPopulationReader(scenario).readFile("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/plansWithCarsR0.10.xml.gz");
	new MatsimNetworkReader(scenario.getNetwork()).readFile("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/networkc.xml.gz");
	this.network = (NetworkImpl) scenario.getNetwork();
	
	Config config2 = ConfigUtils.createConfig();	
	Scenario scenario2 = ScenarioUtils.createScenario(config2);

	
	for (Person p : scenario.getPopulation().getPersons().values()){
		Plan plan = p.getSelectedPlan();
		if (plan.getPlanElements().get(1) instanceof Leg){
			Leg l = (Leg) plan.getPlanElements().get(1) ;
			{
				Activity a1 = (Activity) plan.getPlanElements().get(0);
				Activity a2 = (Activity) plan.getPlanElements().get(2);
				Coord c1 = a1.getCoord();
				if (c1 == null) c1 = scenario.getNetwork().getLinks().get(a1.getLinkId()).getCoord();
				Coord c2 = a2.getCoord();
				if (c2 == null) c2 = scenario.getNetwork().getLinks().get(a2.getLinkId()).getCoord();
				if ((c1==null)||(c2==null)) continue;
				Person p2 = scenario2.getPopulation().getFactory().createPerson(p.getId());
				Plan plan2 = scenario2.getPopulation().getFactory().createPlan();
				p2.addPlan(plan2);
				if (geometry.contains(MGC.coord2Point(c1))&&geometry.contains(MGC.coord2Point(c2))){
					// do nothing, taxi trip within the circle
					scenario2.getPopulation().addPerson(p);

				}
				else if (geometry.contains(MGC.coord2Point(c2))){
					Id<Link> newStart = getNearestInboundHubLinkId(c1);
					Activity newA1 = scenario2.getPopulation().getFactory().createActivityFromLinkId("hub", newStart);
					newA1.setEndTime(a1.getEndTime());
					plan2.addActivity(newA1);
					Leg newLeg = scenario2.getPopulation().getFactory().createLeg("taxi");
					newLeg.setRoute(new GenericRouteImpl(newA1.getLinkId(),a2.getLinkId()));
					plan2.addLeg(newLeg);
					plan2.addActivity(a2);
					scenario2.getPopulation().addPerson(p2);

					
				}else if (geometry.contains(MGC.coord2Point(c1))){
					Id<Link> newDest = getNearestOutboundHubLinkId(c2);
					Activity newA2 = scenario2.getPopulation().getFactory().createActivityFromLinkId("hub", newDest);
					newA2.setStartTime(a2.getStartTime());
					Leg newLeg = scenario2.getPopulation().getFactory().createLeg("taxi");
					newLeg.setRoute(new GenericRouteImpl(a1.getLinkId(),newA2.getLinkId()));
					plan2.addActivity(a1);
					plan2.addLeg(newLeg);
					plan2.addActivity(newA2);


					scenario2.getPopulation().addPerson(p2);
				} else {
//					if (l.getMode().equals("taxi")) scenario2.getPopulation().addPerson(p);
				}
				
				
			}
		
			
		}
		
	}
	
	new PopulationWriter(scenario2.getPopulation()).write("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/subscenarios/mobhubs/plans0.1.xml.gz");
}


private Id<Link> getNearestInboundHubLinkId(Coord c2) {
	Id<Link> w1 = Id.createLinkId(94131);
	Id<Link> s1 = Id.createLinkId(44682);
	Id<Link> n1 = Id.createLinkId(96257);
	Id<Link> e1 = Id.createLinkId(12374);
	List<Link> links = new ArrayList<>();
	links.add(scenario.getNetwork().getLinks().get(e1));
	links.add(scenario.getNetwork().getLinks().get(n1));
	links.add(scenario.getNetwork().getLinks().get(s1));
	links.add(scenario.getNetwork().getLinks().get(w1));
	
	double dist = Double.MAX_VALUE;
	Link bestLink = null;
	for (Link l : links){
		double currentDist = CoordUtils.calcEuclideanDistance(c2, l.getCoord());
		if (currentDist<dist) {
			dist = currentDist;
			bestLink = l;
		} 
	}

	return bestLink.getId();
}

private Id<Link> getNearestOutboundHubLinkId(Coord c1) {
	Id<Link> w2 = Id.createLinkId(108352);
	
	Id<Link> s2 = Id.createLinkId(44683);
	
	Id<Link> n2 = Id.createLinkId(92964);
	
	Id<Link> e2 = Id.createLinkId(12375);
	
	List<Link> links = new ArrayList<>();
	links.add(scenario.getNetwork().getLinks().get(e2));
	links.add(scenario.getNetwork().getLinks().get(n2));
	links.add(scenario.getNetwork().getLinks().get(s2));
	links.add(scenario.getNetwork().getLinks().get(w2));
	
	double dist = Double.MAX_VALUE;
	Link bestLink = null;
	for (Link l : links){
		double currentDist = CoordUtils.calcEuclideanDistance(c1, l.getCoord());
		if (currentDist<dist) {
			dist = currentDist;
			bestLink = l;
		} 
	}

	return bestLink.getId();}

static	Geometry readShapeFile(String filename) {

		Geometry geo = null;
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
			try {
				GeometryFactory geometryFactory = new GeometryFactory();
				WKTReader wktReader = new WKTReader(geometryFactory);

				geo = wktReader.read((ft.getAttribute("the_geom")).toString());

			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return geo;

	}

}
