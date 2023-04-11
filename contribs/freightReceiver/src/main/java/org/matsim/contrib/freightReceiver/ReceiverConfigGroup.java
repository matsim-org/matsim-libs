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
package org.matsim.contrib.freightReceiver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public final class ReceiverConfigGroup extends ReflectiveConfigGroup {

    public ReceiverConfigGroup() { super(NAME); }

    @SuppressWarnings("unused")
    private static final Logger LOG = LogManager.getLogger(ReceiverConfigGroup.class);

    public static final String NAME = "freightReceiver";

    final static String COSTSHARING = "collaboration";
    final static String RECEIVER_FILE = "receiversFile";
    final static String CARRIERS_FILE = "carriersFile";
    final static String NONCOLLABORATING_FEE = "noncollaboratingFee";
    final static String CREATE_PNG = "createPNG";
    final static String REPLANNING_TYPE = "replanningModules";

    private String receiversFile = "./receivers.xml.gz";
    private String carriersFile = "./carriers.xml.gz";
    private String costSharing = "proportional";
    private boolean createPNG = true;
    private ReceiverReplanningType replanningType = ReceiverReplanningType.serviceTime;
    private int receiverReplanningInterval = 1;

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(COSTSHARING, "The mechanism by which the carriers' costs are shared among receivers. " +
                "Available values are 'proportional' (default), and 'marginal'.");
        comments.put(RECEIVER_FILE, "Path to the file containing the receivers.");
        comments.put(CARRIERS_FILE, "Path to the file containing the carriers.");
        comments.put(NONCOLLABORATING_FEE, "The fixed fee charged per receiver NOT collaborating in a coalition.");
        comments.put(CREATE_PNG, "Should PNG output be created? Default is 'true'");
        comments.put(REPLANNING_TYPE, "Replanning strategy used. Available values are 'serviceTime' (default), " +
                "'timeWindow' and 'deliveryFrequency'.");
        return comments;
    }

//    @StringGetter(RECEIVER_FILE)
    public String getReceiversFile() {
        return receiversFile;
    }

//    @StringSetter(RECEIVER_FILE)
    public void setReceiversFile(String receiversFile) {
        this.receiversFile = receiversFile;
    }

    @StringGetter(CARRIERS_FILE)
    public String getCarriersFile() {
        return carriersFile;
    }

    @StringSetter(CARRIERS_FILE)
    public void setCarriersFile(String carriersFile) {
        this.carriersFile = carriersFile;
    }

    @StringGetter(COSTSHARING)
    public String getCostSharing() {
        return costSharing;
    }

    @StringSetter(COSTSHARING)
    public void setCostSharing(String costSharing) {
        this.costSharing = costSharing;
    }

    @StringGetter(CREATE_PNG)
    public boolean isCreatePNG() { return createPNG; }

    @StringSetter(CREATE_PNG)
    public void setCreatePNG(boolean createPNG) { this.createPNG = createPNG; }

    @StringGetter(REPLANNING_TYPE)
    public ReceiverReplanningType getReplanningType() { return replanningType; }

    @StringSetter(REPLANNING_TYPE)
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
