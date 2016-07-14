/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.snapshot;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotWriter;

import playground.benjamin.utils.BkNumberUtils;

/**
* @author amit
*/

public class MyPositionSnapShotWriter implements SnapshotWriter {

	private BufferedWriter out = null;
	private double currentTime = -1;
	private Scenario scenario; 
	private Map<Id<Person>, Id<Link>> person2link = new HashMap<>();
	private final static Logger LOG = Logger.getLogger(MyPositionSnapShotWriter.class);

	public static enum Labels { TIME, VEHICLE, LINK_ID, DISTANCE_FROM_FROMNODE, SPEED } ;

	@Inject 
	public MyPositionSnapShotWriter(Scenario scenario) {
		this.scenario = scenario;
		String filename = scenario.getConfig().controler().getOutputDirectory()+"/agentPositions.txt";
	
		// first check if easting northing is free from any correction due to placement on 2d space
		if (scenario.getConfig().qsim().getLinkWidthForVis() !=  0. || 
				( (NetworkImpl) scenario.getNetwork() ).getEffectiveLaneWidth() != 0.) 
		{
			throw new RuntimeException("This snapshot writer is useful if plotting the positions of the vehicles in one-dimensitonal space."
					+ "Either of link width for vis in qsim or effective lane width in the network is not zero.");
		}
		
		try {
			this.out = IOUtils.getBufferedWriter(filename, true);
			String header = Labels.VEHICLE
					+ "\t" + Labels.TIME
					+ "\t" + Labels.LINK_ID
					+ "\t" + Labels.DISTANCE_FROM_FROMNODE
					+ "\t" + Labels.SPEED
					+ "\n";
			this.out.write(header);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addAgent(AgentSnapshotInfo position) {

		//don't need it
		if (position.getAgentState() == AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY) return;
		
		Tuple<Id<Link>, Double> dist_linkId = getDistanceFromFromNode(position.getId(), position.getEasting(), position.getNorthing());
		
		String buffer = position.getId().toString()
				+ "\t" + (int)this.currentTime
				+ "\t" + dist_linkId.getFirst()
				+ "\t" + dist_linkId.getSecond()
				+ "\t" + position.getColorValueBetweenZeroAndOne()
				+ "\n";
		try {
			out.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void beginSnapshot(double time) {
		this.person2link.clear();
		this.currentTime = time;
	}

	@Override
	public void endSnapshot() {
		this.currentTime = -1;
	}

	@Override
	public void finish() {
		person2link.clear();
		
		if (this.out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This should work for all types of network provided easting and northing are free from any corrections for positioning in the 2D space.
	 */
	private Tuple<Id<Link>, Double> getDistanceFromFromNode (final Id<Person> personId, final double easting, final double northing) {
		
		Id<Link> agentPrevLink = person2link.get(personId);
		Map<Id<Link>,Double> link2positions = new HashMap<>(); 
		
		for (Link l : scenario.getNetwork().getLinks().values()) {
			double distFromFromNode = Double.NaN;
			Coord fromNode = l.getFromNode().getCoord();
			Coord toNode = l.getToNode().getCoord();

			double distFromNodeAndPoint = Point2D.distance(fromNode.getX(), fromNode.getY(), easting, northing);
			double distPointAndToNode = Point2D.distance(toNode.getX(), toNode.getY(), easting, northing);
			double distFromNodeAndToNode = Point2D.distance(fromNode.getX(), fromNode.getY(), toNode.getX(), toNode.getY());

			if ( Math.abs( distFromNodeAndPoint + distPointAndToNode - distFromNodeAndToNode ) < 0.01) { // assuming link to be straight line
				// 0.01 to ignore rounding errors, In general, if AC + CB = AB => C lies on AB
				distFromFromNode = BkNumberUtils.roundDouble(distFromNodeAndPoint, 2);
				link2positions.put(l.getId(), distFromFromNode);
			}
		}
		
		//if an agent is on a node => fromNode for a link and to Node for another; store such info
//		if( link2positions.isEmpty() ) throw new RuntimeException("Easting, northing ("+ easting +","+northing +") is outside the network.");
//		else
		if(link2positions.size() == 1) 
		{
			Iterator<Entry<Id<Link>, Double>> it = link2positions.entrySet().iterator();
			return new Tuple<>(it.next().getKey(),it.next().getValue());
		} 
		else if(agentPrevLink!=null && link2positions.containsKey(agentPrevLink) ) 
		{ // if it was already on some link, then it is most likely to be at the end of the link
			return new Tuple<>(agentPrevLink, link2positions.get(link2positions));
		} 
		else 
		{ // agent is never seen before, most likely departed thus should be at the end of the link
			Iterator<Entry<Id<Link>, Double>> it = link2positions.entrySet().iterator();
			while (it.hasNext()) 
			{
				if(it.next().getValue()!=0.) 
				{
					return new Tuple<>(it.next().getKey(),it.next().getValue());
				} 
			}
		}
		if ( link2positions.isEmpty()) {
			// this is possible if storage cap =1 or linkLength = euclidien dist between nodes.
			LOG.warn("Can not dertermine the link information. Thus getting the nearest link for given easting northing.");
			Link l = NetworkUtils.getNearestLink(scenario.getNetwork(), new Coord(easting, northing));
			double dist_f_fromNode = Point2D.distance(easting, northing, l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY());
			return new Tuple<>(l.getId(),dist_f_fromNode);
		
		}
		return null;
	}
}