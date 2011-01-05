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

import org.apache.log4j.Logger;

/**
 * @author droeder
 *
 */
public class DaColorScheme {
	private final static Logger log = Logger.getLogger(DaColorScheme.class);

	private Color c0 = new Color(102,0,51);
	private Color c1 = new Color(0, 0 ,102);
	private Color c2 = new Color(0, 102, 0);
	private Color c3 = new Color(102,0,0);
	private Color c4 = new Color(0,102,102);
	private Color c5 = new Color(102,102,0);
	private Color c6 = new Color(51,0,102);
	private Color c7 = new Color(102,102,102);
	private Color c8 = new Color(0,51,102);
	private Color c9 = new Color(0,102,51);
	private Color c10 = new Color(51,51,102);
	private Color c11 = new Color(51,102,51);
	private Color c12 = new Color(102,51,51);

	public Color getColor(int i){
		if(i==0){
			return c0;
		}else if(i==1){
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
		}else if(i==7){
			return c7;
		}else if(i==8){
			return c8;
		}else if(i==9){
			return c9;
		}else if(i==10){
			return c10;
		}else if(i==11){
			return c11;
		}else if(i==12){
			return c12;
		}else{
			log.error("no color for this argument");
			return new Color(0);
		}
	}


}
