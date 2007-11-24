/* *********************************************************************** *
 * project: org.matsim.*
 * TempHb.java
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
import java.util.List;

import org.matsim.utils.geometry.shared.Coord;

public class TempHb {

	public String ID = null;
	public String name = null;
	public Coord coord = new Coord(0.0,0.0);
	public List<TempHP> hps= new ArrayList<TempHP>();

	public TempHb(){
		super();
	}

	public TempHb(final String id){
		this.ID=id;
	}

	public TempHb(final String id,final String xCoord,final String yCoord){
		this.coord.setXY(Double.parseDouble(xCoord), Double.parseDouble(yCoord));
		this.ID=id;
	}

	public void calcCoords(){
		if(this.hps.isEmpty()==false){
			double x=0.0;
			double y=0.0;
			double cnt=0.0;

			for (TempHP hp : this.hps) {
				x+=hp.getCoord().getX();
				y+=hp.getCoord().getY();
				cnt+=1.0;
			}
			x=x/cnt;
			y=y/cnt;
			this.coord.setXY(x, y);
		} else {
			System.out.println("hb nr. "+this.ID+" has no hps!");
		}
	}

}
