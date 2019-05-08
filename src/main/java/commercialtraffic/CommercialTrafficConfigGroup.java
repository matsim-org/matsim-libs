/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package commercialtraffic;/*
 * created by jbischoff, 08.05.2019
 */

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.net.URL;
import java.util.Map;

public class CommercialTrafficConfigGroup extends ReflectiveConfigGroup {


    @NotBlank
    private String carriersFile;
    public static final String CARRIERSFILEDE = "carriersFile";
    public static final String CARRIERSFILEDESC = "Freight Carriers File, according to MATSim freight contrib";


    @NotBlank
    private String carriersVehicleTypesFile;
    public static final String CARRIERSVEHICLETYPED = "carriersVehicleTypeFile";
    public static final String CARRIERSVEHICLETYPEDESC = "Carrier Vehicle Types file, according to MATSim freight contrib";

    @Positive
    private double firstLegTraveltimeBufferFactor = 2.0;
    public static final String FIRSTLEGBUFFER = "firstLegBufferFactor";
    public static final String FIRSTLEGBUFFERDESC = "Buffer travel time factor for the first leg of a freight tour.";

    @Positive
    private int jspritIterations = 100;
    public static final String JSPRITITERS = "jspritIterations";
    public static final String JSPRITITERSDESC = "Number of jsprit Iterations. These take place at the beginning of each MATSim iteration";


    public static final String GROUP_NAME = "commercialTraffic";

    public CommercialTrafficConfigGroup() {
        super(GROUP_NAME);
    }

    public static CommercialTrafficConfigGroup get(Config config) {
        return (CommercialTrafficConfigGroup) config.getModules().get(GROUP_NAME);
    }


    /**
     * @return -- {@value #CARRIERSFILEDESC}
     */
    @StringGetter(CARRIERSFILEDE)
    public String getCarriersFile() {
        return carriersFile;
    }

    public URL getCarriersFileUrl(URL context) {
        return ConfigGroup.getInputFileURL(context, this.carriersFile);
    }

    /**
     * @param -- {@value #CARRIERSFILEDESC}
     */
    @StringSetter(CARRIERSFILEDE)
    public void setCarriersFile(String carriersFile) {
        this.carriersFile = carriersFile;
    }

    /**
     * @return -- {@value #CARRIERSVEHICLETYPEDESC}
     */
    @StringGetter(CARRIERSVEHICLETYPED)
    public String getCarriersVehicleTypesFile() {
        return carriersVehicleTypesFile;
    }

    public URL getCarriersVehicleTypesFileUrl(URL context) {
        return ConfigGroup.getInputFileURL(context, this.carriersVehicleTypesFile);
    }


    /**
     * @param -- {@value #CARRIERSVEHICLETYPEDESC}
     */
    @StringSetter(CARRIERSVEHICLETYPED)
    public void setCarriersVehicleTypesFile(String carriersVehicleTypesFile) {
        this.carriersVehicleTypesFile = carriersVehicleTypesFile;
    }

    /**
     * @return -- {@value #FIRSTLEGBUFFERDESC}
     */
    @StringGetter(FIRSTLEGBUFFER)
    public double getFirstLegTraveltimeBufferFactor() {
        return firstLegTraveltimeBufferFactor;
    }

    /**
     * @param -- {@value #FIRSTLEGBUFFERDESC}
     */
    @StringSetter(FIRSTLEGBUFFER)
    public void setFirstLegTraveltimeBufferFactor(double firstLegTraveltimeBufferFactor) {
        this.firstLegTraveltimeBufferFactor = firstLegTraveltimeBufferFactor;
    }

    /**
     * @return jspritIterations --{@value #JSPRITITERSDESC}
     */
    @StringGetter(JSPRITITERS)
    public int getJspritIterations() {
        return jspritIterations;
    }

    /**
     * @param jspritIterations --{@value #JSPRITITERSDESC}
     */
    @StringSetter(JSPRITITERS)
    public void setJspritIterations(int jspritIterations) {
        this.jspritIterations = jspritIterations;
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(CARRIERSFILEDE, CARRIERSFILEDESC);
        map.put(CARRIERSVEHICLETYPED, CARRIERSVEHICLETYPEDESC);
        map.put(FIRSTLEGBUFFER, FIRSTLEGBUFFERDESC);
        map.put(JSPRITITERS, JSPRITITERSDESC);
        return map;
    }
}
