package org.matsim.contrib.taxibus.run.configuration;

import java.net.URL;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.config.*;

public class TaxibusConfigGroup extends ReflectiveConfigGroup {

	private static final Logger log = Logger.getLogger(TaxibusConfigGroup.class);

	public static final String GROUP_NAME = "taxibusConfig";
	// general
	private static final String VEHICLES_FILE = "vehiclesFile";
	private static final String ALGORITHM = "algorithm";
	private static final String PICKUP_DURATION = "pickupDuration";
	private static final String DROPOFF_DURATION = "dropoffDuration";

	// clustered
	private static final String VEHCAP = "vehicleCapacity";
	private static final String VEHICLESONDISPATCH = "vehiclesDispatchedAtSameTime";
	private static final String CLUSTERING_PERIOD_MIN = "clustering_period_min";
	private static final String PREBOOK_PERIOD_MIN = "prebook_period_min";
	private static final String CLUSTERINGROUNDS = "clusteringRounds";
	private static final String MINOCCUPANCY = "minOccupancy";
	private static final String SERVICE_AREA_1_CENTROID_LINK = "serviceAreaCentroid_1_Link";
	private static final String SERVICE_AREA_2_CENTROID_LINK = "serviceAreaCentroid_2_Link";
	private static final String SERVICE_AREA_1_RADIUS = "serviceArea_1_Radius_m";
	private static final String SERVICE_AREA_2_RADIUS = "serviceArea_2_Radius_m";

	private static final String RETURN_TO_DEPOT = "ReturnToDepot";
	private static final String STOPSFILE = "stopsFile";
	private static final String MAXWALK = "maxWalkDistance";


	// general
	private String taxiIdentifier = "taxibus";
	private String vehiclesFile = null;
	private String stopsfile = null;
	private double maxWalkDistance = 500;
	private double pickupDuration = 60.0;
	private double dropoffDuration = 120.0;
	private String algorithm;
	// clustered
	private int numberOfVehiclesDispatchedAtSameTime = 8;
	private int vehCap = 8;
	private double clustering_period_min = 15;
	private double prebook_period_min = 15;
	private int clusteringRounds = 100;
	private double minOccupancy = 3;
	private String serviceAreaCentroid_1 = null;
	private String serviceAreaCentroid_2 = null;
	private double serviceArea_2_Radius_m = 20000;
	private double serviceArea_1_Radius_m = 20000;
	private boolean returnToDepot = false;
	
	

	public TaxibusConfigGroup() {
		super(GROUP_NAME);
		log.info("Loading Taxibus config group...");
	}

	public static TaxibusConfigGroup get(Config config) {
		return (TaxibusConfigGroup)config.getModules().get(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();

		map.put(VEHICLES_FILE, "Taxi Vehicles file");
		map.put(ALGORITHM, "Taxibus algorithms: Possible parameters are clustered_jsprit, jsprit");

		map.put(STOPSFILE, "Stop locations. If set, stops will be used for routing, otherwise remove line or set to null");
		map.put(VEHICLESONDISPATCH, "[Clustered_jsprit] Number of vehicles dispatched at the same time");
		map.put(VEHCAP, "[Clustered_jsprit] Vehicle capacity per vehicle.");
		map.put(CLUSTERING_PERIOD_MIN,
				"[Clustered_jsprit, jsprit] Period in minutes after which clustering is repeated. Default 15 minutes.");
		map.put(PREBOOK_PERIOD_MIN, "[Clustered_jsprit, jsprit]Prebooking period for requests. Default 15 minutes.");
		map.put(CLUSTERINGROUNDS, "[Clustered_jsprit]Rounds for clustering requests together. Default is 100.");
		map.put(MINOCCUPANCY,
				"[Clustered_jsprit]Minimum taxibus occupancy. Default is 3. If less requests occur, a single bus takes them all.");
		map.put(SERVICE_AREA_1_CENTROID_LINK,
				"[Clustered_jsprit, jsprit] Link Id that sets taxibus service area 1 centroid. Service areas may overlap for jsprit.");
		map.put(SERVICE_AREA_2_CENTROID_LINK,
				"[Clustered_jsprit, jsprit] Link Id that sets taxibus service area 2 centroid. Service areas may overlap for jsprit.");
		map.put(SERVICE_AREA_1_RADIUS,
				"[Clustered_jsprit, jsprit] Radius (in meters) around service area 1 where taxibus trips are possible");
		map.put(SERVICE_AREA_2_RADIUS,
				"[Clustered_jsprit, jsprit] Radius (in meters) around service area 2 where taxibus trips are possible");
		map.put(RETURN_TO_DEPOT,
				"[Clustered_jsprit, jsprit] Determines whether the bus returns to its depot (=startLink) after each ride");

		return map;
	}

	public String getTaxiIdentifier() {
		return taxiIdentifier;
	}

	/**
	 * @return the returnToDepot
	 */
	@StringGetter(RETURN_TO_DEPOT)
	public boolean isReturnToDepot() {
		return returnToDepot;
	}

	/**
	 * @param returnToDepot
	 *            the returnToDepot to set
	 */
	@StringSetter(RETURN_TO_DEPOT)
	public void setReturnToDepot(boolean returnToDepot) {
		this.returnToDepot = returnToDepot;
	}


	public URL getVehiclesFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.vehiclesFile);
	}

	@StringGetter(ALGORITHM)
	public String getAlgorithm() {
		return algorithm;
	}

	

	@StringGetter(PICKUP_DURATION)
	public double getPickupDuration() {
		return pickupDuration;
	}

	@StringGetter(DROPOFF_DURATION)
	public double getDropoffDuration() {
		return dropoffDuration;
	}

	@StringGetter(VEHICLESONDISPATCH)
	public int getNumberOfVehiclesDispatchedAtSameTime() {
		return numberOfVehiclesDispatchedAtSameTime;
	}

	@StringGetter(VEHCAP)
	public int getVehCap() {
		return vehCap;
	}

	@StringGetter(VEHICLES_FILE)
	public String getVehiclesFile() {
		return vehiclesFile;
	}

	@StringSetter(VEHICLES_FILE)
	public void setVehiclesFile(String vehiclesFile) {
		this.vehiclesFile = vehiclesFile;
	}

	@StringSetter(ALGORITHM)
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	@StringSetter(PICKUP_DURATION)
	public void setPickupDuration(double pickupDuration) {
		this.pickupDuration = pickupDuration;
	}

	@StringSetter(DROPOFF_DURATION)
	public void setDropoffDuration(double dropoffDuration) {
		this.dropoffDuration = dropoffDuration;
	}

	@StringSetter(VEHICLESONDISPATCH)
	public void setNumberOfVehiclesDispatchedAtSameTime(int numberOfVehiclesDispatchedAtSameTime) {
		this.numberOfVehiclesDispatchedAtSameTime = numberOfVehiclesDispatchedAtSameTime;
	}



	@StringGetter(CLUSTERING_PERIOD_MIN)
	public double getClustering_period_min() {
		return clustering_period_min;
	}

	@StringSetter(CLUSTERING_PERIOD_MIN)
	public void setClustering_period_min(double clustering_period_min) {
		this.clustering_period_min = clustering_period_min;
	}

	@StringGetter(PREBOOK_PERIOD_MIN)
	public double getPrebook_period_min() {
		return prebook_period_min;
	}

	@StringSetter(PREBOOK_PERIOD_MIN)
	public void setPrebook_period_min(double prebook_period_min) {
		this.prebook_period_min = prebook_period_min;
	}

	@StringGetter(CLUSTERINGROUNDS)
	public int getClusteringRounds() {
		return clusteringRounds;
	}

	@StringSetter(CLUSTERINGROUNDS)
	public void setClusteringRounds(int clusteringRounds) {
		this.clusteringRounds = clusteringRounds;
	}

	@StringGetter(MINOCCUPANCY)
	public double getMinOccupancy() {
		return minOccupancy;
	}

	@StringSetter(MINOCCUPANCY)
	public void setMinOccupancy(double minOccupancy) {
		this.minOccupancy = minOccupancy;
	}

	@StringSetter(VEHCAP)
	public void setVehCap(int vehCap) {
		this.vehCap = vehCap;
	}

	@StringGetter(SERVICE_AREA_1_CENTROID_LINK)
	public String getServiceAreaCentroid_1() {
		return serviceAreaCentroid_1;
	}

	@StringSetter(SERVICE_AREA_1_CENTROID_LINK)
	public void setServiceAreaCentroid_1(String serviceAreaCentroid_1) {
		this.serviceAreaCentroid_1 = serviceAreaCentroid_1;
	}

	@StringGetter(SERVICE_AREA_2_CENTROID_LINK)
	public String getServiceAreaCentroid_2() {
		return serviceAreaCentroid_2;
	}

	@StringSetter(SERVICE_AREA_2_CENTROID_LINK)
	public void setServiceAreaCentroid_2(String serviceAreaCentroid_2) {
		this.serviceAreaCentroid_2 = serviceAreaCentroid_2;
	}

	@StringGetter(SERVICE_AREA_2_RADIUS)
	public double getServiceArea_2_Radius_m() {
		return serviceArea_2_Radius_m;
	}

	@StringSetter(SERVICE_AREA_2_RADIUS)
	public void setServiceArea_2_Radius_m(double serviceArea_2_Radius_m) {
		this.serviceArea_2_Radius_m = serviceArea_2_Radius_m;
	}

	@StringGetter(SERVICE_AREA_1_RADIUS)
	public double getServiceArea_1_Radius_m() {
		return serviceArea_1_Radius_m;
	}

	@StringSetter(SERVICE_AREA_1_RADIUS)
	public void setServiceArea_1_Radius_m(double serviceArea_1_Radius_m) {
		this.serviceArea_1_Radius_m = serviceArea_1_Radius_m;
	}

	@StringGetter(STOPSFILE)
	public String getStopsfile() {
		return stopsfile;
	}

	@StringSetter(STOPSFILE)
	public void setStopsfile(String stopsfile) {
		this.stopsfile = stopsfile;
	}

	@StringGetter(MAXWALK)
	public double getMaxWalkDistance() {
		return maxWalkDistance;
	}

	@StringSetter(MAXWALK)
	public void setMaxWalkDistance(double maxWalkDistance) {
		this.maxWalkDistance = maxWalkDistance;
	}
	
	public URL getTransitStopsFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.stopsfile);
	}



}
