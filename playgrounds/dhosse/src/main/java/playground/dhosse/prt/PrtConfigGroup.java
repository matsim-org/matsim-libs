package playground.dhosse.prt;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;

public class PrtConfigGroup extends ConfigGroup {
	
	private static final Logger log = Logger.getLogger(PrtConfigGroup.class);

	public static final String GROUP_NAME = "prt";
	
	private static final String VEHICLES_FILE = "vehiclesFile";
	private static final String EVENTS_FILE = "changeEventsFile";
	private static final String TAXI_RANKS_FILE = "ranksFile";
	private static final String OUTPUT_DIRECTORY = "outputDir";
	private static final String ALGORITHM_CONFIG = "algorithm";
	private static final String VEHICLE_CAPACITY = "vehicleCapacity";
	private static final String PICKUP_DURATION = "pickupDuration";
	private static final String DROPOFF_DURATION = "dropoffDuration";
	private static final String DESTINATION_KNOWN = "destinationKnown";
	private static final String ONLINE_VEHICLE_TRACLER = "onlineVehicleTracker";
	private static final String FIXED_COST = "fixedCost";
	private static final String VAR_COST_D = "varCostD";
	
	private String prtIdentifier = "prt";
	private String vehiclesFile = null;
	private String ranksFile = null;
	private String eventsFile = null;
	private String outputDir = null;
	private String algorithmConfig = null;
	private int vehicleMaximumCapacity = 1;
	private double passengerPickupDuration = 0.0;
	private double passengerDropoffDuration = 0.0;
	private double fixedCost = 0.0;
	private double varCostD = 0.0;
	private boolean destinationKnown = false;
	private boolean onlineVehicleTracker = false;
	
	public PrtConfigGroup() {
		super(GROUP_NAME);
		log.info("Loading PRT config group...");
	}
	
	@Override
	public void addParam(final String key, final String value){
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;

		
		if(VEHICLES_FILE.equals(key)){
			this.vehiclesFile = value;
		} else if(TAXI_RANKS_FILE.equals(key)){
			this.ranksFile = value;
		} else if(EVENTS_FILE.equals(key)){
			if(!value.equals("")){
				this.eventsFile = value;
			}
		} else if(OUTPUT_DIRECTORY.equals(key)){
			this.outputDir = value;
		} else if(ALGORITHM_CONFIG.equals(key)){
			this.algorithmConfig = value;
		} else if(VEHICLE_CAPACITY.equals(key)){
			this.vehicleMaximumCapacity = Integer.parseInt(value);
		} else if(PICKUP_DURATION.equals(key)){
			this.passengerPickupDuration = Double.parseDouble(value);
		} else if(DROPOFF_DURATION.equals(key)){
			this.passengerDropoffDuration = Double.parseDouble(value);
		} else if(DESTINATION_KNOWN.equals(key)){
			this.destinationKnown = Boolean.getBoolean(value);
		} else if(ONLINE_VEHICLE_TRACLER.equals(key)){
			this.onlineVehicleTracker = Boolean.getBoolean(value);
		} else if(FIXED_COST.equals(key)){
			this.fixedCost = Double.parseDouble(value);
		} else if(VAR_COST_D.equals(key)){
			this.varCostD = Double.parseDouble(value);
		} else {
			log.error("unknown parameter: " + key + "...");
		}
		
	}
	
	@Override
	public TreeMap<String, String> getParams() {
		
		TreeMap<String, String> map = new TreeMap<>();
		
		map.put(VEHICLES_FILE, this.vehiclesFile);
		map.put(TAXI_RANKS_FILE, this.ranksFile);
		map.put(EVENTS_FILE, this.eventsFile);
		map.put(OUTPUT_DIRECTORY, this.outputDir);
		map.put(ALGORITHM_CONFIG, this.algorithmConfig);
		map.put(VEHICLE_CAPACITY, Integer.toString(this.vehicleMaximumCapacity));
		map.put(PICKUP_DURATION, Double.toString(this.passengerPickupDuration));
		map.put(DROPOFF_DURATION, Double.toString(this.passengerDropoffDuration));
		map.put(DESTINATION_KNOWN, Boolean.toString(destinationKnown));
		map.put(ONLINE_VEHICLE_TRACLER, Boolean.toString(this.onlineVehicleTracker));
		map.put(FIXED_COST, Double.toString(this.fixedCost));
		map.put(VAR_COST_D, Double.toString(this.varCostD));
		
		return map;
		
	}
	
	public String getPrtIdentifier(){
		return this.prtIdentifier;
	}
	
	public String getVehiclesFile(){
		return this.vehiclesFile;
	}
	
	public String getRanksFile(){
		return this.ranksFile;
	}
	
	public String getEventsFile(){
		return this.eventsFile;
	}
	
	public String getPrtOutputDirectory(){
		return this.outputDir;
	}
	
	public String getAlgorithmConfig(){
		return this.algorithmConfig;
	}
	
	public int getVehicleCapacity(){
		return this.vehicleMaximumCapacity;
	}
	
	public double getPickupDuration(){
		return this.passengerPickupDuration;
	}
	
	public double getDropoffDuration(){
		return this.passengerDropoffDuration;
	}
	
	public boolean getDestinationKnown(){
		return this.destinationKnown;
	}
	
	public void setVehiclesFile(String file){
		this.vehiclesFile = file;
	}
	
	public void setEventsFile(String file){
		this.eventsFile = file;
	}
	
	public boolean getUseOnlineVehicleTracker(){
		return this.onlineVehicleTracker;
	}
	
	public double getFixedCost(){
		return this.fixedCost;
	}
	
	public double getVariableCostsD(){
		return this.varCostD;
	}

}
