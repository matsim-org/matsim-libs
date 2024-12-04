/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers;

import jakarta.validation.constraints.Positive;
import java.net.URL;
import java.util.Map;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class FreightCarriersConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUPNAME="freightCarriers" ;

    private String carriersFile;
    static final String CARRIERS_FILE = "carriersFile";
    private static final String CARRIERS_FILE_DESC = "Freight Carriers File, according to MATSim freight contrib";

    private String carriersVehicleTypesFile;
    static final String CARRIERS_VEHICLE_TYPE = "carriersVehicleTypeFile";
    private static final String CARRIERS_VEHICLE_TYPE_DESC = "Carrier Vehicle Types file, according to MATSim freight contrib";

    private String vehicleRoutingAlgorithmFile;
    static final String VEHICLE_ROUTING_ALGORITHM = "vehicleRoutingAlgorithmFile";
    private static final String VEHICLE_ROUTING_ALGORITHM_DESC = "(Optional) Vehicle Routing Algorithm File, according to jsprit library. "
           + "Empty value \"\" means a default algorithm is used.";

    @Positive
    private int travelTimeSliceWidth = 1800;
    static final String TRAVEL_TIME_SLICE_WIDTH = "travelTimeSliceWidth";
    private static final String TRAVEL_TIME_SLICE_WIDTH_DESC = "time slice width used for calculation of travel times in seconds." +
            " The smaller the value, the more precise the calculation of routing costs but the longer the computation time." +
            " Default value is 1800 seconds.";

    public enum UseDistanceConstraintForTourPlanning {noDistanceConstraint, basedOnEnergyConsumption}

	static final String USE_DISTANCE_CONSTRAINT = "useDistanceConstraintForTourPlanning";
    private UseDistanceConstraintForTourPlanning useDistanceConstraintForTourPlanning = UseDistanceConstraintForTourPlanning.noDistanceConstraint;
    private static final String USE_DISTANCE_CONSTRAINT_DESC = "Use distance constraint within the tour planning phase. This does NOT ensure that the tours in MATSim will respect this limitation";

    public FreightCarriersConfigGroup() {
        super(GROUPNAME);
    }

    //### CarriersFile ###
    /**
     * @return -- {@value #CARRIERS_FILE_DESC}
     */
    @StringGetter(CARRIERS_FILE)
    public String getCarriersFile() {
        return carriersFile;
    }

    URL getCarriersFileUrl(URL context) {
        return ConfigGroup.getInputFileURL(context, this.carriersFile);
    }

    /**
     * @param -- {@value #CARRIERS_FILE_DESC}
     */
    @StringSetter(CARRIERS_FILE)
    public void setCarriersFile(String carriersFile) {
        this.carriersFile = carriersFile;
    }


    //### CarriersVehicleTypeFile ###
    /**
     * @return -- {@value #CARRIERS_VEHICLE_TYPE_DESC}
     */
    @StringGetter(CARRIERS_VEHICLE_TYPE)
    public String getCarriersVehicleTypesFile() {
        return carriersVehicleTypesFile;
    }

    URL getCarriersVehicleTypesFileUrl(URL context) {
        return ConfigGroup.getInputFileURL(context, this.carriersVehicleTypesFile);
    }

    /**
     * @param -- {@value #CARRIERS_VEHICLE_TYPE_DESC}
     */
    @StringSetter(CARRIERS_VEHICLE_TYPE)
    public void setCarriersVehicleTypesFile(String carriersVehicleTypesFile) {
        this.carriersVehicleTypesFile = carriersVehicleTypesFile;
    }

    //### VehicleRoutingAlgorithmFile ###
    /**
     * @return -- {@value #VEHICLE_ROUTING_ALGORITHM_DESC}
     */
    @StringGetter(VEHICLE_ROUTING_ALGORITHM)
    public String getVehicleRoutingAlgorithmFile() {
        return vehicleRoutingAlgorithmFile;
    }

    URL getVehicleRoutingAlgorithmFileUrl(URL context) {
        return ConfigGroup.getInputFileURL(context, this.vehicleRoutingAlgorithmFile);
    }

    /**
     * @param -- {@value #VEHICLE_ROUTING_ALGORITHM_DESC}
     */
    @StringSetter(VEHICLE_ROUTING_ALGORITHM)
    public void setVehicleRoutingAlgorithmFileFile(String vehicleRoutingAlgorithmFile) {
        this.vehicleRoutingAlgorithmFile = vehicleRoutingAlgorithmFile;
    }


    //### TravelTimeSliceWidth ###
    /**
     * @return travelTimeSliceWidth --{@value #TRAVEL_TIME_SLICE_WIDTH_DESC}
     */
    @StringGetter(TRAVEL_TIME_SLICE_WIDTH)
    public int getTravelTimeSliceWidth() {
        return travelTimeSliceWidth;
    }

    /**
     * @param travelTimeSliceWidth --{@value #TRAVEL_TIME_SLICE_WIDTH_DESC}
     */
    @StringSetter(TRAVEL_TIME_SLICE_WIDTH)
    public void setTravelTimeSliceWidth(int travelTimeSliceWidth) {
        this.travelTimeSliceWidth = travelTimeSliceWidth;
    }

    //### TimeWindowHandling ###
    public enum TimeWindowHandling{ ignore, enforceBeginnings }
    private TimeWindowHandling timeWindowHandling = TimeWindowHandling.enforceBeginnings ;
    /**
     * Physically enforces beginnings of time windows for freight activities, i.e. freight agents
     * wait before closed doors until they can deliver / pick up their goods, and then take their required duration.
     */
    public void setTimeWindowHandling( TimeWindowHandling handling ) {
        this.timeWindowHandling = handling ;
    }
    public TimeWindowHandling getTimeWindowHandling() {
        return this.timeWindowHandling ;
    }



    //---
    //---
    /**
	 * @return useDistanceConstraint
	 */
    @StringGetter(USE_DISTANCE_CONSTRAINT)
	public UseDistanceConstraintForTourPlanning getUseDistanceConstraintForTourPlanning() {
		return useDistanceConstraintForTourPlanning;
	}
	/**
	 * @param useDistanceConstraintForTourPlanning {@value #USE_DISTANCE_CONSTRAINT_DESC}
	 */
    @StringSetter(USE_DISTANCE_CONSTRAINT)
	public void setUseDistanceConstraintForTourPlanning(UseDistanceConstraintForTourPlanning useDistanceConstraintForTourPlanning) {
		this.useDistanceConstraintForTourPlanning = useDistanceConstraintForTourPlanning;
	}

	//---
	//---
	@Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(CARRIERS_FILE, CARRIERS_FILE_DESC);
        map.put(CARRIERS_VEHICLE_TYPE, CARRIERS_VEHICLE_TYPE_DESC);
        map.put(VEHICLE_ROUTING_ALGORITHM, VEHICLE_ROUTING_ALGORITHM_DESC);
        map.put(TRAVEL_TIME_SLICE_WIDTH, TRAVEL_TIME_SLICE_WIDTH_DESC);
        map.put(USE_DISTANCE_CONSTRAINT, USE_DISTANCE_CONSTRAINT_DESC);
        return map;
    }

}
