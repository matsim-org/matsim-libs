/* *********************************************************************** *
 * project: org.matsim.*
 * Color.java
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
 * This class is used for the kml <color> tags. It provides several constructors with which
 * color objects can be created conveniently.
 * @author dgrether
 *
 */
public class Color {
	/**
	 * constant string for output
	 */
	private static final String COLORSTARTTAG = "<color>";
	/**
	 * constant string for output
	 */
	private static final String COLORENDTAG = "</color>";
	/**
	 * constant for highest hex value
	 */
	private static final String FF = "ff";
	/**
	 * alpha value of this color
	 */
	private int alpha;
	/**
	 * blue value of this color
	 */
	private int blue;	
	/**
	 * green value of this color
	 */
	private int green;
	/**
	 * red value of this color
	 */
	private int red;
	
	public static final Color DEFAULT_COLOR = new Color("ff","ff","ff","ff");
	
//	in hex: alpha blue green red
//	<color>ffffffff</color>
	
	/**
	 * Creates a color object from an kml native implementation
	 * @param alpha string containing a hex value in 00..ff
	 * @param blue string containing a hex value in 00..ff
	 * @param green string containing a hex value in 00..ff
	 * @param red string containing a hex value in 00..ff
	 * 
	 */
	public Color(final String alpha, final String blue, final String green, final String red) {
		this.alpha = Integer.valueOf(alpha, 16);
		this.blue = Integer.valueOf(blue, 16);
		this.green = Integer.valueOf(green, 16);
		this.red = Integer.valueOf(red, 16);
	}
	/**
	 * Creates a color object from an RGB value, alpha is set to maximum
	 * @param red a value in 0..255
	 * @param green a value in 0..255
	 * @param blue a value in 0..255
	 */
	public Color(final int red, final int green, final int blue) {
		this.alpha = Integer.valueOf(FF, 16);
		this.blue = blue;
		this.green = green;
		this.red = red;
	}
	
	/**
	 * Creates a color object from an RGB value
	 * @param alpha a value in 0..255
	 * @param red a value in 0..255
	 * @param green a value in 0..255
	 * @param blue a value in 0..255
	 */
	public Color(final int alpha, final int red, final int green, final int blue) {
		this.alpha = alpha;
		this.blue = blue;
		this.green = green;
		this.red = red;
	}
	
	private String to2DigitHexString(final int value) {
		String s = Integer.toHexString(value);
		if (s.length() == 1) {
			StringBuffer b = new StringBuffer();
			b.append("0");
			b.append(s);
			return b.toString();
		}
		else {
			return s;
		}
		
	}
	
  /**
   * Returns a String useable for kml output.
   */
  @Override
	public String toString() {
  	StringBuffer buffer = new StringBuffer();
  	buffer.append(COLORSTARTTAG);
  	buffer.append(to2DigitHexString(this.alpha));
  	buffer.append(to2DigitHexString(this.blue));
  	buffer.append(to2DigitHexString(this.green));
  	buffer.append(to2DigitHexString(this.red));
  	buffer.append(COLORENDTAG);
  	return buffer.toString();
  }
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Color) {
			Color c = (Color) obj;
			return ((this.alpha == c.alpha) && (this.blue == c.blue) && (this.green == c.green) && (this.red == c.red));
		}
		return super.equals(obj);
	}
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		StringBuffer buffer = new StringBuffer();
  	buffer.append(Integer.toHexString(this.alpha));
  	buffer.append(Integer.toHexString(this.blue));
  	buffer.append(Integer.toHexString(this.green));
  	buffer.append(Integer.toHexString(this.red));
  	return buffer.toString().hashCode();
	}
}
