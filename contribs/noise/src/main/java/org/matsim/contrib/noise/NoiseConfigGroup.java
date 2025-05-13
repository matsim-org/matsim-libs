/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 *
 */
package org.matsim.contrib.noise;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.StringUtils;

import java.net.URL;
import java.util.*;


/**
 * Provides the parameters required to build a simple grid with some basic spatial functionality.
 * Provides the parameters required to compute noise emissions, immissions and damages.
 *
 * @author ikaddoura, nkuehnel
 */
public final class NoiseConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "noise";
	private static final String RECEIVER_POINT_GAP = "receiverPointGap";
	private static final String TRANSFORMATION_FACTORY = "transformationFactory";
	private static final String CONSIDERED_ACTIVITIES_FOR_DAMAGE_CALCULATION = "consideredActivitiesForDamageCalculation";
	private static final String CONSIDERED_ACTIVITIES_FOR_RECEIVER_POINT_GRID = "consideredActivitiesForReceiverPointGrid";
	private static final String RECEIVER_POINTS_GRID_MIN_X = "receiverPointsGridMinX";
	private static final String RECEIVER_POINTS_GRID_MAX_X = "receiverPointsGridMaxX";
	private static final String RECEIVER_POINTS_GRID_MIN_Y = "receiverPointsGridMinY";
	private static final String RECEIVER_POINTS_GRID_MAX_Y = "receiverPointsGridMaxY";
	private static final String RECEIVER_POINTS_CSV_FILE = "receiverPointsCSVFile";
	private static final String RECEIVER_POINTS_CSV_FILE_COORDINATE_SYSTEM = "receiverPointsCSVFileCoordinateSystem";
	private static final String ANNUAL_COST_RATE = "annualCostRate";
	private static final String TIME_BIN_SIZE_NOISE_COMPUTATION = "timeBinSizeNoiseComputation";
	private static final String SCALE_FACTOR = "scaleFactor";
	private static final String RELEVANT_RADIUS = "relevantRadius";
	private static final String TUNNEL_LINK_ID_FILE = "tunnelLinkIdFile";
	private static final String TUNNEL_LINK_IDS = "tunnelLinkIDs";
	private static final String WRITE_OUTPUT_ITERATION = "writeOutputIteration";
	private static final String USE_ACTUAL_SPEED_LEVEL = "useActualSpeedLevel";
	private static final String ALLOW_FOR_SPEEDS_OUTSIDE_THE_VALID_RANGE = "allowForSpeedsOutsideTheValidRange";
	private static final String THROW_NOISE_EVENTS_AFFECTED = "throwNoiseEventsAffected";
	private static final String COMPUTE_NOISE_DAMAGES = "computeNoiseDamages";
	private static final String COMPUTE_CAUSING_AGENTS = "computeCausingAgents";
	private static final String THROW_NOISE_EVENTS_CAUSED = "throwNoiseEventsCaused";
	private static final String COMPUTE_POPULATION_UNITS = "computePopulationUnits";
	private static final String INTERNALIZE_NOISE_DAMAGES = "internalizeNoiseDamages";
	private static final String COMPUTE_AVG_NOISE_COST_PER_LINK_AND_TIME = "computeAvgNoiseCostPerLinkAndTime";
	private static final String HGV_ID_PREFIXES = "hgvIdPrefixes";
	private static final String BUS_ID_IDENTIFIER = "busIdIdentifier";
	private static final String NOISE_TOLL_FACTOR = "noiseTollFactor";
	private static final String NOISE_ALLOCATION_APPROACH = "noiseAllocationApproach";
	private static final String RECEIVER_POINT_GAP_CMT = "horizontal and vertical distance between receiver points in x-/y-coordinate units";
	private static final String WRITE_OUTPUT_ITERATION_CMT = "Specifies how often the noise-specific output is written out.";
	private static final String CONSIDER_NOISE_BARRIERS = "considerNoiseBarriers";
	private static final String CONSIDER_NOISE_REFLECTION = "considerNoiseReflection";
	private static final String NOISE_BARRIERS_GEOJSON_FILE = "noiseBarriersGeojsonPath";
	private static final String NOISE_BARRIERS_SOURCE_CRS = "source coordinate reference system of noise barriers geojson file";
	private static final String NETWORK_MODES_TO_IGNORE = "networkModesToIgnore";
	private static final String NOISE_COMPUTATION_METHOD = "noiseComputationMethod";
	private static final String USE_DEM = "useDGM";
	private static final String DEM_FILE = "DGMFile";

	public NoiseConfigGroup() {
		super(GROUP_NAME);
	}

	private static final Logger log = LogManager.getLogger(NoiseConfigGroup.class);

	private double receiverPointGap = 250.;

	private String[] consideredActivitiesForReceiverPointGrid = {"home*", "work*"};
	private String[] consideredActivitiesForDamageCalculation = {"home*", "work*"};

	private double receiverPointsGridMinX = 0.;
	private double receiverPointsGridMinY = 0.;
	private double receiverPointsGridMaxX = 0.;
	private double receiverPointsGridMaxY = 0.;

	private String receiverPointsCSVFile = null;
	private String receiverPointsCSVFileCoordinateSystem = TransformationFactory.DHDN_SoldnerBerlin;

	private static double annualCostRateEws = (85.0 / (1.95583)) * (Math.pow(1.02, (2014 - 1995)));
	private double annualCostRate = annualCostRateEws;
	// -- 1st term is EWS value (85DM/"dB(A) above German threshold value") converted to Euro
	// -- 2nd term is 2 pct inflation

	private double timeBinSizeNoiseComputation = 3600.0;
	private double scaleFactor = 1.;
	private double relevantRadius = 500.;
	private String tunnelLinkIdFile = null;
	private int writeOutputIteration = 10;
	private boolean useActualSpeedLevel = true;
	private boolean allowForSpeedsOutsideTheValidRange = false;

	private boolean throwNoiseEventsAffected = true;
	private boolean computeNoiseDamages = true;
	private boolean internalizeNoiseDamages = true; // throw money events based on caused noise cost
	private boolean computeAvgNoiseCostPerLinkAndTime = true;
	private boolean computeCausingAgents = true;
	private boolean throwNoiseEventsCaused = true;
	private boolean computePopulationUnits = true;

	public enum NoiseAllocationApproach {
		AverageCost, MarginalCost
	}

	private NoiseAllocationApproach noiseAllocationApproach = NoiseAllocationApproach.AverageCost;

	private String[] hgvIdPrefixes = {"lkw", "truck", "freight"};
	private Set<String> networkModesToIgnore = CollectionUtils.stringArrayToSet( new String[]{TransportMode.bike, TransportMode.walk, TransportMode.transit_walk, TransportMode.non_network_walk} );
	private Set<String> busIdIdentifier = new HashSet<>();
	private Set<Id<Link>> tunnelLinkIDs = new HashSet<>();

	private double noiseTollFactor = 1.0;

	private boolean considerNoiseBarriers = false;
	private boolean considerNoiseReflection = false;
	private String noiseBarriersFilePath = null;
	private String noiseBarriersSourceCrs = null;

	private boolean useDEM = false;
	private String demFile = null;

	public enum NoiseComputationMethod {
		RLS90, RLS19
	}

	private NoiseComputationMethod noiseComputationMethod = NoiseComputationMethod.RLS90;

	// ########################################################################################################

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();

		comments.put(RECEIVER_POINT_GAP, RECEIVER_POINT_GAP_CMT);
		comments.put(TRANSFORMATION_FACTORY, "coordinate system; so far only tested for 'TransformationFactory.DHDN_GK4'");
		comments.put(CONSIDERED_ACTIVITIES_FOR_DAMAGE_CALCULATION, "Specifies the activity types that are considered when computing noise damages (= the activities at which being exposed to noise results in noise damages). A list of the exact activity types, e.g. 'home,work_8hours,work_4hours', the prefixes 'home*,work*' or both, e.g. 'home,work*'.\"");
		comments.put(CONSIDERED_ACTIVITIES_FOR_RECEIVER_POINT_GRID, "Creates a grid of noise receiver points which contains all agents' activity locations of the specified types. A list of the exact activity types, e.g. 'home,work_8hours,work_4hours', the prefixes 'home*,work*' or both, e.g. 'home,work*'.\"");
		comments.put(RECEIVER_POINTS_GRID_MIN_X, "Specifies a boundary coordinate min/max x/y value of the receiver point grid. "
			+ "0.0 means the boundary coordinates are ignored and the grid is created based on the agents' activity coordinates of the specified activity types "
			+ "(see parameter 'consideredActivitiesForReceiverPointGrid').");
		comments.put(RECEIVER_POINTS_GRID_MAX_X, "Specifies a boundary coordinate min/max x/y value of the receiver point grid. "
			+ "0.0 means the boundary coordinates are ignored and the grid is created based on the agents' activity coordinates of the specified activity types "
			+ "(see parameter 'consideredActivitiesForReceiverPointGrid').");
		comments.put(RECEIVER_POINTS_GRID_MIN_Y, "Specifies a boundary coordinate min/max x/y value of the receiver point grid. "
			+ "0.0 means the boundary coordinates are ignored and the grid is created based on the agents' activity coordinates of the specified activity types "
			+ "(see parameter 'consideredActivitiesForReceiverPointGrid').");
		comments.put(RECEIVER_POINTS_GRID_MAX_Y, "Specifies a boundary coordinate min/max x/y value of the receiver point grid. "
			+ "0.0 means the boundary coordinates are ignored and the grid is created based on the agents' activity coordinates of the specified activity types "
			+ "(see parameter 'consideredActivitiesForReceiverPointGrid').");
		comments.put(RECEIVER_POINTS_CSV_FILE, "A csv file which provides the ReceiverPoint coordinates (first column: id, second column: x-coordinate, third column: y-coordinate, separator: ',')");
		comments.put(RECEIVER_POINTS_CSV_FILE_COORDINATE_SYSTEM, "The coordinate reference system of the provided ReceiverPoint csv file.");
		comments.put(ANNUAL_COST_RATE, "annual noise cost rate [in EUR per exposed pulation unit]; following the German EWS approach");
		comments.put(TIME_BIN_SIZE_NOISE_COMPUTATION, "Specifies the temporal resolution, i.e. the time bin size [in seconds] to compute noise levels.");
		comments.put(SCALE_FACTOR, "Set to '1.' for a 100 percent sample size. Set to '10.' for a 10 percent sample size. Set to '100.' for a 1 percent sample size.");
		comments.put(RELEVANT_RADIUS, "Specifies the radius [in coordinate units] around each receiver point links are taken into account.");
		comments.put(TUNNEL_LINK_ID_FILE, "Specifies a csv file which contains all tunnel link IDs.");
		comments.put(TUNNEL_LINK_IDS, "Specifies the tunnel link IDs. Will be ignored in case a the tunnel link IDs are provided as file (see parameter 'tunnelLinkIdFile').");

		comments.put(WRITE_OUTPUT_ITERATION, WRITE_OUTPUT_ITERATION_CMT);
		comments.put(USE_ACTUAL_SPEED_LEVEL, "Set to 'true' if the actual speed level should be used to compute noise levels. Set to 'false' if the freespeed level should be used to compute noise levels.");
		comments.put(ALLOW_FOR_SPEEDS_OUTSIDE_THE_VALID_RANGE, "Set to 'true' if speed levels below 30 km/h or above 80 km/h (HGV) / 130 km/h (car) should be used to compute noise levels. Set to 'false' if speed levels outside of the valid range should not be used to compute noise levels (recommended).");

		comments.put(THROW_NOISE_EVENTS_AFFECTED, "Set to 'true' if noise events (providing information about the affected agent) should be thrown. Otherwise set to 'false'.");
		comments.put(COMPUTE_NOISE_DAMAGES, "Set to 'true' if noise damages should be computed. Otherwise set to 'false'.");
		comments.put(COMPUTE_CAUSING_AGENTS, "Set to 'true' if the noise damages should be traced back and a causing agent should be identified. Otherwise set to 'false'.");
		comments.put(THROW_NOISE_EVENTS_CAUSED, "Set to 'true' if noise events (providing information about the causing agent) should be thrown. Otherwise set to 'false'.");
		comments.put(COMPUTE_POPULATION_UNITS, "Set to 'true' if population densities should be computed. Otherwise set to 'false'.");
		comments.put(INTERNALIZE_NOISE_DAMAGES, "Set to 'true' if money events should be thrown based on the caused noise damages. Otherwise set to 'false'.");
		comments.put(COMPUTE_AVG_NOISE_COST_PER_LINK_AND_TIME, "Set to 'true' if average noise cost per link and time bin should be computed (required by the default noise travel distutility uesed for routing)."
			+ "Set to 'false' if you use your own statistics for your own travel disutility.");

		comments.put(HGV_ID_PREFIXES, "Specifies the HGV (heavy goods vehicles, trucks) ID prefix.");
		comments.put(BUS_ID_IDENTIFIER, "Specifies the public transit vehicle ID identifiers. Buses are treated as HGV, other public transit vehicles are neglected.");

		comments.put(NOISE_TOLL_FACTOR, "To be used for sensitivity analysis. Default: 1.0 (= the parameter has no effect)");

		comments.put(CONSIDER_NOISE_BARRIERS, "Set to 'true' if noise barriers / building shielding should be considered. Otherwise set to 'false'.");
		comments.put(CONSIDER_NOISE_REFLECTION, "Set to 'true' if reflections should be considered. Otherwise set to 'false'. Has a considerable performance impact.");
		comments.put(NOISE_BARRIERS_GEOJSON_FILE, "Path to the geojson file for noise barriers.");
		comments.put(NOISE_BARRIERS_SOURCE_CRS, "Source coordinate reference system of noise barriers geojson file.");

		comments.put(USE_DEM, "Set to 'true' if a DEM (digital elevation model) should be used for road gradients. Otherwise set to 'false'.");
		comments.put(DEM_FILE, "Path to the geoTiff file of the DEM.");

		comments.put(NETWORK_MODES_TO_IGNORE, "Specifies the network modes to be excluded from the noise computation. By default, the following modes are excluded: [bike, walk, transit_walk, non_network_walk].");

		comments.put(NOISE_COMPUTATION_METHOD, "Specifies the computation method of different guidelines: " + Arrays.toString(NoiseComputationMethod.values()));

		return comments;
	}

	// ########################################################################################################

	@Override
	protected void checkConsistency(Config config) {
		this.checkGridParametersForConsistency();
		this.checkNoiseParametersForConsistency(config);
		if (Math.abs(this.getAnnualCostRate() - annualCostRateEws) < 0.001) {
			log.warn("you are using old EWS noise annual cost rates; please go through https://ec.europa" +
				".eu/transport/themes/sustainable/studies/sustainable_en to find mor modern values.  kai/Ihab, dec'19");
		}
	}

	private void checkGridParametersForConsistency() {

		List<String> consideredActivitiesForReceiverPointGridList = new ArrayList<>();
		List<String> consideredActivitiesForDamagesList = new ArrayList<>();

		Collections.addAll(consideredActivitiesForDamagesList, consideredActivitiesForDamageCalculation);
		consideredActivitiesForReceiverPointGridList.addAll(Arrays.asList(consideredActivitiesForReceiverPointGrid));

		if (this.receiverPointGap == 0.) {
			throw new RuntimeException("The receiver point gap is 0. Aborting...");
		}

		if (consideredActivitiesForReceiverPointGridList.size() == 0 && this.receiverPointsGridMinX == 0. && this.receiverPointsGridMinY == 0. && this.receiverPointsGridMaxX == 0. && receiverPointsGridMaxY == 0.) {
			throw new RuntimeException("NEITHER providing a considered activity type for the minimum and maximum coordinates of the receiver point grid area "
				+ "NOR providing receiver point grid minimum and maximum coordinates. Aborting...");
		}
	}

	private void checkNoiseParametersForConsistency(Config config) {

		if (this.internalizeNoiseDamages) {

			// required for internalization
			if (this.computeCausingAgents == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setComputeCausingAgents(true);
			}

			// required for internalization, i.e. the scoring
			if (this.throwNoiseEventsCaused == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setThrowNoiseEventsCaused(true);
			}

		}

		if (this.computeCausingAgents
			|| this.internalizeNoiseDamages
			|| this.throwNoiseEventsAffected
			|| this.throwNoiseEventsCaused
		) {

			// required
			if (this.computeNoiseDamages == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setComputeNoiseDamages(true);
			}

			if (this.computePopulationUnits == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setComputePopulationUnits(true);
			}
		}

		if (this.computeNoiseDamages) {

			// required
			if (this.computePopulationUnits == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setComputePopulationUnits(true);
			}
		}

		if (this.throwNoiseEventsCaused) {

			// required
			if (this.computeCausingAgents == false) {
				log.warn("Inconsistent parameters will be adjusted:");
				this.setComputeCausingAgents(true);
			}
		}

		if (this.useActualSpeedLevel && this.allowForSpeedsOutsideTheValidRange) {
			log.warn("Using the actual vehicle speeds for the noise computation may result in very low speed levels due to congestion."
				+ " The RLS computation approach defines a range of valid speed levels: for cars: 30-130 km/h; for HGV: 30-80 km/h."
				+ " 20 km/h or 10 km/h may still result in an 'okay' estimate of the traffic noise. However, 1 km/h or lower speeds will definitly make no sense."
				+ " It is therefore recommended not to use speeds outside of the range of valid parameters!");
		}

		if(considerNoiseReflection) {
			if (!this.considerNoiseBarriers) {
				if (this.noiseBarriersFilePath == null || "".equals(this.noiseBarriersFilePath)) {
					log.warn("Cannot consider noise reflection without a specified file path to the geojson file of barriers / buildings.");
					this.considerNoiseBarriers = false;
				}
			}
		}
		if (this.considerNoiseBarriers) {
			if (this.noiseBarriersFilePath == null || "".equals(this.noiseBarriersFilePath)) {
				log.warn("Cannot consider noise barriers without a specified file path to the geojson file of barriers / buildings.");
				this.considerNoiseBarriers = false;
			}
		}

		List<String> walkAndBikeModes = List.of(TransportMode.bike, TransportMode.walk, TransportMode.transit_walk, TransportMode.non_network_walk);
		String exclude = "[";
		for (String mode : walkAndBikeModes){
			exclude += mode + ",";
		}
		exclude = exclude.substring(0, exclude.lastIndexOf(',')) + "]";
		String warning = "You should set networkModesToIgnore such that all of the standard walk and bike modes are included!" +
			" These are " + exclude + ".";
		if (this.networkModesToIgnore.isEmpty() ||
			! this.networkModesToIgnore.containsAll(walkAndBikeModes)){
			boolean bikeOrWalkIsNetworkMode = config.routing().getNetworkModes().stream()
				.filter(networkMode -> networkModesToIgnore.contains(networkMode))
				.findAny()
				.isPresent();
			if (bikeOrWalkIsNetworkMode){
				//TODO: use enum for consistencyCheckLevel and replace all RunTimeExceptions thrown within this class.
				throw new RuntimeException(warning + " You configured one of these modes as networkMode in config().routing.getNetworkModes()! This will lead to wrong results!" +
					"Make sure ignore all network modes that do not model a car, a heavy-goods-vehicle (HGV) or a transit vehicle to be considered by the noise analysis!!! Will abort now...");
			} else {
				log.warn( warning +
					" Otherwise, your results will turn wrong as soon as you are routing these modes on the network (which luckily appears not to be the case in your current setup).");
			}
		}

		Set<String> defaultHGVIdPrefixes = Set.of("lkw", "truck", "freight");

		if (! defaultHGVIdPrefixes.stream().allMatch(defaultPrefix ->
			Arrays.stream(this.hgvIdPrefixes).anyMatch(prefix -> defaultPrefix.equals(prefix)))){
			log.warn("You are not considering all of [lkw, truck, freight] as HGV prefixes, which you should! Especially when you use scenarios created by VSP!!");
		}
	}

	// ########################################################################################################

	@StringGetter(RECEIVER_POINT_GAP)
	public double getReceiverPointGap() {
		return receiverPointGap;
	}

	/**
	 * @param receiverPointGap -- {@value #RECEIVER_POINT_GAP_CMT}
	 */
	@StringSetter(RECEIVER_POINT_GAP)
	public void setReceiverPointGap(double receiverPointGap) {
		log.info("setting the horizontal/vertical distance between each receiver point to " + receiverPointGap);
		this.receiverPointGap = receiverPointGap;
	}

	@StringGetter(RECEIVER_POINTS_GRID_MIN_X)
	public double getReceiverPointsGridMinX() {
		return receiverPointsGridMinX;
	}

	@StringSetter(RECEIVER_POINTS_GRID_MIN_X)
	public void setReceiverPointsGridMinX(double receiverPointsGridMinX) {
		log.info("setting receiverPoints grid MinX Coordinate to " + receiverPointsGridMinX);
		this.receiverPointsGridMinX = receiverPointsGridMinX;
	}

	@StringGetter(RECEIVER_POINTS_GRID_MIN_Y)
	public double getReceiverPointsGridMinY() {
		return receiverPointsGridMinY;
	}

	@StringSetter(RECEIVER_POINTS_GRID_MIN_Y)
	public void setReceiverPointsGridMinY(double receiverPointsGridMinY) {
		log.info("setting receiverPoints grid MinY Coordinate to " + receiverPointsGridMinY);
		this.receiverPointsGridMinY = receiverPointsGridMinY;
	}

	@StringGetter(RECEIVER_POINTS_GRID_MAX_X)
	public double getReceiverPointsGridMaxX() {
		return receiverPointsGridMaxX;
	}

	@StringSetter(RECEIVER_POINTS_GRID_MAX_X)
	public void setReceiverPointsGridMaxX(double receiverPointsGridMaxX) {
		log.info("setting receiverPoints grid MaxX Coordinate to " + receiverPointsGridMaxX);
		this.receiverPointsGridMaxX = receiverPointsGridMaxX;
	}

	@StringGetter(RECEIVER_POINTS_GRID_MAX_Y)
	public double getReceiverPointsGridMaxY() {
		return receiverPointsGridMaxY;
	}

	@StringSetter(RECEIVER_POINTS_GRID_MAX_Y)
	public void setReceiverPointsGridMaxY(double receiverPointsGridMaxY) {
		log.info("setting receiverPoints grid MaxY Coordinate to " + receiverPointsGridMaxY);
		this.receiverPointsGridMaxY = receiverPointsGridMaxY;
	}

	public String[] getConsideredActivitiesForReceiverPointGridArray() {
		return consideredActivitiesForReceiverPointGrid;
	}

	public void setConsideredActivitiesForReceiverPointGridArray(String[] consideredActivitiesForReceiverPointGrid) {
		log.info("setting considered activities for receiver point grid to: ");
		for (int i = 0; i < consideredActivitiesForReceiverPointGrid.length; i++) {
			log.info(consideredActivitiesForReceiverPointGrid[i]);
		}
		this.consideredActivitiesForReceiverPointGrid = consideredActivitiesForReceiverPointGrid;
	}

	public String[] getConsideredActivitiesForDamageCalculationArray() {
		return consideredActivitiesForDamageCalculation;
	}

	public void setConsideredActivitiesForDamageCalculationArray(String[] consideredActivitiesForSpatialFunctionality) {
		log.info("setting considered activities for spatial functionality to: ");
		for (int i = 0; i < consideredActivitiesForSpatialFunctionality.length; i++) {
			log.info(consideredActivitiesForSpatialFunctionality[i]);
		}
		this.consideredActivitiesForDamageCalculation = consideredActivitiesForSpatialFunctionality;
	}

	@StringGetter(CONSIDERED_ACTIVITIES_FOR_RECEIVER_POINT_GRID)
	private String getConsideredActivitiesForReceiverPointGrid() {
		return CollectionUtils.arrayToString(consideredActivitiesForReceiverPointGrid);
	}

	@StringSetter(CONSIDERED_ACTIVITIES_FOR_RECEIVER_POINT_GRID)
	public void setConsideredActivitiesForReceiverPointGrid(String consideredActivitiesForReceiverPointGridString) {
		this.setConsideredActivitiesForReceiverPointGridArray(CollectionUtils.stringToArray(consideredActivitiesForReceiverPointGridString));
	}

	@StringGetter(CONSIDERED_ACTIVITIES_FOR_DAMAGE_CALCULATION)
	public String getConsideredActivitiesForDamageCalculation() {
		return CollectionUtils.arrayToString(consideredActivitiesForDamageCalculation);
	}

	@StringSetter(CONSIDERED_ACTIVITIES_FOR_DAMAGE_CALCULATION)
	public void setConsideredActivitiesForDamageCalculation(String consideredActivitiesForSpatialFunctionalityString) {
		this.setConsideredActivitiesForDamageCalculationArray(CollectionUtils.stringToArray(consideredActivitiesForSpatialFunctionalityString));
	}

	// ###

	@StringGetter(THROW_NOISE_EVENTS_AFFECTED)
	public boolean isThrowNoiseEventsAffected() {
		return throwNoiseEventsAffected;
	}

	@StringSetter(THROW_NOISE_EVENTS_AFFECTED)
	public void setThrowNoiseEventsAffected(boolean throwNoiseEventsAffected) {
		log.info("Throwing noise events for the affected agents: " + throwNoiseEventsAffected);
		this.throwNoiseEventsAffected = throwNoiseEventsAffected;
	}

	@StringGetter(THROW_NOISE_EVENTS_CAUSED)
	public boolean isThrowNoiseEventsCaused() {
		return throwNoiseEventsCaused;
	}

	@StringSetter(THROW_NOISE_EVENTS_CAUSED)
	public void setThrowNoiseEventsCaused(boolean throwNoiseEventsCaused) {
		log.info("Throwing noise events for the causing agents: " + throwNoiseEventsCaused);
		this.throwNoiseEventsCaused = throwNoiseEventsCaused;
	}

	@StringGetter(COMPUTE_CAUSING_AGENTS)
	public boolean isComputeCausingAgents() {
		return computeCausingAgents;
	}

	@StringSetter(COMPUTE_CAUSING_AGENTS)
	public void setComputeCausingAgents(boolean computeCausingAgents) {
		log.info("Allocating the noise damages to the causing agents: " + computeCausingAgents);
		this.computeCausingAgents = computeCausingAgents;
	}

	@StringSetter(ANNUAL_COST_RATE)
	public void setAnnualCostRate(double annualCostRate) {
		log.info("setting the annual cost rate to " + annualCostRate);
		this.annualCostRate = annualCostRate;
	}

	@StringSetter(TIME_BIN_SIZE_NOISE_COMPUTATION)
	public void setTimeBinSizeNoiseComputation(double timeBinSizeNoiseComputation) {
		log.info("setting the time bin size for the computation of noise to " + timeBinSizeNoiseComputation);
		this.timeBinSizeNoiseComputation = timeBinSizeNoiseComputation;
	}

	@StringSetter(SCALE_FACTOR)
	public void setScaleFactor(double scaleFactor) {
		log.info("setting the scale factor to " + scaleFactor);
		this.scaleFactor = scaleFactor;
	}

	@StringSetter(RELEVANT_RADIUS)
	public void setRelevantRadius(double relevantRadius) {
		log.info("setting the radius of relevant links around each receiver point to " + relevantRadius);
		this.relevantRadius = relevantRadius;
	}

	@StringGetter(ANNUAL_COST_RATE)
	public double getAnnualCostRate() {
		return annualCostRate;
	}

	@StringGetter(TIME_BIN_SIZE_NOISE_COMPUTATION)
	public double getTimeBinSizeNoiseComputation() {
		return timeBinSizeNoiseComputation;
	}

	@StringGetter(SCALE_FACTOR)
	public double getScaleFactor() {
		return scaleFactor;
	}

	@StringGetter(RELEVANT_RADIUS)
	public double getRelevantRadius() {
		return relevantRadius;
	}

	@StringGetter(INTERNALIZE_NOISE_DAMAGES)
	public boolean isInternalizeNoiseDamages() {
		return internalizeNoiseDamages;
	}

	@StringSetter(INTERNALIZE_NOISE_DAMAGES)
	public void setInternalizeNoiseDamages(boolean internalizeNoiseDamages) {
		log.info("Internalizing noise damages: " + internalizeNoiseDamages);
		this.internalizeNoiseDamages = internalizeNoiseDamages;
	}

	@StringGetter(COMPUTE_NOISE_DAMAGES)
	public boolean isComputeNoiseDamages() {
		return computeNoiseDamages;
	}

	@StringSetter(COMPUTE_NOISE_DAMAGES)
	public void setComputeNoiseDamages(boolean computeNoiseDamages) {
		log.info("Computing noise damages: " + computeNoiseDamages);
		this.computeNoiseDamages = computeNoiseDamages;
	}

	@StringGetter(NOISE_ALLOCATION_APPROACH)
	public NoiseAllocationApproach getNoiseAllocationApproach() {
		return noiseAllocationApproach;
	}

	@StringSetter(NOISE_ALLOCATION_APPROACH)
	public void setNoiseAllocationApproach(NoiseAllocationApproach noiseAllocationApproach) {
		log.info("Noise allocation approach: " + noiseAllocationApproach);
		this.noiseAllocationApproach = noiseAllocationApproach;
	}

	@StringGetter(WRITE_OUTPUT_ITERATION)
	public int getWriteOutputIteration() {
		return writeOutputIteration;
	}

	/**
	 * @param writeOutputIteration -- {@value #WRITE_OUTPUT_ITERATION_CMT}
	 */
	@StringSetter(WRITE_OUTPUT_ITERATION)
	public void setWriteOutputIteration(int writeOutputIteration) {
		log.info("Writing output every " + writeOutputIteration + " iteration.");
		this.writeOutputIteration = writeOutputIteration;
	}

	@StringSetter(TUNNEL_LINK_ID_FILE)
	public void setTunnelLinkIdFile(String tunnelLinkIdFile) {
		log.info("setting file which contains the tunnel link Ids to " + tunnelLinkIdFile + ".");
		this.tunnelLinkIdFile = tunnelLinkIdFile;
	}

	@StringGetter(TUNNEL_LINK_ID_FILE)
	public String getTunnelLinkIdFile() {
		return tunnelLinkIdFile;
	}

	@StringGetter(USE_ACTUAL_SPEED_LEVEL)
	public boolean isUseActualSpeedLevel() {
		return useActualSpeedLevel;
	}

	@StringSetter(USE_ACTUAL_SPEED_LEVEL)
	public void setUseActualSpeedLevel(boolean useActualSpeedLevel) {
		log.info("Using the actual speed level for noise calculation: " + useActualSpeedLevel);
		this.useActualSpeedLevel = useActualSpeedLevel;
	}

	@StringGetter(COMPUTE_POPULATION_UNITS)
	public boolean isComputePopulationUnits() {
		return computePopulationUnits;
	}

	@StringSetter(COMPUTE_POPULATION_UNITS)
	public void setComputePopulationUnits(boolean computePopulationUnits) {
		log.info("Computing population units: " + computePopulationUnits);
		this.computePopulationUnits = computePopulationUnits;
	}

	@StringGetter(ALLOW_FOR_SPEEDS_OUTSIDE_THE_VALID_RANGE)
	public boolean isAllowForSpeedsOutsideTheValidRange() {
		return allowForSpeedsOutsideTheValidRange;
	}

	@StringSetter(ALLOW_FOR_SPEEDS_OUTSIDE_THE_VALID_RANGE)
	public void setAllowForSpeedsOutsideTheValidRange(boolean allowForSpeedsOutsideTheValidRange) {
		log.info("Allowing for speeds above or below the valid range (cars: 30-130 km/h; HGV: 30-80 km/h): " + allowForSpeedsOutsideTheValidRange);
		this.allowForSpeedsOutsideTheValidRange = allowForSpeedsOutsideTheValidRange;
	}

	// #######

	@StringGetter(HGV_ID_PREFIXES)
	private String getHgvIdPrefixes() {
		return CollectionUtils.arrayToString(hgvIdPrefixes);
	}

	@StringSetter(HGV_ID_PREFIXES)
	public void setHgvIdPrefixes(String hgvIdPrefixes) {
		this.setHgvIdPrefixesArray(CollectionUtils.stringToArray(hgvIdPrefixes));
	}

	@StringGetter(TUNNEL_LINK_IDS)
	private String getTunnelLinkIDs() {
		return this.linkIdSetToString(tunnelLinkIDs);
	}

	@StringSetter(TUNNEL_LINK_IDS)
	public void setTunnelLinkIDs(String tunnelLinkIDs) {
		this.setTunnelLinkIDsSet(stringToLinkIdSet(tunnelLinkIDs));
	}

	@StringGetter(RECEIVER_POINTS_CSV_FILE)
	public String getReceiverPointsCSVFile() {
		return receiverPointsCSVFile;
	}

	@StringSetter(RECEIVER_POINTS_CSV_FILE)
	public void setReceiverPointsCSVFile(String receiverPointsGridCSVFile) {
		this.receiverPointsCSVFile = receiverPointsGridCSVFile;
	}

	@StringGetter(RECEIVER_POINTS_CSV_FILE_COORDINATE_SYSTEM)
	public String getReceiverPointsCSVFileCoordinateSystem() {
		return receiverPointsCSVFileCoordinateSystem;
	}

	@StringSetter(RECEIVER_POINTS_CSV_FILE_COORDINATE_SYSTEM)
	public void setReceiverPointsCSVFileCoordinateSystem(String receiverPointsCSVFileCoordinateSystem) {
		this.receiverPointsCSVFileCoordinateSystem = receiverPointsCSVFileCoordinateSystem;
	}

	public void setHgvIdPrefixesArray(String[] hgvIdPrefix) {
		log.info("setting the HGV Id Prefixes to " + hgvIdPrefix.toString());
		this.hgvIdPrefixes = hgvIdPrefix;
	}

	public void setTunnelLinkIDsSet(Set<Id<Link>> tunnelLinkIDs) {
		log.info("setting tunnel link IDs to " + tunnelLinkIDs.toString());
		this.tunnelLinkIDs = tunnelLinkIDs;
	}

	public String[] getHgvIdPrefixesArray() {
		return hgvIdPrefixes;
	}

	public Set<Id<Link>> getTunnelLinkIDsSet() {
		return tunnelLinkIDs;
	}

	@StringGetter(BUS_ID_IDENTIFIER)
	private String getBusIdPrefixes() {
		return CollectionUtils.setToString(busIdIdentifier);
	}

	@StringSetter(BUS_ID_IDENTIFIER)
	public void setBusIdIdentifiers(String busIdPrefixes) {
		this.setBusIdIdentifierSet(CollectionUtils.stringToSet(busIdPrefixes));
	}

	public Set<String> getBusIdIdentifierSet() {
		return busIdIdentifier;
	}

	public void setBusIdIdentifierSet(Set<String> busIdPrefixes) {
		log.info("setting the bus Id identifiers to : " + busIdPrefixes.toString());
		this.busIdIdentifier = busIdPrefixes;
	}

	@StringGetter(NETWORK_MODES_TO_IGNORE)
	public String getNetworkModesToIgnore() {
		return CollectionUtils.setToString(networkModesToIgnore);
	}

	@StringSetter(NETWORK_MODES_TO_IGNORE)
	public void setNetworkModesToIgnore(String networkModesToIgnore) {
		this.setNetworkModesToIgnoreSet(CollectionUtils.stringToSet(networkModesToIgnore));
	}

	public Set<String> getNetworkModesToIgnoreSet() {
		return networkModesToIgnore;
	}

	public void setNetworkModesToIgnoreSet(Set<String> networkModesToIgnore) {
		log.info("setting the network modes to ignore to : " + networkModesToIgnore.toString());
		this.networkModesToIgnore = networkModesToIgnore;
	}

	private String linkIdSetToString(Set<Id<Link>> linkIds) {
		String linkIdsString = null;
		boolean first = true;
		for (Id<Link> id : linkIds) {
			if (first) {
				linkIdsString = id.toString();
				first = false;
			} else {
				linkIdsString = linkIdsString + "," + id;
			}
		}
		return linkIdsString;
	}

	private Set<Id<Link>> stringToLinkIdSet(String linkIds) {
		if (linkIds == null) {
			return Collections.emptySet();
		}
		String[] parts = StringUtils.explode(linkIds, ',');
		Set<Id<Link>> tmp = new LinkedHashSet<Id<Link>>();
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.length() > 0) {
				tmp.add(Id.createLinkId(trimmed.intern()));
			}
		}
		return tmp;
	}

	@StringGetter(NOISE_TOLL_FACTOR)
	public double getNoiseTollFactor() {
		return noiseTollFactor;
	}

	@StringSetter(NOISE_TOLL_FACTOR)
	public void setNoiseTollFactor(double noiseTollFactor) {
		this.noiseTollFactor = noiseTollFactor;
	}

	@StringGetter(COMPUTE_AVG_NOISE_COST_PER_LINK_AND_TIME)
	public boolean isComputeAvgNoiseCostPerLinkAndTime() {
		return computeAvgNoiseCostPerLinkAndTime;
	}

	@StringSetter(COMPUTE_AVG_NOISE_COST_PER_LINK_AND_TIME)
	public void setComputeAvgNoiseCostPerLinkAndTime(boolean computeAvgNoiseCostPerLinkAndTime) {
		this.computeAvgNoiseCostPerLinkAndTime = computeAvgNoiseCostPerLinkAndTime;
	}

	public URL getTunnelLinkIDsFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.getTunnelLinkIdFile());
	}

	@StringGetter(CONSIDER_NOISE_BARRIERS)
	public boolean isConsiderNoiseBarriers() {
		return this.considerNoiseBarriers;
	}

	@StringSetter(CONSIDER_NOISE_BARRIERS)
	public void setConsiderNoiseBarriers(boolean considerNoiseBarriers) {
		this.considerNoiseBarriers = considerNoiseBarriers;
	}

	@StringGetter(CONSIDER_NOISE_REFLECTION)
	public boolean isConsiderNoiseReflection() {
		return this.considerNoiseReflection;
	}

	@StringSetter(CONSIDER_NOISE_REFLECTION)
	public void setConsiderNoiseReflection(boolean considerNoiseReflection) {
		this.considerNoiseReflection = considerNoiseReflection;
	}

	@StringGetter(NOISE_BARRIERS_GEOJSON_FILE)
	public String getNoiseBarriersFilePath() {
		return this.noiseBarriersFilePath;
	}

	@StringSetter(NOISE_BARRIERS_GEOJSON_FILE)
	public void setNoiseBarriersFilePath(String noiseBarriersFilePath) {
		this.noiseBarriersFilePath = noiseBarriersFilePath;
	}

	@StringGetter(USE_DEM)
	public boolean isUseDEM() {
		return this.useDEM;
	}

	@StringSetter(USE_DEM)
	public void setUseDEM(boolean useDEM) {
		this.useDEM = useDEM;
	}

	@StringGetter(DEM_FILE)
	public String getDEMFile() {
		return this.demFile;
	}

	@StringSetter(DEM_FILE)
	public void setDEMFilePath(String demFilePath) {
		this.demFile = demFilePath;
	}

	@StringGetter(NOISE_BARRIERS_SOURCE_CRS)
	public String getNoiseBarriersSourceCRS() {
		return this.noiseBarriersSourceCrs;
	}

	@StringSetter(NOISE_BARRIERS_SOURCE_CRS)
	public void setNoiseBarriersSourceCRS(String noiseBarriersSourceCrs) {
		this.noiseBarriersSourceCrs = noiseBarriersSourceCrs;
	}

	@StringGetter(NOISE_COMPUTATION_METHOD)
	public NoiseComputationMethod getNoiseComputationMethod() {
		return this.noiseComputationMethod;
	}

	@StringSetter(NOISE_COMPUTATION_METHOD)
	public void setNoiseComputationMethod(NoiseComputationMethod noiseComputationMethod) {
		this.noiseComputationMethod = noiseComputationMethod;
	}
}
