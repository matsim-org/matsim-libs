package saleem.stockholmscenario.GTFSData;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.sergioo.gtfs2PTSchedule2011.GTFS2MATSimTransitSchedule;





public class RouteGenerator {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		(new MatsimNetworkReader(scenario)).readFile("H:\\Matsim\\GTFS Hypothetical\\network.xml");
		Network network = scenario.getNetwork();
		GTFS2MATSimTransitSchedule g2m = new GTFS2MATSimTransitSchedule(new File[]{new File("H:\\Matsim\\GTFS Hypothetical\\buses")}, new String[]{"car"}, network, new String[]{"weekday","weeksatday","daily"}, TransformationFactory.WGS84_SVY21);
		// TODO Auto-generated method stub
		TransitSchedule outputSchedule = g2m.getTransitSchedule();
	}

}
