/* *********************************************************************** *
 * project: org.matsim.*
 * CoordWGS84.java
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

package org.matsim.utils.geometry.shared;

/**
 * Holds a 2D coordinate in the World Geodetic System 1984. <br>
 *
 * The purpose of this class is to provide conversion between different
 * coordinate systems. <br>
 * In order to add another conversion,
 *
 * <ol>
 * 	<li> Add a factory method to calculate WGS84 methods from your <br>
 * coordinate system<br>
 *  (such as createFromCH1903(...)).
 *  <li> Add method(s) to reversely transform WGS84 coordinates. <br>
 *  (such as getLongitudeFromCH1903Dates(...)).
 * </ol>
 *
 * @see <a href="http://de.wikipedia.org/wiki/WGS84">de.wikipedia.org/wiki/WGS84</a>
 */
public class CoordWGS84 {

	private static boolean shownWarningCH1903 = false;

	private final double longitude;
	private final double latitude;

	// constructors
	private CoordWGS84(final double longitude, final double latitude) {
		super();
		this.longitude = longitude;
		this.latitude = latitude;
	}

	// factory methods

	/**
	 * Constructs a new CoordWGS84 object from Swiss National Coordinate values.<br>
	 *
	 * <b>WARNING:</b><br>
	 * In the CH1903 coordiante system, the y-value describes easting, while the
	 * x-value describes northing. In many other coordinate systems, the meaning
	 * of x and y are just the othe way round.<br>
	 * In the CH1903 coordinate system, the y-value is always greater than
	 * the x-value for coordinates within the Swiss boundary.
	 * In some data, x and y are designated the other way round.
	 * In this case, exchange x and y when constructing the CoordWGS84 object
	 * from Swiss coordinates.
	 *
	 * @param x x-value of Swiss National Coordinate (<b>Northing!</b>)
	 * @param y y-value of Swiss National Coordinate (<b>Easting!</b>)
	 * @return the created WGS84 coordinate
	 */
	public static CoordWGS84 createFromCH1903(final double x, final double y) {

		if (y <= x && !shownWarningCH1903) {
			shownWarningCH1903 = true;
			System.err.println("WARNING: In WGS84.createFromCH1903(final double x, final double y)" +
					System.getProperty("line.separator") +
					"In the CH1903 coordinate system, the y-value is always greater than the x-value for coordinates within the Swiss boundary." +
					System.getProperty("line.separator") +
					"However, in some datasets x and y are designated the other way round (which is wrong)." +
					System.getProperty("line.separator") +
					"In this case, exchange x and y when constructing the CoordWGS84 object from Swiss coordinates." +
					System.getProperty("line.separator"));
		}

		double longitude = CoordWGS84.getLongitudeFromCH1903Dates(x, y);
		double latitude = CoordWGS84.getLatitudeFromCH1903Dates(x, y);

		return new CoordWGS84(longitude, latitude);

	}

	/**
	 * Constructs a new CoordWGS84 object from WGS degrees.
	 *
	 * @param longitude longitude of the WGS84 coordinate
	 * @param latitude latitude of the WGS84 coordinate
	 * @return the created WGS84 coordinate
	 */
	public static CoordWGS84 createFromWGS84(final double longitude, final double latitude) {

		return new CoordWGS84(longitude, latitude);

	}

	/**
	 * @param lonDegree degree part of longitude
	 * @param lonMinute minute part of longitude
	 * @param lonSecond second part of longitude
	 * @param latDegree degree part of latitude
	 * @param latMinute minute part of latitude
	 * @param latSecond second part of latitude
	 * @return the created WGS84 coordiante
	 */
	public static CoordWGS84 createFromWGS84(
			final double lonDegree,
			final double lonMinute,
			final double lonSecond,
			final double latDegree,
			final double latMinute,
			final double latSecond) {

		double longitude = lonDegree + lonMinute / 60 + lonSecond / 3600;
		double latitude = latDegree + latMinute / 60 + latSecond / 3600;

		return new CoordWGS84(longitude, latitude);

	}

//	public CoordWGS84(final CH1903Date x, final CH1903Date y) {
//
//		if (y.getDoubleValue() <= x.getDoubleValue()) {
//			Gbl.warningMsg(
//					this.getClass(),
//					"CoordWGS84(final CH1903Date x, final CH1903Date y)",
//					System.getProperty("line.separator") +
//					"In the CH1903 coordinate system, the y-value is always greater than the x-value for coordinates within the Swiss boundary." +
//					System.getProperty("line.separator") +
//					"However, in some datasets x and y are designated the other way round (which is wrong)." +
//					System.getProperty("line.separator") +
//					"In this case, exchange x and y when constructing the CoordWGS84 object from Swiss coordinates." +
//					System.getProperty("line.separator"));
//		}
//		this.longitude = this.getLongitudeFromCH1903Dates(x, y);
//		this.latitude = this.getLatitudeFromCH1903Dates(x, y);
//
//	}
//
//	public CoordWGS84(final WGS84Date longitude, final WGS84Date latitude) {
//
//		this.longitude = longitude.getDoubleValue();
//		this.latitude = latitude.getDoubleValue();
//
//	}
//
//	public CoordWGS84(
//			final double lonDegree,
//			final double lonMinute,
//			final double lonSecond,
//			final double latDegree,
//			final double latMinute,
//			final double latSecond) {
//
//		this.longitude = lonDegree + lonMinute / 60 + lonSecond / 3600;
//		this.latitude = latDegree + latMinute / 60 + latSecond / 3600;
//
//	}

	// getters

	/**
	 * @return the latitude (unit: degree)
	 */
	public double getLatitude() {
		return this.latitude;
	}

	/**
	 * @return the longitude (unit: degree)
	 */
	public double getLongitude() {
		return this.longitude;
	}

	/**
	 * @return the x-value of this coordinate in the Swiss National
	 * Coordinate System
	 *
	 * @see <a href="http://www.swisstopo.ch/pub/down/basics/geo/system/ch1903_wgs84_de.pdf">Swisstopo Umrechnungen (PDF)</a>
	 */
	public double getXCH1903() {

		double lonNorm = this.getLonNorm();
		double latNorm = this.getLatNorm();

		return
			200147.07 +
			308807.95 * latNorm +
			3745.25 * Math.pow(lonNorm, 2) +
			76.63 * Math.pow(latNorm, 2) -
			194.56 * Math.pow(lonNorm, 2) * latNorm +
			119.79 * Math.pow(latNorm, 3);
	}

	/**
	 * @return the y-value of this coordinate in the Swiss National
	 * Coordinate System
	 *
	 * @see <a href="http://www.swisstopo.ch/pub/down/basics/geo/system/ch1903_wgs84_de.pdf">Swisstopo Umrechnungen (PDF)</a>
	 */
	public double getYCH1903() {

		double lonNorm = this.getLonNorm();
		double latNorm = this.getLatNorm();

		return
			600072.37 +
			211455.93 * lonNorm -
			10938.51 * lonNorm * latNorm -
			0.36 * lonNorm * Math.pow(latNorm, 2) -
			44.54 * Math.pow(lonNorm, 3);
	}

	// internal methods

	private double getLonNorm() {
		return (this.longitude * 3600 - 26782.5) / 10000;
	}

	private double getLatNorm() {
		return (this.latitude * 3600 - 169028.66) / 10000;
	}


	// CH1903 -> WGS84

	private static double getYNorm(final double y) {
		return (y - 600000) / 1000000;
	}

	private static double getXNorm(final double x) {
		return (x - 200000) / 1000000;
	}

	private static double getLongitudeFromCH1903Dates(final double x, final double y) {

		double xNorm = CoordWGS84.getXNorm(x);
		double yNorm = CoordWGS84.getYNorm(y);

		double longitude10000Sec =
			2.6779094 +
			4.728982 * yNorm +
			0.791484 * yNorm * xNorm +
			0.1306 * yNorm * Math.pow(xNorm, 2) -
			0.0436 * Math.pow(yNorm, 3);

		return longitude10000Sec * 100 / 36;

	}

	private static double getLatitudeFromCH1903Dates(final double x, final double y) {

		double xNorm = CoordWGS84.getXNorm(x);
		double yNorm = CoordWGS84.getYNorm(y);

		double latitude10000Sec =
			16.9023892 +
			3.238272 * xNorm -
			0.270978 * Math.pow(yNorm, 2) -
			0.002528 * Math.pow(xNorm, 2) -
			0.0447 * Math.pow(yNorm, 2) * xNorm -
			0.0140 * Math.pow(xNorm, 3);

		return latitude10000Sec * 100 / 36;
	}

}
