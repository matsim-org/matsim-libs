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
import org.matsim.contrib.freight.vrp.algorithms.rr.InitialSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreate;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateReport;
import org.matsim.contrib.freight.vrp.algorithms.rr.factories.DistributionTourAlgoFactory;
import org.matsim.contrib.freight.vrp.basics.Coordinate;
import org.matsim.contrib.freight.vrp.basics.CrowFlyCosts;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Locations;
import org.matsim.contrib.freight.vrp.basics.PickORDeliveryCapacityAndTWConstraint;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VrpBuilder;
import org.matsim.contrib.freight.vrp.basics.VrpUtils;
import org.matsim.core.utils.io.IOUtils;


/**
 * test instances for the capacitated vrp. instances are from christophides, mingozzi and toth
 * and can be found at:
 * http://neo.lcc.uma.es/radi-aeb/WebVRP/
 * @author stefan schroeder
 *
 */

public class Christophides {
	
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
	
//	private Locations locations;
	
	private String fileNameOfInstance;
	
	private int vehicleCapacity;
	
	private String depotId;
	
	private String instanceName;
	
	public Christophides(String fileNameOfInstance, String instanceName) {
		this.fileNameOfInstance = fileNameOfInstance;
		this.instanceName = instanceName;
	}
	
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.INFO);
		Christophides christophides = new Christophides("/Users/stefan/Documents/Schroeder/Dissertation/vrpInstances/vrp_christofides_mingozzi_toth/vrpnc1.txt", "vrpnc1");
		christophides.run();
		
	}
	
	public static void print(){
		System.out.println("foo");
	}

	public void run(){
		MyLocations myLocations = new MyLocations();
		Collection<Job> jobs = new ArrayList<Job>();
		readLocationsAndJobs(myLocations,jobs);
		VrpBuilder vrpBuilder = new VrpBuilder(new CrowFlyCosts(myLocations), new PickORDeliveryCapacityAndTWConstraint());
		for(Job j : jobs){
			vrpBuilder.addJob(j);
		}
		for(int i=0;i<10;i++){
			vrpBuilder.addVehicle(VrpUtils.createVehicle("" + (i+1), depotId, vehicleCapacity));
		}
		RuinAndRecreate algo = createAlgo(vrpBuilder.build());
		algo.run();
	}
	
	private RuinAndRecreate createAlgo(VehicleRoutingProblem vrp) {
		RuinAndRecreateFactory factory = new DistributionTourAlgoFactory();
		factory.setIterations(500);
		factory.setWarmUp(100);
		ChartListener chartListener = new ChartListener();
		chartListener.setFilename("output/"+instanceName+".png");
		RuinAndRecreateReport report = new RuinAndRecreateReport();
		factory.addRuinAndRecreateListener(chartListener);
		factory.addRuinAndRecreateListener(report);
		return factory.createAlgorithm(vrp, new InitialSolution().createInitialSolution(vrp));
	}


	private void readLocationsAndJobs(MyLocations locations, Collection<Job> jobs) {
		BufferedReader reader = IOUtils.getBufferedReader(fileNameOfInstance);
		int counter = 0;
		String line = null;
		Integer vehicleCapacity = null; 
		try {
			while((line = reader.readLine()) != null){
				line = line.replace("\r", "");
				line = line.trim();
				String[] tokens = line.split(" ");
				if(counter == 0){
					vehicleCapacity = Integer.parseInt(tokens[1].trim());
					this.vehicleCapacity = vehicleCapacity;
				}
				else if(counter == 1){
					String id = "" + counter;
					Coordinate depotCoord = makeCoord(tokens[0].trim(),tokens[1].trim());
					locations.addLocation(id, depotCoord);
					depotId = id;
				}
				else{
					String id = "" + counter;
					Coordinate customerCoord = makeCoord(tokens[0].trim(),tokens[1].trim());
					int demand = Integer.parseInt(tokens[2].trim());
					locations.addLocation(id, customerCoord);
					jobs.add(VrpUtils.createShipment(""+counter, "1", id, demand, VrpUtils.createTimeWindow(0.0,Double.MAX_VALUE), 
							VrpUtils.createTimeWindow(0.0, Double.MAX_VALUE)));
				}
				counter++;
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
