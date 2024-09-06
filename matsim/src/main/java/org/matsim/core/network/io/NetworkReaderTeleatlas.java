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

package org.matsim.core.network.io;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.gis.GeoFileReader;

/**
 * A reader for TeleAtlas network description files. The reader is based on
 * <strong>Tele Atlas MultiNet Shapefile 4.3.2.1 Format Specifications document
 * version Final v1.0, June 2007</strong> and uses the input shape files:
 * <ul>
 * <li><em>jc.shp</em>: junction shape file (typically called xyz________jc.shp)
 * </li>
 * <li><em>nw.shp</em>: junction shape file (typically called xyz________nw.shp)
 * </li>
 * </ul>
 *
 * @author balmermi
 */
public final class NetworkReaderTeleatlas implements MatsimSomeReader {

	// ////////////////////////////////////////////////////////////////////
	// member variables
	// ////////////////////////////////////////////////////////////////////

	private final static Logger log = LogManager.getLogger(NetworkReaderTeleatlas.class);

	private final Network network;

	/**
	 * path and name to the Tele Atlas MultiNet junction (jc) Shapefile
	 */
	private final String jcShpFileName; // teleatlas junction shape file name

	/**
	 * path and name to the Tele Atlas MultiNet network (nw) Shapefile
	 */
	private final String nwShpFileName; // teleatlas network shape file name

	/**
	 * option flag: if set, the reader ignores all network links with
	 * <p>
	 * <code>{@link #LINK_FRCTYP_NAME} == 8</code>
	 * </p>
	 * <p>
	 * <b>Default:</b> <code>false</code>
	 * </p>
	 */
	public boolean ignoreFrcType8 = false;

	/**
	 * option flag: if set, the reader ignores all network links with
	 * <p>
	 * <code>{@link #LINK_FRCTYP_NAME} == 8 && {@link #LINK_ONEWAY_NAME} == "N"</code>
	 * </p>
	 * <p>
	 * <b>Default:</b> <code>false</code>
	 * </p>
	 */
	public boolean ignoreFrcType7onewayN = false;

	/**
	 * option parameter: the reader redefines the number of lanes (see permlanes
	 * in {@link Link}) if the value of
	 * <code>{@link #LINK_LANES_NAME}<1</code>. In that case the link gets 2 lanes
	 * for <code>{@link #LINK_FRCTYP_NAME}</code> less or equal this parameter, 1
	 * lane otherwise.
	 * <p>
	 * <b>Default:</b> <code>3</code>
	 * </p>
	 */
	public int maxFrcTypeForDoubleLaneLink = 3;

	/**
	 * option parameter [in km/h]: Tele Atlas MultiNet Shapefile does not define
	 * capacities for links. Therefore, link flow capacities must be derived from
	 * the given data. Below the given parameter the link gets capacity = 1000
	 * [veh/h] per lane assigned, 2000 [veh/h] otherwise.
	 * <p>
	 * <b>Default:</b> <code>40</code>(km/h)
	 * </p>
	 */
	public int minSpeedForNormalCapacity = 40; // km/h

	private static final String NODE_ID_NAME = "ID";
	private static final String NODE_FEATTYP_NAME = "FEATTYP";
	private static final String NODE_JNCTTYP_NAME = "JNCTTYP";

	private static final String LINK_ID_NAME = "ID";
	private static final String LINK_FEATTYP_NAME = "FEATTYP";
	private static final String LINK_FERRYTYP_NAME = "FT";
	private static final String LINK_FJNCTID_NAME = "F_JNCTID";
	private static final String LINK_TJNCTID_NAME = "T_JNCTID";
	private static final String LINK_LENGTH_NAME = "METERS";
	private static final String LINK_FRCTYP_NAME = "FRC";
	private static final String LINK_ONEWAY_NAME = "ONEWAY";
	private static final String LINK_SPEED_NAME = "KPH";
	private static final String LINK_LANES_NAME = "LANES";

	// TODO balmermi: probably we also need to consider the following attributes
	// but for the ch-teleatlas-net version 2008 it was not necessary
	// PRIVATERD Private Road
	// 0: No Special Restriction (default)
	// 2: Not Publicly Accessible
	// CONSTATUS Construction Status
	// Blank: Not Under Construction (default)
	// FT: Under Construction in Positive Direction
	// N: Under Construction in Both Directions
	// TF: Under Construction in Negative Direction
	// F_BP From (Start) Blocked Passage
	// 0: No Blocked Passage at Start Junction (default)
	// 1: Blocked Passage at Start Junction
	// T_BP To (End) Blocked Passage
	// 0: No Blocked Passage at End Junction (default)
	// 2: Blocked Passage at End Junction

	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////

	/**
	 * Instantiate a new Tele Atlas MultiNet Shapefile reader based on the
	 * junction and the network shape file.
	 *
	 * @param network
	 *          MATSim network database in which the reader stores the data
	 * @param jcShpFileName
	 *          Tele Atlas MultiNet junction Shapefile
	 * @param nwShpFileName
	 *          Tele Atlas MultiNet network Shapefile
	 */
	public NetworkReaderTeleatlas(final Network network, final String jcShpFileName, final String nwShpFileName) {
		this.network = network;
		this.jcShpFileName = jcShpFileName;
		this.nwShpFileName = nwShpFileName;
	}

	// ////////////////////////////////////////////////////////////////////
	// read methods
	// ////////////////////////////////////////////////////////////////////

	public void read() throws IOException {
		log.info("reading nodes from Junction Shape file '" + this.jcShpFileName + "'...");
		this.readNodesFromJCshp();
		log.info("done.");
		log.info("reading links from Network Shape file '" + this.nwShpFileName + "'...");
		this.readLinksFromNWshp();
		log.info("done.");
		// TODO balmermi: adding date and check if you get more info from the input
		// files to include into the name
		network.setName("teleatlas");
	}

	// ////////////////////////////////////////////////////////////////////

	/**
	 * Reads Tele Atlas MultiNet junction Shapefile given in
	 * <code>{@link #jcShpFileName}</code>.
	 *
	 * <p>
	 * It uses the following feature attributes:
	 * <ul>
	 * <li>center coordinate</li>
	 * <li><code>{@link #NODE_ID_NAME} (Feature Identification)</code></li>
	 * <li><code>{@link #NODE_FEATTYP_NAME} (Feature Type)</code></li>
	 * <ul>
	 * <li><code>4120: Junction</code></li>
	 * <li><code>4220: Railway Element Junction</code></li>
	 * </ul>
	 * <li><code>{@link #NODE_JNCTTYP_NAME} (Junction Type)</code></li>
	 * <ul>
	 * <li><code>0: Junction (default)</code></li>
	 * <li><code>2: Bifurcation</code></li>
	 * <li><code>3: Railway Crossing</code></li>
	 * <li><code>4: Country Border Crossing</code></li>
	 * <li><code>5: Ferry Operated by Train Crossing</code></li>
	 * <li><code>6: Internal Data Set Border Crossing</code></li>
	 * </ul>
	 * </ul>
	 * </p>
	 * The MATSim {@link Node#type} is set as
	 *
	 * <pre>
	 * <code>{@link Node#type} = {@link #NODE_FEATTYP_NAME}+"-"+{@link #NODE_JNCTTYP_NAME}</code>
	 * </pre>
	 *
	 */
	private void readNodesFromJCshp() throws IOException {
		int nCnt = network.getNodes().size();
		SimpleFeatureSource fs = GeoFileReader.readDataFile(jcShpFileName);
		SimpleFeatureIterator fIt = fs.getFeatures().features();
		while (fIt.hasNext()) {
			SimpleFeature f = fIt.next();
			// get node attributes
			// Coordinate c = f.getBounds().centre();
			BoundingBox bb = f.getBounds();
			Coord c = new Coord((bb.getMinX() + bb.getMaxX()) / 2.0, (bb.getMinY() + bb.getMaxY()) / 2.0);

			Object id = f.getAttribute(NODE_ID_NAME);
			int feattyp = Integer.parseInt(f.getAttribute(NODE_FEATTYP_NAME).toString());
			if ((feattyp != 4120) && (feattyp != 4220)) {
				throw new IllegalArgumentException(NODE_ID_NAME + "=" + id + ": " + NODE_FEATTYP_NAME + "=" + feattyp + " not allowed.");
			}
			int jncttyp = Integer.parseInt(f.getAttribute(NODE_JNCTTYP_NAME).toString());
			if ((jncttyp < 0) || (jncttyp == 1) || (jncttyp > 6)) {
				throw new IllegalArgumentException(NODE_ID_NAME + "=" + id + ": " + NODE_JNCTTYP_NAME + "=" + jncttyp + " not allowed.");
			}
			if (id == null) {
				throw new IllegalArgumentException("In " + jcShpFileName + ": There is at least one feature that does not have an ID set.");
			}
			String type = feattyp + "-" + jncttyp;
			Node n = network.getFactory().createNode(Id.create(id.toString(), Node.class), c);
			final String type1 = type;
			Node r = n;
			NetworkUtils.setType(r,type1);
			network.addNode(n);
		}
		fIt.close();
		nCnt = network.getNodes().size() - nCnt;
		log.info("  " + nCnt + " nodes added to the network.");
	}

	// ////////////////////////////////////////////////////////////////////

	/**
	 * Reads Tele Atlas MultiNet network Shapefile given in
	 * <code>{@link #nwShpFileName}</code>.
	 *
	 * <p>
	 * It uses the following feature attributes:
	 * <ul>
	 * <li><code>{@link #LINK_ID_NAME} (Feature Identification)</code></li>
	 * <li><code>{@link #LINK_FEATTYP_NAME} (Feature Type)</code></li>
	 * <ul>
	 * <li><code>4110: Road Element</code></li>
	 * <li><code>4130: Ferry Connection Element</code></li>
	 * <li><code>4165: Address Area Boundary Element</code></li>
	 * </ul>
	 * <li><code>{@link #LINK_FERRYTYP_NAME} (Ferry Type)</code></li>
	 * <ul>
	 * <li><code>0: No Ferry (default)</code></li>
	 * <li><code>1: Ferry Operated by Ship or Hovercraft</code></li>
	 * <li><code>2: Ferry Operated by Train</code></li>
	 * </ul>
	 * <li>
	 * <code>{@link #LINK_FJNCTID_NAME} (From (Start) Junction Identification)</code>
	 * </li>
	 * <li>
	 * <code>{@link #LINK_TJNCTID_NAME} (To (End) Junction Identification)</code></li>
	 * <li><code>{@link #LINK_LENGTH_NAME} (Feature Length (meters))</code></li>
	 * <li><code>{@link #LINK_FRCTYP_NAME} (Functional Road Class)</code></li>
	 * <li><code>{@link #LINK_FERRYTYP_NAME} (Ferry Type)</code></li>
	 * <ul>
	 * <li><code>-1: Not Applicable (for {@link #LINK_FEATTYP_NAME} 4165)</code></li>
	 * <li><code>0: Motorway, Freeway, or Other Major Road</code></li>
	 * <li><code>1: a Major Road Less Important than a Motorway</code></li>
	 * <li><code>2: Other Major Road</code></li>
	 * <li><code>3: Secondary Road</code></li>
	 * <li><code>4: Local Connecting Road</code></li>
	 * <li><code>5: Local Road of High Importance</code></li>
	 * <li><code>6: Local Road</code></li>
	 * <li><code>7: Local Road of Minor Importance</code></li>
	 * <li><code>8: Other Road</code></li>
	 * </ul>
	 * <li><code>{@link #LINK_ONEWAY_NAME} (Direction of Traffic Flow)</code></li>
	 * <ul>
	 * <li><code>Blank: Open in Both Directions (default)</code></li>
	 * <li><code>FT: Open in Positive Direction</code></li>
	 * <li><code>N: Closed in Both Directions</code></li>
	 * <li><code>TF: Open in Negative Direction</code></li>
	 * </ul>
	 * <li>
	 * <code>{@link #LINK_SPEED_NAME} (Calculated Average Speed (kilometers per hour))</code>
	 * </li>
	 * <li><code>{@link #LINK_LANES_NAME} (Number of Lanes)</code></li>
	 * </ul>
	 *
	 * <b>Conversion rules:</b>
	 * <ul>
	 * <li>Links that refer to not existing from- or to-link will be ignored
	 * (produces a wanring message)</li>
	 * <li>Links with {@link #LINK_FEATTYP_NAME} not equal to 4110 or 4130 will be
	 * ignored</li>
	 * <li>Links with {@link #LINK_FRCTYP_NAME} less than zero will be ignored</li>
	 * <li>Links with {@link #LINK_FRCTYP_NAME} equals 8 will be ignored if
	 * {@link #ignoreFrcType8} flag is set</li>
	 * <li>Links with {@link #LINK_FRCTYP_NAME} equals 7 and
	 * {@link #LINK_ONEWAY_NAME} equals "N" will be ignored if
	 * {@link #ignoreFrcType7onewayN} is set</li>
	 * <li>Links with {@link #LINK_LANES_NAME} less than 1 will get lanes
	 * according to the rule defined by {@link #maxFrcTypeForDoubleLaneLink}</li>
	 * <li>Links get capacities according to the rule defined by
	 * {@link #minSpeedForNormalCapacity}</li>
	 * <li>Directed links in the MATSim network DB will be created according to
	 * the information in {@link #LINK_ONEWAY_NAME} (two links if " " or "N", one
	 * link for "TF" or "FT"). The link id is defined as
	 * <code>{@link #LINK_ID_NAME}+"TF"</code>,
	 * <code>{@link #LINK_ID_NAME}+"FT"</code> resp.</li>
	 * <li>The {@link Link#type} is set as:
	 *
	 * <pre>
	 * <code>{@link Link#type} = {@link #LINK_FRCTYP_NAME}+"-"+{@link #LINK_FEATTYP_NAME}+"-"+{@link #LINK_FERRYTYP_NAME}</code>
	 * </pre>
	 *
	 * </li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException
	 */
	private void readLinksFromNWshp() throws IOException {
		int lCnt = network.getLinks().size();
		int ignoreCnt = 0;
		SimpleFeatureSource fs = GeoFileReader.readDataFile(this.nwShpFileName);
		SimpleFeatureIterator fIt = fs.getFeatures().features();
		while (fIt.hasNext()) {
			SimpleFeature f = fIt.next();
			boolean ignore = false;
			// get link attributes
			Object id = f.getAttribute(LINK_ID_NAME);
			int featTyp = Integer.parseInt(f.getAttribute(LINK_FEATTYP_NAME).toString());
			if ((featTyp != 4110) && (featTyp != 4130) && (featTyp != 4165)) {
				throw new IllegalArgumentException(LINK_ID_NAME + "=" + id + ": " + LINK_FEATTYP_NAME + "=" + featTyp + " not allowed.");
			}
			int ferryType = Integer.parseInt(f.getAttribute(LINK_FERRYTYP_NAME).toString());
			if ((ferryType < 0) || (ferryType > 2)) {
				throw new IllegalArgumentException(LINK_ID_NAME + "=" + id + ": " + LINK_FERRYTYP_NAME + "=" + ferryType + " not allowed.");
			}
			Id<Node> fromJunctionId = Id.create(f.getAttribute(LINK_FJNCTID_NAME).toString(), Node.class);
			Id<Node> toJunctionId = Id.create(f.getAttribute(LINK_TJNCTID_NAME).toString(), Node.class);
			double length = Double.parseDouble(f.getAttribute(LINK_LENGTH_NAME).toString());
			int linksType = Integer.parseInt(f.getAttribute(LINK_FRCTYP_NAME).toString());
			if ((linksType < -1) || (linksType > 8)) {
				throw new IllegalArgumentException(LINK_ID_NAME + "=" + id + ": " + LINK_FRCTYP_NAME + "=" + linksType + " not allowed.");
			}
			String oneway = f.getAttribute(LINK_ONEWAY_NAME).toString();
			if (!oneway.equals(" ") && !oneway.equals("FT") && !oneway.equals("TF") && !oneway.equals("N")) {
				throw new IllegalArgumentException(LINK_ID_NAME + "=" + id + ": " + LINK_ONEWAY_NAME + "=" + oneway + " not allowed.");
			}
			double speed = Double.parseDouble(f.getAttribute(LINK_SPEED_NAME).toString());
			double lanes = Double.parseDouble(f.getAttribute(LINK_LANES_NAME).toString());
			// ignore link where from node or to node is missing
			Node fNode = network.getNodes().get(fromJunctionId);
			Node tNode = network.getNodes().get(toJunctionId);
			if ((fNode == null) || (tNode == null)) {
				log.warn("  linkId=" + id.toString()
						+ ": at least one of the two junctions do not exist. Ignoring and proceeding anyway...");
				ignore = true;
			}
			// ignore link that is not a 'Road Element' (4110) or a 'Ferry Connection Element' (4130)
			// There are 'Address Area Boundary Element' (4165) links that will be ignored
			if ((featTyp != 4110) && (featTyp != 4130)) {
				log.trace("  linkId=" + id.toString() + ": ignoring " + LINK_FEATTYP_NAME + "=" + featTyp + ".");
				ignore = true;
			}
			// ignore links FRC types = -1 [Not Applicable (for FeatTyp 4165)]
			if (linksType < 0) {
				log.trace("  linkId=" + id.toString() + ": ignoring " + LINK_FRCTYP_NAME + "=" + linksType + ".");
				ignore = true;
			}
			// option flag: ignore links FRC type = 8 [Other Road]
			if (this.ignoreFrcType8 && (7 < linksType)) {
				log.trace("  linkId=" + id.toString() + ": ignoring " + LINK_FRCTYP_NAME + "=" + linksType + ".");
				ignore = true;
			}
			// ignore links FRC types = 7 [Local Road of Minor Importance] that are ONEWAY = N [Closed in Both Directions]
			// links with type < 7 also contains ONEWAY = N but should be open anyway....
			if (this.ignoreFrcType7onewayN && ((linksType == 7) && oneway.equals("N"))) {
				log.trace("  linkId=" + id.toString() + ": ignoring " + LINK_FRCTYP_NAME + "=" + linksType + " with " + LINK_ONEWAY_NAME
						+ "=" + oneway);
				ignore = true;
			}

			// simple rule for number of lanes (a lot of them are = 0, so we need to define something)
			if (lanes < 1) {
				if (linksType <= this.maxFrcTypeForDoubleLaneLink) {
					lanes = 2;
				} else {
					lanes = 1;
				}
			}
			// simple rule for setting capacities
			double cap;
			if (speed < minSpeedForNormalCapacity) {
				cap = lanes * 1000;
			} else {
				cap = lanes * 2000;
			}

			if (ignore) {
				ignoreCnt++;
			} else {
				if (oneway.equals(" ") || oneway.equals("N")) {
					Link l = network.getFactory().createLink(Id.create(id.toString() + "FT", Link.class), fNode, tNode);
					l.setLength(length);
					l.setFreespeed(speed / 3.6);
					l.setCapacity(cap);
					l.setNumberOfLanes(lanes);
					NetworkUtils.setOrigId(l, id.toString() ) ;
					NetworkUtils.setType(l, linksType + "-" + featTyp + "-" + ferryType);
					l = network.getFactory().createLink(Id.create(id.toString() + "TF", Link.class), tNode, fNode);
					l.setLength(length);
					l.setFreespeed(speed / 3.6);
					l.setCapacity(cap);
					l.setNumberOfLanes(lanes);
					NetworkUtils.setOrigId(l, id.toString() ) ;
					NetworkUtils.setType(l, linksType + "-" + featTyp + "-" + ferryType);
				} else if (oneway.equals("FT")) {
					Link l = network.getFactory().createLink(Id.create(id.toString() + oneway, Link.class), fNode, tNode);
					l.setLength(length);
					l.setFreespeed(speed / 3.6);
					l.setCapacity(cap);
					l.setNumberOfLanes(lanes);
					NetworkUtils.setOrigId(l, id.toString() ) ;
					NetworkUtils.setType(l, linksType + "-" + featTyp + "-" + ferryType);
				} else if (oneway.equals("TF")) {
					Link l = network.getFactory().createLink(Id.create(id.toString() + oneway, Link.class), tNode, fNode);
					l.setLength(length);
					l.setFreespeed(speed / 3.6);
					l.setCapacity(cap);
					l.setNumberOfLanes(lanes);
					NetworkUtils.setOrigId(l, id.toString() ) ;
					NetworkUtils.setType(l, linksType + "-" + featTyp + "-" + ferryType);
				} else {
					throw new IllegalArgumentException("linkId=" + id.toString() + ": " + LINK_ONEWAY_NAME + "=" + oneway + " not known!");
				}
			}
		}
		fIt.close();

		network.setCapacityPeriod(3600.0);

		lCnt = network.getLinks().size() - lCnt;
		log.info("  " + lCnt + " links added to the network layer.");
		log.info("  " + ignoreCnt + " links ignored from the input shape file.");
	}

	// ////////////////////////////////////////////////////////////////////
	// print methods
	// ////////////////////////////////////////////////////////////////////

	/**
	 * prints the variable settings to the STDOUT
	 *
	 * @param prefix
	 *          a prefix for each line of the STDOUT
	 */
	public final void printInfo(final String prefix) {
		System.out.println(prefix + "configuration of " + this.getClass().getName() + ":");
		System.out.println(prefix + "  MATSim network:");
		System.out.println(prefix + "    ignoreFrcType8:              " + ignoreFrcType8);
		System.out.println(prefix + "    ignoreFrcType7onewayN:       " + ignoreFrcType7onewayN);
		System.out.println(prefix + "    maxFrcTypeForDoubleLaneLink: " + maxFrcTypeForDoubleLaneLink);
		System.out.println(prefix + "    minSpeedForNormalCapacity:   " + minSpeedForNormalCapacity);
		System.out.println(prefix + "  junction shape:");
		System.out.println(prefix + "    jcShpFileName:      " + jcShpFileName);
		System.out.println(prefix + "    NODE_ID_NAME:       " + NODE_ID_NAME);
		System.out.println(prefix + "    NODE_FEATTYP_NAME:  " + NODE_FEATTYP_NAME);
		System.out.println(prefix + "    NODE_JNCTTYP_NAME:  " + NODE_JNCTTYP_NAME);
		System.out.println(prefix + "  network shape:");
		System.out.println(prefix + "    nwShpFileName:      " + nwShpFileName);
		System.out.println(prefix + "    LINK_ID_NAME:       " + LINK_ID_NAME);
		System.out.println(prefix + "    LINK_FEATTYP_NAME:  " + LINK_FEATTYP_NAME);
		System.out.println(prefix + "    LINK_FERRYTYP_NAME: " + LINK_FERRYTYP_NAME);
		System.out.println(prefix + "    LINK_FJNCTID_NAME:  " + LINK_FJNCTID_NAME);
		System.out.println(prefix + "    LINK_TJNCTID_NAME:  " + LINK_TJNCTID_NAME);
		System.out.println(prefix + "    LINK_LENGTH_NAME:   " + LINK_LENGTH_NAME);
		System.out.println(prefix + "    LINK_FRCTYP_NAME:   " + LINK_FRCTYP_NAME);
		System.out.println(prefix + "    LINK_ONEWAY_NAME:   " + LINK_ONEWAY_NAME);
		System.out.println(prefix + "    LINK_SPEED_NAME:    " + LINK_SPEED_NAME);
		System.out.println(prefix + "    LINK_LANES_NAME:    " + LINK_LANES_NAME);
		System.out.println(prefix + "done.");
	}
}
