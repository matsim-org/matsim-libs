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

import org.matsim.api.core.v01.Id;

public class SingleEvent {

	private String activity;
	private String Personal_id; //
	public int getVisum_road_Section_Nr() {
		return Visum_road_Section_Nr;
	}


	public void setVisum_road_Section_Nr(int visum_road_Section_Nr) {
		Visum_road_Section_Nr = visum_road_Section_Nr;
	}


	private int Link_id;
	private double averageSpeed;
	private String travelTime;
	private String enterTime;
	private double linkLength;
	private int Hbefa_Road_type;
	private double emissionFactor;	
	private double emissionFractions;	
	private int Visum_road_Section_Nr; //
	private int Visum_road_type_no; //
	private int freeVelocity;
	private double emissions;	

	

	

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


	public int getVisum_road_type_no() {
		return Visum_road_type_no;
	}


	public void setVisum_road_type_no(int visum_road_type_no) {
		Visum_road_type_no = visum_road_type_no;
	}


	public SingleEvent(String activity, String travelTimeString, 
			double averageSpeed, String personalId, double length3,int Link_id,int Visum_road_Section_Nr, int Visum_road_type_no) {
	
		this.activity=activity;
		this.travelTime=travelTimeString;
		this.averageSpeed=averageSpeed;
		this.Personal_id=personalId;
		this.linkLength=length3;
		this.Link_id=Link_id;
		this.Visum_road_Section_Nr= Visum_road_Section_Nr;
		this.Visum_road_type_no = Visum_road_type_no;
	}
	

	
	public SingleEvent(String activity, String travelTimeString, 
			double averageSpeed, String personalId, double length3,int Link_id,int Visum_road_Section_Nr, int Visum_road_type_no,String enterTime, int freeVelocity  ) {
	
		this.activity=activity;
		this.travelTime=travelTimeString;
		this.averageSpeed=averageSpeed;
		this.Personal_id=personalId;
		this.linkLength=length3;
		this.Link_id=Link_id;
		this.Visum_road_Section_Nr= Visum_road_Section_Nr;
		this.Visum_road_type_no = Visum_road_type_no;
		this.enterTime=enterTime;
		this.freeVelocity =freeVelocity;

	}


	public SingleEvent(String activity,String travelTime,double averageSpeed, 
				String personId,double distance,int roadType,String enterTime, int freeVelocity){

		this.activity=activity;
		this.travelTime=travelTime;
		this.averageSpeed=averageSpeed;
		this.Personal_id=personId;
		this.linkLength=distance;
		this.Visum_road_type_no = roadType;
		this.enterTime=enterTime;
		this.freeVelocity =freeVelocity;	}


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


	public String getPersonal_id() {
		return Personal_id;
	}


	public void setPersonal_id(String personal_id) {
		Personal_id = personal_id;
	}


	public int getLink_id() {
		return Link_id;
	}


	public void setLink_id(int link_id) {
		Link_id = link_id;
	}


	public double getAverageSpeed() {
		return averageSpeed;
	}


	public void setAverageSpeed(double averageSpeed) {
		this.averageSpeed = averageSpeed;
	}


	public String getTravelTime() {
		return travelTime;
	}


	public void setTravelTime(String travelTime) {
		this.travelTime = travelTime;
	}
	
	public String getEnterTime() {
		return enterTime;
	}


	public void setEnterTime(String travelTime) {
		this.enterTime = enterTime;
	}


	public double getLinkLength() {
		return linkLength;
	}


	public void setLinkLength(double linkLength) {
		this.linkLength = linkLength;
	}


	public int getVisumRoadType() {
		return this.Visum_road_type_no;
	}

}