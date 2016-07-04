package playground.gthunig.mrOptiCheck;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.util.CSVReaders;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import playground.gthunig.utils.StopWatch;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gthunig on 04.07.2016.
 */
public class MNDOptiCheck {

	public static void main(String[] args) {

		String networkFile = "../../../runs-svn/nmbm_minibuses/nmbm/output/jtlu14b/jtlu14b.output_network.xml";
		String transitScheduleFile = "../../../runs-svn/nmbm_minibuses/nmbm/output/jtlu14b/ITERS/it.300/jtlu14b.300.transitScheduleScored.xml";
		String stopsFile = "../../../shared-svn/projects/maxess/data/nmb/minibus-pt/jtlu14b/matrix_grid_1000/ptStops.csv";

		Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getConfig().transit().setUseTransit(true);

		// Read in public transport schedule
		TransitScheduleReader reader = new TransitScheduleReader(scenario);
		reader.readFile(transitScheduleFile);
		TransitSchedule transitSchedule = scenario.getTransitSchedule();

		// constructor of TransitRouterImpl needs TransitRouterConfig. This is why it is instantiated here.
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig());
		TransitRouter transitRouter = new TransitRouterImpl(transitRouterConfig, transitSchedule);

		Double departureTime = 8. * 60 * 60;

		List<String[]> stopsLines = CSVReaders.readCSV(stopsFile);
		stopsLines.remove(0);
		List<Coord> coords = new ArrayList<>();
		for (int i = 0; i < stopsLines.size(); i++) {
			if (i % 64 == 0) {
				coords.add(new Coord(Double.valueOf(stopsLines.get(i)[1]), Double.valueOf(stopsLines.get(i)[2])));
			}
		}
		System.out.println("#Coords = " + coords.size());
		StopWatch stopWatch = new StopWatch();
		for (Coord origin : coords) {
			for (Coord destination : coords) {
				transitRouter.calcRoute(new FakeFacility(origin), new FakeFacility(destination), departureTime, null);
			}
		}
		System.out.println(stopWatch.getElapsedTime());
	}
}
