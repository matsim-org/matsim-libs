package playground.mzilske.latitude;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.mzilske.osm.JXMapOTFVisClient;

import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.latitude.Latitude;
import com.google.api.services.latitude.model.Location;


public class HelloLatitude {

	private static final String COORDINATE_SYSTEM = TransformationFactory.DHDN_GK4;
	private static CoordinateTransformation t = TransformationFactory.getCoordinateTransformation("WGS84", COORDINATE_SYSTEM);

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(COORDINATE_SYSTEM);
		config.otfVis().setMapOverlayMode(true);
		MatsimRandom.reset(config.global().getRandomSeed());
		if (config.getQSimConfigGroup() == null) {
			config.addQSimConfigGroup(new QSimConfigGroup());
		}
		Scenario scenario = ScenarioUtils.createScenario(config);

		getLatitude(scenario);
		play(scenario);


	}

	private static void getLatitude(Scenario scenario) {
		String SCOPE = "https://www.googleapis.com/auth/latitude.all.best";
		JacksonFactory jsonFactory = new JacksonFactory();
		HttpTransport transport = new NetHttpTransport();
		OAuth2ClientCredentials.errorIfNotSpecified();
		GoogleAccessProtectedResource accessProtectedResource;
		try {
			accessProtectedResource = OAuth2Native.authorize(transport,
					jsonFactory,
					new LocalServerReceiver(),
					null,
					"google-chrome",
					OAuth2ClientCredentials.CLIENT_ID,
					OAuth2ClientCredentials.CLIENT_SECRET,
					SCOPE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Latitude latitude = new Latitude(new NetHttpTransport(), accessProtectedResource, jsonFactory);
		try {

			List<Location> locations = latitude.location.list().setGranularity("best").execute().getItems();
			filterLocations(locations);
			createLinks(scenario, locations);
			createActivities(scenario, locations);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}

	private static void filterLocations(List<Location> locations) {
		Iterator<Location> i = locations.iterator();
		while (i.hasNext()) {
			Location l = i.next();
			if (l.getAccuracy() == null) {
				i.remove();
			}
		}
	}

	private static void createLinks(Scenario scenario, List<Location> locations) {
		Coord prev = null;
		Node prevNode = null;
		for (Location location : locations) {
			System.out.println(location + " " + new Date(Long.parseLong((String) location.getTimestampMs())));
			Coord coord = getCoord(location);
			Node node = scenario.getNetwork().getFactory().createNode(new IdImpl((String) location.getTimestampMs()), coord);
			scenario.getNetwork().addNode(node);
			System.out.println(coord);
			if (prev != null) {
				System.out.println(CoordUtils.calcDistance(prev, coord));
				Link link = scenario.getNetwork().getFactory().createLink(new IdImpl(prevNode.getId().toString() + node.getId().toString()), prevNode, node);
				scenario.getNetwork().addLink(link);
			}
			prev = coord;
			prevNode = node;
		}
	}

	private static void createActivities(Scenario scenario, List<Location> locations) {
		Coord prev = null;
		Person p = scenario.getPopulation().getFactory().createPerson(new IdImpl("p"));
		Plan pl = scenario.getPopulation().getFactory().createPlan();
		for (Location location : locations) {
			System.out.println(location + " " + new Date(Long.parseLong((String) location.getTimestampMs())));
			Coord coord = getCoord(location);
			Activity activity = scenario.getPopulation().getFactory().createActivityFromCoord("unknown", coord);
			pl.addActivity(activity);
			System.out.println(coord);
			if (prev != null) {
				System.out.println(CoordUtils.calcDistance(prev, coord));
				Leg leg = scenario.getPopulation().getFactory().createLeg("unknown");
				pl.addLeg(leg);
			}
			prev = coord;
		}
		p.addPlan(pl);
		scenario.getPopulation().addPerson(p);
	}

	private static Coord getCoord(Location location) {
		return t.transform(new CoordImpl(((BigDecimal) location.getLongitude()).doubleValue(), ((BigDecimal) location.getLatitude()).doubleValue()));
	}

	private static void play(Scenario scenario) {
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		JXMapOTFVisClient.run(scenario.getConfig(), server);
		qSim.run();
	}

}
