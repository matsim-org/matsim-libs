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

package org.matsim.network;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;

public class NetworkReaderTeleatlas implements NetworkReader {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(NetworkReaderTeleatlas.class);
	
	private final NetworkLayer network;
	private final String jcShpFileName; // teleatlas junction shape file name
	private final String nwShpFileName; // teleatlas network shape file name
	
	public boolean ignoreFrcType8 = false;
	public boolean ignoreFrcType7onewayN = false;
	public int maxFrcTypeForDoubleLaneLink = 3;
	public int minSpeedForNormalCapacity = 40; // km/h

	public final String nodeIdName = "ID";
	public final String nodeFeattypName = "FEATTYP";
	public final String nodeJncttypName = "JNCTTYP";

	public final String linkIdName = "ID";
	public final String linkFeatTypName = "FEATTYP";
	public final String linkFerryTypeName = "FT";
	public final String linkFromJunctionIdName = "F_JNCTID";
	public final String linkToJunctionIdName = "T_JNCTID";
	public final String linkLengthName = "METERS";
	public final String linkFrcTypeName = "FRC";
	public final String linkOnewayName = "ONEWAY";
	public final String linkSpeedName = "KPH";
	public final String linkLanesName = "LANES";
	// TODO balmermi: probably we also need to consider the following attributes
	//                but for the ch-teleatlas-net version 2008 it was not necessary
	// PRIVATERD Private Road
	//   0: No Special Restriction (default)
	//   2: Not Publicly Accessible
	// CONSTATUS Construction Status
	//   Blank: Not Under Construction (default)
	//   FT: Under Construction in Positive Direction
	//   N: Under Construction in Both Directions
	//   TF: Under Construction in Negative Direction
	// F_BP From (Start) Blocked Passage
	//   0: No Blocked Passage at Start Junction (default)
	//   1: Blocked Passage at Start Junction
	// T_BP To (End) Blocked Passage
	//   0: No Blocked Passage at End Junction (default)
	//   2: Blocked Passage at End Junction
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkReaderTeleatlas(final NetworkLayer network, final String jcShpFileName, final String nwShpFileName) {
		this.network = network;
		this.jcShpFileName = jcShpFileName;
		this.nwShpFileName = nwShpFileName;
	}


	//////////////////////////////////////////////////////////////////////
	// read methods
	//////////////////////////////////////////////////////////////////////

	public void read() throws IOException {
		log.info("reading nodes from Junction Shape file '"+this.jcShpFileName+"'...");
		this.readNodesFromJCshp();
		log.info("done.");
		log.info("reading links from Network Shape file '"+this.nwShpFileName+"'...");
		this.readLinksFromNWshp();
		log.info("done.");
		// TODO balmermi: adding date
		network.setName("teleatlas");
		log.info("Important note: Use the network cleaner to produce a strongly connected di-graph.");
	}

	//////////////////////////////////////////////////////////////////////

	private final void readNodesFromJCshp() throws IOException {
		int nCnt = network.getNodes().size();
		FeatureSource fs = ShapeFileReader.readDataFile(jcShpFileName);
		for (Object o : fs.getFeatures()) {
			Feature f = (Feature)o;
			// get node attributes
			Coordinate c = f.getBounds().centre();
			Object id = f.getAttribute(nodeIdName);
			int feattyp = Integer.parseInt(f.getAttribute(nodeFeattypName).toString());
			if ((feattyp != 4120) && (feattyp != 4220)) { throw new IllegalArgumentException(nodeIdName+"="+id+": "+nodeFeattypName+"="+feattyp+" not allowed."); }
			int jncttyp = Integer.parseInt(f.getAttribute(nodeJncttypName).toString());
			if ((jncttyp < 0) || (jncttyp == 1) || (jncttyp > 6)) { throw new IllegalArgumentException(nodeIdName+"="+id+": "+nodeJncttypName+"="+jncttyp+" not allowed."); }
			if (id == null) { throw new IllegalArgumentException("In "+jcShpFileName+": There is at least one feature that does not have an ID set."); }
			String type = feattyp+"-"+jncttyp;
			network.createNode(new IdImpl(id.toString()),new CoordImpl(c.x,c.y),type);
		}
		nCnt = network.getNodes().size()-nCnt;
		log.info("  "+nCnt+" nodes added to the network.");
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void readLinksFromNWshp() throws IOException {
		int lCnt = network.getLinks().size();
		int ignoreCnt = 0;
		FeatureSource fs = ShapeFileReader.readDataFile(this.nwShpFileName);
		for (Object o : fs.getFeatures()) {
			Feature f = (Feature)o;
			boolean ignore = false;
			// get link attributes
			Object id = f.getAttribute(this.linkIdName);
			int featTyp = Integer.parseInt(f.getAttribute(this.linkFeatTypName).toString());
			if ((featTyp != 4110) && (featTyp != 4130) && (featTyp != 4165)) { throw new IllegalArgumentException(linkIdName+"="+id+": "+linkFeatTypName+"="+featTyp+" not allowed."); }
			int ferryType = Integer.parseInt(f.getAttribute(this.linkFerryTypeName).toString());
			if ((ferryType < 0) || (ferryType > 2)) { throw new IllegalArgumentException(linkIdName+"="+id+": "+linkFerryTypeName+"="+ferryType+" not allowed."); }
			Id fromJunctionId = new IdImpl(f.getAttribute(this.linkFromJunctionIdName).toString());
			Id toJunctionId = new IdImpl(f.getAttribute(this.linkToJunctionIdName).toString());
			double length = Double.parseDouble(f.getAttribute(this.linkLengthName).toString());
			int linksType = Integer.parseInt(f.getAttribute(this.linkFrcTypeName).toString());
			if ((linksType < -1) || (linksType > 8)) { throw new IllegalArgumentException(linkIdName+"="+id+": "+linkFrcTypeName+"="+linksType+" not allowed."); }
			String oneway = f.getAttribute(this.linkOnewayName).toString();
			if (!oneway.equals(" ") && !oneway.equals("FT") && !oneway.equals("TF") && !oneway.equals("N")) { throw new IllegalArgumentException(linkIdName+"="+id+": "+linkOnewayName+"="+oneway+" not allowed."); }
			double speed = Double.parseDouble(f.getAttribute(this.linkSpeedName).toString());
			double lanes = Double.parseDouble(f.getAttribute(this.linkLanesName).toString());
			// ignore link where from node or to node is missing
			Node fNode = network.getNode(fromJunctionId);
			Node tNode = network.getNode(toJunctionId);
			if ((fNode == null) || (tNode == null)) { log.warn("  linkId="+id.toString()+": at least one of the two junctions do not exist. Ignoring and proceeding anyway..."); ignore = true; }
			// ignore link that is not a 'Road Element' (4110) or a 'Ferry Connection Element' (4130)
			// There are 'Address Area Boundary Element' (4165) links that will be ignored
			if ((featTyp != 4110) && (featTyp != 4130)) { log.debug("  linkId="+id.toString()+": ignoring "+linkFeatTypName+"="+featTyp+"."); ignore = true; }
			// ignore links FRC types = -1 [Not Applicable (for FeatTyp 4165)]
			if (linksType < 0) { log.debug("  linkId="+id.toString()+": ignoring "+linkFrcTypeName+"="+linksType+"."); ignore = true; }
			// option flag: ignore links FRC type = 8 [Other Road]
			if (this.ignoreFrcType8 && (7 < linksType)) { log.info("  linkId="+id.toString()+": ignoring "+linkFrcTypeName+"="+linksType+"."); ignore = true; }
			// ignore links FRC types = 7 [Local Road of Minor Importance] that are ONEWAY = N [Closed in Both Directions]
			// links with type < 7 also contains ONEWAY = N but should be open anyway....
			if (this.ignoreFrcType7onewayN && ((linksType == 7) && oneway.equals("N"))) { log.info("  linkId="+id.toString()+": ignoring "+linkFrcTypeName+"="+linksType+" with "+linkOnewayName+"="+oneway+""); ignore = true; }

			// simple rule for number of lanes (a lot of them are = 0, so we need to define something)
			if (lanes < 1) {
				if (linksType <= this.maxFrcTypeForDoubleLaneLink) { lanes = 2; }
				else { lanes = 1; }
			}
			// simple rule for setting capacities
			double cap = -1;
			if (speed < minSpeedForNormalCapacity) { cap = lanes*1000; }
			else { cap = lanes*2000; }
			
			if (ignore) { ignoreCnt++; }
			else {
				if (oneway.equals(" ") || oneway.equals("N")) {
					network.createLink(new IdImpl(id.toString()+"FT"),fNode,tNode,length,speed/3.6,cap,lanes,id.toString(),linksType+"-"+featTyp+"-"+ferryType);
					network.createLink(new IdImpl(id.toString()+"TF"),tNode,fNode,length,speed/3.6,cap,lanes,id.toString(),linksType+"-"+featTyp+"-"+ferryType);
				}
				else if (oneway.equals("FT")) {
					network.createLink(new IdImpl(id.toString()+oneway),fNode,tNode,length,speed/3.6,cap,lanes,id.toString(),linksType+"-"+featTyp+"-"+ferryType);
				}
				else if (oneway.equals("TF")) {
					network.createLink(new IdImpl(id.toString()+oneway),tNode,fNode,length,speed/3.6,cap,lanes,id.toString(),linksType+"-"+featTyp+"-"+ferryType);
				}
				else {
					throw new IllegalArgumentException("linkId="+id.toString()+": "+linkOnewayName+"="+oneway+" not known!");
				}
			}
		}
		
		network.setCapacityPeriod(3600.0);

		lCnt = network.getLinks().size()-lCnt;
		log.info("  "+lCnt+" links added to the network layer.");
		log.info("  "+ignoreCnt+" links ignored from the input shape file.");
	}
}
