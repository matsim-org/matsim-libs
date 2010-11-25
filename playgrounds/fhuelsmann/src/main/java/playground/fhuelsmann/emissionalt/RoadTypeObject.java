/* *********************************************************************** *
 * project: org.matsim.*
 * BKickControler
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.fhuelsmann.emissionalt;
public class RoadTypeObject{

private int ObjectId;
private int Road_type_nur;
private int RoadSection;
public RoadTypeObject(int objectId, int road_type_nur, int roadSection) {
	super();
	ObjectId = objectId;
	Road_type_nur = road_type_nur;
	RoadSection = roadSection;
}

public int getObjectId() {
	return ObjectId;
}
public void setObjectId(int objectId) {
	ObjectId = objectId;
}
public int getRoad_type_nur() {
	return Road_type_nur;
}
public void setRoad_type_nur(int road_type_nur) {
	Road_type_nur = road_type_nur;
}
public int getRoadSection() {
	return RoadSection;
}
public void setRoadSection(int roadSection) {
	RoadSection = roadSection;
}



	
}
