package playground.dhosse.prt.launch;

import static playground.michalm.taxi.optimizer.AbstractTaxiOptimizerParams.TravelTimeSource.*;

import playground.dhosse.prt.optimizer.PrtNPersonsOptimizer;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.AbstractTaxiOptimizerParams.*;
import playground.michalm.taxi.optimizer.assignment.AssignmentTaxiOptimizer;
import playground.michalm.taxi.optimizer.fifo.FifoTaxiOptimizer;
import playground.michalm.taxi.optimizer.mip.MIPTaxiOptimizer;
import playground.michalm.taxi.optimizer.rules.RuleBasedTaxiOptimizer;

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
    public enum Goal
    {
        MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL;
    };



	public enum AlgorithmConfig
	{
	    NOS_TW_FF(AlgorithmType.NO_SCHEDULING, Goal.MIN_WAIT_TIME, FREE_FLOW_SPEED),

	    NOS_TW_15M(AlgorithmType.NO_SCHEDULING, Goal.MIN_WAIT_TIME, EVENTS),

	    NOS_TP_FF(AlgorithmType.NO_SCHEDULING, Goal.MIN_PICKUP_TIME, FREE_FLOW_SPEED),

//	    NOS_TP_15M(AlgorithmType.NO_SCHEDULING, MIN_PICKUP_TIME, EVENTS),

	    NOS_DSE_FF(AlgorithmType.NO_SCHEDULING, Goal.DEMAND_SUPPLY_EQUIL, FREE_FLOW_SPEED),

//	    NOS_DSE_15M(AlgorithmType.NO_SCHEDULING, DEMAND_SUPPLY_EQUIL, EVENTS),

//	    OTS_TW_15M(AlgorithmType.ONE_TIME_SCHEDULING, MIN_WAIT_TIME, EVENTS),

//	    OTS_TP_15M(AlgorithmType.ONE_TIME_SCHEDULING, MIN_PICKUP_TIME, EVENTS),

	    RES_TW_FF(AlgorithmType.RE_SCHEDULING, Goal.MIN_WAIT_TIME, FREE_FLOW_SPEED),

//	    RES_TW_15M(AlgorithmType.RE_SCHEDULING, MIN_WAIT_TIME, EVENTS),

//	    RES_TP_FF(AlgorithmType.RE_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED),

//	    RES_TP_15M(AlgorithmType.RE_SCHEDULING, MIN_PICKUP_TIME, EVENTS),

	    APS_TW_FF(AlgorithmType.AP_SCHEDULING, Goal.MIN_WAIT_TIME, FREE_FLOW_SPEED),

//	    APS_TW_15M(AlgorithmType.AP_SCHEDULING, MIN_WAIT_TIME, EVENTS),

	    APS_TP_FF(AlgorithmType.AP_SCHEDULING, Goal.MIN_PICKUP_TIME, FREE_FLOW_SPEED),

//	    APS_TP_15M(AlgorithmType.AP_SCHEDULING, MIN_PICKUP_TIME, EVENTS),

	    APS_DSE_FF(AlgorithmType.AP_SCHEDULING, Goal.DEMAND_SUPPLY_EQUIL, FREE_FLOW_SPEED),

//	    APS_DSE_15M(AlgorithmType.AP_SCHEDULING, DEMAND_SUPPLY_EQUIL, EVENTS),

	    MIP_TW_FF(AlgorithmType.MIP_SCHEDULING, Goal.MIN_WAIT_TIME, FREE_FLOW_SPEED),
	    
	    NP_TW_FF(AlgorithmType.N_PERSONS, Goal.MIN_WAIT_TIME, FREE_FLOW_SPEED),
	    
	    NP_TW_15M(AlgorithmType.N_PERSONS, Goal.MIN_WAIT_TIME, EVENTS),
	    
	    NP_TP_FF(AlgorithmType.N_PERSONS, Goal.MIN_PICKUP_TIME, FREE_FLOW_SPEED);

	    static enum AlgorithmType
	    {
	        NO_SCHEDULING, //
	        RE_SCHEDULING, //
	        AP_SCHEDULING, //
	        MIP_SCHEDULING,
	        N_PERSONS;
	    }


	    final AlgorithmType algorithmType;
	    final Goal goal;
	    final TravelTimeSource ttimeSource;
	    
	    AlgorithmConfig(AlgorithmType algorithmType, Goal goal, TravelTimeSource ttimeSource)
	    {
	        this.algorithmType = algorithmType;
	        this.goal = goal;
	        this.ttimeSource = ttimeSource;
	    }


	    public TaxiOptimizer createTaxiOptimizer(TaxiOptimizerContext optimConfig)
	    {
	        switch (algorithmType) {
	            case NO_SCHEDULING:
	                return new RuleBasedTaxiOptimizer(optimConfig);

	            case RE_SCHEDULING:
	                return new FifoTaxiOptimizer(optimConfig);

	            case AP_SCHEDULING:
	                return new AssignmentTaxiOptimizer(optimConfig);

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
	}
	
}
