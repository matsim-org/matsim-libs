/* *********************************************************************** *
 * project: org.matsim.*
 * Household.java
 *                                                                         *
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

package playground.balmermi.teleatlas;



public class NwElement {

	public Long id;

	public static enum NwFeatureType {
		ROAD_ELEMENT, FERRY_CONNECTION, ADDRESS_AREA_BOUNDARY
	}
	public NwFeatureType featType;

	public static enum FerryType {
		NO_FERRY, SHIP_HOVERCRAFT, TRAIN
	}
	public FerryType ferryType;

	public JcElement fromJunction;
	public JcElement toJunction;

	public Double length;

	public static enum FunctionalRoadClass {
		UNDEFINED, MOTORWAY, MAJOR_ROAD_HIGH, MAJOR_ROAD_LOW, SECONDARY_ROAD,
		LOCAL_CONNECTING_ROAD, LOCAL_ROAD_HIGH, LOCAL_ROAD_MEDIUM, LOCAL_ROAD_LOW,
		OTHER_ROAD
	}
	public FunctionalRoadClass frc;

	public static enum NetTwoClass {
		UNDEFINED, MOTORWAY, MAIN_AXIS, CONNECTION_AXIS_HIGH, CONNECTION_AXIS_LOW,
		COUNTRYSIDE_LOCAL_ROAD_LOW, PARKING_ACCESS_ROAD, RESTRICTED_PEDESTRIAN
	}
	public NetTwoClass net2Class;

	public static enum FormOfWay {
		UNDEFINED, MOTORWAY, MULTI_CARRIAGEWAY, SINGLE_CARRIAGEWAY, ROUNDABOUT,
		ETA_PARKING_PLACE, ETA_PARKING_GARAGE, ETA_UNSTRUCT_TRAFFIC_SQUARE,
		SLIP_ROAD, SERVICE_ROAD, ENTRANCE_EXIT_CARPARK, PEDESTRIAN_ZONE,
		WALKWAY, SPECIAL_TRAFFIC_FIGURES, AUTHORITIES, CONNECTOR, CUL_DE_SAC
	}
	public FormOfWay fow;

	public static enum Freeway {
		NO_FREEWAY, FREEWAY
	}
	public Freeway freeway;

	public static enum PrivateRoad {
		NO_SPECIAL_RESTRICTION, SPECIAL_RESTRICTION
	}
	public PrivateRoad privat;

	public static enum OneWay {
		OPEN, OPEN_FT, CLOSED, OPEN_TF
	}
	public OneWay oneway;

	public Integer nOfLanes;

	public static enum SpeedCategory {
		GT130, R101_130, R91_100, R71_90, R51_70, R31_50, R11_30, LT11, OTHER
	}
	public SpeedCategory speedCat;

	//////////////////////////////////////////////////////////////////////
	// print method
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		StringBuffer str = new StringBuffer();
		str.append(this.getClass().getSimpleName()).append(':');
		str.append("id=").append(id).append(';');
		str.append("featType=").append(featType).append(';');
		str.append("ferryType=").append(ferryType).append(';');
		if (fromJunction == null) { str.append("fromJunctionId=").append("null").append(';'); }
		else { str.append("fromJunctionId=").append(fromJunction.id).append(';'); }
		if (toJunction == null) { str.append("toJunctionId=").append("null").append(';'); }
		else { str.append("toJunctionId=").append(toJunction.id).append(';'); }
		str.append("length=").append(length).append(';');
		str.append("frc=").append(frc).append(';');
		str.append("net2Class=").append(net2Class).append(';');
		str.append("fow=").append(fow).append(';');
		str.append("freeway=").append(freeway).append(';');
		str.append("privat=").append(privat).append(';');
		str.append("oneway=").append(oneway).append(';');
		str.append("nOfLanes=").append(nOfLanes).append(';');
		str.append("speedCat=").append(speedCat).append(';');
		return str.toString();
	}
}
