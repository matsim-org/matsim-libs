package playground.mzilske.latitude;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
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
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:32633");
		MatsimRandom.reset(config.global().getRandomSeed());
		if (config.getQSimConfigGroup() == null) {
			config.addQSimConfigGroup(new QSimConfigGroup());
		}
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		getLatitude(scenario);
//		play(scenario);
		
		
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
			for (Location location : locations) {
				System.out.println(location);
			}
			CoordinateTransformation t = TransformationFactory.getCoordinateTransformation("WGS84", "EPSG:32633");
			Node n1 = scenario.getNetwork().getFactory().createNode(new IdImpl("first"), t.transform(new CoordImpl(((BigDecimal) locations.get(0).getLongitude()).doubleValue(), ((BigDecimal) locations.get(0).getLatitude()).doubleValue())));
			Node n2 = scenario.getNetwork().getFactory().createNode(new IdImpl("last"), t.transform(new CoordImpl(((BigDecimal) locations.get(locations.size()-1).getLongitude()).doubleValue(), ((BigDecimal) locations.get(locations.size()-1).getLatitude()).doubleValue())));
			scenario.getNetwork().addNode(n1);
			scenario.getNetwork().addNode(n2);
			Link l = scenario.getNetwork().getFactory().createLink(new IdImpl("link"), n1, n2);
			scenario.getNetwork().addLink(l);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	private static void play(Scenario scenario) {
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		JXMapOTFVisClient.run(scenario.getConfig(), server);
		qSim.run();
	}

}
