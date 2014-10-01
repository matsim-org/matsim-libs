package playground.southafrica.freight.digicore.analysis.geographicClassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;

import com.vividsolutions.jts.geom.MultiPolygon;


public class GeographicClassifierCallable implements Callable<Tuple<Id<Vehicle>, Integer>> {
	private File vehicleFile;
	private final double threshold;
	private MultiPolygon area;
	private final Counter counter;
	Map<String, List<Id<Vehicle>>> lists;
	
	public GeographicClassifierCallable(File vehicleFile, MultiPolygon area, double threshold, Counter threadCounter){
		this.vehicleFile = vehicleFile;
		this.area = area;
		this.threshold = threshold;
		this.counter = threadCounter;
		
		this.lists = new HashMap<String, List<Id<Vehicle>>>();
		lists.put("intra", new ArrayList<Id<Vehicle>>());
		lists.put("inter", new ArrayList<Id<Vehicle>>());
		lists.put("extra", new ArrayList<Id<Vehicle>>());
	}
	
	public Tuple<Id<Vehicle>, Integer> call() throws Exception{
		/* Read the Digicore vehicle file. */
		DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
		dvr.parse(this.vehicleFile.getAbsolutePath());
		DigicoreVehicle vehicle = dvr.getVehicle();

		/* Determine if intra, inter, or extra. */
		int vehicleType = vehicle.determineIfIntraInterExtraVehicle(area, threshold);

		counter.incCounter();
		return new Tuple<Id<Vehicle>, Integer>(vehicle.getId(), new Integer(vehicleType));
	}
}
