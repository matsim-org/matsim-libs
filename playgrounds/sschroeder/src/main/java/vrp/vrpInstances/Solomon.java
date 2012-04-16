package vrp.vrpInstances;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.ChartListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.DistributionTourWithTimeWindowsAlgoFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.InitialSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreate;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateReport;
import org.matsim.contrib.freight.vrp.basics.Coordinate;
import org.matsim.contrib.freight.vrp.basics.CrowFlyCosts;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Locations;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VrpBuilder;
import org.matsim.contrib.freight.vrp.basics.VrpUtils;
import org.matsim.contrib.freight.vrp.constraints.PickORDeliveryCapacityAndTWConstraint;
import org.matsim.core.utils.io.IOUtils;

/**
 * test instances for the capacitated vrp with time windows. instances are from solomon
 * and can be found at:
 * http://neo.lcc.uma.es/radi-aeb/WebVRP/
 * @author stefan schroeder
 *
 */



public class Solomon {

	
	static class MyLocations implements Locations{

		private Map<String,Coordinate> locations = new HashMap<String, Coordinate>();

		public void addLocation(String id, Coordinate coord){
			locations.put(id, coord);
		}

		@Override
		public Coordinate getCoord(String id) {
			return locations.get(id);
		}
	}

	private static Logger logger = Logger.getLogger(Christophides.class);

	private VrpBuilder vrpBuilder;

	//private Locations locations;

	private String fileNameOfInstance;

	private int vehicleCapacity;

	private String depotId;

	private String instanceName;


	public Solomon(String fileNameOfInstance, String instanceName) {
		super();
		this.fileNameOfInstance = fileNameOfInstance;
		this.instanceName = instanceName;
	}
	
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.INFO);
		Solomon solomon = new Solomon("/Users/schroeder/Documents/diss/instances/solomon_100/C101.txt", "100_C103");
		solomon.run();

	}

	public void run(){
		MyLocations myLocations = new MyLocations();
		Collection<Job> jobs = new ArrayList<Job>();
		readLocationsAndJobs(myLocations,jobs);
		VrpBuilder vrpBuilder = new VrpBuilder(new CrowFlyCosts(myLocations), new PickORDeliveryCapacityAndTWConstraint());
		for(Job j : jobs){
			vrpBuilder.addJob(j);
		}
		for(int i=0;i<20;i++){
			vrpBuilder.addVehicle(VrpUtils.createVehicle("" + (i+1), depotId, vehicleCapacity));
		}
		RuinAndRecreate algo = createAlgo(vrpBuilder.build());
		algo.run();
	}
	

	private RuinAndRecreate createAlgo(VehicleRoutingProblem vrp) {
		RuinAndRecreateFactory factory = new DistributionTourWithTimeWindowsAlgoFactory();
		factory.setIterations(5000);
		factory.setWarmUp(4000);
		ChartListener chartListener = new ChartListener();
		chartListener.setFilename("output/"+instanceName+".png");
		RuinAndRecreateReport report = new RuinAndRecreateReport();
//		factory.addRuinAndRecreateListener(chartListener);
		factory.addRuinAndRecreateListener(report);
		return factory.createAlgorithm(vrp, new InitialSolution().createInitialSolution(vrp));
	}
	
	private void readLocationsAndJobs(MyLocations locations, Collection<Job> jobs){
		BufferedReader reader = IOUtils.getBufferedReader(fileNameOfInstance);
		String line = null;
		int counter = 0;
		try {
			while((line = reader.readLine()) != null){
				line = line.replace("\r", "");
				line = line.trim();
				String[] tokens = line.split(" +");
				counter++;
				if(counter == 5){
					int vehicleCap = Integer.parseInt(tokens[1]);
					this.vehicleCapacity = vehicleCap;
					continue;
				}
				
				if(counter > 9){
					Coordinate coord = makeCoord(tokens[1],tokens[2]);
					double depotStart = 0.0;
					double depotEnd = Double.MAX_VALUE;
					String customerId = tokens[0];
					locations.addLocation(customerId, coord);
					int demand = Integer.parseInt(tokens[3]);
					double start = Double.parseDouble(tokens[4]);
					double end = Double.parseDouble(tokens[5]);
					double serviceTime = Double.parseDouble(tokens[6]);
					if(counter == 10){
						depotStart = start;
						depotEnd = end;
						depotId = tokens[0];
					}
					else{
						Shipment shipment = VrpUtils.createShipment("" + counter, depotId, customerId, demand, 
								VrpUtils.createTimeWindow(depotStart, depotEnd), VrpUtils.createTimeWindow(start, end));
						shipment.setDeliveryServiceTime(serviceTime);
						jobs.add(shipment);
					}
				}
			}
			reader.close();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Coordinate makeCoord(String xString, String yString) {
		double x = Double.parseDouble(xString);
		double y = Double.parseDouble(yString);
		return new Coordinate(x,y);
	}

}
