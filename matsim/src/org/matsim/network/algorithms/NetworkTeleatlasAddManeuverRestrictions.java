/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkTeleatlasAddManeuverRestrictions.java
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

package org.matsim.network.algorithms;

import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.feature.Feature;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.collections.Tuple;
import org.matsim.utils.gis.ShapeFileReader;

public class NetworkTeleatlasAddManeuverRestrictions {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(NetworkTeleatlasAddManeuverRestrictions.class);
	
	private final NetworkExpandNode neModule = new NetworkExpandNode();

	private final String mnShpFileName; // teleatlas maneuvers shape file name
	private final String mpDbfFileName; // teleatlas maneuver paths dbf file name
	
	public boolean removeUTurns = false;
	public double expansionRadius = 0.000030; // WGS84
	public double linkSeparation = 0.000005; // WGS84

	public final String mnIdName = "ID";
	public final String mnFeatTypeName = "FEATTYP";
	public final String mnJnctIdName = "JNCTID";
	
	public final String mpIdName = "ID";
	public final String mpSeqNrName = "SEQNR";
	public final String mpTrpelIDName = "TRPELID";

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkTeleatlasAddManeuverRestrictions(final String mnShpFileName, final String mpDbfFileName) {
		log.info("init " + this.getClass().getName() + " module...");
		this.mnShpFileName = mnShpFileName;
		this.mpDbfFileName = mpDbfFileName;
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final NetworkLayer network) throws Exception {
		log.info("running " + this.getClass().getName() + " module...");
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
		if (mpIdNameIndex < 0) { throw new NoSuchFieldException("Field name '"+mpIdName+"' not found."); }
		if (mpSeqNrNameIndex < 0) { throw new NoSuchFieldException("Field name '"+mpSeqNrName+"' not found."); }
		if (mpTrpelIDNameIndex < 0) { throw new NoSuchFieldException("Field name '"+mpTrpelIDName+"' not found."); }
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
				Tuple<ArrayList<Node>,ArrayList<Link>> t = neModule.expandNode(network,nodeId,turns,expansionRadius,linkSeparation);
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
		log.info("done.");
	}
}
