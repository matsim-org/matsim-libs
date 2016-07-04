package playground.gthunig.mrOptiCheck;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.transitSchedule.api.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author gthunig on 04.07.2016.
 */
public class MNDOptiCheck {

	public static void main(String[] args) {

		File file = new File("../../../runs-svn/nmbm_minibuses/nmbm/output/jtlu14b/ITERS/it.300/jtlu14b.output_network.xml");
		System.out.println("file.getAbsolutePath() = " + file.getAbsolutePath());
		try {
			System.out.println("file.getCanonicalPath() = " + file.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

//        String networkFile = "playgrounds/dziemke/input/NMBM_PT_V1.xml";
//        String transitScheduleFile = "playgrounds/dziemke/input/Transitschedule_PT_V1_WithVehicles.xml";
		String networkFile = "../../../runs-svn/nmbm_minibuses/nmbm/output/jtlu14b/ITERS/it.300/jtlu14b.output_network.xml";
		String transitScheduleFile = "../../../runs-svn/nmbm_minibuses/nmbm/output/jtlu14b/ITERS/it.300/jtlu14b.300.transitScheduleScored.xml";

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
//        Coord origin = new Coord(137547.07266149623,-3706738.5909946687);
//        Coord destination = new Coord(140245.15520623303,-3693657.6437037485);
//        Coord origin = new Coord(143583.9441831379, -3699678.99131796);jtlu14b
//        Coord destination = new Coord(150583.9441831379,-3699678.99131796);
		Coord origin = new Coord(111583.94418313791,-3714678.99131796);
		Coord destination = new Coord(153583.9441831379,-3688678.99131796);

		System.out.println("StartLink Alberts origin " + origin);

		List<Leg> legList = transitRouter.calcRoute(new FakeFacility(origin), new FakeFacility(destination), departureTime, null);
		ArrayList<Coord> legEnds = new ArrayList<>();
		ArrayList<Coord> ptStops = new ArrayList<>();
		legEnds.add(origin);


		legEnds.add(destination);
		System.out.println("StartLink Alberts origin " + destination);

	}

	private static Coord invertCoord(Coord coord, CoordinateTransformation coordinateTransformation) {
		return new Coord(coordinateTransformation.transform(coord).getY(),
				coordinateTransformation.transform(coord).getX());
	}
}
