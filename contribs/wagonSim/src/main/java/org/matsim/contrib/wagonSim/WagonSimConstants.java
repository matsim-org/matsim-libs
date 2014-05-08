/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.wagonSim;

import java.text.SimpleDateFormat;


/**
 * @author droeder
 *
 */
public final class WagonSimConstants {
	
	private WagonSimConstants(){}
	
	public static final SimpleDateFormat DATE_FORMAT_DDMMYYYYHHMMSS = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	public static final SimpleDateFormat DATE_FORMAT_YYYYMMDDHHMMSS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss");

	public static final String DEFAULT_VEHICLE_TYPE = "dLT";
	
	public static final String WAGON_LENGTH = "length";
	public static final String WAGON_GROSS_WEIGHT = "grossWeight";
	public static final String DESTINATION = "destination";
	public static final String ORIGIN = "origin";
	
	public static final String NODE_COUNTRY_ID = "countryId";
	public static final String NODE_NAME = "name";
	public static final String NODE_HOME_COUNTRY = "homeCountry";
	public static final String NODE_STATION = "station";
	public static final String NODE_VALID = "valid";
	public static final String NODE_CLUSTER_ID = "clusterId";

	public static final String LINK_SIMULATE = "simulate";
	public static final String LINK_GLOBAL = "global";
	public static final String LINK_CLOSED = "closed";
	public static final String LINK_VALID = "valid";
	public static final String LINK_MAXTRAINLENGTH = "maxTrainLength";
	public static final String LINK_TYPE = "type";
	public static final String LINK_VFACTOR = "vFactor";
	public static final String LINK_OWNERID = "ownerId";
	public static final String LINK_OWNERNAME = "ownerName";
	public static final double DEFAULT_CAPACITY = 99999.0;
	public static final double DEFAULT_FREESPEED = 99999.0;
	public static final double DEFAULT_NUMLANES = 1.0;
	public static final double DEFAULT_LENGTH_LOOPLINK = 50.0;
	public static final String DEFAULT_LINK_MODE = "train";
	
	public static final String NODE_PRODUCTIONNODE_TYPE = "productionNodeType";
	public static final String NODE_INFRANODE_ID = "infraNodeId";
	public static final String NODE_MAXTRAINCREATION = "maxTrainCreation";
	public static final String NODE_MAXWAGONSHUNTINGS = "maxWagonShuntings";
	public static final String NODE_SHUNTINGTIME = "shuntingTime";
	public static final String NODE_PARENTRECEPTIONNODE_ID = "parentReceptionNode";
	public static final String NODE_ISBORDER = "isBorder";
	public static final String NODE_MINSERVICE = "minService";
	public static final String NODE_DELIVERYTYPE_ID = "deliveryType";
	public static final String NODE_DELIVERYTYPE_DESC = "deliveryTypeDesc";
	public static final String NODE_DELIVERYTYPE_DISTR = "deliveryTypeDistr";
	
	public static final String STOP_STATION_ID = "stationId";
	public static final String STOP_MIN_SHUNTING_TIME = "minShuntingTime";
	
	public static final String TRAIN_TYPE = "trainType";
	public static final String TRAIN_MAX_SPEED = "maxSpeed";
	public static final String TRAIN_MAX_WEIGHT = "maxWeight";
	public static final String TRAIN_MAX_LENGTH = "maxLength";
	
	public static final double ADDITIONAL_DISUTILITY = -100000;
	
	public static final String SHUNTING_TABLE_LOCID = "locomitiveId";
	public static final String SHUNTING_TABLE_NODEID = "nodeId";
	public static final String SHUNTING_TABLE_SHUNTINGFLAG = "shuntingAllowed";
}

