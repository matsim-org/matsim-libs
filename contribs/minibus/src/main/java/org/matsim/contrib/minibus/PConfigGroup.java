/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus;

import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.minibus.operator.BasicOperator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.vehicles.VehicleType.DoorOperationMode;

/**
 * Config group to configure p
 * 
 * @author aneumann
 *
 */
public final class PConfigGroup extends ConfigGroup{
	
	/**
	 * TODO [AN] This one has to be checked
	 */
	private static final long serialVersionUID = 4840713748058034511L;
	private static final Logger log = Logger.getLogger(PConfigGroup.class);
	
	public static enum StopLocationSelector {allCarLinks ("allCarLinks"), 
		junctionApproachesAndBetweenJunctions ("junctionApproachesAndBetweenJunctions");
		public final String name;
		StopLocationSelector(String name) {this.name = name;}
	}
	
	// Tags
	
	public static final String GROUP_NAME = "p";
	
	private static final String P_IDENTIFIER = "pIdentifier";
	private static final String MIN_X = "minX";
	private static final String MIN_Y = "minY";
	private static final String MAX_X = "maxX";
	private static final String MAX_Y = "maxY";
	private static final String SERVICEAREAFILE = "serviceAreaFile";
	private static final String OPERATOR_TYPE = "operatorType";
	private static final String NUMBER_OF_OPERATORS = "numberOfOperators";
	private static final String PAX_PER_VEHICLE = "paxPerVehicle";
	private static final String PCE = "passengerCarEquivalents";
	private static final String VEHICLE_MAXIMUM_VELOCITY = "vehicleMaximumVelocity";
	private static final String DELAY_PER_BOARDING_PASSENGER = "delayPerBoardingPassenger";
	private static final String DELAY_PER_ALIGHTING_PASSENGER = "delayPerAlightingPassenger";
	private static final String DOOR_OPERATION_MODE = "doorOperationMode";
	private static final String NUMBER_OF_ITERATIONS_FOR_PROSPECTING = "numberOfIterationsForProspecting";
	private static final String INITIAL_BUDGET = "initialBudget";
	private static final String COST_PER_VEHICLE_AND_DAY = "costPerVehicleAndDay";
	private static final String COST_PER_KILOMETER = "costPerKilometer";
	private static final String COST_PER_HOUR = "costPerHour";
	private static final String EARNINGS_PER_BOARDING_PASSENGER = "earningsPerBoardingPassenger";
	private static final String EARNINGS_PER_KILOMETER_AND_PASSENGER = "earningsPerKilometerAndPassenger";
	private static final String PRICE_PER_VEHICLE_BOUGHT = "pricePerVehicleBought";
	private static final String PRICE_PER_VEHICLE_SOLD = "pricePerVehicleSold";
	private static final String START_WITH_24_HOURS = "startWith24Hours";
	private static final String MIN_OPERATION_TIME = "minOperationTime";
	private static final String MIN_INITIAL_STOP_DISTANCE = "minInitialStopDistance";
	private static final String USEFRANCHISE = "useFranchise";
	private static final String WRITESTATS_INTERVAL = "writeStatsInterval";
	private static final String LOG_OPERATORS = "logOperators";
	private static final String WRITE_METRICS = "writeMetrics";
	private static final String WRITE_GEXF_STATS_INTERVAL = "writeGexfStatsInterval";
	private static final String ROUTE_PROVIDER = "routeProvider";
	private static final String SPEED_LIMIT_FOR_STOPS = "speedLimitForStops";
	private static final String PLANNING_SPEED_FACTOR = "planningSpeedFactor";
	private static final String DRIVER_REST_TIME = "driverRestTime";
	private static final String GRID_SIZE = "gridSize";
	private static final String TIMESLOT_SIZE = "timeSlotSize";
	private static final String USE_ADAPTIVE_NUMBER_OF_OPERATORS = "useAdaptiveNumberOfOperators";
	private static final String SHARE_OF_OPERATORS_WITH_PROFIT = "shareOfOperatorsWithProfit";
	private static final String DISABLE_CREATION_OF_NEW_OPERATORS_IN_ITERATION = "disableCreationOfNewOperatorsInIteration";
	private static final String REROUTE_AGENTS_STUCK = "reRouteAgentsStuck";
	private static final String PASSENGERS_BOARD_EVERY_LINE = "passengersBoardEveryLine";
	private static final String TRANSIT_SCHEDULE_TO_START_WITH = "transitScheduleToStartWith";
	private static final String MERGE_TRANSIT_LINE = "mergeTransitLine";
	private static final String PT_ENABLER = "ptEnabler";
	private static final String OPERATIONMODE = "OperationMode";
	private static final String TOPOTYPESFORSTOPS = "TopoTypesForStops";
	private static final String MIN_CAPACITY_FOR_STOPS = "minCapacityForStops";
	private static final String STOP_LOCATION_SELECTOR = "stopLocationSelector";
	private static final String STOP_LOCATION_SELECTOR_PARAMETER = "stopLocationSelectorParameter";
	
	private static final String PMODULE = "Module_";
	private static final String PMODULE_PROBABILITY = "ModuleProbability_";
	private static final String PMODULE_DISABLEINITERATION = "ModuleDisableInIteration_";
	private static final String PMODULE_PARAMETER = "ModuleParameter_";
	
	private static final String SUBSIDY_APPROACH = "subsidyApproach";
	
	// Defaults
	private String pIdentifier = "p_";
	private double minX = -Double.MAX_VALUE;
	private double minY = -Double.MAX_VALUE;	
	private double maxX = Double.MAX_VALUE;
	private double maxY = Double.MAX_VALUE;
	private String serviceAreaFile = "";
	private String operatorType = BasicOperator.OPERATOR_NAME;
	private int numberOfOperators = 1;
	private int paxPerVehicle = 10;
	private double passengerCarEquivalents = 1.0;
	private double vehicleMaximumVelocity = Double.POSITIVE_INFINITY;
	private double delayPerBoardingPassenger = 2.0;
	private double delayPerAlightingPassenger = 1.0;
	private DoorOperationMode doorOperationMode = DoorOperationMode.serial;
	private int numberOfIterationsForProspecting = 0;
	private double initialBudget = 0.0;
	private double costPerVehicleAndDay = 0.0;
	private double costPerKilometer = 0.30;
	private double costPerHour = 0.0;
	private boolean startWith24Hours = false;
	private double minOperationTime = 6 * 3600;
	private double minInitialStopDistance = 1.0;
	private double earningsPerBoardingPassenger = 0.0;
	private double earningsPerKilometerAndPassenger = 0.50;
	private double pricePerVehicleBought = 1000.0;
	private double pricePerVehicleSold = 1000.0;
	private boolean useFranchise = false;
	private int writeStatsInterval = 0;
	private boolean logOperators = false;
	private boolean writeMetrics = false;
	private int writeGexfStatsInterval = 0;
	private String routeProvider = "SimpleCircleScheduleProvider";
	private double speedLimitForStops = Double.MAX_VALUE;
	private double planningSpeedFactor = 1.0;
	private double driverRestTime = 1.0 * 60.0;
	private double gridSize = Double.MAX_VALUE;
	private double timeSlotSize = Double.MAX_VALUE;
	private boolean useAdaptiveNumberOfOperators = false;
	private double shareOfOperatorsWithProfit = 0.50;
	private int disableCreationOfNewOperatorsInIteration = Integer.MAX_VALUE;
	private boolean reRouteAgentsStuck = false;
	private boolean passengersBoardEveryLine = false;
	private String transitScheduleToStartWith = null;
	private boolean mergeTransitLine = false;
	private String ptEnabler = null;
	private String operationMode = TransportMode.pt;
	private String topoTypesForStops = null;
	private double minCapacityForStops = 0.0;
	private StopLocationSelector stopLocationSelector = StopLocationSelector.allCarLinks;
	private String stopLocationSelectorParameter = "";
	private String subsidyApproach = null;

	// Strategies
	private final LinkedHashMap<Id<PStrategySettings>, PStrategySettings> strategies = new LinkedHashMap<>();
	
	
	public PConfigGroup(){
		super(GROUP_NAME);
		log.info("Started...");
		log.warn("SerialVersionUID has to be checked. Current one is " + PConfigGroup.serialVersionUID);
	}
	
	// Setter
	
	@Override
	public void addParam(final String key, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;
		
		if (P_IDENTIFIER.equals(key)){
			this.pIdentifier = value;
		}else if (SERVICEAREAFILE.equals(key)) {
			this.serviceAreaFile = value;
		}else if (MIN_X.equals(key)) {
			this.minX = Double.parseDouble(value);
		} else if (MIN_Y.equals(key)) {
			this.minY = Double.parseDouble(value);
		} else if (MAX_X.equals(key)) {
			this.maxX = Double.parseDouble(value);
		} else if (MAX_Y.equals(key)) {
			this.maxY = Double.parseDouble(value);
		}else if (OPERATOR_TYPE.equals(key)){
			this.operatorType = value;
		} else if (NUMBER_OF_OPERATORS.equals(key)) {
			this.numberOfOperators = Integer.parseInt(value);
		} else if (NUMBER_OF_ITERATIONS_FOR_PROSPECTING.equals(key)) {
			this.numberOfIterationsForProspecting = Integer.parseInt(value);
		} else if (INITIAL_BUDGET.equals(key)) {
			this.initialBudget = Double.parseDouble(value);
		} else if (PAX_PER_VEHICLE.equals(key)) {
			this.paxPerVehicle = Integer.parseInt(value);
		} else if (PCE.equals(key)) {
			this.passengerCarEquivalents = Double.parseDouble(value);
		} else if (VEHICLE_MAXIMUM_VELOCITY.equals(key)) {
			this.vehicleMaximumVelocity = Double.parseDouble(value);
		} else if (DELAY_PER_BOARDING_PASSENGER.equals(key)) {
			this.delayPerBoardingPassenger = Double.parseDouble(value);
		} else if (DELAY_PER_ALIGHTING_PASSENGER.equals(key)) {
			this.delayPerAlightingPassenger = Double.parseDouble(value);
		} else if (DOOR_OPERATION_MODE.equals(key)) { 
			if (DoorOperationMode.serial.toString().equalsIgnoreCase(value)){
				this.doorOperationMode = DoorOperationMode.serial;
			} else if (DoorOperationMode.parallel.toString().equalsIgnoreCase(value)){
				this.doorOperationMode = DoorOperationMode.parallel;
			} 
		} else if (COST_PER_VEHICLE_AND_DAY.equals(key)){
			this.costPerVehicleAndDay = Double.parseDouble(value);
		} else if (COST_PER_KILOMETER.equals(key)){
			this.costPerKilometer = Double.parseDouble(value);
		} else if (COST_PER_HOUR.equals(key)){
			this.costPerHour = Double.parseDouble(value);
		} else if (EARNINGS_PER_BOARDING_PASSENGER.equals(key)){
			this.earningsPerBoardingPassenger = Double.parseDouble(value);
		} else if (EARNINGS_PER_KILOMETER_AND_PASSENGER.equals(key)){
			this.earningsPerKilometerAndPassenger = Double.parseDouble(value);
		} else if (PRICE_PER_VEHICLE_BOUGHT.equals(key)){
			this.pricePerVehicleBought = Double.parseDouble(value);
		} else if (PRICE_PER_VEHICLE_SOLD.equals(key)){
			this.pricePerVehicleSold = Double.parseDouble(value);
		} else if (START_WITH_24_HOURS.equals(key)){
			this.startWith24Hours = Boolean.parseBoolean(value);
		} else if (MIN_OPERATION_TIME.equals(key)){
			this.minOperationTime = Double.parseDouble(value);
		} else if (MIN_INITIAL_STOP_DISTANCE.equals(key)){
			this.minInitialStopDistance = Double.parseDouble(value);
		} else if (USEFRANCHISE.equals(key)){
			this.useFranchise = Boolean.parseBoolean(value);
		} else if (WRITESTATS_INTERVAL.equals(key)){
			this.writeStatsInterval = Integer.parseInt(value);
		} else if (LOG_OPERATORS.equals(key)){
			this.logOperators = Boolean.parseBoolean(value);		
		} else if (WRITE_METRICS.equals(key)){
			this.writeMetrics = Boolean.parseBoolean(value);		
		} else if (WRITE_GEXF_STATS_INTERVAL.equals(key)) {
			this.writeGexfStatsInterval = Integer.parseInt(value);
		} else if (ROUTE_PROVIDER.equals(key)){
			this.routeProvider = value;
		} else if (SPEED_LIMIT_FOR_STOPS.equals(key)){
			this.speedLimitForStops = Double.parseDouble(value);
		} else if (PLANNING_SPEED_FACTOR.equals(key)){
			this.planningSpeedFactor = Double.parseDouble(value);
		} else if (DRIVER_REST_TIME.equals(key)){
			this.driverRestTime = Double.parseDouble(value);
		} else if (GRID_SIZE.equals(key)){
			this.gridSize = Double.parseDouble(value);
		} else if (TIMESLOT_SIZE.equals(key)){
			this.timeSlotSize = Double.parseDouble(value);
		} else if (USE_ADAPTIVE_NUMBER_OF_OPERATORS.equals(key)){
			this.useAdaptiveNumberOfOperators = Boolean.parseBoolean(value);
		} else if (SHARE_OF_OPERATORS_WITH_PROFIT.equals(key)){
			this.shareOfOperatorsWithProfit = Double.parseDouble(value);
		} else if (DISABLE_CREATION_OF_NEW_OPERATORS_IN_ITERATION.equals(key)){
			this.disableCreationOfNewOperatorsInIteration = Integer.parseInt(value);
		} else if (REROUTE_AGENTS_STUCK.equals(key)){
			this.reRouteAgentsStuck = Boolean.parseBoolean(value);
		} else if (PASSENGERS_BOARD_EVERY_LINE.equals(key)){
			this.passengersBoardEveryLine = Boolean.parseBoolean(value);
		} else if (TRANSIT_SCHEDULE_TO_START_WITH.equals(key)){
			this.transitScheduleToStartWith = value;
		} else if (MERGE_TRANSIT_LINE.equals(key)){
			this.mergeTransitLine = Boolean.parseBoolean(value);
		} else if (PT_ENABLER.equals(key)){
			this.ptEnabler = value;
		} else if (OPERATIONMODE.equals(key)){
			this.operationMode = value;
		} else if (TOPOTYPESFORSTOPS.equals(key)){
			this.topoTypesForStops = value;
		} else if (MIN_CAPACITY_FOR_STOPS.equals(key)){
			this.minCapacityForStops = Double.parseDouble(value);
		} else if (STOP_LOCATION_SELECTOR.equals(key)){
			if (value.equals(StopLocationSelector.allCarLinks.name)) {this.stopLocationSelector = StopLocationSelector.allCarLinks;}
			else if (value.equals(StopLocationSelector.junctionApproachesAndBetweenJunctions.name)) {this.stopLocationSelector = StopLocationSelector.junctionApproachesAndBetweenJunctions;}
			else {log.error("unknown parameter value: " + key + ": " + value);}
		} else if (STOP_LOCATION_SELECTOR_PARAMETER.equals(key)){
			this.stopLocationSelectorParameter = value;
		} else if (key != null && key.startsWith(PMODULE)) {
			PStrategySettings settings = getStrategySettings(Id.create(key.substring(PMODULE.length()), PStrategySettings.class), true);
			settings.setModuleName(value);
		} else if (key != null && key.startsWith(PMODULE_PROBABILITY)) {
			PStrategySettings settings = getStrategySettings(Id.create(key.substring(PMODULE_PROBABILITY.length()), PStrategySettings.class), true);
			settings.setProbability(Double.parseDouble(value));
		} else if (key != null && key.startsWith(PMODULE_DISABLEINITERATION)) {
			PStrategySettings settings = getStrategySettings(Id.create(key.substring(PMODULE_DISABLEINITERATION.length()), PStrategySettings.class), true);
			settings.setDisableInIteration(Integer.parseInt(value));
		} else if (key != null && key.startsWith(PMODULE_PARAMETER)) {
			PStrategySettings settings = getStrategySettings(Id.create(key.substring(PMODULE_PARAMETER.length()), PStrategySettings.class), true);
			settings.setParameters(value);
		} else if (SUBSIDY_APPROACH.equals(key)) {
			this.subsidyApproach = value;
		} else {
			log.error("unknown parameter: " + key + "...");
		}
	}
	
	// Getter
	
	@Override
	public TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<>();
		
		map.put(P_IDENTIFIER, this.pIdentifier);
		map.put(MIN_X, Double.toString(this.minX));
		map.put(MIN_Y, Double.toString(this.minY));
		map.put(MAX_X, Double.toString(this.maxX));
		map.put(MAX_Y, Double.toString(this.maxY));
		map.put(SERVICEAREAFILE, this.serviceAreaFile);
		map.put(OPERATOR_TYPE, this.operatorType);
		map.put(NUMBER_OF_OPERATORS, Integer.toString(this.numberOfOperators));
		map.put(NUMBER_OF_ITERATIONS_FOR_PROSPECTING, Integer.toString(this.numberOfIterationsForProspecting));
		map.put(INITIAL_BUDGET, Double.toString(this.initialBudget));
		map.put(PAX_PER_VEHICLE, Integer.toString(this.paxPerVehicle));
		map.put(DELAY_PER_BOARDING_PASSENGER, Double.toString(this.delayPerBoardingPassenger));
		map.put(DELAY_PER_ALIGHTING_PASSENGER, Double.toString(this.delayPerAlightingPassenger));
		map.put(DOOR_OPERATION_MODE, this.doorOperationMode.toString());
		map.put(PCE, Double.toString(this.passengerCarEquivalents));
		map.put(VEHICLE_MAXIMUM_VELOCITY, Double.toString(this.vehicleMaximumVelocity));
		map.put(COST_PER_VEHICLE_AND_DAY, Double.toString(this.costPerVehicleAndDay));
		map.put(COST_PER_KILOMETER, Double.toString(this.costPerKilometer));
		map.put(COST_PER_HOUR, Double.toString(this.costPerHour));
		map.put(EARNINGS_PER_BOARDING_PASSENGER, Double.toString(this.earningsPerBoardingPassenger));
		map.put(EARNINGS_PER_KILOMETER_AND_PASSENGER, Double.toString(this.earningsPerKilometerAndPassenger));
		map.put(PRICE_PER_VEHICLE_BOUGHT, Double.toString(this.pricePerVehicleBought));
		map.put(PRICE_PER_VEHICLE_SOLD, Double.toString(this.pricePerVehicleSold));
		map.put(START_WITH_24_HOURS, Boolean.toString(this.startWith24Hours));
		map.put(MIN_OPERATION_TIME, Double.toString(this.minOperationTime));
		map.put(MIN_INITIAL_STOP_DISTANCE, Double.toString(this.minInitialStopDistance));
		map.put(USEFRANCHISE, Boolean.toString(this.useFranchise));
		map.put(WRITESTATS_INTERVAL, Integer.toString(this.writeStatsInterval));
		map.put(LOG_OPERATORS, Boolean.toString(this.logOperators));
		map.put(WRITE_METRICS, Boolean.toString(this.writeMetrics));
		map.put(WRITE_GEXF_STATS_INTERVAL, Integer.toString(this.writeGexfStatsInterval));
		map.put(ROUTE_PROVIDER, this.routeProvider);
		map.put(SPEED_LIMIT_FOR_STOPS, Double.toString(this.speedLimitForStops));
		map.put(PLANNING_SPEED_FACTOR, Double.toString(this.planningSpeedFactor));
		map.put(DRIVER_REST_TIME, Double.toString(this.driverRestTime));
		map.put(GRID_SIZE, Double.toString(this.gridSize));
		map.put(TIMESLOT_SIZE, Double.toString(this.timeSlotSize));
		map.put(USE_ADAPTIVE_NUMBER_OF_OPERATORS, Boolean.toString(this.useAdaptiveNumberOfOperators));
		map.put(SHARE_OF_OPERATORS_WITH_PROFIT, Double.toString(this.shareOfOperatorsWithProfit));
		map.put(DISABLE_CREATION_OF_NEW_OPERATORS_IN_ITERATION, Integer.toString(this.disableCreationOfNewOperatorsInIteration));
		map.put(REROUTE_AGENTS_STUCK, Boolean.toString(this.reRouteAgentsStuck));
		map.put(PASSENGERS_BOARD_EVERY_LINE, Boolean.toString(this.passengersBoardEveryLine));
		map.put(TRANSIT_SCHEDULE_TO_START_WITH, this.transitScheduleToStartWith);
		map.put(MERGE_TRANSIT_LINE, Boolean.toString(this.mergeTransitLine));
		map.put(OPERATIONMODE, this.operationMode);
		map.put(TOPOTYPESFORSTOPS, this.topoTypesForStops);
		map.put(MIN_CAPACITY_FOR_STOPS, Double.toString(this.minCapacityForStops));
		map.put(STOP_LOCATION_SELECTOR, this.stopLocationSelector.name);
		map.put(STOP_LOCATION_SELECTOR_PARAMETER, this.stopLocationSelectorParameter);
		map.put(SUBSIDY_APPROACH, this.subsidyApproach);
		
		for (Entry<Id<PStrategySettings>, PStrategySettings> entry : this.strategies.entrySet()) {
			map.put(PMODULE + entry.getKey().toString(), entry.getValue().getModuleName());
			map.put(PMODULE_PROBABILITY + entry.getKey().toString(), Double.toString(entry.getValue().getProbability()));
			map.put(PMODULE_DISABLEINITERATION + entry.getKey().toString(), Integer.toString(entry.getValue().getDisableInIteration()));
			map.put(PMODULE_PARAMETER + entry.getKey().toString(), entry.getValue().getParametersAsString());
		}
		
		return map;
	}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		
		map.put(P_IDENTIFIER, "This String will be used to identify all components of the paratransit system, e.g. vehicles and drivers");
		map.put(MIN_X, "min x coordinate for service area");
		map.put(MIN_Y, "min y coordinate for service area");
		map.put(MAX_X, "max x coordinate for service area");
		map.put(MAX_Y, "max y coordinate for service area");
		map.put(SERVICEAREAFILE, "a shapefile containing a shape of the service-area or a textfile containing a sequence of x/y values, describing a line string");
		map.put(OPERATOR_TYPE, "Type of operator to be used");
		map.put(NUMBER_OF_OPERATORS, "number of operators operating");
		map.put(NUMBER_OF_ITERATIONS_FOR_PROSPECTING, "number of iterations an operator will survive with a negative scoring");
		map.put(INITIAL_BUDGET, "The budget a new operator is initialized with");
		map.put(PAX_PER_VEHICLE, "number of passengers per vehicle");
		map.put(PCE, "Passenger car equilvalents of one paratransit vehicle");
		map.put(VEHICLE_MAXIMUM_VELOCITY, "Maximum velocity of minibuses. Default is Double.POSITIVE_INFINITY.");
		map.put(DELAY_PER_BOARDING_PASSENGER, "The amount of time a vehicle is delayed by one single boarding passenger in seconds.");
		map.put(DELAY_PER_ALIGHTING_PASSENGER, "The amount of time a vehicle is delayed by one single alighting passenger in seconds.");
		map.put(DOOR_OPERATION_MODE, "serial and parallel are permitted. Default is serial.");
		map.put(COST_PER_VEHICLE_AND_DAY, "cost per vehicle and day - will prevent companies from operating only short periods of a day");
		map.put(COST_PER_KILOMETER, "cost per vehicle and kilometer travelled");
		map.put(COST_PER_HOUR, "cost per vehicle and hour in service");
		map.put(EARNINGS_PER_BOARDING_PASSENGER, "Price an agent has to pay when boarding, regardless how far he will travel");
		map.put(EARNINGS_PER_KILOMETER_AND_PASSENGER, "earnings per passenger kilometer");
		map.put(PRICE_PER_VEHICLE_BOUGHT, "price of one vehicle bought");
		map.put(PRICE_PER_VEHICLE_SOLD, "price of one vehicle sold");
		map.put(START_WITH_24_HOURS, "Initial plan will start operating 0-24 hours");
		map.put(MIN_OPERATION_TIME, "min time of operation of each operator in seconds");
		map.put(MIN_INITIAL_STOP_DISTANCE, "min distance the two initial stops of a new operator's first route should be apart. Default is 1.0. Set to 0.0 to allow for the same stop being picked as start and end stop.");
		map.put(USEFRANCHISE, "Will use a franchise system if set to true");
		map.put(WRITESTATS_INTERVAL, "interval in which statistics will be plotted. Set to zero to turn this feature off. Set to something larger than the total number of iterations to turn off the plots, but write the statistics file anyway");
		map.put(LOG_OPERATORS, "will log operators individually if set to true");
		map.put(WRITE_METRICS, "will calculate common performance metrics if set to true, default is false");
		map.put(WRITE_GEXF_STATS_INTERVAL, "number of iterations the gexf output gets updated. Set to zero to turn this feature off");
		map.put(ROUTE_PROVIDER, "The route provider used. Currently, there are SimpleCircleScheduleProvider and SimpleBackAndForthScheduleProvider");
		map.put(SPEED_LIMIT_FOR_STOPS, "Link cannot serve as paratransit stop, if its speed limit is equal or higher than the limit set here. Default is +INF");
		map.put(PLANNING_SPEED_FACTOR, "Freespeed of link will be modified by factor. Resulting link travel time is written to transit schedule. Default is 1.0 aka freespeed of the link.");
		map.put(DRIVER_REST_TIME, "The rest time of a driver at the end of a complete tour in seconds. Defaults to 60 seconds.");
		map.put(GRID_SIZE, "The grid size (length and height) for aggregating stuff in various modules (RandomStopProvider, ActivityLocationsParatransitUser, PFranchise). Default of Double.maxvalue effectively aggregates all data points into one gridPoint");
		map.put(TIMESLOT_SIZE, "The size of a time slot aggregating stuff in various modules (TimeProvider, CreateNewPlan). Default of Double.maxvalue effectively aggregates all data points into one time slot");
		map.put(USE_ADAPTIVE_NUMBER_OF_OPERATORS, "Will try to adapt the number of operators to meet the given share of profitable operators if set to true");
		map.put(SHARE_OF_OPERATORS_WITH_PROFIT, "Target share of profitable operators - Set " + USE_ADAPTIVE_NUMBER_OF_OPERATORS + "=true to enable this feature");
		map.put(DISABLE_CREATION_OF_NEW_OPERATORS_IN_ITERATION, "No more new operators will be found beginning with the iteration specified");
		map.put(REROUTE_AGENTS_STUCK, "All agents stuck will be rerouted at the beginning of an iteration, if set to true.");
		map.put(PASSENGERS_BOARD_EVERY_LINE, "Agents will board every vehicles serving the destination (stop), if set to true. Set to false, to force agents to take only vehicles of the line planned. Default is false.");
		map.put(TRANSIT_SCHEDULE_TO_START_WITH, "Will initialize one operator for each transit line with the given time of operation and number of vehicles");
		map.put(MERGE_TRANSIT_LINE, "Merges all routes of a transit line that have the same sequence of stops. Does not respect the time profile of the routes. Default is false.");
		map.put(OPERATIONMODE, "the mode of transport in which the paratransit operates");
		map.put(TOPOTYPESFORSTOPS, "comma separated integer-values, as used in NetworkCalcTopoTypes");
		map.put(MIN_CAPACITY_FOR_STOPS, "Link cannot serve as paratransit stop, if its capacity is lower than the limit set here. Default is 0.");
		map.put(STOP_LOCATION_SELECTOR, "The paratransit stop locator, either one stop per car link (allCarLinks) or on approaches to junction areas and some stops between junction areas (junctionApproachesAndBetweenJunctions). Default is allCarLinks.");
		map.put(STOP_LOCATION_SELECTOR_PARAMETER, "Parameters for the paratransit stop locator. For allCarLinks there are no parameters to set. \nFor junctionApproachesAndBetweenJunctions, which is based on the IntersectionSimplifier, there is pmin (maximum distance betwen 2 nodes to be merged into the same cluster, should be smaller than the maximum transfer distance), epsilon (minimum number of nodes to consider it a cluster) and rough distance between stops (used in NetworkSimplifier). \nDefault is \"\" for allCarLinks and \"50.0,2,500\" for junctionApproachesAndBetweenJunctions.");
		map.put(SUBSIDY_APPROACH, "Optional: add a subsidy to the operators' scores. Currently implemented: 'null': no subsidy; 'perPassenger': a subsidy of 100000 monetary units per passenger");
		
		for (Entry<Id<PStrategySettings>, PStrategySettings>  entry : this.strategies.entrySet()) {
			map.put(PMODULE + entry.getKey().toString(), "name of strategy");
			map.put(PMODULE_PROBABILITY + entry.getKey().toString(), "probability that a strategy is applied to a given a plan. despite its name, this really is a ``weight''");
			map.put(PMODULE_DISABLEINITERATION + entry.getKey().toString(), "removes the strategy from the choice set at the beginning of the given iteration");
			map.put(PMODULE_PARAMETER + entry.getKey().toString(), "parameters of the strategy");
		}

		return map;
	}
	
	public String getPIdentifier(){
		return this.pIdentifier;
	}
	
	public String getServiceAreaFile(){
		return this.serviceAreaFile;
	}
	
	public double getMinX() {
		return this.minX;
	}

	public double getMinY() {
		return this.minY;
	}

	public double getMaxX() {
		return this.maxX;
	}

	public double getMaxY() {
		return this.maxY;
	}

	public String getOperatorType() {
		return this.operatorType;
	}
	
	public int getNumberOfOperators() {
		return this.numberOfOperators;
	}
	
	public int getNumberOfIterationsForProspecting() {
		return this.numberOfIterationsForProspecting;
	}
	
	public double getInitialBudget() {
		return this.initialBudget;
	}
	
	public int getPaxPerVehicle() {
		return this.paxPerVehicle;
	}
	
	public double getPassengerCarEquivalents() {
		return this.passengerCarEquivalents;
	}
	
	public double getVehicleMaximumVelocity() {
		return this.vehicleMaximumVelocity;
	}
	
	public double getDelayPerBoardingPassenger() {
		return this.delayPerBoardingPassenger;
	}
	
	public double getDelayPerAlightingPassenger() {
		return this.delayPerAlightingPassenger;
	}
	
	public DoorOperationMode getDoorOperationMode() {
		return this.doorOperationMode;
	}
	
	public double getCostPerVehicleAndDay() {
		return this.costPerVehicleAndDay;
	}
	
	public double getCostPerKilometer() {
		return this.costPerKilometer;
	}
	
	public double getCostPerHour() {
		return this.costPerHour;
	}

	public double getEarningsPerBoardingPassenger() {
		return this.earningsPerBoardingPassenger;
	}
	
	public double getEarningsPerKilometerAndPassenger() {
		return this.earningsPerKilometerAndPassenger;
	}
		
	public double getPricePerVehicleBought() {
		return this.pricePerVehicleBought;
	}

	public double getPricePerVehicleSold() {
		return this.pricePerVehicleSold;
	}

	public double getMinOperationTime() {
		return this.minOperationTime;
	}
	
	public double getMinInitialStopDistance() {
		return this.minInitialStopDistance;
	}
	
	public boolean getStartWith24Hours() {
		return this.startWith24Hours;
	}

	public boolean getUseFranchise() {
		return this.useFranchise;
	}
	
	public int getWriteStatsInterval() {
		return this.writeStatsInterval;
	}
	
	public boolean getLogOperators() {
		return this.logOperators;
	}
	
	public boolean getWriteMetrics() {
		return this.writeMetrics;
	}
	
	public int getGexfInterval(){
		return this.writeGexfStatsInterval;
	}
	
	public String getRouteProvider(){
		return this.routeProvider;
	}
	
	public double getSpeedLimitForStops(){
		return this.speedLimitForStops;
	}
	
	public double getPlanningSpeedFactor(){
		return this.planningSpeedFactor;
	}
	
	public double getDriverRestTime(){
		return this.driverRestTime;
	}
	
	public double getGridSize(){
		return this.gridSize;
	}
	
	public double getTimeSlotSize(){
		return this.timeSlotSize;
	}
	
	public boolean getUseAdaptiveNumberOfOperators() {
		return this.useAdaptiveNumberOfOperators;
	}

	public double getShareOfOperatorsWithProfit() {
		return this.shareOfOperatorsWithProfit;
	}
	
	public int getDisableCreationOfNewOperatorsInIteration() {
		return this.disableCreationOfNewOperatorsInIteration;
	}
	
	public boolean getReRouteAgentsStuck() {
		return this.reRouteAgentsStuck;
	}
	
	public boolean getPassengersBoardEveryLine() {
		return this.passengersBoardEveryLine;
	}
	
	public String getTransitScheduleToStartWith() {
		return this.transitScheduleToStartWith;
	}

	public boolean getMergeTransitLine() {
		return this.mergeTransitLine;
	}
	
	public String getPtEnabler() {
		return this.ptEnabler;
	}

	public String getMode() {
		return this.operationMode;
	}
	
	public void setSubsidyApproach( String val ) {
		this.subsidyApproach = val ;
	}
	public String getSubsidyApproach() {
		return this.subsidyApproach;
	}

	public List<Integer> getTopoTypesForStops() {
		if(this.topoTypesForStops == null){
			return null;
		}
		List<Integer> list = new ArrayList<>();
		for(String s: this.topoTypesForStops.split(",")){
			list.add(Integer.parseInt(s.trim()));
		}
		return list;
	}
	
	public double getMinCapacityForStops(){
		return this.minCapacityForStops;
	}
	
	public StopLocationSelector getStopLocationSelector() {
		return this.stopLocationSelector;
	}
	
	public String getStopLocationSelectorParameter() {
		return this.stopLocationSelectorParameter;
	}

	public Collection<PStrategySettings> getStrategySettings() {
		return this.strategies.values();
	}
	
	private PStrategySettings getStrategySettings(final Id<PStrategySettings> strategyId, final boolean createIfMissing) {
		PStrategySettings settings = this.strategies.get(strategyId);
		if (settings == null && createIfMissing) {
			settings = new PStrategySettings(strategyId);
			this.strategies.put(strategyId, settings);
		}
		return settings;
	}
	
	public static class PStrategySettings{
		private Id<PStrategySettings> id;
		private double probability = -1.0;
		private int disableInIteration = -1;
		private String moduleName = null;
		private String[] parameters = null;

		public PStrategySettings(final Id<PStrategySettings> id) {
			this.id = id;
		}

		public void setProbability(final double probability) {
			this.probability = probability;
		}

		public double getProbability() {
			return this.probability;
		}

		public void setDisableInIteration(int disableInIteration) {
			this.disableInIteration = disableInIteration;
		}
		
		public int getDisableInIteration() {
			return this.disableInIteration;
		}

		public void setModuleName(final String moduleName) {
			this.moduleName = moduleName;
		}

		public String getModuleName() {
			return this.moduleName;
		}		

		public Id<PStrategySettings> getId() {
			return this.id;
		}

		public void setId(final Id<PStrategySettings> id) {
			this.id = id;
		}
		
		public ArrayList<String> getParametersAsArrayList(){
			ArrayList<String> list = new ArrayList<>();
			
			if (this.parameters != null) {
				for (String parameter : this.parameters) {
					list.add(parameter);
				}
			}
			
			return list;
		}
		
		public String getParametersAsString() {
			StringBuffer strBuffer = new StringBuffer();
			
			if (this.parameters != null) {
		        strBuffer.append(this.parameters[0]);
		        for (int i = 1; i < this.parameters.length; i++) {
		            strBuffer.append(",");
		            strBuffer.append(this.parameters[i]);
		        }
		    }
			
			return strBuffer.toString();
		}

		public void setParameters(String parameter) {
			if (!parameter.equalsIgnoreCase("")) {
				String[] parts = StringUtils.explode(parameter, ',');
				this.parameters = new String[parts.length];
				for (int i = 0, n = parts.length; i < n; i++) {
					this.parameters[i] = parts[i].trim().intern();
				}
			}			
		}

	}
	@Override
	protected void checkConsistency( Config config ) {
		
		if (this.mergeTransitLine) {
			log.info("All routes of a minibus transit line with the same stop sequence will be merged into one single transit route. Note that the transit schedules written to the output directory do not contain all minibus routes anymore.");
			
			if (!logOperators) {
				log.warn("Transit lines will be merged. Activate the operator logger to retrieve more detailed information on particular routes.");
			}
		}
	}

}