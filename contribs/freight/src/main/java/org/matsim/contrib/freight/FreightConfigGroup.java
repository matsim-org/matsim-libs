/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import javax.validation.constraints.Positive;
import java.net.URL;
import java.util.Map;

public class FreightConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUPNAME="freight" ;

    private String carriersFile;
    public static final String CARRIERSFILEDE = "carriersFile";
    private static final String CARRIERSFILEDESC = "Freight Carriers File, according to MATSim freight contrib";

    private String carriersVehicleTypesFile;
    public static final String CARRIERSVEHICLETYPED = "carriersVehicleTypeFile";
    private static final String CARRIERSVEHICLETYPEDESC = "Carrier Vehicle Types file, according to MATSim freight contrib";

    private String vehicleRoutingAlgortihmFile;
    public static final String VEHICLEROUTINGALGORITHM = "vehicleRoutingAlgortihmFile";
    private static final String VEHICLEROUTINGALGORITHMDESC = "(Optional) Vehicle Routing Algorithm File, according to jsprit library. "
           + "Empty value \"\" means an default algorithm is used.";

    @Positive
    private int travelTimeSliceWidth = 1800;
    public static final String TRAVELTIMESLICEWIDTH = "travelTimeSliceWidth";
    private static final String TRAVELTIMESLICEWIDTHDESC = "time slice width used for calculation of travel times in seconds." +
            " The smaller the value, the more precise the calculation of routing costs but the longer the computation time." +
            " Default value is 1800 seconds.";
    
    public enum UseDistanceConstraintForTourPlanning {noDistanceConstraint, basedOnEnergyConsumption};
    private static final String USE_DISTANCE_CONSTRAINT = "useDistanceConstraintForTourPlanning";
    private UseDistanceConstraintForTourPlanning useDistanceConstraintForTourPlanning = UseDistanceConstraintForTourPlanning.noDistanceConstraint;
    private static final String USE_DISTANCE_CONSTRAINT_DESC = "Use distant constraint within the tour planning phase. This does NOT ensure that the tours in MATSim will respect this limitation";

    public FreightConfigGroup() {
        super(GROUPNAME);
    }

    //### CarriersFile ###
    /**
     * @return -- {@value #CARRIERSFILEDESC}
     */
//    @StringGetter(CARRIERSFILEDE)
    public String getCarriersFile() {
        return carriersFile;
    }

    URL getCarriersFileUrl(URL context) {
        return ConfigGroup.getInputFileURL(context, this.carriersFile);
    }

    /**
     * @param -- {@value #CARRIERSFILEDESC}
     */
//    @StringSetter(CARRIERSFILEDE)
    public void setCarriersFile(String carriersFile) {
        this.carriersFile = carriersFile;
    }

    
    //### CarriersVehicleTypeFile ###
    /**
     * @return -- {@value #CARRIERSVEHICLETYPEDESC}
     */
//    @StringGetter(CARRIERSVEHICLETYPED)
    public String getCarriersVehicleTypesFile() {
        return carriersVehicleTypesFile;
    }

    URL getCarriersVehicleTypesFileUrl(URL context) {
        return ConfigGroup.getInputFileURL(context, this.carriersVehicleTypesFile);
    }
    
    /**
     * @param -- {@value #CARRIERSVEHICLETYPEDESC}
     */
//    @StringSetter(CARRIERSVEHICLETYPED)
    public void setCarriersVehicleTypesFile(String carriersVehicleTypesFile) {
        this.carriersVehicleTypesFile = carriersVehicleTypesFile;
    }

    //### VehicleRoutingAlgorithmFile ###
    /**
     * @return -- {@value #VEHICLEROUTINGALGORITHMDESC}
     */
//    @StringGetter(VEHICLEROUTINGALGORITHM)
    public String getVehicleRoutingAlgortihmFile() {
        return vehicleRoutingAlgortihmFile;
    }

    URL getVehicleAlgortihemsFileUrl(URL context) {
        return ConfigGroup.getInputFileURL(context, this.vehicleRoutingAlgortihmFile);
    }

    /**
     * @param -- {@value #VEHICLEROUTINGALGORITHMDESC}
     */
//    @StringSetter(VEHICLEROUTINGALGORITHM)
    public void setVehicleRoutingAlgortihmFileFile(String vehicleRoutingAlgortihmFile) {
        this.vehicleRoutingAlgortihmFile = vehicleRoutingAlgortihmFile;
    }


    //### TravelTimeSliceWidth ###
    /**
     * @return travelTimeSliceWidth --{@value #TRAVELTIMESLICEWIDTHDESC}
     */
//    @StringGetter(TRAVELTIMESLICEWIDTH)
    public int getTravelTimeSliceWidth() {
        return travelTimeSliceWidth;
    }

    /**
     * @param travelTimeSliceWidth --{@value #TRAVELTIMESLICEWIDTHDESC}
     */
//    @StringSetter(JSPRITTIMESLICEWIDTH)
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
        map.put(CARRIERSFILEDE, CARRIERSFILEDESC);
        map.put(CARRIERSVEHICLETYPED, CARRIERSVEHICLETYPEDESC);
        map.put(VEHICLEROUTINGALGORITHM, VEHICLEROUTINGALGORITHMDESC);
        map.put(TRAVELTIMESLICEWIDTH, TRAVELTIMESLICEWIDTHDESC);
        map.put(USE_DISTANCE_CONSTRAINT, USE_DISTANCE_CONSTRAINT_DESC);
        return map;
    }

}
