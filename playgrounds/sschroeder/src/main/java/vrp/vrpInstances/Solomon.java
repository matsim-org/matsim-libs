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
 * test instances for the capacitated vrp with time windows. instances are from solomon
 * and can be found at:
 * http://neo.lcc.uma.es/radi-aeb/WebVRP/
 * @author stefan schroeder
 *
 */



public class Solomon {
	
private SingleDepotVRPBuilder vrpBuilder;
	
	private String fileNameOfInstance;
	
	private String instanceName;
	
	public Solomon(String fileNameOfInstance, String instanceName) {
		super();
		this.fileNameOfInstance = fileNameOfInstance;
		this.instanceName = instanceName;
	}
	
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.INFO);
		Solomon solomon = new Solomon("/Users/stefan/Documents/workspace/VehicleRouting/instances/cvrptw_solomon/nOfCust100/R111.txt", "100_R111");
		solomon.run();
	}

	public void run(){
		vrpBuilder = new SingleDepotVRPBuilder();
		readProblem(fileNameOfInstance);
		SingleDepotVRP vrp = createVRP();
		RuinAndRecreate algo = createAlgo(vrp);
		algo.run();
	}
	
	private SingleDepotVRP createVRP() {
		vrpBuilder.setConstraints(new TWAndCapacityConstraint());
		CrowFlyCosts costs = new CrowFlyCosts();
		costs.speed = 1;
		vrpBuilder.setCosts(costs);
		return vrpBuilder.buildVRP();
	}

	private RuinAndRecreate createAlgo(SingleDepotVRP vrp) {
		RuinAndRecreateWithTimeWindowsFactory factory = new RuinAndRecreateWithTimeWindowsFactory();
		factory.setIterations(1000);
		factory.setWarmUp(100);
		ChartListener chartListener = new ChartListener();
		chartListener.setFilename("vrp/solomon/"+instanceName+".png");
		RuinAndRecreateReport report = new RuinAndRecreateReport();
		factory.addRuinAndRecreateListener(chartListener);
		factory.addRuinAndRecreateListener(report);
		return factory.createAlgorithm(vrp, new SingleDepotInitialSolutionFactoryImpl().createInitialSolution(vrp),vrp.getVehicleType().capacity);
	}
	
	private void readProblem(String fileNameOfInstance){
		BufferedReader reader = IOUtils.getBufferedReader(fileNameOfInstance);
		String line = null;
		int counter = 0;
		int customerCounter = 0;
		Integer nOfCustomer = null;
		try {
			while((line = reader.readLine()) != null){
				line = line.replace("\r", "");
				line = line.trim();
				String[] tokens = line.split(" +");
				counter++;
				if(counter == 5){
					int vehicleCap = Integer.parseInt(tokens[1]);
					nOfCustomer = Integer.parseInt(tokens[0]);
					vrpBuilder.setVehicleType(vehicleCap);
					continue;
				}
				
				if(counter > 9){
					customerCounter++;
					Coordinate coord = makeCoord(tokens[1],tokens[2]);
					int demand = Integer.parseInt(tokens[3]);
					double start = Double.parseDouble(tokens[4]);
					double end = Double.parseDouble(tokens[5]);
					double serviceTime = Double.parseDouble(tokens[6]);
					if(counter == 10){
						Customer depot = vrpBuilder.createAndAddCustomer(""+customerCounter, vrpBuilder.getNodeFactory().createNode(""+customerCounter, coord), demand, start, end, serviceTime);
						vrpBuilder.setDepot(depot);
					}
					else{
						vrpBuilder.createAndAddCustomer(""+customerCounter, vrpBuilder.getNodeFactory().createNode(""+customerCounter, coord), demand, start, end, serviceTime);
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
