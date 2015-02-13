package playground.dhosse.prt.launch;

import static org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource.DISTANCE;
import static org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource.TIME;
import static org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource.EVENTS;
import static org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource.FREE_FLOW_SPEED;
import static playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration.Goal.DEMAND_SUPPLY_EQUIL;
import static playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration.Goal.MIN_PICKUP_TIME;
import static playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration.Goal.MIN_WAIT_TIME;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.core.utils.io.IOUtils;

import playground.dhosse.prt.launch.PrtParameters.AlgorithmConfig.AlgorithmType;
import playground.dhosse.prt.optimizer.PrtNPersonsOptimizer;
import playground.dhosse.prt.optimizer.PrtOptimizer;
import playground.michalm.taxi.optimizer.AbstractTaxiOptimizer;
import playground.michalm.taxi.optimizer.TaxiOptimizer;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration.Goal;
import playground.michalm.taxi.optimizer.assignment.APSTaxiOptimizer;
import playground.michalm.taxi.optimizer.fifo.NOSTaxiOptimizer;
import playground.michalm.taxi.optimizer.fifo.OTSTaxiOptimizer;
import playground.michalm.taxi.optimizer.fifo.RESTaxiOptimizer;
import playground.michalm.taxi.optimizer.mip.MIPTaxiOptimizer;
import playground.michalm.taxi.scheduler.TaxiSchedulerParams;

public class PrtParameters {

//	private final String netFile = "netFile";
//	private final String plansFile = "plansFile";
//	private final String taxisFile = "taxisFile";
//	private final String ranksFile = "ranksFile";
//	private final String algorithmConfigString = "algorithmConfig";
//	private final String destinationKnownString = "destinationKnown";
//	private final String pickupDurationString = "pickupDuration";
//	private final String dropoffDurationString = "dropoffDuration";
//	private final String otfVisString = "otfVis";
//	private final String separator = " ";
//	private final String vehCap = "capacity";
//	private final String eventsFile = "eventsFile";
//	
//	String workingDir;
//	String pathToNetworkFile;
//	String pathToPlansFile;
//	String pathToTaxisFile;
//	String pathToRanksFile;
//	String pathToEventsFile;
//	AlgorithmConfig algorithmConfig;
//	boolean destinationKnown = false;
//	double pickupDuration;
//	double dropoffDuration;
//	int capacity;
//	boolean otfVis = false;
//	boolean nPersons = false;
//	TaxiSchedulerParams taxiParams;
//	
//	
//	public PrtParameters(String args[]){
//		
//		final String parametersFileName = args[0];
//		this.parse(parametersFileName);
//		
//		this.taxiParams = new TaxiSchedulerParams(this.destinationKnown, this.pickupDuration, this.dropoffDuration);
//		
//	}
//	
//	private void parse(String filename){
//		
//		this.workingDir = new File(filename).getParent() + "/";
//		BufferedReader reader = IOUtils.getBufferedReader(filename);
//		
//		String line = null;
//		
//		try {
//			while( (line = reader.readLine()) != null ){
//				
//				if(line.startsWith("#")) continue;
//				
//				String[] lineParts = line.split(this.separator);
//				
//				if(lineParts[0].equals(this.netFile)){
//					this.pathToNetworkFile = this.workingDir + lineParts[1];
//				}
//				else if(lineParts[0].equals(this.plansFile)){
//					this.pathToPlansFile = this.workingDir + lineParts[1];
//				}
//				else if(lineParts[0].equals(this.taxisFile)){
//					this.pathToTaxisFile = this.workingDir + lineParts[1];
//				}
//				else if(lineParts[0].equals(this.ranksFile)){
//					this.pathToRanksFile = this.workingDir + lineParts[1];
//				}
//				else if(lineParts[0].equals(this.algorithmConfigString)){
//					
//					this.algorithmConfig = AlgorithmConfig.valueOf(lineParts[1]);
//					
//					if(this.algorithmConfig.algorithmType.equals(AlgorithmType.N_PERSONS)){
//						this.nPersons = true;
//					}
//				}
//				
//				else if(lineParts[0].equals(this.destinationKnownString)){
//					this.destinationKnown = true;
//				}
//				else if(lineParts[0].equals(this.pickupDurationString)){
//					this.pickupDuration = Double.valueOf(lineParts[1]);
//				}
//				else if(lineParts[0].equals(this.dropoffDurationString)){
//					this.dropoffDuration = Double.valueOf(lineParts[1]);
//				}
//				else if(lineParts[0].equals(this.otfVisString)){
//					this.otfVis = true;
//				}
//				else if(lineParts[0].equals(this.vehCap)){
//					this.capacity = Integer.valueOf(lineParts[1]);
//				}
//				else if(lineParts[0].equals(this.eventsFile)){
//					this.pathToEventsFile = this.workingDir + lineParts[1];
//				}
//				
//			}
//			
//			reader.close();
//			
//		} catch (IOException e) {
//			
//			e.printStackTrace();
//			
//		}
//		
//	}
	
	public enum AlgorithmConfig
	{
	    NOS_TW_TD(AlgorithmType.NO_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, DISTANCE),

	    NOS_TW_FF(AlgorithmType.NO_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

//	    NOS_TW_15M(AlgorithmType.NO_SCHEDULING, MIN_WAIT_TIME, EVENTS, TIME),

	    NOS_TP_TD(AlgorithmType.NO_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, DISTANCE),

	    NOS_TP_FF(AlgorithmType.NO_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

//	    NOS_TP_15M(AlgorithmType.NO_SCHEDULING, MIN_PICKUP_TIME, EVENTS, TIME),

	    NOS_DSE_TD(AlgorithmType.NO_SCHEDULING, DEMAND_SUPPLY_EQUIL, FREE_FLOW_SPEED, DISTANCE),

	    NOS_DSE_FF(AlgorithmType.NO_SCHEDULING, DEMAND_SUPPLY_EQUIL, FREE_FLOW_SPEED, TIME),

//	    NOS_DSE_15M(AlgorithmType.NO_SCHEDULING, DEMAND_SUPPLY_EQUIL, EVENTS, TIME),

	    OTS_TW_FF(AlgorithmType.ONE_TIME_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

//	    OTS_TW_15M(AlgorithmType.ONE_TIME_SCHEDULING, MIN_WAIT_TIME, EVENTS, TIME),

	    OTS_TP_FF(AlgorithmType.ONE_TIME_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

//	    OTS_TP_15M(AlgorithmType.ONE_TIME_SCHEDULING, MIN_PICKUP_TIME, EVENTS, TIME),

	    RES_TW_FF(AlgorithmType.RE_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

//	    RES_TW_15M(AlgorithmType.RE_SCHEDULING, MIN_WAIT_TIME, EVENTS, TIME),

	    RES_TP_FF(AlgorithmType.RE_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

//	    RES_TP_15M(AlgorithmType.RE_SCHEDULING, MIN_PICKUP_TIME, EVENTS, TIME),

	    APS_TW_FF(AlgorithmType.AP_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

//	    APS_TW_15M(AlgorithmType.AP_SCHEDULING, MIN_WAIT_TIME, EVENTS, TIME),

	    APS_TP_FF(AlgorithmType.AP_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

//	    APS_TP_15M(AlgorithmType.AP_SCHEDULING, MIN_PICKUP_TIME, EVENTS, TIME),

	    APS_DSE_FF(AlgorithmType.AP_SCHEDULING, DEMAND_SUPPLY_EQUIL, FREE_FLOW_SPEED, TIME),

//	    APS_DSE_15M(AlgorithmType.AP_SCHEDULING, DEMAND_SUPPLY_EQUIL, EVENTS, TIME),

	    MIP_TW_FF(AlgorithmType.MIP_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),
	    
	    NP_TW_FF(AlgorithmType.N_PERSONS, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),
	    
	    NP_TW_15M(AlgorithmType.N_PERSONS, MIN_WAIT_TIME, EVENTS, TIME),
	    
	    NP_TP_FF(AlgorithmType.N_PERSONS, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME);

	    static enum AlgorithmType
	    {
	        NO_SCHEDULING, //
	        ONE_TIME_SCHEDULING, //
	        RE_SCHEDULING, //
	        AP_SCHEDULING, //
	        MIP_SCHEDULING,
	        N_PERSONS;
	    }


	    final AlgorithmType algorithmType;
	    final Goal goal;
	    final TravelTimeSource ttimeSource;
	    final TravelDisutilitySource tdisSource;
	    
	    AlgorithmConfig(AlgorithmType algorithmType, Goal goal, TravelTimeSource ttimeSource,
	            TravelDisutilitySource tdisSource)
	    {
	        this.algorithmType = algorithmType;
	        this.goal = goal;
	        this.ttimeSource = ttimeSource;
	        this.tdisSource = tdisSource;
	    }


	    public TaxiOptimizer createTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
	    {
	        switch (algorithmType) {
	            case NO_SCHEDULING:
	                return new NOSTaxiOptimizer(optimConfig);

	            case ONE_TIME_SCHEDULING:
	                return new OTSTaxiOptimizer(optimConfig);

	            case RE_SCHEDULING:
	                return new RESTaxiOptimizer(optimConfig);

	            case AP_SCHEDULING:
	                return new APSTaxiOptimizer(optimConfig);

	            case MIP_SCHEDULING:
	                return new MIPTaxiOptimizer(optimConfig);
	                
	            case N_PERSONS:
	            	return new PrtNPersonsOptimizer(optimConfig);

	            default:
	                throw new IllegalStateException();
	        }
	    }
	    
	    public AlgorithmType getAlgorithmType(){
	    	return this.algorithmType;
	    }
	    
	    public Goal getGoal(){
	    	return this.goal;
	    }
	    
	    public TravelTimeSource getTravelTimeSource(){
	    	return this.ttimeSource;
	    }
	    
	    public TravelDisutilitySource getTravelDisutilitySource(){
	    	return this.tdisSource;
	    }
	    
	}
	
}
