package vrp.vrpInstances;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityConstraint;
import vrp.algorithms.ruinAndRecreate.factories.StandardRuinAndRecreateFactory;
import vrp.api.Customer;
import vrp.api.SingleDepotVRP;
import vrp.basics.Coordinate;
import vrp.basics.CrowFlyDistance;
import vrp.basics.SingleDepotSolutionFactoryImpl;
import vrp.basics.SingleDepotVRPBuilder;
import freight.vrp.ChartListener;
import freight.vrp.RuinAndRecreateReport;

/**
 * test instances for the capacitated vrp. instances are from christophides, mingozzi and toth
 * and can be found at:
 * http://neo.lcc.uma.es/radi-aeb/WebVRP/
 * @author stefan schroeder
 *
 */

public class Christophides {
	
	private SingleDepotVRPBuilder vrpBuilder;
	
	private String fileNameOfInstance;
	
	private String instanceName;
	
	public Christophides(String fileNameOfInstance, String instanceName) {
		this.fileNameOfInstance = fileNameOfInstance;
		this.instanceName = instanceName;
	}
	
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.INFO);
		Christophides christophides = new Christophides("/Users/stefan/Documents/workspace/VehicleRouting/instances/vrp_christofides_mingozzi_toth/vrpnc14.txt", "vrpnc14");
		christophides.run();
	}

	public void run(){
		vrpBuilder = new SingleDepotVRPBuilder();
		readProblem(fileNameOfInstance);
		SingleDepotVRP vrp = createVRP();
		RuinAndRecreate algo = createAlgo(vrp);
		algo.run();
	}
	
	private RuinAndRecreate createAlgo(SingleDepotVRP vrp) {
		StandardRuinAndRecreateFactory factory = new StandardRuinAndRecreateFactory();
		factory.setIterations(1000);
		factory.setWarmUp(100);
		ChartListener chartListener = new ChartListener();
		chartListener.setFilename("vrp/christophides/"+instanceName+".png");
		RuinAndRecreateReport report = new RuinAndRecreateReport();
		factory.addRuinAndRecreateListener(chartListener);
		factory.addRuinAndRecreateListener(report);
		return factory.createAlgorithm(vrp, new SingleDepotSolutionFactoryImpl().createInitialSolution(vrp),vrp.getVehicleType().capacity);
	}

	private SingleDepotVRP createVRP() {
		vrpBuilder.setConstraints(new CapacityConstraint());
		vrpBuilder.setCosts(new CrowFlyDistance());
		return vrpBuilder.buildVRP();
	}

	private void readProblem(String filename) {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
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
					vrpBuilder.setVehicleType(vehicleCapacity);
				}
				else if(counter == 1){
					Coordinate depotCoord = makeCoord(tokens[0].trim(),tokens[1].trim());
					Customer depot = vrpBuilder.createAndAddCustomer(""+counter, vrpBuilder.getNodeFactory().createNode(""+counter,depotCoord), 0, 0.0, 0.0, 0.0);
					vrpBuilder.setDepot(depot);
				}
				else{
					Coordinate customerCoord = makeCoord(tokens[0].trim(),tokens[1].trim());
					int demand = Integer.parseInt(tokens[2].trim());
					vrpBuilder.createAndAddCustomer(""+counter, vrpBuilder.getNodeFactory().createNode(""+counter,customerCoord), demand, 0.0, 0.0, 0.0);
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
