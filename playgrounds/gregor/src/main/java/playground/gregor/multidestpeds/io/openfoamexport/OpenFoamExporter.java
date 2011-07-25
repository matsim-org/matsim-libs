package playground.gregor.multidestpeds.io.openfoamexport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.multidestpeds.densityestimation.DensityEstimatorFactory;
import playground.gregor.multidestpeds.densityestimation.NNGaussianKernelEstimator;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.events.DoubleValueStringKeyAtCoordinateEvent;
import playground.gregor.sim2d_v2.events.DoubleValueStringKeyAtCoordinateEventHandler;
import playground.gregor.sim2d_v2.events.XYZEventsFileReader;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;


public class OpenFoamExporter implements DoubleValueStringKeyAtCoordinateEventHandler{

	private static final String  CONSTANT = "constant";
	private static final String BOUNDARY_DATA = "boundaryData";
	private static final String POINTS = "points";
	//	private static final String NORTH_PORT ="northPort";?
	//	private static final String WEST_PORT ="westPort";

	//	private static final Map<String,String> groupMapping =

	double currentTime = -1;
	private final Stack<DoubleValueStringKeyAtCoordinateEvent> densities = new Stack<DoubleValueStringKeyAtCoordinateEvent>();
	private final String outputDir;

	private boolean initialized = false;
	private final Map<String, List<Coordinate>> ports;
	private final Map<Coordinate,String> coordPortMapping = new HashMap<Coordinate,String>();
	private final Map<String, String> groupMapping;
	private final Map<String, Set<String>> portGroupMapping;
	private final List<PedestrianGroup> groups;

	public OpenFoamExporter(String outputDir,Map<String,List<Coordinate>> ports, Map<String, String> groupMapping, Map<String, Set<String>> portGroupMapping, List<PedestrianGroup> groups) {
		this.outputDir = outputDir;
		this.ports = ports;
		this.groupMapping = groupMapping;
		this.portGroupMapping = portGroupMapping;
		this.groups = groups;

	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(DoubleValueStringKeyAtCoordinateEvent e) {
		if (this.densities.size() > 0 && e.getTime() > this.currentTime) {
			processFrame(this.densities.peek().getTime());
			this.currentTime = e.getTime();
		} else if (e.getTime() > this.currentTime){
			this.currentTime = e.getTime();
		}
		this.densities.push(e);
	}

	private void processFrame(double time) {
		if (!this.initialized) {
			init();
		}

		String timeString = doubleTimeToFloatString(time);
		for (String port : this.ports.keySet()) {
			initFrameAtPort(port,timeString);
		}
		Map<String,Map<String,Map<Coordinate,Double>>> densities = new HashMap<String, Map<String,Map<Coordinate,Double>>>();
		while (!this.densities.isEmpty()) {
			DoubleValueStringKeyAtCoordinateEvent e = this.densities.pop();
			Coordinate c = e.getCoordinate();
			double rho = e.getValue();
			String key = e.getKey();

			String port = this.coordPortMapping.get(c);
			if (!this.portGroupMapping.get(port).contains(key)){
				continue;
			}
			String group = this.groupMapping.get(key);
			Map<String, Map<Coordinate, Double>> pMap = densities.get(port);
			if (pMap == null){
				pMap = new HashMap<String, Map<Coordinate,Double>>();
				densities.put(port, pMap);
			}
			Map<Coordinate, Double> gMap = pMap.get(group);
			if (gMap == null) {
				gMap = new HashMap<Coordinate, Double>();
				pMap.put(group, gMap);
			}
			gMap.put(c, rho);
		}
		writeDensities(densities,timeString);
	}

	private void writeDensities(
			Map<String, Map<String, Map<Coordinate, Double>>> densities,
			String timeString) {
		for (Entry<String, Map<String, Map<Coordinate, Double>>>  e : densities.entrySet()) {
			String port = e.getKey();
			Map<String, Map<Coordinate, Double>> pMap = e.getValue();
			for ( Entry<String, Map<Coordinate, Double>> ee : pMap.entrySet()) {
				String group = ee.getKey();
				List<Double> rhos = new ArrayList<Double>();
				for (Coordinate c : this.ports.get(port)) {
					rhos.add(ee.getValue().get(c));
				}
				String location =  CONSTANT + "/"+ BOUNDARY_DATA + "/" + port + "/" + timeString;
				try {
					new DensityFileCreator(this.outputDir,location,group,rhos).create();
				} catch (IOException e1) {
					e1.printStackTrace();
					System.exit(-3);
				}
			}

		}

	}

	private void initFrameAtPort(String port, String timeString) {
		String baseDir = this.outputDir + "/" + CONSTANT + "/"+ BOUNDARY_DATA + "/" + port;
		new File(baseDir + "/" + timeString).mkdir();
	}

	private String doubleTimeToFloatString(double time) {
		return Float.toString((float)time);
	}

	private void init() {
		File path = new File(this.outputDir);
		if (path.exists()) {
			try {
				recRm(path);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		path.mkdir();

		//port directories
		for (Entry<String, List<Coordinate>> e : this.ports.entrySet()) {
			new File(this.outputDir + "/" + CONSTANT + "/"+ BOUNDARY_DATA + "/" + e.getKey()).mkdirs();
			try {
				new PointsFileCreator(this.outputDir,  CONSTANT + "/"+ BOUNDARY_DATA + "/" + e.getKey() ,e.getValue()).create();
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(-1);
			}
		}

		//coordinate port mapping
		for (Entry<String,List<Coordinate>> e : this.ports.entrySet()) {
			String port = e.getKey();
			for (Coordinate c : e.getValue()) {
				this.coordPortMapping.put(c, port);
			}
		}

		// pedestrianProperties
		try {
			new PedestrianPropertiesFileCreator(this.outputDir,CONSTANT,this.groups).create();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		this.initialized = true;
	}


	private void recRm(File path) throws IOException
	{
		if (path.isDirectory()) {
			for (File child : path.listFiles()) {
				recRm(child);
			}
		}
		if (!path.delete()) {
			throw new IOException("Could not delete " + path);
		}
	}

	@Deprecated
	// should be configured in an OpenFoamConfigGroup
	public static void main(String [] args) {

		String events = "/Users/laemmel/devel/dfg/events.xml";
		String outputDir = "/Users/laemmel/tmp/openfoam";

		Map<String,List<Coordinate>> ports = new HashMap<String,List<Coordinate>>();
		List<Coordinate> queryCoords = new ArrayList<Coordinate>();

		//northPort
		queryCoords.add(new Coordinate(-.5,-1,0));
		queryCoords.add(new Coordinate(-.5,-1.5,0));
		queryCoords.add(new Coordinate(-.5,-2,0));
		queryCoords.add(new Coordinate(-.5,-2.5,0));
		ports.put("northPort", new ArrayList<Coordinate>(queryCoords));

		//westPort
		queryCoords.add(new Coordinate(1,-4,0));
		queryCoords.add(new Coordinate(1.5,-4,0));
		queryCoords.add(new Coordinate(2,-4,0));
		queryCoords.add(new Coordinate(2.5,-4,0));
		ports.put("westPort", new ArrayList<Coordinate>(queryCoords.subList(4, 8)));


		//group mapping
		Map<String,String> groupMapping = new HashMap<String,String>();
		groupMapping.put("g", "ped1Rho");
		groupMapping.put("r", "ped2Rho");

		//group port mapping
		Map<String,Set<String>> portGroupMapping = new HashMap<String,Set<String>>();
		HashSet<String> set1 = new HashSet<String>();
		set1.add("g");
		portGroupMapping.put("westPort", set1);
		HashSet<String> set2 = new HashSet<String>();
		set2.add("r");
		portGroupMapping.put("northPort", set2);


		//pedestrian groups
		List<PedestrianGroup> groups = new ArrayList<PedestrianGroup>();
		PedestrianGroup grp1 = new PedestrianGroup("ped1");
		grp1.setOrigin("westPort", new Coordinate(1,0,0));
		grp1.addDestination("eastPort", -100, new Coordinate(1,0,0));
		groups.add(grp1);
		PedestrianGroup grp2 = new PedestrianGroup("ped2");
		grp2.setOrigin("northPort", new Coordinate(0,-1,0));
		grp2.addDestination("southPort", -100, new Coordinate(0,-1,0));
		groups.add(grp2);

		OpenFoamExporter exporter = new OpenFoamExporter(outputDir,ports,groupMapping,portGroupMapping,groups);

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(exporter);

		Config c = ConfigUtils.loadConfig("/Users/laemmel/devel/dfg/config2d.xml");
		initSim2dConfig(c);
		Scenario sc = ScenarioUtils.loadScenario(c);
		new ScenarioLoader2DImpl(sc).load2DScenario();




		DensityEstimatorFactory fac = new DensityEstimatorFactory(manager, sc);
		NNGaussianKernelEstimator densityEst = fac.createDensityEstimator(queryCoords);
		manager.addHandler(densityEst);

		new XYZEventsFileReader(manager).parse(events);
	}

	private static void initSim2dConfig(Config c) {
		Module module = c.getModule("sim2d");
		Sim2DConfigGroup s = null;
		if (module == null) {
			s = new Sim2DConfigGroup();
		} else {
			s = new Sim2DConfigGroup(module);
		}
		c.getModules().put("sim2d", s);

	}

}
