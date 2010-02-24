/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder;

import java.awt.Color;

import org.jfree.util.Log;

/**
 * @author droeder
 *
 */
public class DaColorScheme {
	
	public Color c1 = new Color(0, 0 ,128);
	public Color c2 = new Color(0, 128, 0);
	public Color c3 = new Color(250,250,0);
	public Color c4 = new Color(128,0,0);
	public Color c5 = new Color(0,0,250);
	public Color c6 = new Color(51,128,204);
	int ii = 0;
	
	public Color getColor(int i){
		if(i == 1){
			return c1;
		}else if(i==2){
			return c2;
		}else if(i==3){
			return c3;
		}else if(i==4){
			return c4;
		}else if(i==5){
			return c5;
		}else if(i==6){
			return c6;
		}else{
			Log.error("no color for this argument");
			ii++;
			return new Color(ii);
		}
	}
	

}
