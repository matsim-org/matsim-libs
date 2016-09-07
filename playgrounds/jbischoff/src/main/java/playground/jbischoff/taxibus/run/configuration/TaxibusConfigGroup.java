package playground.jbischoff.taxibus.run.configuration;

import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;

public class TaxibusConfigGroup extends ConfigGroup {

	private static final Logger log = Logger.getLogger(TaxibusConfigGroup.class);

	public static final String GROUP_NAME = "taxibusConfig";

	private static final String VEHICLES_FILE = "vehiclesFile";
	private static final String TAXI_RANKS_FILE = "ranksFile";
	private static final String ALGORITHM = "algorithm";
	private static final String PICKUP_DURATION = "pickupDuration";
	private static final String DROPOFF_DURATION = "dropoffDuration";
	private static final String OTFVIS = "otfvis";
	private static final String VEHCAP = "vehicleCapacity";

	private static final String LINES = "linesFile";
	private static final String ZONESSHP = "zonesShape";
	private static final String ZONESXML = "zonesXML";

	private static final String DETOURFACTOR = "detourFactor";

	private static final String BALANCING = "balanceLines";
	private static final String VEHICLESONDISPATCH = "vehiclesDispatchedAtSameTime";
	private static final String DISTANCEMEASURE = "distanceCalculationCostCriteria";
	
	private static final String PREBOOK = "prebookTrips";
	
	private static final String DESTINATIONID = "commonDestinationLinkId";
	
	

	private String taxiIdentifier = "taxibus";
	private String vehiclesFile = null;
	private boolean prebookTrips = true;
	private String ranksFile = null;
	private String outputDir = null;
	private String distanceCalculationCostCriteria = "beeline";

	private double pickupDuration = 60.0;
	private double dropoffDuration = 120.0;
	private int numberOfVehiclesDispatchedAtSameTime = 8;
	private int vehCap = 8;
	private double detourFactor = 1.2;

	private String algorithm;

	private boolean otfvis = false;

	private String linesFile = null;
	private String zonesShpFile = null;
	private String zonesXmlFile = null;
	
	private String destinationLinkId = null;

	private String balancingMethod = "return";

	public TaxibusConfigGroup() {
		super(GROUP_NAME);
		log.info("Loading Taxibus config group...");
	}

	@Override
	public void addParam(final String key, final String value) {

		if ("null".equalsIgnoreCase(value))
			return;

		if (VEHICLES_FILE.equals(key)) {
			this.vehiclesFile = value;
		} else if (TAXI_RANKS_FILE.equals(key)) {
			this.ranksFile = value;
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
		} else if (VEHCAP.equals(key)) {
			this.vehCap = Integer.parseInt(value);
		} else if (DETOURFACTOR.equals(key)) {
			this.detourFactor = Double.parseDouble(value);
		} else if (PREBOOK.equals(key)) {
			this.prebookTrips= Boolean.parseBoolean(value);
		}  else if (DESTINATIONID.equals(key)) {
			this.destinationLinkId = value ;
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
		map.put(ALGORITHM, algorithm);
		map.put(OTFVIS, Boolean.toString(otfvis));
		map.put(LINES, linesFile);
		map.put(ZONESSHP, zonesShpFile);
		map.put(ZONESXML, zonesXmlFile);
		map.put(BALANCING, balancingMethod);
		map.put(DISTANCEMEASURE, distanceCalculationCostCriteria);
		map.put(VEHICLESONDISPATCH, Integer.toString(numberOfVehiclesDispatchedAtSameTime));
		map.put(VEHCAP, Integer.toString(vehCap));
		map.put(DETOURFACTOR, Double.toString(detourFactor));
		map.put(PREBOOK, Boolean.toString(prebookTrips));
		map.put(DESTINATIONID, destinationLinkId);
		return map;

	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();

		map.put(VEHICLES_FILE, "Taxi Vehicles file");
		map.put(TAXI_RANKS_FILE, "Taxi rank file; optional if you don't use ranks");
		map.put(ALGORITHM, "Taxibus algorithms: Possible parameters are line, multipleLine, sharedTaxi (...)");
		map.put(PREBOOK,"Defines whether trips are prebooked at simulation / activity start");
		map.put(OTFVIS, "show simulation in OTFVis");
		map.put(DETOURFACTOR, "shared Taxi detour factor. Default = 1.2");
		map.put(ZONESSHP, "Zones shape file, if required by algorithm.");
		map.put(ZONESXML, "Zones xml file, if required by algorithm.");
		map.put(LINES, "Lines file, if required by algorithm. Uses zone IDs for reference");
		map.put(BALANCING,
				"Balancing vehicles between line. Possible parameters: same (returns to same line), return (return line), balanced (balances between lines)");
		map.put(DISTANCEMEASURE, "Mode in which distance is measured. One of: beeline, earliestArrival");
		map.put(VEHICLESONDISPATCH, "Number of vehicles dispatched at the same time - per line");
		map.put(VEHCAP, "Vehicle capacity per vehicle.");
		map.put(DESTINATIONID, "Common destination link id for statebased optimizer");

		return map;
	}

	public String getTaxiIdentifier() {
		return taxiIdentifier;
	}

	/**
	 * @return the destinationLinkId
	 */
	public String getDestinationLinkId() {
		return destinationLinkId;
	}
	
	public URL getVehiclesFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.vehiclesFile);
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

	public double getDetourFactor() {
		return detourFactor;
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

	public URL getLinesFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.linesFile);
	}	

	public URL getZonesShpFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.zonesShpFile);
	}

	public URL getZonesXmlFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.zonesXmlFile);
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
	public boolean isPrebookTrips() {
		return prebookTrips;
	}
}
