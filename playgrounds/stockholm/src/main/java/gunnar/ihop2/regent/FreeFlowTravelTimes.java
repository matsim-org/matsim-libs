package gunnar.ihop2.regent;

import gunnar.ihop2.regent.costwriting.TravelTimeMatrices;
import gunnar.ihop2.regent.demandreading.ZonalSystem;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FreeFlowTravelTimes {

	public FreeFlowTravelTimes() {
	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String networkFileName = "./test/matsim-testrun/input/network-plain.xml";
		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", networkFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final String zonesShapeFileName = "./test/regentmatsim/input/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);

		final int startTime_s = 0;
		final int binSize_s = 3600;
		final int binCnt = 1;
		final int sampleCnt = 1;

		final TravelTime linkTTs = new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time,
					Person person, Vehicle vehicle) {
				return link.getLength() / link.getFreespeed();
			}			
		};

		final TravelTimeMatrices travelTimeMatrices = new TravelTimeMatrices(
				scenario.getNetwork(), linkTTs,
				// null,
				zonalSystem, new Random(), startTime_s, binSize_s, binCnt,
				sampleCnt);
		travelTimeMatrices
				.writeToFile("./test/matsim-testrun/freeflow-traveltimes.xml");

		System.out.println("... DONE");
	}
	
}
