
 /* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
 *                                                                         
 * *********************************************************************** */


package playground.fhuelsmann.emission;
import org.matsim.api.core.v01.Id;


public class SingleEvent {

	private String activity;
	private Id Personal_id; //
	private Id Link_id;
	private double averageSpeed;
	private double travelTime;
	private double enterTime;
	private double linkLength;
	private int Hbefa_Road_type;
	private double emissionFactor;	
	private double emissionFractions;	
	private Id Visum_road_Section_Nr; //
	private int freeVelocity;
	private int  roadType;
	
	private double emissions;	
	private double noxEmissions;
	private double noxFractions;
	

	public int getFreeVelocity() {
		return freeVelocity;
	}


	public void setFreeVelocity(int freeVelocity) {
		this.freeVelocity = freeVelocity;
	}


	public int  getRoadType() {
		return roadType;
	}


	public void setRoadType(int  roadType) {
		this.roadType = roadType;
	}


	public double getNoxFractions() {
		return noxFractions;
	}


	public void setNoxFractions(double noxFractions) {
		this.noxFractions = noxFractions;
	}


	public double getNoxEmissions() {
		return noxEmissions;
	}


	public void setNoxEmissions(double noxEmissions) {
		this.noxEmissions = noxEmissions;
	}


	public Id getVisum_road_Section_Nr() {
		return Visum_road_Section_Nr;
	}


	public void setVisum_road_Section_Nr(Id visum_road_Section_Nr) {
		Visum_road_Section_Nr = visum_road_Section_Nr;
	}
	

	public double getEmissions() {
		return emissions;
	}


	public void setEmissions(double emissions) {
		this.emissions = emissions;
	}


	public double getEmissionFactor() {
		return emissionFactor;
	}


	public void setEmissionFactor(double emissionFactor) {
		this.emissionFactor = emissionFactor;
	}
	

	public double getEmissionFractions() {
		return emissionFractions;
	}


	public void setEmissionFractions(double emissionFractions) {
		this.emissionFractions= emissionFractions;
	}


	public int getHbefa_Road_type() {
		return Hbefa_Road_type;
	}


	public void setHbefa_Road_type(int hbefa_Road_type) {
		Hbefa_Road_type = hbefa_Road_type;
	}
	

	
	public SingleEvent(String activity, double travelTime, 
			double averageSpeed, Id personalId, double length3,Id Link_id,Id Visum_road_Section_Nr, int Visum_road_type_no) {
	
		this.activity=activity;
		this.travelTime=travelTime;
		this.averageSpeed=averageSpeed;
		this.Personal_id=personalId;
		this.linkLength=length3;
		this.Link_id=Link_id;
		this.Visum_road_Section_Nr= Visum_road_Section_Nr;
		this.roadType = Visum_road_type_no;
	}
	

	public SingleEvent(String activity, double travelTime,
			double averageSpeed, Id personId, double length,
			Id roadSectionNrId, double enterTime, int freeVelocity, int visumRoadType) {
		
		this.activity=activity;
		this.travelTime = travelTime;
		this.averageSpeed= averageSpeed;
		this.Personal_id=personId;
		this.linkLength=length;
		this.Visum_road_Section_Nr=roadSectionNrId;
		this.enterTime= enterTime;
		this.freeVelocity= freeVelocity;
		this.roadType =visumRoadType;
		this.Link_id= roadSectionNrId;
		
		
	}
	

	
	public SingleEvent(String activity,double travelTime,double averageSpeed, 
				Id personId,double distance,int  roadType,double enterTime, int freeVelocity,Id Link_id){

		this.activity=activity;
		this.travelTime=travelTime;
		this.averageSpeed=averageSpeed;
		this.Personal_id=personId;
		this.linkLength=distance;
		this.roadType = roadType;
		this.enterTime=enterTime;
		this.freeVelocity =freeVelocity;
		this.Link_id=Link_id;}


	public int getfreeVelocity() {
		return freeVelocity;
	}


	public void setfreeVelocity(int freeVelocity) {
		this.freeVelocity = freeVelocity;
	}
	public String getActivity() {
		return activity;
	}


	public void setActivity(String activity) {
		this.activity = activity;
	}


	public Id getPersonal_id() {
		return Personal_id;
	}


	public void setPersonal_id(Id personal_id) {
		Personal_id = personal_id;
	}


	public Id getLink_id() {
		return Link_id;
	}


	public void setLink_id(Id link_id) {
		Link_id = link_id;
	}


	public double getAverageSpeed() {
		return averageSpeed;
	}


	public void setAverageSpeed(double averageSpeed) {
		this.averageSpeed = averageSpeed;
	}


	public double getTravelTime() {
		return travelTime;
	}


	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}
	
	public double getEnterTime() {
		return enterTime;
	}


	public void setEnterTime(double enterTime) {
		this.enterTime = enterTime;
	}


	public double getLinkLength() {
		return linkLength;
	}


	public void setLinkLength(double linkLength) {
		this.linkLength = linkLength;
	}

}