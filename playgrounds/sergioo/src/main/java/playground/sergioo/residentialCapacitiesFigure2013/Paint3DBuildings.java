package playground.sergioo.residentialCapacitiesFigure2013;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilitiesFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityOption;

import playground.sergioo.workplaceCapacities2012.gui.BSSimpleNetworkWindow;
import playground.sergioo.workplaceCapacities2012.gui.WorkersBSPainter;

public class Paint3DBuildings {

	private static final String SEPARATOR = ";";

	//Main
	/**
	 * 
	 * @param args
	 * 0 - Network file
	 * 1 - CSV file with residential capacities
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String[] schedules = {"home"};
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		ActivityFacilitiesFactory factory = new ActivityFacilitiesFactoryImpl();
		BufferedReader reader = new BufferedReader(new FileReader(args[1]));
		reader.readLine();
		String line = reader.readLine();
		Map<Id<ActivityFacility>, String> types = new HashMap<Id<ActivityFacility>, String>();
		long i = 0;
		while(line!=null) {
			String[] parts = line.split(SEPARATOR);
			Id<ActivityFacility> id = Id.create(i, ActivityFacility.class);
			types.put(id, parts[0]);
			ActivityFacility facility = factory.createActivityFacility(id, transformation.transform(new CoordImpl(parts[2], parts[1])));
			ActivityOption option = factory.createActivityOption("home");
			option.setCapacity(new Double(parts[3]));
			facility.addActivityOption(option);
			scenario.getActivityFacilities().addActivityFacility(facility);
			line = reader.readLine();
			i++;
		}
		reader.close();
		WorkersBSPainter painter = new WorkersBSPainter(scenario.getNetwork(), 40);
		painter.setData(scenario.getActivityFacilities(), schedules, types);
		new BSSimpleNetworkWindow("Building capacities", painter).setVisible(true);
	}

}
