package vrp.vrpInstances;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.JobActivity;
import org.matsim.contrib.freight.vrp.basics.Locations;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
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



public class TDSolomon {

	
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

	private List<Double> timeBins;
	
	private List<Double> speedValues;

	private double depotStart;

	private double depotEnd;

	public TDSolomon(List<Double> timeBins, List<Double> speedValues, String fileNameOfInstance, String instanceName) {
		super();
		this.timeBins = timeBins;
		this.speedValues = speedValues;
		this.fileNameOfInstance = fileNameOfInstance;
		this.instanceName = instanceName;
	}
	
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.INFO);
		List<Double> timeBins = new ArrayList<Double>();
		timeBins.add(0.2);
		timeBins.add(0.4);
		timeBins.add(0.6);
		timeBins.add(0.8);
		timeBins.add(1.0);
		
		List<Double> speedValues = new ArrayList<Double>();
		speedValues.add(1.0);
		speedValues.add(2.5);
		speedValues.add(1.75);
		speedValues.add(2.5);
		speedValues.add(1.0);
		
//		for(int i=0;i<9;i++){
			TDSolomon solomon = new TDSolomon(timeBins,speedValues,
					"/Users/stefan/Documents/Schroeder/Dissertation/vrpInstances/cvrptw_solomon/nOfCust100/R104.txt", "100_R104_TD");
			solomon.run();
//		}
	}

	public void run(){
		MyLocations myLocations = new MyLocations();
		Collection<Job> jobs = new ArrayList<Job>();
		readLocationsAndJobs(myLocations,jobs);
		List<Double> timeBins_ = new ArrayList<Double>();
		for(Double d : timeBins){
			timeBins_.add(d*depotEnd);
		}
		TDCosts tdCosts = new TDCosts(myLocations, timeBins_, speedValues);
		VrpBuilder vrpBuilder = new VrpBuilder(tdCosts, new PickORDeliveryCapacityAndTWConstraint());
		for(Job j : jobs){
			vrpBuilder.addJob(j);
		}
		for(int i=0;i<20;i++){
			Vehicle vehicle = VrpUtils.createVehicle("" + (i+1), depotId, vehicleCapacity);
			vehicle.setEarliestDeparture(depotStart);
			vehicle.setLatestArrival(depotEnd);
			vrpBuilder.addVehicle(vehicle);
		}
		RuinAndRecreate algo = createAlgo(vrpBuilder.build());
		algo.run();
//		validate(algo.getSolution(),tdCosts);
	}
	

	private void validate(Collection<Tour> solution, TDCosts tdCosts) {
		for(Tour t : solution){
			if(t.getActivities().size() <= 2){
				continue;
			}
			double earliestTourStartTime = t.getActivities().getFirst().getEarliestArrTime();
			String currentLocation = t.getActivities().getFirst().getLocationId();
			double currentTime = earliestTourStartTime;
			System.out.println("tourstart@"+currentLocation+"@"+currentTime);
			for(int i=1;i<t.getActivities().size()-1;i++){
				TourActivity thisActivity = t.getActivities().get(i);
				JobActivity thisJobActivity = (JobActivity)t.getActivities().get(i);
				if(thisJobActivity instanceof Pickup){
					continue;
				}
				Shipment s = (Shipment) thisJobActivity.getJob();
				double transportationTime = tdCosts.getTransportTime(currentLocation, s.getToId(), currentTime);
				System.out.println("on the road: " + transportationTime);
				currentTime += transportationTime;
				System.out.println("arrive@"+s.getToId()+"@"+currentTime);
				System.out.println("planedArrival: " + thisActivity.getEarliestArrTime());
				if(currentTime > s.getDeliveryTW().getEnd()){
					System.out.println("WARN: arrived too late" + " currentTime=" + currentTime + " latestDeliverTime=" + s.getDeliveryTW().getEnd());
				}
				if(currentTime < s.getDeliveryTW().getStart()){
					System.out.println("wait " + (s.getDeliveryTW().getStart()-currentTime));
					currentTime = s.getDeliveryTW().getStart();
				}
				System.out.println("delivery which costs " + s.getDeliveryServiceTime());
				currentTime += s.getDeliveryServiceTime();
				currentLocation = s.getToId();
			}
			currentTime += tdCosts.getTransportTime(currentLocation, t.getActivities().getLast().getLocationId(), currentTime);
			if(currentTime > t.getActivities().getLast().getLatestArrTime()){
				System.out.println("arrived too late. currentTime=" + currentTime + " latestArrTime=" + t.getActivities().getLast().getLatestArrTime());
			}
			System.out.println("tour finished successfully");
			System.out.println("");
		}
		
	}

	private RuinAndRecreate createAlgo(VehicleRoutingProblem vrp) {
		RuinAndRecreateFactory factory = new DistributionTourWithTimeWindowsAlgoFactory();
		factory.setIterations(1000);
		factory.setWarmUp(50);
		ChartListener chartListener = new ChartListener();
		chartListener.setFilename("output/"+instanceName+".png");
		RuinAndRecreateReport report = new RuinAndRecreateReport();
		factory.addRuinAndRecreateListener(chartListener);
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
