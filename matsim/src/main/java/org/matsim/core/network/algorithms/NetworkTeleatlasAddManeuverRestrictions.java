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

package org.matsim.core.network.algorithms;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;
import org.matsim.core.network.io.NetworkReaderTeleatlas;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.io.IOUtils;

/**
 * Adds maneuver restrictions to a MATSim {@link Network network} created
 * by {@link NetworkReaderTeleatlas}. The input maneuver shape file and the maneuver path DBF file
 * is based on <strong>Tele Atlas MultiNet Shapefile 4.3.2.1 Format Specifications
 * document version Final v1.0, June 2007</strong>.
 *
 * @author balmermi
 */
public final class NetworkTeleatlasAddManeuverRestrictions implements NetworkRunnable {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = LogManager.getLogger(NetworkTeleatlasAddManeuverRestrictions.class);

	/**
	 * path and name to the Tele Atlas MultiNet maneuver (mn) Shape file
	 */
	private final String mnShpFileName; // teleatlas maneuvers shape file name

	/**
	 * path and name to the Tele Atlas MultiNet maneuver path (mp) DBF file
	 */
	private final String mpDbfFileName; // teleatlas maneuver paths dbf file name

	/**
	 * option flag: if set, expanded {@link Node nodes} (nodes that contains maneuver restrictions)
	 * will not include new virtual {@link Link links} providing a u-turn maneuver at the
	 * junction.
	 */
	public boolean removeUTurns = false;

	/**
	 * option parameter: defines the radius how much a {@link Node node} will be expanded.
	 * <p><b>Default:</b> <code>0.000030</code> (appropriate for world coordinate system WGS84)</p>
	 *
	 * @see NetworkExpandNode
	 */
	public double expansionRadius = 0.000030; // WGS84

	/**
	 * option parameter: defines the offset of a in- and out-link pair of an expanded {@link Node node}.
	 * <p><b>Default:</b> <code>0.000005</code> (appropriate for world coordinate system WGS84)</p>
	 *
	 * @see NetworkExpandNode
	 */
	public double linkSeparation = 0.000005; // WGS84

	private static final String MN_ID_NAME = "ID";
	private static final String MN_FEATTYP_NAME = "FEATTYP";
	private static final String MN_JNCTID_NAME = "JNCTID";

	private static final String MP_ID_NAME = "ID";
	private static final String MP_SEQNR_NAME = "SEQNR";
	private static final String MP_TRPELID_NAME = "TRPELID";

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	/**
	 * To create maneuver restrictions to a Tele Atlas MultiNet {@link Network network}.
	 *
	 * @param mnShpFileName Tele Atlas MultiNet maneuver Shape file
	 * @param mpDbfFileName Tele Atlas MultiNet maneuver path DBF file
	 */
	public NetworkTeleatlasAddManeuverRestrictions(final String mnShpFileName, final String mpDbfFileName) {
		log.info("init " + this.getClass().getName() + " module...");
		this.mnShpFileName = mnShpFileName;
		this.mpDbfFileName = mpDbfFileName;
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	/**
	 * Reading and assigning (expanding {@link Node nodes}) maneuver restrictions to the {@link Network network}.
	 *
	 * <p>It uses the following attributes from the Tele Atlas MultiNet maneuver Shape file:
	 * <ul>
	 *   <li>{@link #MN_ID_NAME} (Feature Identification)</li>
	 *   <li>
	 *     {@link #MN_FEATTYP_NAME} (Feature Type)
	 *     <ul>
	 *       <li>9401: Bifurcation</li>
	 *       <li>2104: Priority Maneuver</li>
	 *       <li>2103: Prohibited Maneuver</li>
	 *       <li>2102: Restricted Maneuver</li>
	 *       <li>2101: Calculated/Derived Prohibited Maneuver</li>
	 *     </ul>
	 *   </li>
	 *   <li>{@link #MN_JNCTID_NAME} (Junction Identification of the Location of the Maneuver Sign)</li>
	 * </ul></p>
	 * <p>And it uses the following attributes from the Tele Atlas MultiNet maneuver path DBF file:
	 * <ul>
	 *   <li>{@link #MP_ID_NAME} (Feature Identification)</li>
	 *   <li>{@link #MP_SEQNR_NAME} (Transportation Element Sequential Number within the Maneuver)</li>
	 *   <li>{@link #MP_TRPELID_NAME} (Transportation Element Identification)</li>
	 * </ul></p>
	 * <p><b>Conversion rules:</b>
	 * <ul>
	 *   <li>The maneuver types 'Bifurcation' (9401) and 'Priority Maneuver' (2104) are ignored</li>
	 *   <li>Maneuver restriction sequences that contain links that are not incident to the given
	 *   junction are ignored</li>
	 *   <li>Maneuver restrictions will be modelled in the natwork topology by expanding
	 *   {@link Node nodes} with {@link NetworkExpandNode node expanding procedure}.</li>
	 * </ul></p>
	 *
	 * @param network MATSim {@link Network network} created by {@link NetworkReaderTeleatlas}.
	 * @throws RuntimeException with another Exception in it in the case something goes wrong
	 */
	@Override
	public void run(final Network network) {
		try {
			run2(network);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void run2(final Network network) throws Exception {
		log.info("running " + this.getClass().getName() + " module...");
		NetworkExpandNode neModule = new NetworkExpandNode(network, expansionRadius, this.linkSeparation);

		TreeMap<String, TreeMap<Integer,Id<Link>>> mSequences = new TreeMap<>();

		try (FileInputStream fis = new FileInputStream(this.mpDbfFileName)) {
			DbaseFileReader r = new DbaseFileReader(fis.getChannel(), true, IOUtils.CHARSET_WINDOWS_ISO88591);
			// get header indices
			int mpIdNameIndex = -1;
			int mpSeqNrNameIndex = -1;
			int mpTrpelIDNameIndex = -1;
			for (int i=0; i<r.getHeader().getNumFields(); i++) {
				if (r.getHeader().getFieldName(i).equals(MP_ID_NAME)) { mpIdNameIndex = i; }
				if (r.getHeader().getFieldName(i).equals(MP_SEQNR_NAME)) { mpSeqNrNameIndex = i; }
				if (r.getHeader().getFieldName(i).equals(MP_TRPELID_NAME)) { mpTrpelIDNameIndex = i; }
			}
			if (mpIdNameIndex < 0) { throw new NoSuchFieldException("Field name '"+MP_ID_NAME+"' not found."); }
			if (mpSeqNrNameIndex < 0) { throw new NoSuchFieldException("Field name '"+MP_SEQNR_NAME+"' not found."); }
			if (mpTrpelIDNameIndex < 0) { throw new NoSuchFieldException("Field name '"+MP_TRPELID_NAME+"' not found."); }
			log.trace("  FieldName-->Index:");
			log.trace("    "+MP_ID_NAME+"-->"+mpIdNameIndex);
			log.trace("    "+MP_SEQNR_NAME+"-->"+mpSeqNrNameIndex);
			log.trace("    "+MP_TRPELID_NAME+"-->"+mpTrpelIDNameIndex);

			// create mp data structure
			// TreeMap<mpId,TreeMap<mpSeqNr,linkId>>
			log.info("  parsing meneuver paths dbf file...");
			while (r.hasNext()) {
				Object[] entries = r.readEntry();
				String mpId = entries[mpIdNameIndex].toString();
				int mpSeqNr = Integer.parseInt(entries[mpSeqNrNameIndex].toString());
				Id<Link> linkId = Id.create(entries[mpTrpelIDNameIndex].toString(), Link.class);
				TreeMap<Integer,Id<Link>> mSequence = mSequences.get(mpId);
				if (mSequence == null) {
					mSequence = new TreeMap<>();
					mSequences.put(mpId, mSequence);
				}
				if (mSequence.put(mpSeqNr,linkId) != null) {
					fis.close();
					throw new IllegalArgumentException(MP_ID_NAME+"="+mpId+": "+MP_SEQNR_NAME+" "+mpSeqNr+" already exists.");
				}
			}
			log.info("    "+mSequences.size()+" maneuvers sequences stored.");
			log.info("  done.");
			r.close();
		}

		// store the maneuver list of the nodes
		// TreeMap<NodeId,ArrayList<Tuple<MnId,MnFeatType>>>
		log.info("  parsing meneuver shape file...");
		TreeMap<Id<Node>, ArrayList<Tuple<String, Integer>>> maneuvers = new TreeMap<>();
		SimpleFeatureSource fs = GeoFileReader.readDataFile(this.mnShpFileName);
		SimpleFeatureIterator fIt = fs.getFeatures().features();
		while (fIt.hasNext()) {
			SimpleFeature f = fIt.next();
			int featType = Integer.parseInt(f.getAttribute(MN_FEATTYP_NAME).toString());
			if ((featType == 2103) || (featType == 2102) || (featType == 2101)) {
				// keep 'Prohibited Maneuver' (2103), 'Restricted Maneuver' (2102) and 'Calculated/Derived Prohibited Maneuver' (2101)
				Id<Node> nodeId = Id.create(f.getAttribute(MN_JNCTID_NAME).toString(), Node.class);
				ArrayList<Tuple<String, Integer>> ms = maneuvers.get(nodeId);
				if (ms == null) {
					ms = new ArrayList<>();
				}
				Tuple<String, Integer> m = new Tuple<>(f.getAttribute(MN_ID_NAME).toString(), featType);
				ms.add(m);
				maneuvers.put(nodeId, ms);
			}
			else if ((featType == 9401) || (featType == 2104)) {
				//ignore 'Bifurcation' (9401) and 'Priority Maneuver' (2104)
			}
			else {
				throw new IllegalArgumentException("mnId="+f.getAttribute(MN_ID_NAME)+": "+MN_FEATTYP_NAME+"="+featType+" not known.");
			}
		}
		fIt.close();
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
		for (Map.Entry<Id<Node>, ArrayList<Tuple<String, Integer>>> entry : maneuvers.entrySet()) {
			Id<Node> nodeId = entry.getKey();
			if (network.getNodes().get(nodeId) == null) {
				log.trace("  nodeid="+nodeId+": maneuvers exist for that node but node is missing. Ignoring and proceeding anyway...");
				nodesIgnoredCnt++;
			} else {
				// node found
				Node n = network.getNodes().get(nodeId);
				// init maneuver matrix
				// TreeMap<fromLinkId,TreeMap<toLinkId,turnAllowed>>
				TreeMap<Id<Link>, TreeMap<Id<Link>, Boolean>> mmatrix = new TreeMap<>();
				// assign maneuvers for given node to the matrix
				ArrayList<Tuple<String, Integer>> ms = entry.getValue();
				for (Tuple<String, Integer> m : ms) {
					// get maneuver path sequence for given maneuver
					TreeMap<Integer,Id<Link>> mSequence = mSequences.get(m.getFirst());
					if (mSequence == null) { throw new Exception("nodeid="+nodeId+"; mnId="+m.getFirst()+": no maneuver sequence given."); }
					if (mSequence.size() < 2) { throw new Exception("nodeid="+nodeId+"; mnId="+m.getFirst()+": mSequenceSize="+mSequence.size()+" not alowed!"); }
					// get the first element of the sequence, defining the start link for the maneuver
					Id<Link> firstLinkid = mSequence.values().iterator().next();
					// go through each other element (target link of the maneuver) of the sequence by sequence number
					for (Id<Link> otherLinkId : mSequence.values()) {
						// get the start link and the target link of the maneuver
						Link inLink = n.getInLinks().get(Id.create(firstLinkid+"FT", Link.class));
						if (inLink == null) {
							inLink = n.getInLinks().get(Id.create(firstLinkid+"TF", Link.class));
						}
						Link outLink = n.getOutLinks().get(Id.create(otherLinkId+"FT", Link.class));
						if (outLink == null) {
							outLink = n.getOutLinks().get(Id.create(otherLinkId+"TF", Link.class));
						}
						if ((inLink != null) && (outLink != null)) {
							// start and target link found and they are incident to the given node
							if (m.getSecond() == 2102) {
								// restricted maneuver: given start and target link path is allowed to drive
								// store it to the matrix
								TreeMap<Id<Link>, Boolean> outLinkMap = mmatrix.get(inLink.getId());
								if (outLinkMap == null) {
									outLinkMap = new TreeMap<>();
								}
								outLinkMap.put(outLink.getId(), Boolean.TRUE);
								mmatrix.put(inLink.getId(),outLinkMap);
							}
							else {
								// prohibited maneuver: given start and target link path is not allowed to drive
								// store it to the matrix
								TreeMap<Id<Link>,Boolean> outLinkMap = mmatrix.get(inLink.getId());
								if (outLinkMap == null) {
									outLinkMap = new TreeMap<>();
								}
								outLinkMap.put(outLink.getId(), Boolean.FALSE);
								mmatrix.put(inLink.getId(),outLinkMap);
							}
							maneuverAssignedCnt++;
						}
						else { maneuverIgnoredCnt++; }
					}
				}
				// complete the matrix
				for (TreeMap<Id<Link>, Boolean> fromLinkEntry : mmatrix.values()) {
					// detect inlinks with restricted maneuvers
					boolean hasRestrictedManeuver = false;
					for (Boolean b : fromLinkEntry.values()) {
						if (b) { hasRestrictedManeuver = true; }
					}
					// add missing toLink maneuvers
					for (Id<Link> toLinkId : n.getOutLinks().keySet()) {
						if (!fromLinkEntry.containsKey(toLinkId)) {
							fromLinkEntry.put(toLinkId, !hasRestrictedManeuver);
						}
					}
				}
				// add allowed maneuvers for fromLinks which were not assigned yet.
				for (Id<Link> fromLinkId : n.getInLinks().keySet()) {
					if (!mmatrix.containsKey(fromLinkId)) {
						mmatrix.put(fromLinkId, new TreeMap<Id<Link>, Boolean>());
						for (Id<Link> toLinkId : n.getOutLinks().keySet()) {
							mmatrix.get(fromLinkId).put(toLinkId, Boolean.TRUE);
						}
					}
				}
				// remove all U-turns from the matrix
				if (this.removeUTurns) {
					for (Id<Link> fromLinkId : n.getInLinks().keySet()) {
						String str1 = fromLinkId.toString().substring(0,fromLinkId.toString().length()-2);
						for (Id<Link> toLinkId : n.getOutLinks().keySet()) {
							String str2 = toLinkId.toString().substring(0,toLinkId.toString().length()-2);
							if (str1.equals(str2)) {
								mmatrix.get(fromLinkId).put(toLinkId, Boolean.FALSE);
							}
						}
					}
				}
				// create arraylist with turn tuples
				ArrayList<TurnInfo> turns = new ArrayList<>();
				for (Map.Entry<Id<Link>, TreeMap<Id<Link>, Boolean>> fromLinkEntry : mmatrix.entrySet()) {
					Id<Link> fromLinkId = fromLinkEntry.getKey();
					for (Map.Entry<Id<Link>, Boolean> toLinkEntry : fromLinkEntry.getValue().entrySet()) {
						if (toLinkEntry.getValue()) {
							turns.add(new TurnInfo(fromLinkId, toLinkEntry.getKey()));
						}
					}
				}
				// expand the node
				Tuple<List<Node>, List<Link>> t = neModule.expandNode(nodeId, turns);
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

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * prints the variable settings to the STDOUT
	 *
	 * @param prefix a prefix for each line of the STDOUT
	 */
	public final void printInfo(final String prefix) {
		System.out.println(prefix+"configuration of "+this.getClass().getName()+":");
		System.out.println(prefix+"  options:");
		System.out.println(prefix+"    removeUTurns:    "+removeUTurns);
		System.out.println(prefix+"    expansionRadius: "+expansionRadius);
		System.out.println(prefix+"    linkSeparation:  "+linkSeparation);
		System.out.println(prefix+"  maneuver shape:");
		System.out.println(prefix+"    mnShpFileName:   "+mnShpFileName);
		System.out.println(prefix+"    MN_ID_NAME:      "+MN_ID_NAME);
		System.out.println(prefix+"    MN_FEATTYP_NAME: "+MN_FEATTYP_NAME);
		System.out.println(prefix+"    MN_JNCTID_NAME:  "+MN_JNCTID_NAME);
		System.out.println(prefix+"  maneuver path dbf:");
		System.out.println(prefix+"    mpDbfFileName:   "+mpDbfFileName);
		System.out.println(prefix+"    MP_ID_NAME:      "+MP_ID_NAME);
		System.out.println(prefix+"    MP_SEQNR_NAME:   "+MP_SEQNR_NAME);
		System.out.println(prefix+"    MP_TRPELID_NAME: "+MP_TRPELID_NAME);
		System.out.println(prefix+"done.");
	}
}
