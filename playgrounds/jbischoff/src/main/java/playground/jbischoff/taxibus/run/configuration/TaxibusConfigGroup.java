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
	private static final String PICKUP_DURATION = "pickupDuration";
	private static final String DROPOFF_DURATION = "dropoffDuration";
	private static final String OTFVIS = "otfvis";
	private static final String VEHCAP = "vehicleCapacity";
	
	private static final String LINES = "linesFile";
	private static final String ZONESSHP = "zonesShape";
	private static final String ZONESXML = "zonesXML";
	

	
	private static final String BALANCING = "balanceLines";
	private static final String VEHICLESONDISPATCH = "vehiclesDispatchedAtSameTime";
	private static final String DISTANCEMEASURE = "distanceCalculationCostCriteria";
	

	private String taxiIdentifier = "taxibus";
	private String vehiclesFile = null;
	private String ranksFile = null;
	private String outputDir = null;
	private String distanceCalculationCostCriteria = "beeline";

	private double pickupDuration = 60.0;
	private double dropoffDuration = 120.0;
	private int numberOfVehiclesDispatchedAtSameTime = 8;
	private int vehCap= 8;
	

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
		} else if (PICKUP_DURATION.equals(key)) {
			this.pickupDuration = Double.parseDouble(value);
		} else if (DROPOFF_DURATION.equals(key)) {
			this.dropoffDuration = Double.parseDouble(value);
		} else if (OTFVIS.equals(key)) {
			this.otfvis = Boolean.parseBoolean(value);
		} else if (ZONESSHP.equals(key)) {
			this.zonesShpFile = value;
		} else if (ZONESXML.equals(key)) {
			this.zonesXmlFile = value;
		} else if (LINES.equals(key)) {
			this.linesFile = value;
		} else if (BALANCING.equals(key)) {
			this.balancingMethod = value;
		} else if (DISTANCEMEASURE.equals(key)) {
			this.distanceCalculationCostCriteria = value;
		} else if (VEHICLESONDISPATCH.equals(key)) {
			this.numberOfVehiclesDispatchedAtSameTime = Integer.parseInt(value);
		}
		else if (VEHCAP.equals(key)){
			this.vehCap = Integer.parseInt(value);
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
		map.put(ALGORITHM, algorithm);
		map.put(OTFVIS, Boolean.toString(otfvis));
		map.put(LINES, linesFile);
		map.put(ZONESSHP, zonesShpFile);
		map.put(ZONESXML, zonesXmlFile);
		map.put(BALANCING, balancingMethod);
		map.put(DISTANCEMEASURE, distanceCalculationCostCriteria);
		map.put(VEHICLESONDISPATCH, Integer.toString(numberOfVehiclesDispatchedAtSameTime));
		map.put(VEHCAP, Integer.toString(vehCap));
		return map;

	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();

		map.put(VEHICLES_FILE, "Taxi Vehicles file");
		map.put(TAXI_RANKS_FILE, "Taxi rank file; optional if you don't use ranks");
		map.put(OUTPUT_DIRECTORY, "Output directory for taxi stats");
		map.put(ALGORITHM, "Taxibus algorithms: Possible parameters are line, multipleLine (...)");

		map.put(OTFVIS, "show simulation in OTFVis");
		map.put(ZONESSHP, "Zones shape file, if required by algorithm.");
		map.put(ZONESXML, "Zones xml file, if required by algorithm.");
		map.put(LINES, "Lines file, if required by algorithm. Uses zone IDs for reference");
		map.put(BALANCING,
				"Balancing vehicles between line. Possible parameters: same (returns to same line), return (return line), balanced (balances between lines)");
		map.put(DISTANCEMEASURE, "Mode in which distance is measured. One of: beeline, earliestArrival");
		map.put(VEHICLESONDISPATCH, "Number of vehicles dispatched at the same time - per line");
		map.put(VEHCAP, "Vehicle capacity per vehicle.");
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

	public String getAlgorithmConfig() {
		return algorithm;
	}

	public double getPickupDuration() {
		return pickupDuration;
	}

	public double getDropoffDuration() {
		return dropoffDuration;
	}

	public boolean isOtfvis() {
		return otfvis;
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
	public int getNumberOfVehiclesDispatchedAtSameTime() {
		return numberOfVehiclesDispatchedAtSameTime;
	}
	public String getDistanceCalculationMode() {
		return distanceCalculationCostCriteria;
	}
	public int getVehCap() {
		return vehCap;
	}
}
