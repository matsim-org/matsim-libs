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
	};
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
	
	public static enum ConstructionStatus {
		NONE, FT, BOTH, TF 
	}
	public ConstructionStatus construction;
	
	public static enum OneWay {
		OPEN, OPEN_FT, CLOSED, OPEN_TF
	}
	public OneWay oneway;
	
	public Double calcSpeed;
	
	public Double travelTime;
	
	public Integer nOfLanes;
	
	public static enum SpeedCategory {
		GT130, R101_130, R91_100, R71_90, R51_70, R31_50, R11_30, LT11
	}
	public SpeedCategory speedCat;
	
	//////////////////////////////////////////////////////////////////////
	// print method
	//////////////////////////////////////////////////////////////////////
	
	@Override
	public final String toString() {
		StringBuffer str = new StringBuffer();
		str.append(this.getClass().getSimpleName()); str.append(':');
		str.append("id="); str.append(id); str.append(';');
		str.append("featType="); str.append(featType); str.append(';');
		str.append("ferryType="); str.append(ferryType); str.append(';');
		if (fromJunction == null) { str.append("fromJunctionId="); str.append("null"); str.append(';'); }
		else { str.append("fromJunctionId="); str.append(fromJunction.id); str.append(';'); }
		if (toJunction == null) { str.append("toJunctionId="); str.append("null"); str.append(';'); }
		else { str.append("toJunctionId="); str.append(toJunction.id); str.append(';'); }
		str.append("length="); str.append(length); str.append(';');
		str.append("frc="); str.append(frc); str.append(';');
		str.append("net2Class="); str.append(net2Class); str.append(';');
		str.append("fow="); str.append(fow); str.append(';');
		str.append("freeway="); str.append(freeway); str.append(';');
		str.append("privat="); str.append(privat); str.append(';');
		str.append("construction="); str.append(construction); str.append(';');
		str.append("oneway="); str.append(oneway); str.append(';');
		str.append("calcSpeed="); str.append(calcSpeed); str.append(';');
		str.append("travelTime="); str.append(travelTime); str.append(';');
		str.append("nOfLanes="); str.append(nOfLanes); str.append(';');
		str.append("speedCat="); str.append(speedCat); str.append(';');
		return str.toString();
	}
}
