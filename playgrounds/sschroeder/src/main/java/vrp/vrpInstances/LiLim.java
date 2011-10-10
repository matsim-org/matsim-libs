package vrp.vrpInstances;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.constraints.TWAndCapacityConstraint;
import vrp.algorithms.ruinAndRecreate.factories.RuinAndRecreateWithTimeWindowsFactory;
import vrp.api.Customer;
import vrp.api.SingleDepotVRP;
import vrp.basics.Coordinate;
import vrp.basics.CrowFlyCosts;
import vrp.basics.SingleDepotInitialSolutionFactoryImpl;
import vrp.basics.SingleDepotVRPBuilder;
import freight.vrp.ChartListener;
import freight.vrp.RuinAndRecreateReport;

/**
 * test instances for the capacitated vrp with pickup and deliveries and time windows. 
 * instances are from li and lim and can be found at:
 * http://www.top.sintef.no/vrp/benchmarks.html
 * @author stefan schroeder
 *
 */


public class LiLim {
	
private SingleDepotVRPBuilder vrpBuilder;
	
	private String fileNameOfInstance;
	
	private String instanceName;
	
	public LiLim(String fileNameOfInstance, String instanceName) {
		this.fileNameOfInstance = fileNameOfInstance;
		this.instanceName = instanceName;
	}
	
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.INFO);
		LiLim liLim = new LiLim("/Users/stefan/Documents/workspace/VehicleRouting/instances/cvrppdtw_lilim/pdp100/lc103.txt", "lc103");
		liLim.run();
	}
	
	public void run(){
		vrpBuilder = new SingleDepotVRPBuilder();
		readProblem(fileNameOfInstance);
		SingleDepotVRP vrp = createVRP();
		RuinAndRecreate algo = createAlgo(vrp);
		algo.run();
	}
	
	private RuinAndRecreate createAlgo(SingleDepotVRP vrp) {
		RuinAndRecreateWithTimeWindowsFactory factory = new RuinAndRecreateWithTimeWindowsFactory();
		factory.setIterations(10000);
		factory.setWarmUp(100);
		ChartListener chartListener = new ChartListener();
		chartListener.setFilename("vrp/liLim/"+instanceName+".png");
		RuinAndRecreateReport report = new RuinAndRecreateReport();
		factory.addRuinAndRecreateListener(chartListener);
		factory.addRuinAndRecreateListener(report);
		return factory.createAlgorithm(vrp, new SingleDepotInitialSolutionFactoryImpl().createInitialSolution(vrp),vrp.getVehicleType().capacity);
	}

	private SingleDepotVRP createVRP() {
		vrpBuilder.setConstraints(new TWAndCapacityConstraint());
		vrpBuilder.setCosts(new CrowFlyCosts());
		return vrpBuilder.buildVRP();
	}

	private void readProblem(String filename) {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line = null;
		boolean firstLine = true;
		try {
			while((line = reader.readLine()) != null){
				line = line.replace("\r", "");
				line = line.trim();
				String[] tokens = line.split("\t");
				if(firstLine){
					int vehicleCapacity = getInt(tokens[1]);
					vrpBuilder.setVehicleType(vehicleCapacity);
					firstLine = false;
					continue;
				}
				else{
					String customerId = tokens[0];
					Coordinate coord = makeCoord(tokens[1], tokens[2]);
					int demand = getInt(tokens[3]);
					double startTimeWindow = getDouble(tokens[4]);
					double endTimeWindow = getDouble(tokens[5]);
					double serviceTime = getDouble(tokens[6]);
					Customer customer = vrpBuilder.createAndAddCustomer(customerId, vrpBuilder.getNodeFactory().createNode(customerId, coord), 
							demand, startTimeWindow, endTimeWindow, serviceTime); 
					if(customerId.equals("0")){
						vrpBuilder.setDepot(customer);
					}
					if(!tokens[7].equals("0")){
						vrpBuilder.addRelation(customerId,tokens[7]);
					}
					else if(!tokens[8].equals("0")){
						vrpBuilder.addRelation(customerId,tokens[8]);
					}
				}
			}
			reader.close();
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
	
	private double getDouble(String string) {
		return Double.parseDouble(string);
	}

	private int getInt(String string) {
		return Integer.parseInt(string);
	}


}
