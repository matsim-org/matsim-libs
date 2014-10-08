/* *********************************************************************** *
 * project: org.matsim.*
 * SCAGShp2Links.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.ucsb.network.algorithms;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author balmermi
 *
 */
public class SCAGShp2Links implements NetworkRunnable {


	private final static Logger log = Logger.getLogger(SCAGShp2Links.class);
	
	private final String linkShpFile;
	private final ObjectAttributes linkObjectAttributes;

	// for link attribute description see "SCAG TransCAD Regional Model Users Guide.pdf" page 104
	private static final double MILE_2_KM = 1.609344;
	public static final String HOV = "hov";
	
	private static final String ID = "ID";
	private static final String FROM_ID = "FROM_ID";
	private static final String TOID_ID = "TO_ID";
	private static final String LENGTH = "LENGTH";
	private static final String DIR = "DIR";
	private static final String FT_LINK_TYPE = "AB_NEW_FAC";
	private static final String TF_LINK_TYPE = "BA_NEW_FAC";
	public static final String LINK_TYPE = "TYPE";
	private static final String FT_FREESPEED = "AB_POSTEDS";
	private static final String TF_FREESPEED = "BA_POSTEDS";
	
	private static final String FT_AM_LANES = "AB_AMLANES";
	private static final String TF_AM_LANES = "BA_AMLANES";
	private static final String FT_PM_LANES = "AB_PMLANES";
	private static final String TF_PM_LANES = "BA_PMLANES";
	private static final String FT_MD_LANES = "AB_MDLANES";
	private static final String TF_MD_LANES = "BA_MDLANES";
	private static final String FT_NT_LANES = "AB_NTLANES";
	private static final String TF_NT_LANES = "BA_NTLANES";
	
	private static final String MODE = "MODE";

	/**
	 * @param nodeShpFile
	 * @param nodeObjectAttributes
	 */
	public SCAGShp2Links(String linkShpFile, ObjectAttributes linkObjectAttributes) {
		super();
		this.linkShpFile = linkShpFile;
		this.linkObjectAttributes = linkObjectAttributes;
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.api.internal.NetworkRunnable#run(org.matsim.api.core.v01.network.Network)
	 */
	@Override
	public void run(Network network) {
		log.info("creating links from "+linkShpFile+" shape file...");
		int fCnt = 0;
		for (SimpleFeature f : ShapeFileReader.getAllFeatures(linkShpFile)) {
			fCnt++;
			
			// link id
			Object id = f.getAttribute(ID);
			if (id == null) { throw new RuntimeException("fCnt "+fCnt+": "+ID+" not found in feature."); }
			Id<Link> linkId = Id.create(id.toString().trim(), Link.class);
			
			// from Node
			Object fromNodeid = f.getAttribute(FROM_ID);
			if (fromNodeid == null) { throw new RuntimeException("fCnt "+fCnt+": "+FROM_ID+" not found in feature."); }
			Node fromNode = network.getNodes().get(Id.create(fromNodeid.toString().trim(), Node.class));
			
			// to Node
			Object toNodeid = f.getAttribute(TOID_ID);
			if (toNodeid == null) { throw new RuntimeException("fCnt "+fCnt+": "+TOID_ID+" not found in feature."); }
			Node toNode = network.getNodes().get(Id.create(toNodeid.toString().trim(), Node.class));
			
			// ignore, if incident nodes do not exist (connector link)
			if ((fromNode == null) || (toNode == null)) { continue; }
			
			// check if the link is a loop
			if (fromNode.getId().equals(toNode.getId())) { log.warn("fCnt "+fCnt+": link id="+linkId.toString()+" is a loop. Will be ignored..."); continue; }

			// link type (per direction)
			int typeFT = Integer.parseInt(f.getAttribute(FT_LINK_TYPE).toString().trim());
			int typeTF = Integer.parseInt(f.getAttribute(TF_LINK_TYPE).toString().trim());
			
			if ((typeFT == 100) || (typeTF == 100)) { log.info("fCnt "+fCnt+": link id="+linkId.toString()+" is a connector (even though the centroid node was not labeled correctly). Will be ignored..."); continue; }
			
			// length
			double length = Double.parseDouble(f.getAttribute(LENGTH).toString().trim()) * MILE_2_KM * 1000.0; // miles to meters

			// freespeed per direction
			double freespeedFT = Double.parseDouble(f.getAttribute(FT_FREESPEED).toString().trim()) * MILE_2_KM / 3.6; // mph to m/s
			double freespeedTF = Double.parseDouble(f.getAttribute(TF_FREESPEED).toString().trim()) * MILE_2_KM / 3.6; // mph to m/s

			// number of lanes FT (getting the largest number of the day)
			int lanesFT = Integer.parseInt(f.getAttribute(FT_AM_LANES).toString().trim());
			int tmp = Integer.parseInt(f.getAttribute(FT_MD_LANES).toString().trim());
			if (tmp > lanesFT) { lanesFT = tmp; }
			tmp = Integer.parseInt(f.getAttribute(FT_PM_LANES).toString().trim());
			if (tmp > lanesFT) { lanesFT = tmp; }
			tmp = Integer.parseInt(f.getAttribute(FT_NT_LANES).toString().trim());
			if (tmp > lanesFT) { lanesFT = tmp; }

			// number of lanes TF (getting the largest number of the day)
			int lanesTF = Integer.parseInt(f.getAttribute(TF_AM_LANES).toString().trim());
			tmp = Integer.parseInt(f.getAttribute(TF_MD_LANES).toString().trim());
			if (tmp > lanesFT) { lanesTF = tmp; }
			tmp = Integer.parseInt(f.getAttribute(TF_PM_LANES).toString().trim());
			if (tmp > lanesFT) { lanesTF = tmp; }
			tmp = Integer.parseInt(f.getAttribute(TF_NT_LANES).toString().trim());
			if (tmp > lanesFT) { lanesTF = tmp; }
			
			// main transport mode
			Set<String> modesFT = new HashSet<String>();
			Set<String> modesTF = new HashSet<String>();
			if (Integer.parseInt(f.getAttribute(MODE).toString().trim()) == 24) {
				modesFT.add(TransportMode.pt); modesTF.add(TransportMode.pt);
//					log.info("pt only link id="+id.toString()+" ignored.");
//					continue;
			}
			else {
				if (((typeFT >= 20) && (typeFT < 30)) || (typeFT == 84) || (typeFT == 85)) { modesFT.add(HOV); } else { modesFT.add(HOV); modesFT.add(TransportMode.car); }
				if (((typeTF >= 20) && (typeTF < 30)) || (typeTF == 84) || (typeTF == 85)) { modesTF.add(HOV); } else { modesTF.add(HOV); modesTF.add(TransportMode.car); }
			}
			
			// get directions
			int direction = Integer.parseInt(f.getAttribute(DIR).toString());

			if ((direction == -1) || (direction == 0)) {
				Link link = network.getFactory().createLink(Id.create(id.toString()+"TF", Link.class), toNode, fromNode);
				link.setLength(length);
				link.setFreespeed(freespeedTF);
				link.setNumberOfLanes(lanesTF);
				link.setCapacity(lanesTF * 2000.0); // TODO [balmermi] more realistic capacities
				link.setAllowedModes(modesTF);
				network.addLink(link);
				linkObjectAttributes.putAttribute(link.getId().toString(),LINK_TYPE,typeFT);
				cleanUp(link);

			}
			if ((direction == 1) || (direction == 0)) {
				Link link = network.getFactory().createLink(Id.create(id.toString()+"FT", Link.class), fromNode, toNode);
				link.setLength(length);
				link.setFreespeed(freespeedFT);
				link.setNumberOfLanes(lanesFT);
				link.setCapacity(lanesFT * 2000.0); // TODO [balmermi] more realistic capacities
				link.setAllowedModes(modesFT);
				network.addLink(link);
				linkObjectAttributes.putAttribute(link.getId().toString(),LINK_TYPE,typeTF);
				cleanUp(link);
			}
			if ((direction < -1) || (direction > 1)) { throw new RuntimeException("fCnt "+fCnt+": "+DIR+"="+direction+" of linkId="+linkId+" not known."); }
		}
		log.info("done. (creating links)");
		
		removePtLinks(network, linkObjectAttributes);
	}
	
	private final void removePtLinks(Network network, ObjectAttributes linkObjectAttributes) {
		Set<Id<Link>> linkIdsToRemove = new HashSet<>();
		for (Link l : network.getLinks().values()) {
			if ((l.getAllowedModes().size() == 1) && (l.getAllowedModes().contains(TransportMode.pt))) {
				linkIdsToRemove.add(l.getId());
			}
		}
		for (Id<Link> lid : linkIdsToRemove) {
			network.removeLink(lid);
			linkObjectAttributes.removeAllAttributes(lid.toString());
		}
		log.info(linkIdsToRemove.size()+" links with pt only removed.");
	}
	
	private final void cleanUp(Link link) {

		if (link.getAllowedModes().contains(HOV)) {
			if (link.getFreespeed() <= 0.0) {
				link.setFreespeed(65.0*MILE_2_KM/3.6);
				log.info("cleanUp linkId="+link.getId()+": setting "+HOV+" link speed from zero to "+link.getFreespeed()+".");
			}
			if (link.getCapacity() <= 0.0) {
				link.setCapacity(2000.0);
				log.info("cleanUp linkId="+link.getId()+": setting "+HOV+" link capacity from zero to "+link.getCapacity()+".");
			}
		}
		
		if (link.getFreespeed() <= 0.0) {
			link.setFreespeed(5.0/3.6);
			log.info("cleanUp linkId="+link.getId()+": setting link speed from zero to "+link.getFreespeed()+".");
		}
		if (link.getCapacity() <= 0.0) {
			link.setCapacity(2000.0);
			log.info("cleanUp linkId="+link.getId()+": setting link capacity from zero to "+link.getCapacity()+".");
		}
		if (link.getLength() <= 0.0) {
			link.setLength(7.5);
			log.info("cleanUp linkId="+link.getId()+": setting link length from zero to "+link.getLength()+".");
		}
		if (link.getNumberOfLanes() <= 0.0) {
			link.setNumberOfLanes(1.0);
			log.info("cleanUp linkId="+link.getId()+": setting link lanes from zero to "+link.getNumberOfLanes()+".");
		}
	}
}
