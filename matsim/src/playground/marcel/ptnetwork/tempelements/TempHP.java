/* *********************************************************************** *
 * project: org.matsim.*
 * TempHP.java
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

package playground.marcel.ptnetwork.tempelements;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.utils.geometry.shared.Coord;

public class TempHP {

	// member variables

	public TempHb hb;
	public TempLine line;

	private String hp_Id;
	private String hb_Id;
	private String name;
	private Coord coord;
	private int direct;

	public LinkedList<String> oldIDs = new LinkedList<String>();

	public List<TempLink> inLinks = new ArrayList<TempLink>();
	public List<TempLink> outLinks = new ArrayList<TempLink>();

	// clone constructor

	public TempHP(TempHP parent) {
		this.hp_Id = parent.hp_Id;
		if(parent.hb_Id != null){
			this.hb_Id = parent.hb_Id;
		} else {
			this.hb_Id = null;
		}
		this.name = parent.name;
		this.direct=parent.direct;
		this.coord =  new Coord(parent.coord);
		this.oldIDs.clear();
		this.oldIDs.addAll(parent.oldIDs);

		this.inLinks.clear();
		this.outLinks.clear();
	}

//	clone and change direction if back==true

	public TempHP(TempHP parent, boolean back) {

		this.hp_Id = parent.hp_Id;

		if (parent.hb_Id != null) {
			this.hb_Id = parent.hb_Id;
		} else {
			this.hb_Id = null;
		}

		this.name = parent.name;
		this.coord =  new Coord(parent.coord.getX(), parent.coord.getY());
		this.oldIDs.clear();
		this.oldIDs.addAll(parent.oldIDs);

		this.inLinks.clear();
		this.outLinks.clear();

		if (back && (parent.direct==1)) {
			this.direct=2;
		} else {
			this.direct=parent.direct;
		}
	}

	public TempHP(){
		this.hp_Id=null;
		this.hb_Id=null;
		this.name=null;
		this.coord= new Coord("0","0");
		this.direct=1;
	}

	public String getHp_Id(){
		return this.hp_Id;
	}

	public void setHp_Id(String Id){
		this.hp_Id=Id;
	}

	public void set10charHp_Id(String id){
		this.hp_Id=id;
		for(int i=0;i<(9-id.length());i++){
			this.hp_Id="0"+this.hp_Id;
		}
		this.hp_Id=this.hp_Id+"1";
	}

	public String getHb_Id(){
		return this.hb_Id;
	}

	public void setHb_Id(String Id){
		this.hb_Id=Id;
	}

	public String getName(){
		return this.name;
	}

	public void setName(String newname){
		this.name=newname;
	}

	public Coord getCoord(){
		return this.coord;
	}

	public void setCoord(double x,double y){
		this.coord.setX(x);
		this.coord.setY(y);
	}

	public void setXCoord(double x){
		this.coord.setX(x);
	}

	public void setYCoord(double Y){
		this.coord.setY(Y);
	}

	public int getDirect(){
		return this.direct;
	}

	public void setDirect(int newDirect){
		this.direct=newDirect;
	}

	public boolean hasOldId(String Id){
		for (String oldId : this.oldIDs) {
			if (oldId.equals(Id)){
				return true;
			}
		}
		return false;
	}
}