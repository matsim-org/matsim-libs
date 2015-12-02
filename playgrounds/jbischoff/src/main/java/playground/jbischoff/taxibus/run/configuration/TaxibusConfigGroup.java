package playground.jbischoff.taxibus.run.configuration;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;

public class TaxibusConfigGroup extends ConfigGroup {

	private static final Logger log = Logger.getLogger(TaxibusConfigGroup.class);

	public static final String GROUP_NAME = "taxibusConfig";

	private static final String VEHICLES_FILE = "vehiclesFile";
	private static final String TAXI_RANKS_FILE = "ranksFile";
	private static final String OUTPUT_DIRECTORY = "outputDir";
	private static final String ALGORITHM = "algorithm";
	private static final String VEHICLE_CAPACITY = "vehicleCapacity";
	private static final String PICKUP_DURATION = "pickupDuration";
	private static final String DROPOFF_DURATION = "dropoffDuration";
	private static final String DESTINATION_KNOWN = "destinationKnown";
	private static final String VEHICLE_DIVERSION = "vehicleDiversion";
	private static final String NEAREST_REQUEST_LIM = "nearestRequestLimit";
	private static final String NEAREST_VEH_LIM= "nearestVehicleLimit";
	private static final String GOAL= "optimizerGoal";
	private static final String OTFVIS= "otfvis";

	private static final String LINES= "linesFile";
	private static final String ZONESSHP = "zonesShape";
	private static final String ZONESXML= "zonesXML";

	private static final String BALANCING = "balanceLines";
	
	
	
	
	

	private String taxiIdentifier = "taxibus";
	private String vehiclesFile = null;
	private String ranksFile = null;
	private String outputDir = null;
	
	private int nearestVehiclesLimit = 0;
	private int nearestRequestsLimit = 0;
	

	private int vehicleMaximumCapacity = 4;
	private double pickupDuration = 60.0;
	private double dropoffDuration = 120.0;
	
	private boolean destinationKnown = false;
	private boolean vehicleDiversion = false; 

	private String algorithm;

	private boolean otfvis = false;
	
	private String linesFile = null;
	private String zonesShpFile = null;
	private String zonesXmlFile = null;
	
	private String balancingMethod = "return";

	public TaxibusConfigGroup() {
		super(GROUP_NAME);
		log.info("Loading Taxi config group...");
	}

	@Override
	public void addParam(final String key, final String value) {
	
		if ("null".equalsIgnoreCase(value))
			return;

		if (VEHICLES_FILE.equals(key)) {
			this.vehiclesFile = value;
		} else if (TAXI_RANKS_FILE.equals(key)) {
			this.ranksFile = value;
		} else if (OUTPUT_DIRECTORY.equals(key)) {
			this.outputDir = value;
		} else if (ALGORITHM.equals(key)) {
			this.algorithm = value;
		} else if (VEHICLE_CAPACITY.equals(key)) {
			this.vehicleMaximumCapacity = Integer.parseInt(value);
		}	else if (PICKUP_DURATION.equals(key)) {
				this.pickupDuration = Double.parseDouble(value);
		}	else if (DROPOFF_DURATION.equals(key)) {
			this.dropoffDuration = Double.parseDouble(value);
		}else if (DESTINATION_KNOWN.equals(key)) {
			this.destinationKnown= Boolean.parseBoolean(value);
		}else if (VEHICLE_DIVERSION.equals(key)) {
			this.vehicleDiversion = Boolean.parseBoolean(value);
		}else if (NEAREST_REQUEST_LIM.equals(key)) {
			this.nearestRequestsLimit = Integer.parseInt(value);
		}else if (NEAREST_VEH_LIM.equals(key)) {
			this.nearestVehiclesLimit = Integer.parseInt(value);
		}	 else if (OTFVIS.equals(key)) {
			this.otfvis = Boolean.parseBoolean(value);
		}else if (ZONESSHP.equals(key)) {
			this.zonesShpFile = value;
		}else if (ZONESXML.equals(key)) {
			this.zonesXmlFile = value;
		}else if (LINES.equals(key)) {
			this.linesFile = value;
		}
		else if (BALANCING.equals(key)) {
			this.balancingMethod = value;
		}
		
		
		
		else {
			log.error("unknown parameter: " + key + "...");
		}

	}

	@Override
	public TreeMap<String, String> getParams() {

		TreeMap<String, String> map = new TreeMap<>();

		map.put(VEHICLES_FILE, this.vehiclesFile);
		map.put(TAXI_RANKS_FILE, this.ranksFile);
		map.put(OUTPUT_DIRECTORY, this.outputDir);
		map.put(VEHICLE_CAPACITY, Integer.toString(vehicleMaximumCapacity));
		map.put(ALGORITHM, algorithm);
		map.put(DESTINATION_KNOWN, Boolean.toString(destinationKnown));
		map.put(VEHICLE_DIVERSION, Boolean.toString(vehicleDiversion));
		map.put(NEAREST_REQUEST_LIM, Integer.toString(nearestRequestsLimit));
		map.put(NEAREST_VEH_LIM, Integer.toString(nearestVehiclesLimit));
		map.put(OTFVIS, Boolean.toString(otfvis));
		map.put(LINES, linesFile);
		map.put(ZONESSHP, zonesShpFile);
		map.put(ZONESXML, zonesXmlFile);
		map.put(BALANCING, balancingMethod);
		return map;

	}
	
	 @Override
	    public Map<String, String> getComments() {
	        Map<String,String> map = super.getComments();
	        
	        map.put(VEHICLES_FILE, "Taxi Vehicles file");
			map.put(TAXI_RANKS_FILE, "Taxi rank file; optional if you don't use ranks");
			map.put(OUTPUT_DIRECTORY, "Output directory for taxi stats");
			map.put(VEHICLE_CAPACITY, "taxicab vehicle capacity. Default = 4");
			map.put(ALGORITHM, "Taxibus algorithms: Possible parameters are default, line, multipleLine (...)");
			
			map.put(DESTINATION_KNOWN,"determines wether the destination known upon ordering a taxi. Works only with some algorithms" );
			map.put(VEHICLE_DIVERSION, "can taxis be re-assigned en route to customer. Default: false. Works only with some algorithms");
			map.put(NEAREST_REQUEST_LIM, "Upper limit for request near a vehicle. Default: off (=0)");
			map.put(NEAREST_VEH_LIM, "Upper limit for vehicles near a request. Default: off (=0)");
			map.put(GOAL, "Optimizer goal, one of: MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL, NULL");
			map.put(OTFVIS, "show simulation in OTFVis");
			map.put(ZONESSHP, "Zones shape file, if required by algorithm.");
			map.put(ZONESXML, "Zones xml file, if required by algorithm.");
			map.put(LINES, "Lines file, if required by algorithm. Uses zone IDs for reference");
			map.put(BALANCING, "Balancing vehicles between line. Possible parameters: same (returns to same line), return (return line), balanced (balances between lines)");
			
			return map;
	 }

	public String getTaxiIdentifier() {
		return taxiIdentifier;
	}

	public String getVehiclesFile() {
		return vehiclesFile;
	}

	public String getRanksFile() {
		return ranksFile;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public int getVehicleMaximumCapacity() {
		return vehicleMaximumCapacity;
	}

	public String getAlgorithmConfig() {
		return algorithm;
	}
	public double getPickupDuration() {
		return pickupDuration;
	}
	public double getDropoffDuration() {
		return dropoffDuration;
	}

	public boolean isDestinationKnown() {
		return destinationKnown;
	}

	public boolean isVehicleDiversion() {
		return vehicleDiversion;
	}
	public boolean isOtfvis() {
		return otfvis;
	}
	
	public int getNearestRequestsLimit() {
		return nearestRequestsLimit;
	}
	public int getNearestVehiclesLimit() {
		return nearestVehiclesLimit;
	}
	public String getLinesFile() {
		return linesFile;
	}
	public String getZonesShpFile() {
		return zonesShpFile;
	}
	public String getZonesXmlFile() {
		return zonesXmlFile;
	}
	public String getBalancingMethod() {
		return balancingMethod;
	};
}
