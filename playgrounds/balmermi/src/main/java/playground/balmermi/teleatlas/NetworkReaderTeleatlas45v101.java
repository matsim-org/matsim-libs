/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkReaderTeleatlas.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.balmermi.teleatlas;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.balmermi.teleatlas.JcElement.JcFeatureType;
import playground.balmermi.teleatlas.JcElement.JunctionType;
import playground.balmermi.teleatlas.NwElement.FerryType;
import playground.balmermi.teleatlas.NwElement.FormOfWay;
import playground.balmermi.teleatlas.NwElement.Freeway;
import playground.balmermi.teleatlas.NwElement.FunctionalRoadClass;
import playground.balmermi.teleatlas.NwElement.NetTwoClass;
import playground.balmermi.teleatlas.NwElement.NwFeatureType;
import playground.balmermi.teleatlas.NwElement.OneWay;
import playground.balmermi.teleatlas.NwElement.PrivateRoad;
import playground.balmermi.teleatlas.NwElement.Ramp;
import playground.balmermi.teleatlas.NwElement.SlipRoad;
import playground.balmermi.teleatlas.NwElement.SpeedCategory;

/**
 * <p>
 * TeleAtlas nw, jc and rr shape file reader.
 * The reader is based on
 * </p>
 * <p>
 * <strong>Tele Atlas MultiNet Shapefile 4.5 Format Specifications Version Final v1.0.1, June 2009</strong>
 * </p>
 * <p>
 * Uses the following shape files:
 * <ul>
 * <li><strong>nw.shp</strong>: junction shape file</li>
 * <li><strong>jc.shp</strong>: junction shape file</li>
 * <li><strong>rr.shp</strong>: railways shape file</li>
 * </ul>
 * </p>
 *
 * @author balmermi
 * @author mrieser
 */
public class NetworkReaderTeleatlas45v101 {

	//////////////////////////////////////////////////////////////////////
	// variables / constants
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(NetworkReaderTeleatlas45v101.class);

	private final TeleatlasData data;

	/**
	 * Feature Identification attribute of junction shape file
	 */
	private static final String JC_ID_NAME = "ID";

	/**
	 * Feature Type attribute of junction shape file
	 * <ul>
	 * <li><code>4120</code>: Junction</li>
	 * <li><code>4220</code>: Railway Element Junction</li>
	 * </ul>
	 */
	private static final String JC_FEATTYP_NAME = "FEATTYP";

	/**
	 * Junction Type attribute of junction shape file
	 * <ul>
	 * <li><code>0</code>: Junction (default)</li>
	 * <li><code>2</code>: Bifurcation</li>
	 * <li><code>3</code>: Railway Crossing</li>
	 * <li><code>4</code>: Country Border Crossing</li>
	 * <li><code>5</code>: Ferry Operated by Train Crossing</li>
	 * <li><code>6</code>: Internal Data Set Border Crossing</li>
	 */
	private static final String JC_JNCTTYP_NAME = "JNCTTYP";

	/**
	 * Feature Identification attribute of network shape file
	 */
	private static final String NW_ID_NAME = "ID";

	/**
	 * Feature Type attribute of network shape file
	 * <ul>
	 * <li><code>4110</code>: Road Element</li>
	 * <li><code>4130</code>: Ferry Connection Element</li>
	 * <li><code>4165</code>: Address Area Boundary Element</li>
	 * </ul>
	 */
	private static final String NW_FEATTYP_NAME = "FEATTYP";

	/**
	 * Ferry Type attribute of network shape file
	 * <ul>
	 * <li><code>0</code>: No Ferry (default)</li>
	 * <li><code>1</code>: Ferry Operated by Ship or Hovercraft</li>
	 * <li><code>2</code>: AFerry Operated by Train</li>
	 * </ul>
	 */
	private static final String NW_FT_NAME = "FT";

	/**
	 * From (Start) Junction Identification attribute of network shape file
	 */
	private static final String NW_FJNCTID_NAME = "F_JNCTID";

	/**
	 * To (End) Junction Identification attribute of network shape file
	 */
	private static final String NW_TJNCTID_NAME = "T_JNCTID";

	/**
	 * Feature Length (meters) attribute of network shape file
	 */
	private static final String NW_METERS_NAME = "METERS";

	/**
	 * Functional Road Class attribute of network shape file
	 * <ul>
	 * <li><code>-1</code>: Not Applicable (for {@link #NW_FEATTYP_NAME} 4165)</li>
	 * <li><code>0</code>: Motorway, Freeway, or Other Major Road</li>
	 * <li><code>1</code>: a Major Road Less Important than a Motorway</li>
	 * <li><code>2</code>: Other Major Road</li>
	 * <li><code>3</code>: Secondary Road</li>
	 * <li><code>4</code>: Local Connecting Road</li>
	 * <li><code>5</code>: Local Road of High Importance</li>
	 * <li><code>6</code>: Local Road</li>
	 * <li><code>7</code>: Local Road of Minor Importance</li>
	 * <li><code>8</code>: Other Road</li>
	 * </ul>
	 */
	private static final String NW_FRC_NAME = "FRC";

	/**
	 * Net 2 Class attribute of network shape file
	 * <ul>
	 * <li><code>-1</code>: Not Applicable (default)</li>
	 * <li><code>0</code>: Main Axes - Motorway and Freeway</li>
	 * <li><code>1</code>: Main Axes - Other Major Road </li>
	 * <li><code>2</code>: important Connection Road between urban areas</li>
	 * <li><code>3</code>: less important Connection Road between urban areas</li>
	 * <li><code>4</code>: Side Road, Neighborhood Road, small landside Road</li>
	 * <li><code>5</code>: Parking Street, Access Road, etc.</li>
	 * <li><code>6</code>: Pedestrian Road, Restricted Road</li>
	 * </ul>
	 * <p>
	 * <b>Note:</b> the given definition is not part of the Tele Atlas
	 * MultiNet Shapefile 4.5 Format Specifications. It is an interpretation
	 * of the use of these values on the Swiss Network (Tele Atlas Abbr. <i>che</i>)
	 * </p>
	 */
	private static final String NW_NET2CLASS_NAME = "NET2CLASS";

	/**
	 * Form of Way attribute of network shape file
	 * <ul>
	 * <li><code>-1</code>: Not Applicable</li>
	 * <li><code>1</code>: Part of Motorway</li>
	 * <li><code>2</code>: Part of Multi Carriageway which is not a Motorway</li>
	 * <li><code>3</code>: Part of a Single Carriageway (default)</li>
	 * <li><code>4</code>: Part of a Roundabout</li>
	 * <li><code>6</code>: Part of an ETA: Parking Place</li>
	 * <li><code>7</code>: Part of an ETA: Parking Garage (Building)</li>
	 * <li><code>8</code>: Part of an ETA: Unstructured Traffic square</li>
	 * <li><code>10</code>: Part of a Slip Road</li>
	 * <li><code>11</code>: Part of a Service Road</li>
	 * <li><code>12</code>: Entrance / Exit to / from a Car Park</li>
	 * <li><code>14</code>: Part of a Pedestrian Zone</li>
	 * <li><code>15</code>: Part of a Walkway</li>
	 * <li><code>17</code>: Special Traffic Figures</li>
	 * <li><code>20</code>: Road for Authorities</li>
	 * <li><code>21</code>: Connector</li>
	 * <li><code>22</code>: Cul-de-Sac</li>
	 * </ul>
	 */
	private static final String NW_FOW_NAME = "FOW";

	/**
	 * Freeway attribute of network shape file
	 * <ul>
	 * <li><code>0</code>: No Part of Freeway (default)</li>
	 * <li><code>1</code>: Part of Freeway</li>
	 * </ul>
	 */
	private static final String NW_FREEWAY_NAME = "FREEWAY";

	/**
	 * Private Road attribute of network shape file
	 * <ul>
	 * <li><code>0</code>: No Special Restrictions (default)</li>
	 * <li><code>2</code>: Generic Restriction</li>
	 * <li><code>4</code>: Residents Only</li>
	 * <li><code>8</code>: Employees Only</li>
	 * <li><code>16</code>: Authorized Personal Only</li>
	 * <li><code>32</code>: Members Only</li>
	 * </ul>
	 * <p>
	 * Bitmask: multiple restrictions at the same time are possible
	 * </p>
	 */
	private static final String NW_PRIVATERD_NAME = "PRIVATERD";

	/**
	 * Direction of Traffic Flow attribute of network shape file
	 * <ul>
	 * <li><code>[Blank]</code>: Open in both Direction (default)</li>
	 * <li><code>FT</code>: Open in positive Direction</li>
	 * <li><code>N</code>: Closed in both Direction</li>
	 * <li><code>TF</code>: Open in negative Direction</li>
	 * </ul>
	 */
	private static final String NW_ONEWAY_NAME = "ONEWAY";

	/**
	 * Number of Lanes attribute of network shape file
	 */
	private static final String NW_LANES_NAME = "LANES";

	/**
	 * Speed Category attribute of network shape file
	 * <ul>
	 * <li><code>1</code>: > 130 km/h</li>
	 * <li><code>2</code>: 101 - 130 km/h</li>
	 * <li><code>3</code>: 91 - 100 km/h</li>
	 * <li><code>4</code>: 71 - 90 km/h</li>
	 * <li><code>5</code>: 51 - 70 km/h</li>
	 * <li><code>6</code>: 31 - 50 km/h</li>
	 * <li><code>7</code>: 11 - 30 km/h</li>
	 * <li><code>8</code>: < 11 km/h</li>
	 * </ul>
	 */
	private static final String NW_SPEEDCAT_NAME = "SPEEDCAT";

	/**
	 * Ramp Category attribute of network shape file
	 * <ul>
	 * <li><code>0</code>: No Exit/Entrance Ramp - Default</li>
	 * <li><code>1</code>: Exit</li>
	 * <li><code>2</code>: Entrance</li>
	 * </ul>
	 */
	private static final String RAMP_NAME = "RAMP";

	/**
	 * Slip Road Category attribute of network shape file
	 * <ul>
	 * <li><code>0</code>: No Slip Road (default)</li>
	 * <li><code>1</code>: Parallel Road</li>
	 * <li><code>2</code>: Slip Road of a Grade Separated Crossing</li>
	 * <li><code>3</code>: Slip Road of a Crossing at Grade</li>
	 * <li><code>18</code>: Major / Minor Slip Road</li>
	 * </ul>
	 */
	private static final String SLIPRD_NAME = "SLIPRD";

//	private static final String RR_ID_NAME = "ID";
//	private static final String RR_FJNCTID_NAME = "F_JNCTID";
//	private static final String RR_TJNCTID_NAME = "T_JNCTID";
//	private static final String RR_METERS_NAME = "METERS";

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public NetworkReaderTeleatlas45v101(final TeleatlasData data) {
		this.data = data;
	}

	/**
	 * Extracts the Junction id from the given {@link Feature} object.
	 *
	 * @param f is a feature of the Tele Atlas MultiNet junction shape file (jc)
	 * @return junction id as a Long
	 * @throws IllegalArgumentException if no id is defined.
	 */
	private static final Long extractJcFeatureIdentification(final Feature f) {
		Object id = f.getAttribute(JC_ID_NAME);
		if (id == null) { throw new IllegalArgumentException("At feature id '"+f.getID()+"': No "+JC_ID_NAME+" given."); }
		return new Long(id.toString());
	}

	/**
	 * Extracts the Junction feature type from the given {@link Feature} object.
	 *
	 * @param f is a feature of the Tele Atlas MultiNet junction shape file (jc)
	 * @return feature type
	 * @throws IllegalArgumentException if the value is unknown (see {@link #JC_FEATTYP_NAME})
	 */
	private static final JcFeatureType extractJcFeatureType(final Feature f) {
		int feattyp = Integer.parseInt(f.getAttribute(JC_FEATTYP_NAME).toString());
		switch (feattyp) {
			case 4120: return JcElement.JcFeatureType.JUNCTION;
			case 4220: return JcElement.JcFeatureType.RAILWAY;
			default: throw new IllegalArgumentException("At feature id '"+f.getID()+"': "+JC_FEATTYP_NAME+"="+feattyp+" not allowed.");
		}
	}

	/**
	 * Extracts the Junction junction type from the given {@link Feature} object.
	 *
	 * @param f is a feature of the Tele Atlas MultiNet junction shape file (jc)
	 * @return junction type
	 */
	private static final JunctionType extractJcJunctionType(final Feature f) {
		int jncttyp = Integer.parseInt(f.getAttribute(JC_JNCTTYP_NAME).toString());
		switch (jncttyp) {
			case 0: return JcElement.JunctionType.JUNCTION;
			case 2: return JcElement.JunctionType.BIFURCATION;
			case 3: return JcElement.JunctionType.RAILWAY_CROSSING;
			case 4: return JcElement.JunctionType.COUNTRY_BORDER_CROSSING;
			case 5: return JcElement.JunctionType.TRAIN_FERRY_CROSSING;
			case 6: return JcElement.JunctionType.INTERNAL_DATASET_BORDER_CROSSING;
			default: return JcElement.JunctionType.JUNCTION;
		}
	}

	/**
	 * Extracts the Network id from the given {@link Feature} object.
	 *
	 * @param f is a feature of the Tele Atlas MultiNet network shape file (nw)
	 * @return network id as a Long
	 * @throws IllegalArgumentException if no id is defined.
	 */
	private static final Long extractNwFeatureIdentification(final Feature f) {
		Object id = f.getAttribute(NW_ID_NAME);
		if (id == null) { throw new IllegalArgumentException("At feature id '"+f.getID()+"': No "+NW_ID_NAME+" given."); }
		return new Long(id.toString());
	}

	/**
	 * Extracts the Network feature type from the given {@link Feature} object.
	 *
	 * @param f is a feature of the Tele Atlas MultiNet network shape file (nw)
	 * @return feature type
	 * @throws IllegalArgumentException if the value is unknown (see {@link #NW_FEATTYP_NAME})
	 */
	private static final NwFeatureType extractNwFeatureType(final Feature f) {
		int feattyp = Integer.parseInt(f.getAttribute(NW_FEATTYP_NAME).toString());
		switch (feattyp) {
			case 4110: return NwElement.NwFeatureType.ROAD_ELEMENT;
			case 4130: return NwElement.NwFeatureType.FERRY_CONNECTION;
			case 4165: return NwElement.NwFeatureType.ADDRESS_AREA_BOUNDARY;
			default: throw new IllegalArgumentException("At feature id '"+f.getID()+"': "+NW_FEATTYP_NAME+"="+feattyp+" not allowed.");
		}
	}

	/**
	 * Extracts the Network ferry type from the given {@link Feature} object.
	 *
	 * @param f is a feature of the Tele Atlas MultiNet network shape file (nw)
	 * @return ferry type
	 */
	private static final FerryType extractNwFerryType(final Feature f) {
		int ferrytype = Integer.parseInt(f.getAttribute(NW_FT_NAME).toString());
		switch (ferrytype) {
			case 0: return NwElement.FerryType.NO_FERRY;
			case 1: return NwElement.FerryType.SHIP_HOVERCRAFT;
			case 2: return NwElement.FerryType.TRAIN;
			default: return NwElement.FerryType.NO_FERRY;
		}
	}

	/**
	 * Extracts the Network from (start) junction identification from the given {@link Feature} object.
	 *
	 * @param f is a feature of the Tele Atlas MultiNet network shape file (nw)
	 * @return from junction as an {@link JcElement}
	 * @throws IllegalArgumentException if no id is defined or junction is not part of the junction map.
	 */
	private final JcElement extractNwFromJunction(final Feature f) {
		Object fromId = f.getAttribute(NW_FJNCTID_NAME);
		if (fromId == null) { throw new IllegalArgumentException("At feature id '"+f.getID()+"': No "+NW_FJNCTID_NAME+" given."); }
		Long id = new Long(fromId.toString());
		JcElement j = this.data.junctionElements.get(id);
		if (j == null) { throw new IllegalArgumentException("At feature id '"+f.getID()+"' and from junction id '"+id+"': no entry found in junctions."); }
		return j;
	}

	/**
	 * Extracts the Network to (end) junction identification from the given {@link Feature} object.
	 *
	 * @param f is a feature of the Tele Atlas MultiNet network shape file (nw)
	 * @return to junction as an {@link JcElement}
	 * @throws IllegalArgumentException if no id is defined or junction is not part of the junction map.
	 */
	private final JcElement extractNwToJunction(final Feature f) {
		Object toId = f.getAttribute(NW_TJNCTID_NAME);
		if (toId == null) { throw new IllegalArgumentException("At feature id '"+f.getID()+"': No "+NW_TJNCTID_NAME+" given."); }
		Long id = new Long(toId.toString());
		JcElement j = this.data.junctionElements.get(id);
		if (j == null) { throw new IllegalArgumentException("At feature id '"+f.getID()+"' and to junction id '"+id+"': no entry found in junctions."); }
		return j;
	}

	/**
	 * Extracts the Network length from the given {@link Feature} object.
	 *
	 * @param f is a feature of the Tele Atlas MultiNet network shape file (nw)
	 * @return length id as a Double
	 * @throws IllegalArgumentException if length is <= 0.0.
	 */
	private static final Double extractNwFeatureLength(final Feature f) {
		Double length = Double.parseDouble(f.getAttribute(NW_METERS_NAME).toString());
		if (length <= 0.0) { throw new IllegalArgumentException("At feature id '"+f.getID()+"': "+NW_METERS_NAME+" = "+length+" not allowed."); }
		return length;
	}

	/**
	 * Extracts the Network functional road class from the given {@link Feature} object.
	 *
	 * @param f is a feature of the Tele Atlas MultiNet network shape file (nw)
	 * @return functional road class
	 * @throws IllegalArgumentException if the value is unknown (see {@link #NW_FRC_NAME})
	 */
	private static final FunctionalRoadClass extractNwFRC(final Feature f) {
		int frc = Integer.parseInt(f.getAttribute(NW_FRC_NAME).toString());
		switch (frc) {
			case -1: return NwElement.FunctionalRoadClass.UNDEFINED;
			case 0: return NwElement.FunctionalRoadClass.MOTORWAY;
			case 1: return NwElement.FunctionalRoadClass.MAJOR_ROAD_HIGH;
			case 2: return NwElement.FunctionalRoadClass.MAJOR_ROAD_LOW;
			case 3: return NwElement.FunctionalRoadClass.SECONDARY_ROAD;
			case 4: return NwElement.FunctionalRoadClass.LOCAL_CONNECTING_ROAD;
			case 5: return NwElement.FunctionalRoadClass.LOCAL_ROAD_HIGH;
			case 6: return NwElement.FunctionalRoadClass.LOCAL_ROAD_MEDIUM;
			case 7: return NwElement.FunctionalRoadClass.LOCAL_ROAD_LOW;
			case 8: return NwElement.FunctionalRoadClass.OTHER_ROAD;
			default: throw new IllegalArgumentException("At feature id '"+f.getID()+"': "+NW_FRC_NAME+"="+frc+" not allowed.");
		}
	}

	/**
	 * Extracts the Network net 2 class from the given {@link Feature} object.
	 *
	 * @param f is a feature of the Tele Atlas MultiNet network shape file (nw)
	 * @return net 2 class
	 */
	private static final NetTwoClass extractNwNet2Class(final Feature f) {
		int n2c = Integer.parseInt(f.getAttribute(NW_NET2CLASS_NAME).toString());
		switch (n2c) {
			case -1: return NwElement.NetTwoClass.UNDEFINED;
			case 0: return NwElement.NetTwoClass.MOTORWAY;
			case 1: return NwElement.NetTwoClass.MAIN_AXIS;
			case 2: return NwElement.NetTwoClass.CONNECTION_AXIS_HIGH;
			case 3: return NwElement.NetTwoClass.CONNECTION_AXIS_LOW;
			case 4: return NwElement.NetTwoClass.COUNTRYSIDE_LOCAL_ROAD_LOW;
			case 5: return NwElement.NetTwoClass.PARKING_ACCESS_ROAD;
			case 6: return NwElement.NetTwoClass.RESTRICTED_PEDESTRIAN;
			default: return NwElement.NetTwoClass.UNDEFINED;
		}
	}

	private static final OneWay extractOneWay(final Feature f) {
		String oneway = f.getAttribute(NW_ONEWAY_NAME).toString();
		if ("FT".equals(oneway)) {
			return OneWay.OPEN_FT;
		} else if ("TF".equals(oneway)) {
			return OneWay.OPEN_TF;
		} else if ("N".equals(oneway)) {
			return OneWay.CLOSED;
		} else return OneWay.OPEN;
	}

	private static final SpeedCategory extractSpeedCategory(final Feature f) {
		int cat = Integer.parseInt(f.getAttribute(NW_SPEEDCAT_NAME).toString());
		switch (cat) {
			case 1:
				return SpeedCategory.GT130;
			case 2:
				return SpeedCategory.R101_130;
			case 3:
				return SpeedCategory.R91_100;
			case 4:
				return SpeedCategory.R71_90;
			case 5:
				return SpeedCategory.R51_70;
			case 6:
				return SpeedCategory.R31_50;
			case 7:
				return SpeedCategory.R11_30;
			case 8:
				return SpeedCategory.LT11;
			default:
				return SpeedCategory.OTHER;
		}
	}

	private static final Integer extractNumberOfLanes(final Feature f) {
		return Integer.valueOf(f.getAttribute(NW_LANES_NAME).toString());
	}

	private static final Freeway extractFreeway(final Feature f) {
		int freeway = Integer.parseInt(f.getAttribute(NW_FREEWAY_NAME).toString());
		switch (freeway) {
			case 0:
				return Freeway.NO_FREEWAY;
			case 1:
				return Freeway.FREEWAY;
			default:
				throw new IllegalArgumentException("At feature id '"+f.getID()+"': "+NW_FREEWAY_NAME+"="+freeway+" not allowed.");
		}
	}

	private static final FormOfWay extractFormOfWay(final Feature f) {
		int fow = Integer.parseInt(f.getAttribute(NW_FOW_NAME).toString());
		switch (fow) {
			case -1:
				return FormOfWay.UNDEFINED;
			case 1:
				return FormOfWay.MOTORWAY;
			case 2:
				return FormOfWay.MULTI_CARRIAGEWAY;
			case 3:
				return FormOfWay.SINGLE_CARRIAGEWAY;
			case 4:
				return FormOfWay.ROUNDABOUT;
			case 6:
				return FormOfWay.ETA_PARKING_PLACE;
			case 7:
				return FormOfWay.ETA_PARKING_GARAGE;
			case 8:
				return FormOfWay.ETA_UNSTRUCT_TRAFFIC_SQUARE;
			case 10:
				return FormOfWay.SLIP_ROAD;
			case 11:
				return FormOfWay.SERVICE_ROAD;
			case 12:
				return FormOfWay.ENTRANCE_EXIT_CARPARK;
			case 14:
				return FormOfWay.PEDESTRIAN_ZONE;
			case 15:
				return FormOfWay.WALKWAY;
			case 17:
				return FormOfWay.SPECIAL_TRAFFIC_FIGURES;
			case 20:
				return FormOfWay.AUTHORITIES;
			case 21:
				return FormOfWay.CONNECTOR;
			case 22:
				return FormOfWay.CUL_DE_SAC;
			default:
				throw new IllegalArgumentException("At feature id '"+f.getID()+"': "+NW_FOW_NAME+"="+fow+" not allowed.");
		}
	}

	private final static PrivateRoad extractPrivateRoad(final Feature f) {
		int pr = Integer.parseInt(f.getAttribute(NW_PRIVATERD_NAME).toString());
		if (pr == 0) {
			return PrivateRoad.NO_SPECIAL_RESTRICTION;
		}
		return PrivateRoad.SPECIAL_RESTRICTION;
	}
	
	private final static Ramp extractRamp(final Feature f) {
		int r = Integer.parseInt(f.getAttribute(RAMP_NAME).toString());
		switch (r) {
		case 0:
			return Ramp.NO_RAMP;
		case 1:
			return Ramp.EXIT;
		case 2:
			return Ramp.ENTRANCE;
		default:
			throw new IllegalArgumentException("At feature id '"+f.getID()+"': "+RAMP_NAME+"="+r+" not allowed.");
		}
	}

	private final static SlipRoad extractSlipRoad(final Feature f) {
		int sr = Integer.parseInt(f.getAttribute(SLIPRD_NAME).toString());
		switch (sr) {
		case 0:
			return SlipRoad.NO_SLIPROAD;
		case 1:
			return SlipRoad.PARALLEL_ROAD;
		case 2:
			return SlipRoad.SLIP_ROAD_OF_GRADE_SEP_CROSSING;
		case 3:
			return SlipRoad.SLIP_ROAD_OF_CROSSING_AT_GRADE;
		case 18:
			return SlipRoad.MAJOR_MINOR_SLIPROAD;
		default:
			throw new IllegalArgumentException("At feature id '"+f.getID()+"': "+SLIPRD_NAME+"="+sr+" not allowed.");
		}
	}

//////////////////////////////////////////////////////////////////////
// parse methods
//////////////////////////////////////////////////////////////////////

/**
 * parsing and extracting junction features
 * @param jcShpFileName defines the path and name of the Tele Atlas MultiNet junction shape file (jc)
 * @throws IOException
 * @throws RuntimeException if the junction shape file does not respect uniqueness of junction feature identification attribute
 */
public final void parseJc(final String jcShpFileName) throws IOException, RuntimeException {
	log.info("creating data structure from Tele Atlas MultiNet junction shape file '"+jcShpFileName+"'...");
	Map<Long,JcElement> map = this.data.junctionElements;

	FeatureSource fs = ShapeFileReader.readDataFile(jcShpFileName);
	for (Object o : fs.getFeatures()) {
		Feature f = (Feature)o;

		JcElement e = new JcElement();
		e.c = f.getBounds().centre();
		e.id = extractJcFeatureIdentification(f);
		e.featType = extractJcFeatureType(f);
		e.juntype = extractJcJunctionType(f);

		if (map.containsKey(e.id)) { throw new RuntimeException("id="+e.id+" already exists."); }
		map.put(e.id,e);
	}
	log.info("=> "+map.size()+" elements parsed.");
	log.info("done.");
}

/**
 * parsing and extracting network features, requires junctions to be loaded already
 * @param nwShpFileName defines the path and name of the Tele Atlas MultiNet network shape file (nw)
 * @throws IOException
 * @throws RuntimeException if the network shape file does not respect
 * uniqueness of network feature identification attribute or if from- or to-junction is not found
 */
public final void parseNw(final String nwShpFileName) throws IOException, RuntimeException {
	log.info("creating data structure from Tele Atlas MultiNet network shape file '"+nwShpFileName+"'...");
	if (this.data.junctionElements == null) {
		throw new IllegalArgumentException("No junction map given.");
	}

	Map<Long,NwElement> map = this.data.networkElements;

	FeatureSource fs = ShapeFileReader.readDataFile(nwShpFileName);
	for (Object o : fs.getFeatures()) {
		Feature f = (Feature)o;

		NwElement e = new NwElement();
		e.id = extractNwFeatureIdentification(f);
		e.featType = extractNwFeatureType(f);
		e.ferryType = extractNwFerryType(f);
		e.fromJunction = extractNwFromJunction(f);
		e.toJunction = extractNwToJunction(f);
		e.length = extractNwFeatureLength(f);
		e.frc = extractNwFRC(f);
		e.net2Class = extractNwNet2Class(f);
		e.oneway = extractOneWay(f);
		e.speedCat = extractSpeedCategory(f);
		e.freeway = extractFreeway(f);
		e.nOfLanes = extractNumberOfLanes(f);
		e.fow = extractFormOfWay(f);
		e.privat = extractPrivateRoad(f);
		e.ramp = extractRamp(f);
		e.slipRoad = extractSlipRoad(f);

		if (map.containsKey(e.id)) { throw new RuntimeException("id="+e.id+" already exists."); }
		map.put(e.id,e);
	}
	log.info("=> "+map.size()+" elements parsed.");
	log.info("done.");
}
}
