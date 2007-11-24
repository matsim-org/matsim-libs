/* *********************************************************************** *
 * project: org.matsim.*
 * Vec2Type.java
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

package org.matsim.utils.vis.kml.fields;
/**
 * 
 * @author dgrether
 *
 */
public class Vec2Type {
	/**
	 * Constants needed for efficient toString()
	 */
	private static final String XSTRING = "x=\"";
	/**
	 * Constants needed for efficient toString()
	 */	
	private static final String YSTRING = "y=\"";
	/**
	 * Constants needed for efficient toString()
	 */
	private static final String XUNITSSTRING = "xunits=\"";
	/**
	 * Constants needed for efficient toString()
	 */
	private static final String YUNITSSTRING = "yunits=\"";
	/**
	 * Constants needed for efficient toString()
	 */
	private static final String QUOTE = "\"";
	/**
	 * Constants needed for efficient toString()
	 */
	private static final String WHITESPACE = " ";
	/**
	 * The kml:unitsEnum Type
	 * @author dgrether
	 *
	 */
	public enum Units {
		fraction, pixels, insetPixels
	}
	/**
	 * attribute of kml:vec2Type initialized with the default value
	 */
	private double x = 1.0d;
	/**
	 * attribute of kml:vec2Type initialized with the default value
	 */
	private double y = 1.0d;
	/**
	 * attribute of kml:vec2Type initialized with the default value
	 */
	private Units xUnits = Units.fraction;
	/**
	 * attribute of kml:vec2Type initialized with the default value
	 */
  private Units yUnits = Units.fraction;
	
  /**
   * Initialises this Vec2Type with the default values.
   */
  public Vec2Type() {
  }
  
  /**
   * Initialises the fields with the given values and overrides default values.
   * @param x
   * @param y
   * @param xunits
   * @param yunits
   */
  public Vec2Type(double x, double y, Units xunits, Units yunits) {
  	this.x = x;
  	this.y = y;
  	this.xUnits = xunits;
  	this.yUnits = yunits;
  }
  
  
  /**
   * Returns a String useable for kml output.
   */
  @Override
	public String toString() {
  	StringBuffer buffer = new StringBuffer();
  	buffer.append(XSTRING);
  	buffer.append(Double.toString(x));
  	buffer.append(QUOTE);
  	buffer.append(WHITESPACE);
   	buffer.append(YSTRING);
   	buffer.append(Double.toString(y));
   	buffer.append(QUOTE);
   	buffer.append(WHITESPACE);
   	buffer.append(XUNITSSTRING);
   	buffer.append(xUnits.toString());
   	buffer.append(QUOTE);
   	buffer.append(WHITESPACE);
   	buffer.append(YUNITSSTRING);
   	buffer.append(yUnits.toString());
   	buffer.append(QUOTE);
  	return buffer.toString();
  }
  
  
  
  

}
