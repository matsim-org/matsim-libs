/* *********************************************************************** *
 * project: org.matsim.*
 * DgColoredIconStyleBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.matsimkml;

import org.apache.log4j.Logger;

import de.micromata.opengis.kml.v_2_2_0.ColorMode;
import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.Style;



/**
 * @author dgrether
 *
 */
public class DgColoredIconStyleBuilder {
	
	private static final Logger log = Logger.getLogger(DgColoredIconStyleBuilder.class);
	
	private int blue;
	private int green;
	private int red;
	private String hexBlue;
	private String hexGreen;
	private String hexRed;
	private int i;
	
	public DgColoredIconStyleBuilder(int steps){
		i = 0;
		this.reset();
	}
	
	
	private void reset(){
		this.blue = 0;
		this.green = 0;
		this.red = 0;
		this.hexBlue = "00";
		this.hexGreen = "00";
		this.hexRed = "00";
	}
	
	private void increment(){
		if (this.blue < 255){
			this.blue++;
			this.hexBlue = this.getHexString(this.blue);
		}
		else if (this.green < 255){
			this.green++;
			this.hexGreen = this.getHexString(this.green);
		}
		else if (this.red < 255){
			this.red++;
			this.hexRed = this.getHexString(this.red);
		}
		else {
			this.reset();
		}
	}
	
	
	private String getHexString(int c){
		String hexColor = Integer.toHexString(c);
		if (hexColor.length() == 1){
			hexColor = "0" + hexColor;
		}
		return hexColor;
	}
	
	
	public Style getNextStyle(){
		i++;
		this.increment();
		final Style style = new Style();
		style.setId("randomColorIcon" + i);
		final IconStyle iconstyle = new IconStyle();
		style.setIconStyle(iconstyle);
		iconstyle.setColor("ff" + this.hexBlue + this.hexGreen + this.hexRed);
		iconstyle.setColorMode(ColorMode.NORMAL);
		iconstyle.setScale(0.7d);
		final Icon icon = new Icon();
		iconstyle.setIcon(icon);
		icon.setHref("http://maps.google.com/mapfiles/kml/pal2/icon18.png");

		return style;
	}
	
}
