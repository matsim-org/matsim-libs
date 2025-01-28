/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
package org.matsim.freight.receiver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public final class ReceiverConfigGroup extends ReflectiveConfigGroup {

    public ReceiverConfigGroup() { super(NAME); }

    @SuppressWarnings("unused")
    private static final Logger LOG = LogManager.getLogger(ReceiverConfigGroup.class);

    public static final String NAME = "freightReceiver";

    final static String ELEMENT_COSTSHARING = "collaboration";
    final static String ELEMENT_RECEIVER_FILE = "receiversFile";
    final static String ELEMENT_CARRIERS_FILE = "carriersFile";
    final static String ELEMENT_NONCOLLABORATING_FEE = "noncollaboratingFee";
    final static String ELEMENT_CREATE_PNG = "createPNG";
    final static String ELEMENT_REPLANNING_TYPE = "replanningModules";

    private String RECEIVERS_FILE = "./receivers.xml.gz";
    // (I converted this from "default" & static to private and non-static: static non-final non-private variables often eventually cause problems.  kai, apr'23)
    private String CARRIERS_FILE = "./carriers.xml.gz";
    // (I converted this from "default" & static to private and non-static: static non-final non-private variables often eventually cause problems.  kai, apr'23)
    private String costSharing = "proportional";
    private boolean createPNG = true;
    private ReceiverReplanningType replanningType = ReceiverReplanningType.serviceTime;
    private int receiverReplanningInterval = 1;

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(ELEMENT_COSTSHARING, "The mechanism by which the carriers' costs are shared among receivers. " +
                "Available values are 'proportional' (default), and 'marginal'.");
        comments.put(ELEMENT_RECEIVER_FILE, "Path to the file containing the receivers.");
        comments.put(ELEMENT_CARRIERS_FILE, "Path to the file containing the carriers.");
        comments.put(ELEMENT_NONCOLLABORATING_FEE, "The fixed fee charged per receiver NOT collaborating in a coalition.");
        comments.put(ELEMENT_CREATE_PNG, "Should PNG output be created? Default is 'true'");
        comments.put(ELEMENT_REPLANNING_TYPE, "Replanning strategy used. Available values are 'serviceTime' (default), " +
                "'timeWindow' and 'deliveryFrequency'.");
        return comments;
    }

//    @StringGetter(RECEIVER_FILE)
    // Here and elsewhere I commented out the StringSetter/Getter stuff.  This makes the config accessible only from code.  Makes it easier to
    // refactor later.  kai, apr'23
    public String getReceiversFile() {
        return RECEIVERS_FILE;
    }

//    @StringSetter(RECEIVER_FILE)
    public void setReceiversFile(String receiversFile) {
        this.RECEIVERS_FILE = receiversFile;
    }

//    @StringGetter(ELEMENT_CARRIERS_FILE)
    public String getCarriersFile() {
        return CARRIERS_FILE;
    }

//    @StringSetter(ELEMENT_CARRIERS_FILE)
    public void setCarriersFile(String carriersFile) {
        this.CARRIERS_FILE = carriersFile;
    }

//    @StringGetter(ELEMENT_COSTSHARING)
    public String getCostSharing() {
        return costSharing;
    }
    // yyyyyy shouldn't this be an enum?  kai, apr'23

//    @StringSetter(ELEMENT_COSTSHARING)
    public void setCostSharing(String costSharing) {
        this.costSharing = costSharing;
    }

//    @StringGetter(ELEMENT_CREATE_PNG)
    public boolean isCreatePNG() { return createPNG; }

//    @StringSetter(ELEMENT_CREATE_PNG)
    public void setCreatePNG(boolean createPNG) { this.createPNG = createPNG; }
    // I would prefer to have "createPNG" as an enum ... allows to add options later (such als "createLotsOfPngs", "createSomePngs" etc.)

//    @StringGetter(ELEMENT_REPLANNING_TYPE)
    public ReceiverReplanningType getReplanningType() { return replanningType; }

//    @StringSetter(ELEMENT_REPLANNING_TYPE)
    public void setReplanningType(ReceiverReplanningType replanningType) {
        this.replanningType = replanningType;
    }

    public int getReceiverReplanningInterval() {
        return receiverReplanningInterval;
    }

    public void setReceiverReplanningInterval(int receiverReplanningInterval) {
        this.receiverReplanningInterval = receiverReplanningInterval;
    }
}
