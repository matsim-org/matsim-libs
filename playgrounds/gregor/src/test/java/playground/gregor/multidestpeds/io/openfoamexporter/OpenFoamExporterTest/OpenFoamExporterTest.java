package playground.gregor.multidestpeds.io.openfoamexporter.OpenFoamExporterTest;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.multidestpeds.io.openfoamexport.OpenFoamExporter;
import playground.gregor.multidestpeds.io.openfoamexport.PedestrianGroup;
import playground.gregor.sim2d_v2.events.DoubleValueStringKeyAtCoordinateEvent;

public class OpenFoamExporterTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testOpenFoamExporter(){
		String outputDir = this.utils.getOutputDirectory();
		String inputDir = this.utils.getInputDirectory();

		Map<String,List<Coordinate>> ports = new HashMap<String,List<Coordinate>>();
		List<Coordinate> queryCoords = new ArrayList<Coordinate>();

		//southPort
		queryCoords.add(new Coordinate(-.1,-1,0));
		queryCoords.add(new Coordinate(.1,-1,0));
		queryCoords.add(new Coordinate(-.1,-1,.1));
		queryCoords.add(new Coordinate(.1,-1,.1));
		ports.put("southPort",new ArrayList<Coordinate>(queryCoords));

		//eastPort
		queryCoords.add(new Coordinate(1,0.45,0));
		queryCoords.add(new Coordinate(1,0.55,0));
		queryCoords.add(new Coordinate(1,0.45,.1));
		queryCoords.add(new Coordinate(1,0.55,.1));
		ports.put("eastPort",new ArrayList<Coordinate>(queryCoords.subList(4, 8)));

		//group mapping
		Map<String,String> groupMapping = new HashMap<String,String>();
		groupMapping.put("g", "ped1Rho");
		groupMapping.put("r", "ped2Rho");

		//group port mapping
		Map<String,Set<String>> portGroupMapping = new HashMap<String,Set<String>>();
		HashSet<String> set1 = new HashSet<String>();
		set1.add("g");
		portGroupMapping.put("southPort", set1);
		HashSet<String> set2 = new HashSet<String>();
		set2.add("r");
		portGroupMapping.put("eastPort", set2);

		//pedestrian groups
		List<PedestrianGroup> groups = new ArrayList<PedestrianGroup>();
		PedestrianGroup grp1 = new PedestrianGroup("ped1");
		grp1.setOrigin("southPort", new Coordinate(0,1,0));
		grp1.addDestination("eastPort", -100, new Coordinate(1,0,0));
		grp1.addDestination("westPort", -100, new Coordinate(-1,0,0));
		groups.add(grp1);
		PedestrianGroup grp2 = new PedestrianGroup("ped2");
		grp2.setOrigin("eastPort", new Coordinate(-1,0,0));
		grp2.addDestination("westPort", -100, new Coordinate(-1,0,0));
		groups.add(grp2);


		OpenFoamExporter ofe = new OpenFoamExporter(outputDir, ports, groupMapping, portGroupMapping, groups);

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(ofe);

		emulateDensityEvents(manager,queryCoords);

		assertEquals("different pedestrian properties file.",CRCChecksum.getCRCFromFile(inputDir + "/constant/pedestrianProperties"), CRCChecksum.getCRCFromFile(outputDir  + "/constant/pedestrianProperties"));
		assertEquals("different density file",CRCChecksum.getCRCFromFile(inputDir + "/constant/boundaryData/eastPort/0.0/ped2Rho"), CRCChecksum.getCRCFromFile(outputDir  + "/constant/boundaryData/eastPort/0.0/ped2Rho"));
		assertEquals("different density file",CRCChecksum.getCRCFromFile(inputDir + "/constant/boundaryData/eastPort/0.99999/ped2Rho"), CRCChecksum.getCRCFromFile(outputDir  + "/constant/boundaryData/eastPort/0.99999/ped2Rho"));
		assertEquals("different density file",CRCChecksum.getCRCFromFile(inputDir + "/constant/boundaryData/eastPort/1.0/ped2Rho"), CRCChecksum.getCRCFromFile(outputDir  + "/constant/boundaryData/eastPort/1.0/ped2Rho"));
		assertEquals("different pointa file",CRCChecksum.getCRCFromFile(inputDir + "/constant/boundaryData/eastPort/points"), CRCChecksum.getCRCFromFile(outputDir  + "/constant/boundaryData/eastPort/points"));

		assertEquals("different density file",CRCChecksum.getCRCFromFile(inputDir + "/constant/boundaryData/southPort/0.0/ped1Rho"), CRCChecksum.getCRCFromFile(outputDir  + "/constant/boundaryData/southPort/0.0/ped1Rho"));
		assertEquals("different density file",CRCChecksum.getCRCFromFile(inputDir + "/constant/boundaryData/southPort/0.99999/ped1Rho"), CRCChecksum.getCRCFromFile(outputDir  + "/constant/boundaryData/southPort/0.99999/ped1Rho"));
		assertEquals("different density file",CRCChecksum.getCRCFromFile(inputDir + "/constant/boundaryData/southPort/1.0/ped1Rho"), CRCChecksum.getCRCFromFile(outputDir  + "/constant/boundaryData/southPort/1.0/ped1Rho"));
		assertEquals("different pointa file",CRCChecksum.getCRCFromFile(inputDir + "/constant/boundaryData/southPort/points"), CRCChecksum.getCRCFromFile(outputDir  + "/constant/boundaryData/southPort/points"));

	}

	private void emulateDensityEvents(EventsManager manager,
			List<Coordinate> queryCoords) {
		//time 0
		//team green
		for (int i = 0; i < 4; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), 1, "g", 0);
			manager.processEvent(e);
		}
		for (int i = 4; i < 8; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), .1, "g", 0);
			manager.processEvent(e);
		}
		//team red
		for (int i = 0; i < 4; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), .1, "r", 0);
			manager.processEvent(e);
		}
		for (int i = 4; i < 8; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), 1, "r", 0);
			manager.processEvent(e);
		}


		//time 0.99999
		//team green
		for (int i = 0; i < 4; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), 1, "g", .99999);
			manager.processEvent(e);
		}
		for (int i = 4; i < 8; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), .1, "g", .99999);
			manager.processEvent(e);
		}
		//team red
		for (int i = 0; i < 4; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), .1, "r", .99999);
			manager.processEvent(e);
		}
		for (int i = 4; i < 8; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), 1, "r", .99999);
			manager.processEvent(e);
		}


		//time 1
		//team green
		for (int i = 0; i < 4; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), 0, "g", 1);
			manager.processEvent(e);
		}
		for (int i = 4; i < 8; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), 0, "g", 1);
			manager.processEvent(e);
		}
		//team red
		for (int i = 0; i < 4; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), 0, "r", 1);
			manager.processEvent(e);
		}
		for (int i = 4; i < 8; i++) {
			Event e = new DoubleValueStringKeyAtCoordinateEvent(queryCoords.get(i), 0, "r", 1);
			manager.processEvent(e);
		}

		Event e = new DoubleValueStringKeyAtCoordinateEvent(new Coordinate(), 0, "dummy", 9999);
		manager.processEvent(e);

	}


}
