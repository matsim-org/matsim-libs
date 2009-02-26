/* *********************************************************************** *
 * project: org.matsim.*
 * TeleatlasParser.java
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

import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.feature.Feature;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.algorithms.NetworkWriteAsTable;
import org.matsim.utils.collections.Tuple;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.gis.ShapeFileReader;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilder;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Network2ESRIShape;
import org.matsim.utils.misc.ArgumentParser;
import org.matsim.utils.misc.Time;

import com.vividsolutions.jts.geom.Coordinate;

public class TeleatlasParser {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(TeleatlasParser.class);
	
	private final NetworkLayer network;
	private String jcShpFileName = null; // teleatlas junction shape file name
	private String nwShpFileName = null; // teleatlas network shape file name
	private String srDbfFileName = null; // teleatlas speed restriction dbf file name
	private String mnShpFileName = null; // teleatlas maneuvers shape file name
	private String mpDbfFileName = null; // teleatlas maneuver paths dbf file name
	private String outputDir = "output";
	
	private boolean ignoreFrcType8 = false;
	private boolean ignoreFrcType7onewayN = false;
	private int maxFrcTypeForDoubleLaneLink = 3;
	private int minSpeedForNormalCapacity = 40; // km/h
	private boolean removeUTurns = false;
	private double expansionRadius = 0.000030; // WGS84
	private double linkSeparation = 0.000005; // WGS84
	private boolean writeNetworkXmlFile = false;
	private boolean writeNetworkShapeFile = false;

	private String nodeIdName = "ID";
	private String nodeFeattypName = "FEATTYP";
	private String nodeJncttypName = "JNCTTYP";

	private String linkIdName = "ID";
	private String linkFeatTypName = "FEATTYP";
	private String linkFerryTypeName = "FT";
	private String linkFromJunctionIdName = "F_JNCTID";
	private String linkToJunctionIdName = "T_JNCTID";
	private String linkLengthName = "METERS";
	private String linkFrcTypeName = "FRC";
	private String linkOnewayName = "ONEWAY";
	private String linkSpeedName = "KPH";
	private String linkLanesName = "LANES";
	// TODO
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
	
	private String srIdName = "ID";
	private String srSpeedName = "SPEED";
	private String srValDirName = "VALDIR";
	private String srVerifiedName = "VERIFIED";
	
	private String mnIdName = "ID";
	private String mnFeatTypeName = "FEATTYP";
	private String mnJnctIdName = "JNCTID";
	
	private String mpIdName = "ID";
	private String mpSeqNrName = "SEQNR";
	private String mpTrpelIDName = "TRPELID";

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public TeleatlasParser() {
		this(new NetworkLayer());
	}

	public TeleatlasParser(final NetworkLayer network) {
		this.network = network;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setJcShpFileName(final String jcShpFileName) {
		this.jcShpFileName = jcShpFileName;
	}

	public final void setNwShpFileName(final String nwShpFileName) {
		this.nwShpFileName = nwShpFileName;
	}

	public final void setSrDbfFileName(final String srDbfFileName) {
		this.srDbfFileName = srDbfFileName;
	}

	public final void setMnShpMpDbfFileName(final String mnShpFileName, final String mpDbfFileName) {
		if ((mnShpFileName == null) ^ (mpDbfFileName == null)) {
			throw new IllegalArgumentException("non or both maneuver input files must be defined.");
		}
		this.mnShpFileName = mnShpFileName;
		this.mpDbfFileName = mpDbfFileName;
	}

	public final void ignoreFrcType8(final boolean flag) {
		this.ignoreFrcType8 = flag;
	}
	
	public final void ignoreFrcType7onewayN(final boolean flag) {
		this.ignoreFrcType7onewayN = flag;
	}

	public final void setMaxFrcTypeForDoubleLaneLink(final int frcType) {
		this.maxFrcTypeForDoubleLaneLink = frcType;
	}

	public final void setMinSpeedForNormalCapacity(final int minSpeed) {
		this.minSpeedForNormalCapacity = minSpeed;
	}
	
	public final void removeUTurns(final boolean flag) {
		this.removeUTurns = flag;
	}

	public final void setExpansionRadius(final double radius) {
		this.expansionRadius = radius;
	}

	public final void setLinkSeparation(final double distance) {
		this.linkSeparation = distance;
	}

	public final void setOutputDir(final String outputDir) {
		if (outputDir == null) {
			throw new IllegalArgumentException("outdir="+outputDir+" not allowed.");
		}
		this.outputDir = outputDir;
	}

	public final void writeNetworkXmlFile(final boolean flag) {
		this.writeNetworkXmlFile = flag;
	}

	public final void writeNetworkShapeFile(final boolean flag) {
		this.writeNetworkShapeFile = flag;
	}

	//////////////////////////////////////////////////////////////////////
	// convert method
	//////////////////////////////////////////////////////////////////////
	
	public final NetworkLayer convert() throws Exception {
		log.info("conversion settings...");
		printSetting();
		log.info("done.");
		
		log.info("adding nodes from Junction Shape file '"+this.jcShpFileName+"'...");
		this.addNodesFromJCshp();
		log.info("done.");

		log.info("adding links from Network Shape file '"+this.nwShpFileName+"'...");
		this.addLinksFromNWshp();
		log.info("done.");
		
		if (this.srDbfFileName != null) {
			log.info("assigning speed restrictions to the links from Shape file '"+this.srDbfFileName+"'...");
			this.addSpeedRestrictionsFromSRdbf();
			log.info("done.");
		}

		if (this.mnShpFileName != null) {
			log.info("assigning maneuver restrictions to the network (expanding nodes) from the shape file '"+this.mnShpFileName+"' and the dbf file '"+this.mpDbfFileName+"'...");
			this.addManeuversRestrictionsFromMNshpAndMPdbf();
			log.info("done.");
		}
		
		if (this.writeNetworkXmlFile) {
			log.info("writing xml file...");
			this.writeXml();
			log.info("done.");
		}
		if (this.writeNetworkShapeFile) {
			log.info("writing shape file...");
			this.writeShape();
			log.info("done.");
		}
		return this.network;
	}
	
	//////////////////////////////////////////////////////////////////////
	// write method
	//////////////////////////////////////////////////////////////////////

	private final void writeXml() {
		new NetworkWriter(network,outputDir+"/output_network.xml.gz").write();
		NetworkWriteAsTable nwat = new NetworkWriteAsTable(outputDir);
		nwat.run(network);
		nwat.close();
	}
	
	private final void writeShape() {
		if (Gbl.getConfig() == null) { Gbl.createConfig(null); }
		Gbl.getConfig().global().setCoordinateSystem("WGS84");
		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(network);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);		
		new Network2ESRIShape(network,outputDir+"/output_links.shp",builder).write();
	}
	
	//////////////////////////////////////////////////////////////////////
	// print method
	//////////////////////////////////////////////////////////////////////

	private final void printSetting() {
		log.info("  input / output:");
		log.info("    jcShpFileName: "+jcShpFileName);
		log.info("    nwShpFileName: "+nwShpFileName);
		log.info("    srDbfFileName: "+srDbfFileName);
		log.info("    mnShpFileName: "+mnShpFileName);
		log.info("    mpDbfFileName: "+mpDbfFileName);
		log.info("    outputDir:     "+outputDir);
		log.info("  options:");
		log.info("    ignoreFrcType8:              "+ignoreFrcType8);
		log.info("    ignoreFrcType7onewayN:       "+ignoreFrcType7onewayN);
		log.info("    maxFrcTypeForDoubleLaneLink: "+maxFrcTypeForDoubleLaneLink);
		log.info("    minSpeedForNormalCapacity:   "+minSpeedForNormalCapacity);
		log.info("    removeUTurns:                "+removeUTurns);
		log.info("    expansionRadius:             "+expansionRadius);
		log.info("    linkSeparation:              "+linkSeparation);
		log.info("    writeNetworkXmlFile:         "+writeNetworkXmlFile);
		log.info("    writeNetworkShapeFile:       "+writeNetworkShapeFile);
		log.info("  junction shape file attributes:");
		log.info("    nodeIdName:      "+nodeIdName);
		log.info("    nodeFeattypName: "+nodeFeattypName);
		log.info("    nodeJncttypName: "+nodeJncttypName);
		log.info("  network shape file attributes:");
		log.info("    linkIdName:             "+linkIdName);
		log.info("    linkFeatTypName:        "+linkFeatTypName);
		log.info("    linkFerryTypeName:      "+linkFerryTypeName);
		log.info("    linkFromJunctionIdName: "+linkFromJunctionIdName);
		log.info("    linkToJunctionIdName:   "+linkToJunctionIdName);
		log.info("    linkLengthName:         "+linkLengthName);
		log.info("    linkFrcTypeName:        "+linkFrcTypeName);
		log.info("    linkOnewayName:         "+linkOnewayName);
		log.info("    linkSpeedName:          "+linkSpeedName);
		log.info("    linkLanesName:          "+linkLanesName);
		if (this.srDbfFileName != null) {
			log.info("  speed restriction dbf file attributes:");
			log.info("    srIdName:       "+srIdName);
			log.info("    srSpeedName:    "+srSpeedName);
			log.info("    srValDirName:   "+srValDirName);
			log.info("    srVerifiedName: "+srVerifiedName);
		}
		if (this.mnShpFileName != null) {
			log.info("  maneuver shape file attributes:");
			log.info("    mnIdName:       "+mnIdName);
			log.info("    mnFeatTypeName: "+mnFeatTypeName);
			log.info("    mnJnctIdName:   "+mnJnctIdName);
		}
		if (this.mpDbfFileName != null) {
			log.info("  maneuver paths dbf file attributes:");
			log.info("    mpIdName: "+mpIdName);
			log.info("    mpSeqNrName: "+mpSeqNrName);
			log.info("    mpTrpelIDName: "+mpTrpelIDName);
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	private final void addNodesFromJCshp() throws Exception {
		int nCnt = network.getNodes().size();
		FeatureSource fs = ShapeFileReader.readDataFile(this.jcShpFileName);
		for (Object o : fs.getFeatures()) {
			Feature f = (Feature)o;
			// get node attributes
			Coordinate c = f.getBounds().centre();
			Object id = f.getAttribute(this.nodeIdName);
			Object feattyp = f.getAttribute(this.nodeFeattypName);
			Object jncttyp = f.getAttribute(this.nodeJncttypName);
			if (id == null) { throw new IllegalArgumentException("In "+jcShpFileName+": There is at least one feature that does not have an ID set."); }
			String type = feattyp+"-"+jncttyp;
			network.createNode(new IdImpl(id.toString()),new CoordImpl(c.x,c.y),type);
		}
		nCnt = network.getNodes().size()-nCnt;
		log.info("  "+nCnt+" nodes added to the network.");
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void addLinksFromNWshp() throws Exception {
		int lCnt = network.getLinks().size();
		int ignoreCnt = 0;
		FeatureSource fs = ShapeFileReader.readDataFile(this.nwShpFileName);
		for (Object o : fs.getFeatures()) {
			Feature f = (Feature)o;
			boolean ignore = false;
			// get link attributes
			Object id = f.getAttribute(this.linkIdName);
			int featTyp = Integer.parseInt(f.getAttribute(this.linkFeatTypName).toString());
			int ferryType = Integer.parseInt(f.getAttribute(this.linkFerryTypeName).toString());
			Id fromJunctionId = new IdImpl(f.getAttribute(this.linkFromJunctionIdName).toString());
			Id toJunctionId = new IdImpl(f.getAttribute(this.linkToJunctionIdName).toString());
			double length = Double.parseDouble(f.getAttribute(this.linkLengthName).toString());
			int linksType = Integer.parseInt(f.getAttribute(this.linkFrcTypeName).toString());
			String oneway = f.getAttribute(this.linkOnewayName).toString();
			double speed = Double.parseDouble(f.getAttribute(this.linkSpeedName).toString());
			double lanes = Double.parseDouble(f.getAttribute(this.linkLanesName).toString());
			// ignore link where from node or to node is missing
			Node fNode = network.getNode(fromJunctionId);
			Node tNode = network.getNode(toJunctionId);
			if ((fNode == null) || (tNode == null)) { log.warn("  linkId="+id.toString()+": at least one of the two junctions do not exist. Ignoring and proceeding anyway..."); ignore = true; }
			// ignore link that is not a 'Road Element' (4110) or a 'Ferry Connection Element' (4130)
			// typically there are 'Address Area Boundary Element' (4165) links that will be ignored
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
		network.setName("teleatlas");

		lCnt = network.getLinks().size()-lCnt;
		log.info("  "+lCnt+" links added to the network layer.");
		log.info("  "+ignoreCnt+" links ignored from the input shape file.");
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void addSpeedRestrictionsFromSRdbf() throws Exception {
		FileChannel in = new FileInputStream(this.srDbfFileName).getChannel();
		DbaseFileReader r = new DbaseFileReader(in,true);
		// getting header indices
		int srIdNameIndex = -1;
		int srSpeedNameIndex = -1;
		int srValDirNameIndex = -1;
		int srVerifiedNameIndex = -1;
		for (int i=0; i<r.getHeader().getNumFields(); i++) {
			if (r.getHeader().getFieldName(i).equals(srIdName)) { srIdNameIndex = i; }
			if (r.getHeader().getFieldName(i).equals(srSpeedName)) { srSpeedNameIndex = i; }
			if (r.getHeader().getFieldName(i).equals(srValDirName)) { srValDirNameIndex = i; }
			if (r.getHeader().getFieldName(i).equals(srVerifiedName)) { srVerifiedNameIndex = i; }
		}
		if (srIdNameIndex < 0) { throw new NoSuchFieldException("Field name '"+srIdName+"' not found."); }
		if (srSpeedNameIndex < 0) { throw new NoSuchFieldException("Field name '"+srSpeedName+"' not found."); }
		if (srValDirNameIndex < 0) { throw new NoSuchFieldException("Field name '"+srValDirName+"' not found."); }
		if (srVerifiedNameIndex < 0) { throw new NoSuchFieldException("Field name '"+srVerifiedName+"' not found."); }
		log.debug("  FieldName-->Index:");
		log.debug("    "+srIdName+"-->"+srIdNameIndex);
		log.debug("    "+srSpeedName+"-->"+srSpeedNameIndex);
		log.debug("    "+srValDirName+"-->"+srValDirNameIndex);
		log.debug("    "+srVerifiedName+"-->"+srVerifiedNameIndex);
	
		int srCnt = 0;
		int srIgnoreCnt = 0;
		while (r.hasNext()) {
			Object[] entries = r.readEntry();
			int verified = Integer.parseInt(entries[srVerifiedNameIndex].toString());
			// assign only verfied speed restricitons
			if (verified == 1) {
				int valdir = Integer.parseInt(entries[srValDirNameIndex].toString());
				String id = entries[srIdNameIndex].toString();
				if (valdir == 1) {
					// Valid in Both Directions
					Link ftLink = network.getLinks().get(new IdImpl(id+"FT"));
					Link tfLink = network.getLinks().get(new IdImpl(id+"TF"));
					if ((ftLink == null) || (tfLink == null)) { log.debug("  linkid="+id+", valdir="+valdir+": at least one link not found. Ignoring and proceeding anyway..."); srIgnoreCnt++; }
					else {
						double speed = Double.parseDouble(entries[srSpeedNameIndex].toString())/3.6;
						// assigning speed restriction only if given freespeed is higher
						if (speed < ftLink.getFreespeed(Time.UNDEFINED_TIME)) { ftLink.setFreespeed(speed); srCnt++; } else { srIgnoreCnt++; }
						if (speed < tfLink.getFreespeed(Time.UNDEFINED_TIME)) { tfLink.setFreespeed(speed); srCnt++; } else { srIgnoreCnt++; }
					}
				}
				else if (valdir == 2) {
					// Valid Only in Positive Direction
					Link ftLink = network.getLinks().get(new IdImpl(id+"FT"));
					if (ftLink == null) { log.debug("  linkid="+id+", valdir="+valdir+": link not found. Ignoring and proceeding anyway..."); srIgnoreCnt++; }
					else {
						double speed = Double.parseDouble(entries[srSpeedNameIndex].toString())/3.6;
						// assigning speed restriction only if given freespeed is higher
						if (speed < ftLink.getFreespeed(Time.UNDEFINED_TIME)) { ftLink.setFreespeed(speed); srCnt++; } else { srIgnoreCnt++; }
					}
				}
				else if (valdir == 3) {
					// Valid Only in Negative Direction
					Link tfLink = network.getLinks().get(new IdImpl(id+"TF"));
					if (tfLink == null) { log.debug("  linkid="+id+", valdir="+valdir+": link not found. Ignoring and proceeding anyway..."); srIgnoreCnt++; }
					else {
						double speed = Double.parseDouble(entries[srSpeedNameIndex].toString())/3.6;
						// assigning speed restriction only if given freespeed is higher
						if (speed < tfLink.getFreespeed(Time.UNDEFINED_TIME)) { tfLink.setFreespeed(speed); srCnt++; } else { srIgnoreCnt++; }
					}
				}
				else { throw new IllegalArgumentException("linkid="+id+": valdir="+valdir+" not known."); }
			}
		}
		log.info("  "+srCnt+" links with restricted speed assigned.");
		log.info("  "+srIgnoreCnt+" speed restrictions ignored (while verified = 1).");
	}

	//////////////////////////////////////////////////////////////////////

	private final void addManeuversRestrictionsFromMNshpAndMPdbf() throws Exception {
		FileChannel in = new FileInputStream(this.mpDbfFileName).getChannel();
		DbaseFileReader r = new DbaseFileReader(in,true);
		// get header indices
		int mpIdNameIndex = -1;
		int mpSeqNrNameIndex = -1;
		int mpTrpelIDNameIndex = -1;
		for (int i=0; i<r.getHeader().getNumFields(); i++) {
			if (r.getHeader().getFieldName(i).equals(mpIdName)) { mpIdNameIndex = i; }
			if (r.getHeader().getFieldName(i).equals(mpSeqNrName)) { mpSeqNrNameIndex = i; }
			if (r.getHeader().getFieldName(i).equals(mpTrpelIDName)) { mpTrpelIDNameIndex = i; }
		}
		if (mpIdNameIndex < 0) { throw new NoSuchFieldException("Field name '"+srIdName+"' not found."); }
		if (mpSeqNrNameIndex < 0) { throw new NoSuchFieldException("Field name '"+srSpeedName+"' not found."); }
		if (mpTrpelIDNameIndex < 0) { throw new NoSuchFieldException("Field name '"+srValDirName+"' not found."); }
		log.debug("  FieldName-->Index:");
		log.debug("    "+mpIdName+"-->"+mpIdNameIndex);
		log.debug("    "+mpSeqNrName+"-->"+mpSeqNrNameIndex);
		log.debug("    "+mpTrpelIDName+"-->"+mpTrpelIDNameIndex);

		// create mp data structure
		// TreeMap<mpId,TreeMap<mpSeqNr,linkId>>
		log.info("  parsing meneuver paths dbf file...");
		TreeMap<Id,TreeMap<Integer,Id>> mSequences = new TreeMap<Id, TreeMap<Integer,Id>>();
		while (r.hasNext()) {
			Object[] entries = r.readEntry();
			Id mpId = new IdImpl(entries[mpIdNameIndex].toString());
			int mpSeqNr = Integer.parseInt(entries[mpSeqNrNameIndex].toString());
			Id linkId = new IdImpl(entries[mpTrpelIDNameIndex].toString());
			TreeMap<Integer,Id> mSequence = mSequences.get(mpId);
			if (mSequence == null) { mSequence = new TreeMap<Integer,Id>(); }
			if (mSequence.put(mpSeqNr,linkId) != null) { throw new IllegalArgumentException(mpIdName+"="+mpId+": "+mpSeqNrName+" "+mpSeqNr+" already exists."); }
			mSequences.put(mpId,mSequence);
		}
		log.info("    "+mSequences.size()+" maneuvers sequences stored.");
		log.info("  done.");
		
		// store the maneuver list of the nodes
		// TreeMap<NodeId,ArrayList<Tuple<MnId,MnFeatType>>>
		log.info("  parsing meneuver shape file...");
		TreeMap<Id,ArrayList<Tuple<Id,Integer>>> maneuvers = new TreeMap<Id,ArrayList<Tuple<Id,Integer>>>();
		FeatureSource fs = ShapeFileReader.readDataFile(this.mnShpFileName);
		for (Object o : fs.getFeatures()) {
			Feature f = (Feature)o;
			int featType = Integer.parseInt(f.getAttribute(mnFeatTypeName).toString());
			if ((featType == 2103) || (featType == 2102) || (featType == 2101)) {
				// keep 'Prohibited Maneuver' (2103), 'Restricted Maneuver' (2102) and 'Calculated/Derived Prohibited Maneuver' (2101)
				Id nodeId = new IdImpl(f.getAttribute(mnJnctIdName).toString());
				ArrayList<Tuple<Id,Integer>> ms = maneuvers.get(nodeId);
				if (ms == null) { ms = new ArrayList<Tuple<Id,Integer>>(); }
				Tuple<Id,Integer> m = new Tuple<Id,Integer>(new IdImpl(f.getAttribute(mnIdName).toString()),featType);
				ms.add(m);
				maneuvers.put(nodeId,ms);
			}
			else if ((featType == 9401) || (featType == 2104)) {
				//ignore 'Bifurcation' (9401) and 'Priority Maneuver' (2104)
			}
			else {
				throw new IllegalArgumentException("mnId="+f.getAttribute(mnIdName)+": "+mnFeatTypeName+"="+featType+" not known.");
			}
		}
		log.info("    "+maneuvers.size()+" nodes with maneuvers stored.");
		log.info("  done.");
		
		// create a maneuver matrix for each given node and
		// expand those nodes
		log.info("  expand nodes according to the given manveuvers...");
		int nodesIgnoredCnt = 0;
		int nodesAssignedCnt = 0;
		int maneuverIgnoredCnt = 0;
		int maneuverAssignedCnt = 0;
		int virtualNodesCnt = 0;
		int virtualLinksCnt = 0;
		for (Id nodeId : maneuvers.keySet()) {
			if (network.getNode(nodeId) == null) { log.debug("  nodeid="+nodeId+": maneuvers exist for that node but node is missing. Ignoring and proceeding anyway..."); nodesIgnoredCnt++; }
			else {
				// node found
				Node n = network.getNode(nodeId);
				// init maneuver matrix
				// TreeMap<fromLinkId,TreeMap<toLinkId,turnAllowed>>
				TreeMap<Id,TreeMap<Id,Boolean>> mmatrix = new TreeMap<Id, TreeMap<Id,Boolean>>();
				// assign maneuvers for given node to the matrix
				ArrayList<Tuple<Id,Integer>> ms = maneuvers.get(nodeId);
				for (int i=0; i<ms.size(); i++) {
					Tuple<Id,Integer> m = ms.get(i);
					// get maneuver path sequence for given maneuver
					TreeMap<Integer,Id> mSequence = mSequences.get(m.getFirst());
					if (mSequence == null) { throw new Exception("nodeid="+nodeId+"; mnId="+m.getFirst()+": no maneuver sequence given."); }
					if (mSequence.size() < 2) { throw new Exception("nodeid="+nodeId+"; mnId="+m.getFirst()+": mSequenceSize="+mSequence.size()+" not alowed!"); }
					// get the first element of the sequence, defining the start link for the maneuver 
					Iterator<Integer> snr_it = mSequence.keySet().iterator();
					Integer snr = snr_it.next();
					// go through each other element (target link of the maneuver) of the sequence by sequence number
					while (snr_it.hasNext()) {
						Integer snr2 = snr_it.next();
						// get the start link and the target link of the maneuver
						Link inLink = n.getInLinks().get(new IdImpl(mSequence.get(snr)+"FT"));
						if (inLink == null) { inLink = n.getInLinks().get(new IdImpl(mSequence.get(snr)+"TF")); }
						Link outLink = n.getOutLinks().get(new IdImpl(mSequence.get(snr2)+"FT"));
						if (outLink == null) { outLink = n.getOutLinks().get(new IdImpl(mSequence.get(snr2)+"TF")); }
						if ((inLink != null) && (outLink != null)) {
							// start and target link found and they are incident to the given node
							if (m.getSecond() == 2102) {
								// restricted maneuver: given start and target link path is allowed to drive
								// store it to the matrix
								TreeMap<Id,Boolean> outLinkMap = mmatrix.get(inLink.getId());
								if (outLinkMap == null) { outLinkMap = new TreeMap<Id,Boolean>(); }
								outLinkMap.put(outLink.getId(),true);
								mmatrix.put(inLink.getId(),outLinkMap);
							}
							else {
								// prohibited maneuver: given start and target link path is not allowed to drive
								// store it to the matrix
								TreeMap<Id,Boolean> outLinkMap = mmatrix.get(inLink.getId());
								if (outLinkMap == null) { outLinkMap = new TreeMap<Id,Boolean>(); }
								outLinkMap.put(outLink.getId(),false);
								mmatrix.put(inLink.getId(),outLinkMap);
							}
							maneuverAssignedCnt++;
						}
						else { maneuverIgnoredCnt++; }
					}
				}
				// complete the matrix
				for (Id fromLinkId : mmatrix.keySet()) {
					// detect inlinks with restricted maneuvers
					boolean hasRestrictedManeuver = false;
					for (Id toLinkId : mmatrix.get(fromLinkId).keySet()) {
						Boolean b = mmatrix.get(fromLinkId).get(toLinkId);
						if (b) { hasRestrictedManeuver = true; }
					}
					// add missing toLink maneuvers
					for (Id toLinkId : n.getOutLinks().keySet()) {
						if (!mmatrix.get(fromLinkId).containsKey(toLinkId)) {
							if (hasRestrictedManeuver) { mmatrix.get(fromLinkId).put(toLinkId,false); }
							else { mmatrix.get(fromLinkId).put(toLinkId,true); }
						}
					}
				}
				// add allowed maneuvers for fromLinks which were not assigned yet.
				for (Id fromLinkId : n.getInLinks().keySet()) {
					if (!mmatrix.containsKey(fromLinkId)) {
						mmatrix.put(fromLinkId,new TreeMap<Id, Boolean>());
						for (Id toLinkId : n.getOutLinks().keySet()) { mmatrix.get(fromLinkId).put(toLinkId,true); }
					}
				}
				// remove all U-turns from the matrix
				if (this.removeUTurns) {
					for (Id fromLinkId : n.getInLinks().keySet()) {
						String str1 = fromLinkId.toString().substring(0,fromLinkId.toString().length()-2);
						for (Id toLinkId : n.getOutLinks().keySet()) {
							String str2 = toLinkId.toString().substring(0,toLinkId.toString().length()-2);
							if (str1.equals(str2)) {
								mmatrix.get(fromLinkId).put(toLinkId,false);
							}
						}
					}
				}
				// create arraylist with turn tuples
				ArrayList<Tuple<Id,Id>> turns = new ArrayList<Tuple<Id,Id>>();
				for (Id fromLinkId : mmatrix.keySet()) {
					for (Id toLinkId : mmatrix.get(fromLinkId).keySet()) {
						Boolean b = mmatrix.get(fromLinkId).get(toLinkId);
						if (b) { turns.add(new Tuple<Id, Id>(fromLinkId,toLinkId)); }
					}
				}
				// expand the node
				Tuple<ArrayList<Node>,ArrayList<Link>> t = expandNode(network,nodeId,turns,expansionRadius,linkSeparation);
				virtualNodesCnt += t.getFirst().size();
				virtualLinksCnt += t.getSecond().size();
				nodesAssignedCnt++;
			}
		}
		log.info("    "+nodesAssignedCnt+" nodes expanded.");
		log.info("    "+maneuverAssignedCnt+" maneuvers assigned.");
		log.info("    "+virtualNodesCnt+" new nodes created.");
		log.info("    "+virtualLinksCnt+" new links created.");
		log.info("    "+nodesIgnoredCnt+" nodes with given maneuvers (2103, 2102 or 2101) ignored.");
		log.info("    "+maneuverIgnoredCnt+" maneuvers ignored (while node was found).");
		log.info("  done.");
	}

	//////////////////////////////////////////////////////////////////////
	// expand method
	//////////////////////////////////////////////////////////////////////

	private final Tuple<ArrayList<Node>,ArrayList<Link>> expandNode(final NetworkLayer network, final Id nodeId, final ArrayList<Tuple<Id,Id>> turns, final double r, final double e) {
		double d = Math.sqrt(r*r-e*e);
		// check the input
		if (network == null) { throw new IllegalArgumentException("network not defined."); }
		Node node = network.getNode(nodeId);
		if (node == null) { throw new IllegalArgumentException("nodeid="+nodeId+": not found in the network."); }
		for (int i=0; i<turns.size(); i++) {
			Id first = turns.get(i).getFirst();
			if (first == null) { throw new IllegalArgumentException("given list contains 'null' values."); }
			if (!node.getInLinks().containsKey(first)) { throw new IllegalArgumentException("nodeid="+nodeId+", linkid="+first+": link not an inlink of given node."); }
			Id second = turns.get(i).getSecond();
			if (second == null) { throw new IllegalArgumentException("given list contains 'null' values."); }
			if (!node.getOutLinks().containsKey(second)) { throw new IllegalArgumentException("nodeid="+nodeId+", linkid="+second+": link not an outlink of given node."); }
		}
		
		// remove the node
		Map<Id,Link> inlinks = new TreeMap<Id, Link>(node.getInLinks());
		Map<Id,Link> outlinks = new TreeMap<Id, Link>(node.getOutLinks());
		if (!network.removeNode(node)) { throw new RuntimeException("nodeid="+nodeId+": Failed to remove node from the network."); }

		ArrayList<Node> newNodes = new ArrayList<Node>(inlinks.size()+outlinks.size());
		ArrayList<Link> newLinks = new ArrayList<Link>(turns.size());
		// add new nodes and connect them with the in and out links
		int nodeIdCnt = 0;
		for (Link inlink : inlinks.values()) {
			Coord c = node.getCoord();
			Coord p = inlink.getFromNode().getCoord();
			Coord pc = new CoordImpl(c.getX()-p.getX(),c.getY()-p.getY());
			double lpc = Math.sqrt(pc.getX()*pc.getX()+pc.getY()*pc.getY());
			double x = p.getX()+(1-d/lpc)*pc.getX()+e/lpc*pc.getY();
			double y = p.getY()+(1-d/lpc)*pc.getY()-e/lpc*pc.getX();
			Node n = network.createNode(new IdImpl(node.getId()+"-"+nodeIdCnt),new CoordImpl(x,y),node.getType());
			newNodes.add(n);
			nodeIdCnt++;
			network.createLink(inlink.getId(),inlink.getFromNode(),n,inlink.getLength(),inlink.getFreespeed(Time.UNDEFINED_TIME),inlink.getCapacity(Time.UNDEFINED_TIME),inlink.getLanes(Time.UNDEFINED_TIME),inlink.getOrigId(),inlink.getType());
		}
		for (Link outlink : outlinks.values()) {
			Coord c = node.getCoord();
			Coord p = outlink.getToNode().getCoord();
			Coord cp = new CoordImpl(p.getX()-c.getX(),p.getY()-c.getY());
			double lcp = Math.sqrt(cp.getX()*cp.getX()+cp.getY()*cp.getY());
			double x = c.getX()+d/lcp*cp.getX()+e/lcp*cp.getY();
			double y = c.getY()+d/lcp*cp.getY()-e/lcp*cp.getX();
			Node n = network.createNode(new IdImpl(node.getId()+"-"+nodeIdCnt),new CoordImpl(x,y),node.getType());
			newNodes.add(n);
			nodeIdCnt++;
			network.createLink(outlink.getId(),n,outlink.getToNode(),outlink.getLength(),outlink.getFreespeed(Time.UNDEFINED_TIME),outlink.getCapacity(Time.UNDEFINED_TIME),outlink.getLanes(Time.UNDEFINED_TIME),outlink.getOrigId(),outlink.getType());
		}
		
		// add virtual links for the turn restrictions
		for (int i=0; i<turns.size(); i++) {
			Tuple<Id,Id> turn = turns.get(i);
			Link fromLink = network.getLink(turn.getFirst());
			Link toLink = network.getLink(turn.getSecond());
			Link l = network.createLink(new IdImpl(fromLink.getId()+"-"+i),fromLink.getToNode(),toLink.getFromNode(),toLink.getFromNode().getCoord().calcDistance(fromLink.getToNode().getCoord()),fromLink.getFreespeed(Time.UNDEFINED_TIME),fromLink.getCapacity(Time.UNDEFINED_TIME),fromLink.getLanes(Time.UNDEFINED_TIME));
			newLinks.add(l);
		}
		return new Tuple<ArrayList<Node>, ArrayList<Link>>(newNodes,newLinks);
	}
	
	//////////////////////////////////////////////////////////////////////
	// main method
	//////////////////////////////////////////////////////////////////////

	private static void parseArguments(TeleatlasParser tp, final String[] args) {
		if (args.length == 0) { System.out.println("Too few arguments."); printUsage(); System.exit(1); }

		Iterator<String> argIter = new ArgumentParser(args).iterator();
		while (argIter.hasNext()) {
			String arg = argIter.next();
			if (arg.equals("--xml")) { tp.writeNetworkXmlFile(true); }
			else if (arg.equals("--shp")) { tp.writeNetworkShapeFile(true); }
			else if (arg.equals("--frc8")) { tp.ignoreFrcType8(true); }
			else if (arg.equals("--frc7N")) { tp.ignoreFrcType7onewayN(true); }
			else if (arg.equals("--maxfrc2l")) {
				ensureNextElement(argIter);
				try { tp.setMaxFrcTypeForDoubleLaneLink(Integer.parseInt(argIter.next())); }
				catch (Exception e) { System.out.println("Cannot understand argument: --maxfrc2l " + arg); printUsage(); System.exit(1); }
			}
			else if (arg.equals("--minsnc")) {
				ensureNextElement(argIter);
				try { tp.setMinSpeedForNormalCapacity(Integer.parseInt(argIter.next())); }
				catch (Exception e) { System.out.println("Cannot understand argument: --minsnc " + arg); printUsage(); System.exit(1); }
			}
			else if (arg.equals("--radius")) {
				ensureNextElement(argIter);
				try { tp.setExpansionRadius(Double.parseDouble(argIter.next())); }
				catch (Exception e) { System.out.println("Cannot understand argument: --radius " + arg); printUsage(); System.exit(1); }
			}
			else if (arg.equals("--offset")) {
				ensureNextElement(argIter);
				try { tp.setLinkSeparation(Double.parseDouble(argIter.next())); }
				catch (Exception e) { System.out.println("Cannot understand argument: --offset " + arg); printUsage(); System.exit(1); }
			}
			else if (arg.equals("--uturn")) { tp.removeUTurns(true); }
			else if (arg.equals("-h") || arg.equals("--help")) { printUsage(); System.exit(0); }
			else if (arg.startsWith("-")) { System.out.println("Unrecognized option " + arg); System.exit(1); }
			else {
				tp.setJcShpFileName(arg);
				ensureNextElement(argIter);
				tp.setNwShpFileName(argIter.next());
				if (argIter.hasNext()) {
					arg = argIter.next(); // dbf, shp&dbf, dir
					if (arg.endsWith(".dbf")) {
						tp.setSrDbfFileName(arg);
						if (argIter.hasNext()) {
							arg = argIter.next(); // shp&dbf, dir
							if (arg.endsWith(".shp")) {
								ensureNextElement(argIter);
								tp.setMnShpMpDbfFileName(arg,argIter.next());
								if (argIter.hasNext()) { // dir
									tp.setOutputDir(argIter.next());
								}
							}
							else {
								tp.setOutputDir(arg);
							}
						}
					}
					else if (arg.endsWith(".shp")) {
						ensureNextElement(argIter);
						tp.setMnShpMpDbfFileName(arg,argIter.next());
						if (argIter.hasNext()) { // dir
							tp.setOutputDir(argIter.next());
						}
					}
					else {
						tp.setOutputDir(arg);
					}
				}
				if (argIter.hasNext()) {
					System.out.println("Too many arguments.");
					printUsage();
					System.exit(1);
				}
			}
		}
	}

	private static void ensureNextElement(final Iterator<String> iter) {
		if (!iter.hasNext()) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}
	}

	private static void printUsage() {
		System.out.println();
		System.out.println("TeleatlasParser");
		System.out.println("Parsers Teleatlas databases into MATSim network data structure.");
		System.out.println("Optional: It also writes a MATSim XML network file and/or a shape file of the data.");
		System.out.println();
		System.out.println("usage: TeleatlasParser [OPTIONS] jcShpFile nwShpFile [srDbfFile] [mnShpFile mpShpFile] [outputDirectory]");
		System.out.println();
		System.out.println("jcShpFile:       Teleatlas Junction Shape File (typically called 'xyz________jc.shp')");
		System.out.println("nwShpFile:       Teleatlas network Shape File (typically called 'xyz________nw.shp')");
		System.out.println("srDbfFile:       Teleatlas speed restriction DBF File (typically called 'xyz________sr.dbf')");
		System.out.println("mnShpFile:       Teleatlas maneuver Shape File (typically called 'xyz________mn.shp')");
		System.out.println("mpShpFile:       Teleatlas maneuver paths DBF File (typically called 'xyz________mp.dbf')");
		System.out.println("outputDirectory: Directory where output files (MATSim XML network file and shape files) are stored.");
		System.out.println("                 default: ./output");
		System.out.println("                 If writing option is set (see below) the files will be stored in:");
		System.out.println("                 <outputDirectory>/output_network.xml.gz");
		System.out.println("                 <outputDirectory>/output_links.shp");
		System.out.println();
		System.out.println("Options:");
		System.out.println("--xml:           If set, MATSim XML network file will be written to <outputDirectory>/output_network.xml.gz.");
		System.out.println("--shp:           If set, network Shape file will be written to <outputDirectory>/output_links.shp");
		System.out.println("--frc8:          If set, links with FRC type = '8' will be ignored from the nwShpFile.");
		System.out.println("--frc7N:         If set, links with FRC type = '7' and ONEWAY = 'N' will be ignored from the nwShpFile.");
		System.out.println("--maxfrc2l FRCtype:");
		System.out.println("                 Defines, which links of the nwShpFile get 2 lanes per direction ('MAX FRC for 2 LANES').");
		System.out.println("                 Teleatlas defines the number of lanes (LANES attribute of the nwShpFile) only for");
		System.out.println("                 a few links. This option defines for links with LANES<'1' how many lanes will be set");
		System.out.println("                 based on the FRC type. E.g. '--maxfrc2l 4' sets 2 lanes for FRC=[0-4] and 1 lane for FRC>4.");
		System.out.println("                 default: '--maxfrc2l 3'");
		System.out.println("--minsnc freespeed:");
		System.out.println("                 Defines, which links of the nwShpFile get capacity[veh/h]=2000*#lanes ('MIN SPEED for NORMAL CAPACITY').");
		System.out.println("                 Teleatlas does not define link capacities. This option sets capactities based on freespeed and");
		System.out.println("                 derived number of lanes. E.g. '--minsnc 20'[km/h] sets capacity[veh/h]=2000*#lanes for freespeed>=20[km/h] and");
		System.out.println("                 capacity[veh/h]=1000*#lanes for freespeed<20[km/h].");
		System.out.println("                 default: '--minsnc 40'");
		System.out.println("--radius NodeExpansionRadius:");
		System.out.println("                 If [mnShpFile mpShpFile] are given, turn maneuvers will be created via expanding the corresponing node");
		System.out.println("                 with virtual nodes. The option defines the radius on which the virtual nodes will be places around");
		System.out.println("                 the expanded node. The unit of 'NodeExpansionRadius' depends on the projection of the input network.");
		System.out.println("                 E.g. for WGS84, '--radius 0.00003'[degrees] suits well. '--radius 0' will place all virtual nodes at the same place,");
		System.out.println("                 causing zero distance virtual links (for turn maneuvers).");
		System.out.println("                 default: '--radius 0.00003'");
		System.out.println("--offset NodeExpansionOffset:");
		System.out.println("                 If [mnShpFile mpShpFile] are given, turn maneuvers will be created via expanding the corresponing node");
		System.out.println("                 with virtual nodes. The option defines the offset against the position of the incident links of the");
		System.out.println("                 expanded node. The unit of 'NodeExpansionOffset' depends on the projection of the input network.");
		System.out.println("                 E.g. for WGS84, '--offset 0.0.000005'[degrees] suits well. '--offset 0' will place virtual nodes of an in- and");
		System.out.println("                 out-link pair at the same place, causing zero distance virtual u-turn links (for turn maneuvers).");
		System.out.println("                 default: '--offset 0.000005'");
		System.out.println("--uturn:         If set and if [mnShpFile mpShpFile] are given, no virtual u-turn links for an in- and out-link pair will be created.");
		System.out.println("-h, --help:      Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2009, matsim.org");
		System.out.println();
	}

	public static void main(String[] args) throws Exception {
		// example arguments:
		// usage: TeleatlasParser [OPTIONS] jcShpFile nwShpFile [srDbfFile] [mnShpFile mpShpFile] [outputDirectory]
//		String options = "--xml --shp --frc8 --frc7N --maxfrc2l 2 --minsnc 40 --radius 7.5 --offset 1 --uturn ";
//		String jcShpFile = "../../input/teleatlas/jc_zurich.shp ";
//		String nwShpFile = "../../input/teleatlas/nw_zurich.shp ";
//		String srDbfFile = "../../input/teleatlas/cheche________sr.dbf ";
//		String mnShpFile = "../../input/teleatlas/cheche________mn.shp ";
//		String mpShpFile = "../../input/teleatlas/cheche________mp.dbf ";
//		String outDir = "../../output ";
//		String str = options+jcShpFile+nwShpFile+srDbfFile+mnShpFile+mpShpFile+outDir;
//		str = str.trim();
//		args = str.split(" ");
		TeleatlasParser tp = new TeleatlasParser();
		parseArguments(tp,args);
		tp.printSetting();
		tp.convert();
		
		// TODO balmermi: more options
		// transform // -t WGS84toCH1903LV03
		// clean network // -c
		// network thinning // -t
	}
}
