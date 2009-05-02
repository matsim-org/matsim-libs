package playground.gregor.gis.gistutorial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.PopulationBuilder;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.world.World;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class DemandGenerator {

	private static int ID = 0;

	public static void main(String [] args) throws IOException {
		
		//out input files
		String netFile = "./inputs/network.xml";
		String zonesFile = "./inputs/zones.shp";
		
		
		//the following 2 lines create a new network layer and reads the links and nodes from network.xml into this network layer
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFile);

		//we need a world object - this will change in future, thats why this methods a marked as deprecated
		World world = Gbl.createWorld();
		world.setNetworkLayer(net);
		
		FeatureSource fts = ShapeFileReader.readDataFile(zonesFile); //reads the shape file in

		
		//lists to classify links into recreation and commercial links
		List<Link> recreation = new ArrayList<Link>(); 
		List<Link> commercial = new ArrayList<Link>();
		
		//a new empty population
		Population pop = new PopulationImpl();

	
		//Iterator to iterate over the features from the shape file
		Iterator<Feature> it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = it.next(); //A feature contains a geometry (in this case a polygon) and an arbitrary number
			//of other attributes
			if (((String)ft.getAttribute("type")).equals("commercial")) {
				List<Link> links = getLinks(ft,net); //gets all link within the corresponding polygon ...
				commercial.addAll(links); // ... and adds them to out "commercial" list
			} else if (((String)ft.getAttribute("type")).equals("recreation")) {
				recreation.addAll(getLinks(ft,net));
			} else if (((String)ft.getAttribute("type")).equals("housing")) {
				List<Link> links = getLinks(ft, net);
				long l = ((Long)ft.getAttribute("inhabitant"));
				createPersons(pop,links,(int)l); //creates l new persons and chooses for each person a random link
				//within the corresponding polygon. after this method call every new generated person has one plan and one home activity
				
			} else {
				throw new RuntimeException("Unknown zone type:" + ft.getAttribute("type"));
			}
		}
		createActivities(pop,recreation,commercial); //this method creates the remaining activities

		new PopulationWriter(pop,"./inputs/population.xml").write(); // and finally the population will be written to a xml file

	}

	private static void createActivities(Population pop, List<Link> recreation,
			List<Link> commercial) {
		
		PopulationBuilder pb =pop.getPopulationBuilder(); //the population builder creates all we need 
		for (Person pers : pop.getPersons().values()) { //this loop iterates over all persons
			
			Plan plan = pers.getRandomPlan(); //each person has exactly one plan, that has been created in createPersons(...)
			Activity homeAct = plan.getFirstActivity(); //every plan has only one activity so far (home activity)
			homeAct.setEndTime(7*3600); // sets the endtime of this activity to 7 am

			Leg leg = pb.createLeg(TransportMode.car);
			plan.addLeg(leg); // there needs to be a log between two activities
			
			
			//work activity on a random link within one of the commercial areas
			Link wLink = commercial.get(MatsimRandom.getRandom().nextInt(commercial.size()));
			Activity work = pb.createActivityFromLinkId("w",wLink.getId());
			double startTime = 8*3600;
			work.setStartTime(startTime);
			work.setEndTime(startTime + 6*3600);
			plan.addActivity(work);

			plan.addLeg(pb.createLeg(TransportMode.car));
			
			//recreation activity on a random link within one of the recreation area 
			Link lLink = recreation.get(MatsimRandom.getRandom().nextInt(recreation.size()));
			Activity leisure = pb.createActivityFromLinkId("l",lLink.getId());
			leisure.setEndTime(3600*19);
			plan.addActivity(leisure);

			plan.addLeg(pb.createLeg(TransportMode.car));
			
			//finally the second home activity - it is clear that this activity needs to be on the same link
			//as the first activity - since in this tutorial our agents do not relocate ;-)
			Activity homeActII = pb.createActivityFromLinkId("h",homeAct.getLink().getId());
			plan.addActivity(homeActII);

		}

	}

	private static void createPersons(Population pop, List<Link> links, int number) {
		PopulationBuilder pb =pop.getPopulationBuilder();
		for (; number > 0; number--) {
			
			Person pers = pb.createPerson(new IdImpl(ID++));
			Plan plan = pb.createPlan(pers);

			int idx = MatsimRandom.getRandom().nextInt(links.size());
			Link link = links.get(idx);
			Activity act = pb.createActivityFromLinkId("h", link.getId());
			plan.addActivity(act);
			pers.addPlan(plan);
			pop.addPerson(pers);
		}

	}

	private static List<Link> getLinks(Feature ft, NetworkLayer net) {
		List<Link> ret = new ArrayList<Link>();
		Geometry geo = ft.getDefaultGeometry();

		for (Link link : net.getLinks().values()) {
			Point p = MGC.coord2Point(link.getCoord());
			if (geo.contains(p)) {
				ret.add(link);
			}
		}

		return ret;
	}

}
