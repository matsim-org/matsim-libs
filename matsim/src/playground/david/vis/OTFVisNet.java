/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVisNet.java
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

package playground.david.vis;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.mobsim.snapshots.PositionInfo;
import org.matsim.plans.Plan;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.vis.netvis.DisplayableNetI;

import playground.david.vis.handler.DefaultAgentHandler;
import playground.david.vis.handler.DefaultLinkHandler;
import playground.david.vis.handler.DefaultNodeHandler;
import playground.david.vis.interfaces.OTFAgentHandler;
import playground.david.vis.interfaces.OTFLinkHandler;
import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFNodeHandler;
import playground.david.vis.interfaces.OTFParamProvider;

/* maybe
 * wrap all agents in an observableagent object
 * have list of observable objects QueueLink can update infos in observable object
 * so we do not need any memory while not watching a sim
 * otherwise put x,y into Vehicle give QueueLink an updatepositions method that only
 * gets called when vis is active
 *
 * Problems:
 * 1 we do NOT have an agent class!
 * 2 should i make link/agent/node Identified, is needed for persistent observation
 * increases interface but exchanges unreadable getParam(4) with clear getId()
 * 3 one handler for all/majority or every agent/link/node ahs its own Handler
 * on the vis side everyone has its own, because the data is stored in that class too
 * but on the serverside, data is stored in the QueueXXX classes
 *
 * CHANGES:
 * Every node/L/A should have a Reference to a datahandler.
 * This reference should be ZERO on startup
 * ONLY if the reference is overridden, we will use the overridden Handler, else some default handler (makes it easier to
 * change the default handeler on the fly)
 * Maybe we have a OTFHandable Interface including setHander getHandler
 * How about the getSrc, which was used right now to either store QueueLink or OTFLinkHndler in OFVisNet.Link
 * Maybe we move OTFVisNet.Link into Src and STORE only Handler in OTFVisNet?
 * So we have
 * OTFVisNet {
 *  Map<String, OTFLinkHandler> links;
 *  ...
 *
 */
public class OTFVisNet implements Serializable, DisplayableNetI {

	public static class DefaultHandler implements OTFNetHandler {

		public OTFAgentHandler<PositionInfo> getAgentHandler() {
			return new DefaultAgentHandler();
		}

		public OTFLinkHandler<QueueLink> getLinkHandler() {
			return new DefaultLinkHandler();
		}

		public OTFNodeHandler<QueueNode> getNodeHandler() {
			return new DefaultNodeHandler();
		}

	}

	private static final long serialVersionUID = 1L;
	public static final double zoomFactorX = 1.;
	public static final double zoomFactorY = 1.;

	protected Map<String, Node> nodes = new TreeMap<String, Node>();
	protected Map<String, Link> links = new TreeMap<String, Link>();
	transient List<OTFAgentHandler> agents = new LinkedList<OTFAgentHandler>(); //used on CLientSide only

	private double minEasting;
	private double maxEasting;
	private double minNorthing;
	private double maxNorthing;

	protected OTFNetHandler handler;

	public OTFVisNet(QueueNetworkLayer source) {

		handler = new DefaultHandler();

		for (QueueNode node : source.getNodes().values()) {
			Node nodeOTF = new Node (node);
   		CoordI coord = node.getCoord();
   		coord.setXY(coord.getX()*zoomFactorX, coord.getY()*zoomFactorY);
   		nodeOTF.setCoords(coord);
			nodes.put(node.getId().toString(), nodeOTF);
		}

		for (QueueLink link : source.getLinks().values()) {
			Link linkOTF = new Link(link);
			links.put(link.getId().toString(), linkOTF);
		}
		updateBoundingBox();

		for (Iterator<Node> it = nodes.values().iterator(); it.hasNext();) {
			Node node = it.next();
			node.setEasting(node.getEasting() - minEasting);
			node.setNorthing(node.getNorthing() - minNorthing);
		}

//		maxEasting = maxEasting - minEasting;
//		maxNorthing = maxNorthing - minNorthing;
//		minEasting = 0;
//		minNorthing = 0;

		connect();

	}

	public void updateBoundingBox(){
		minEasting = Double.POSITIVE_INFINITY;
		maxEasting = Double.NEGATIVE_INFINITY;
		minNorthing = Double.POSITIVE_INFINITY;
		maxNorthing = Double.NEGATIVE_INFINITY;

		for (Iterator<Node> it = nodes.values().iterator(); it.hasNext();) {
			Node node = it.next();
			minEasting = Math.min(minEasting, node.getEasting());
			maxEasting = Math.max(maxEasting, node.getEasting());
			minNorthing = Math.min(minNorthing, node.getNorthing());
			maxNorthing = Math.max(maxNorthing, node.getNorthing());
		}

		final double easting = maxEasting - minEasting;
		final double northing = maxNorthing - minNorthing;

	}

	public void connect() {
		for (Iterator<Node> it = nodes.values().iterator(); it.hasNext();) {
			Node node = it.next();
			node.links = new LinkedList<Link>();
		}

		for (Iterator<Link> it = links.values().iterator(); it.hasNext();) {
			Link link = it.next();
			link.to.addLink(link);
			link.from.addLink(link);
		}

		for (Iterator<Node> it = nodes.values().iterator(); it.hasNext();) {
			Node node = it.next();
			node.sortLinks();
		}
	}
	public Collection<Link> getLinks() {
		return links.values();
	}
	public Collection<Node> getNodes() {
		return nodes.values();
	}

	public synchronized List<OTFAgentHandler> getAgents() {
		return agents;
	}
	public synchronized void setAgents(List<OTFAgentHandler> newAgents) {
		agents = newAgents;
	}


	public class DisplayAgent extends org.matsim.utils.vis.netvis.visNet.DisplayAgent {
		public DisplayAgent(String id, double posInLink_m, int lane) {
			super(posInLink_m, lane);
			this.id=id;
		}

		public String id;
	}

	public class Node implements Serializable {
		private static final long serialVersionUID = 1L;
		String id;
		private double easting;
		private double northing;

		public transient Object src;
		public transient List<Link> links = new LinkedList<Link>();


		public void setCoords(CoordI coords) {
			this.easting = coords.getX();
			this.northing = coords.getY();
		}

		public synchronized double getEasting() {
			return easting;
		}

		public void addLink(Link link) {
			links.add(link);
		}


		public synchronized double getNorthing() {
			return northing;
		}

		Node(BasicNodeI source) {
			id = source.getId().toString();
			easting = ((QueueNode)source).getCoord().getX();
			northing = ((QueueNode)source).getCoord().getY();
			src = source;
		}

		public void setDisplayValue(float f) {
			// TODO Auto-generated method stub

		}

		public void setDisplayText(String string) {
			// TODO Auto-generated method stub

		}

		class ThetaComparator implements Comparator<Link>, Serializable {
			public int compare(final Link link1, final Link link2) {
				float theta1 = theta(easting, northing, link1.getMiddleEasting(), link1.getMiddleNorthing());
				float theta2 = theta(easting, northing, link2.getMiddleEasting(), link2.getMiddleNorthing());

				if (theta1 > theta2)
					return 1;
				if (theta1 < theta2)
					return -1;
				return 0;
			}
		}

       	public float theta(double x1, double y1, double x2, double y2) {
    		float t = 0.f;
    		double dx = x2 - x1;
    		double dy = y2 - y1;

    		if (dx == 0 && dy == 0 ) t = 0.f;
    		else t = (float)(dy/(Math.abs(dx) + Math.abs(dy)));

    		if (dx <0) t = 2-t;
    		else if (dy < 0 ) t = 4+t;
    		return t*90.f;
    	}

		public void sortLinks() {
	       Collections.sort(links, new ThetaComparator());
		}

		public List<Link> getLinks() {
			return links;
		}

		public QueueNode getSrc() {return (QueueNode)src;}

		public void setSrc(OTFNodeHandler data) {
			src = data;

		}

		public void setEasting(double easting) {
			this.easting = easting;
		}

		public void setNorthing(double northing) {
			this.northing = northing;
		};

	}

	public class Agent implements Serializable {
		private String id;
		private float x;
		private float y;
		private int state;

		public float getX() {
			return x;
		}
		public void setX(float x) {
			this.x = x;
		}
		public float getY() {
			return y;
		}
		public void setY(float y) {
			this.y = y;
		}
		public int getState() {
			return state;
		}
		public void setState(int state) {
			this.state = state;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}

	}
	public class Link implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		String id;
		protected Node from = null;
		protected Node to = null;
		transient Object src;
		public final static double laneWidth = 20;


		float displValue = 0;
		int lanes = 1;

		double nodeDist;
		boolean isVisible = false;
		transient private final AffineTransform linear2PlaneTransform = null;

		Link(BasicLinkI source) {
			id = source.getId().toString();
			from = nodes.get(source.getFromNode().getId().toString());
			to = nodes.get(source.getToNode().getId().toString());
			src = source;
			lanes = getSrc().getLanes();

			final double deltaNorthing = getEndNorthing() - getStartNorthing();
			final double deltaEasting = getEndEasting() - getStartEasting();
			double result = deltaNorthing * deltaNorthing;
			result += deltaEasting * deltaEasting;
			nodeDist = Math.sqrt(result);
	        //linear2PlaneTransform = newLinear2PlaneTransform(0., nodeDist);

		}

		public double getMiddleEasting() {
			final double deltaEasting = getEndEasting() - getStartEasting();
			return getStartEasting() + deltaEasting/2;
		}

		public double getMiddleNorthing() {
			final double deltaNorthing = getEndNorthing() - getStartNorthing();
			return getStartNorthing() + deltaNorthing/2;
		}

		public String getId() {
			return id;
		}

		public Node getFromNode() {
			return from;
		}

		public Node getToNode() {
			return to;
		}


		public void setDisplayValue(float f) {
			displValue = f;
		}

		public void setDisplayLabel(String string) {
			id = string;
		}

		public double getStartEasting() {
			return getFromNode().getEasting();
		}

		public double getEndEasting() {
			return getToNode().getEasting();
		}

		public double getStartNorthing() {
			return getFromNode().getNorthing();
		}

		public double getEndNorthing() {
			return getToNode().getNorthing();
		}

		public double getNodeDist() {
			return nodeDist;
		}

//		public AffineTransform getLinear2PlaneTransform() {
//			return linear2PlaneTransform;
//		}

		public int getLanes() {
			return lanes;
		}

		public double getLength_m() {
			return nodeDist;
		}

		public String getDisplayText() {
			return id.toString();
		}

	  private AffineTransform newLinear2PlaneTransform(double offset_m, double displayedLength_m) {

		        // 3. translate link onto original position
		        double tx = getStartEasting();
		        double ty = getStartNorthing();
		        AffineTransform result = AffineTransform.getTranslateInstance(tx, ty);

		        // 2. rotate link into original direction
		        double dx = getEndEasting() - getStartEasting();
		        double dy = getEndNorthing() - getStartNorthing();
		        double theta = Math.atan2(dy, dx);
		        result.rotate(theta);

		        // 1. scale link
		        double sx = displayedLength_m / getLength_m();
		        double sy = 1;
		        result.scale(sx, sy);

		        // 0. translate link by target offset
		        tx = offset_m * getLength_m() / displayedLength_m;
		        ty = 0;
		        result.translate(tx, ty);

		        // result = 3.translate o 2.rotate o 1.scale o 0.translate
		        return result;
		    }


		public synchronized boolean isVisible() {
			return isVisible;
		}


		public synchronized void setVisible(boolean isVisible) {
			this.isVisible = isVisible;
		}


		public QueueLink getSrc() {
			return (QueueLink)src;
		}

		public void setSrc(OTFLinkHandler data) {
			src = data;
		}
	}

	/*
	 * for the DisplayablbeNetI Interface
	 */

    public double minEasting() {
        return minEasting;
    }

    public double maxEasting() {
        return maxEasting;
    }

    public double minNorthing() {
        return minNorthing;
    }

    public double maxNorthing() {
        return maxNorthing;
    }


    /*
     * reading and writing to a bytestream
     *
     */

	public final void readAgents(DataInputStream in) throws IOException {
		List<OTFAgentHandler> newAgents = new ArrayList<OTFAgentHandler>();

		int agentCnt = in.readInt();
		while (agentCnt != 0) {
			for (int i = 0; i < agentCnt; i++) {
				OTFAgentHandler agent;
				agent = handler.getAgentHandler();
				agent.readAgent(in);
				newAgents.add(agent);
			}
			agentCnt = in.readInt();
		};

		setAgents(newAgents);
	}


	  /**
     * Not to be called by extending classes.
     */
	transient Collection<PositionInfo> positions = new ArrayList<PositionInfo>();


	public void writeLinkAgents(QueueLink link, OTFAgentHandler<PositionInfo> agentHandler, DataOutputStream out) throws IOException {
        /*
         * (4) write agents
         */
        positions.clear();
		link.getVehiclePositions(positions);
		if (positions.size() == 0) return;

		out.writeInt(positions.size());

		if (agentHandler != null)
			for (PositionInfo pos : positions) {
			agentHandler.writeAgent(pos, out);
		}
	}


    public final void readNode(OTFVisNet.Node displNode, DataInputStream in) throws IOException {
		in.readInt();
		displNode.setDisplayValue(in.readFloat());
		displNode.setDisplayText(in.readUTF());
		//displNode.links = new LinkedList<Link>();
	}



    public void readMyself(DataInputStream in) throws IOException {

    	//in.reset();

		for (OTFVisNet.Node node : nodes.values())
			if (node != null) {
				OTFNodeHandler data = handler.getNodeHandler();
				data.readNode(in);
				node.setSrc(data);
			}

		for (OTFVisNet.Link link : links.values())
			if (link != null) {
				OTFLinkHandler data = handler.getLinkHandler();
				data.readLink(in);
				link.setSrc(data);
			}

			readAgents(in);

		return;
	}

    public void writeAgents(DataOutputStream out) throws IOException {
		OTFAgentHandler<PositionInfo> agentHandler = handler.getAgentHandler();
		agentHandler.setOTFNet(this);

		for (OTFVisNet.Link link : links.values())
			if (link != null) {
				writeLinkAgents(link.getSrc(), agentHandler, out);
			}
    }

    /*
     * Here i try using a bitmask. Takes 5ms to build for 27k net. 1ms to read. Is about 3.5K
     * this is the best solution
     */
    BitSet actualMask =new BitSet(links.values().size());
	BitSet newMask = new BitSet(links.values().size());
    
    public void buildWriteMask() {
    	int count = 0;
		Gbl.startMeasurement();

		newMask.clear();
		
		for (OTFVisNet.Link link : links.values())
			if (link != null) {
				newMask.set(count++, link.getSrc().hasDrivingCars());
			}
		boolean test = false;
		while (count > 0 ) 
		{
			test =	newMask.get(--count);
		}
		// if one links has been alive last Tick but is not alive now we need to trasfer the new "dead" state one time
		// so for transfering we have to pimp the mask some more
		BitSet xorMask = (BitSet)newMask.clone();
		xorMask.xor(actualMask); // gives us a true for all states that have changed
		xorMask.and(actualMask); // we want only changes from true to false
		actualMask = (BitSet)newMask.clone(); // save actualMask without EXTRA links for next timestep
		// Build mask that could be used for transfering things, i.e. add the true's from xorMask
		newMask.or(xorMask);
		
		Gbl.printElapsedTime();

		System.out.println("...for buildin bitfield with link count " + links.values().size() + " and mask size of " + newMask.size()/8);
    }
    
    /* 
     * this was a try to send "chunks" of enables and disabled links so
     * for a disabled link-chunk i only have to send the number of disabled chunks and then I am finished
     * unfortunately it turns out that on a 27K link network for morning peak I get roughly 8K chunks, which means
     * i have to transfer at least one Status byte and 8K*4byte (for integer) of size information for every second
     * That is 16k Byte, still less than 27K but not so much concerning the increased level of complexity
     * I save for now sending one float (4byte) value per disabled link == 20K*4 =    
     */
    public void buildWriteMaskOLD() {
		int count = 0;
		int disabled = 0;
		
		Gbl.startMeasurement();

		List<Boolean> statusList = new LinkedList<Boolean>();
		List<Integer> countList = new LinkedList<Integer>();
		
		boolean stateActive = false; 
		for (OTFVisNet.Link link : links.values())
			if (link != null) {
				if (count == 0) stateActive = link.getSrc().hasDrivingCars();
				// Check if activity status has changed
				if(link.getSrc().hasDrivingCars() != stateActive){
//					System.out.println("Count activition cluster = " + count + " are " + stateActive);
					statusList.add(stateActive);
					countList.add(count);
					count = 1;
					stateActive = link.getSrc().hasDrivingCars();
					if(stateActive == false) disabled++;
				} else {
					count++;
					if(stateActive == false) disabled++;
				}
			}
   
		Gbl.printElapsedTime();

		System.out.println("Count activition clusters = " + countList.size() + " link count " + links.values().size() +" numer of disabled " + disabled);
}
    
    public void writeMyself(OTFNetHandler newHandler, DataOutputStream out) throws IOException {
    	if (newHandler != null) this.handler = newHandler;

		OTFNodeHandler<QueueNode> nodeHandler = handler.getNodeHandler();
		OTFLinkHandler<QueueLink> linkHandler = handler.getLinkHandler();

		if( nodeHandler != null) {
			for (OTFVisNet.Node node : nodes.values()) {
				if (node != null) {
					nodeHandler.writeNode(node.getSrc(), out);
				}
			}
		}

		buildWriteMask();
		
		if( linkHandler != null) {
			for (OTFVisNet.Link link : links.values()) {
				if (link != null) {
					linkHandler.writeLink(link.getSrc(), out);
				}
			}
		}

		writeAgents(out);

		out.writeInt(0); // terminate agent reading by a zero

		return;
	}

    transient public Point2D.Double lastClicked = null;
    transient public Point2D.Double pos = new Point2D.Double();

    public Link lastLink = null;

    public List<String> selectedAgents = new ArrayList<String>();

	public String getAgentId(Point2D.Double p) {
		String result = new String("");
		Link link = findNearestLink(p);
		if (link != null) {
			// Berechne projektion des punktes auf den link
			double distStart = p.distance(link.from.easting,link.from.northing);
			double distEnd = p.distance(link.to.easting, link.to.northing);
			double relPos = distStart / (distStart + distEnd);
			relPos *= link.nodeDist;
			double bestPos = Double.MAX_VALUE;
			OTFParamProvider bestMatch = null;
			pos = new Point2D.Double();

			// vergleiche mit pos des agent auf dem link
     		for(OTFAgentHandler agentH :  agents) {
     			OTFParamProvider agent = (OTFParamProvider) agentH;
    			int xPosIndex = agent.getIndex("PosX");
     			int yPosIndex = agent.getIndex("PosY");
     			int IdIndex = agent.getIndex("Id");
				double dist = p.distance(agent.getFloatParam(xPosIndex), agent.getFloatParam(yPosIndex));
				if (dist < bestPos ) {
					bestPos = dist;
					bestMatch = agent;
					result = bestMatch.getStringParam(IdIndex);
				}
			}

			if(bestMatch != null) {
				selectedAgents.clear();
				selectedAgents.add(result);
			}
		}
		lastClicked = p;
		lastLink = link;
		// return best match
		System.out.println("Clicked on Agent#: " + result);
		return result;
	}

	// DS TODO There are normally TWO neaarest links, one with the reverse direction
	// right now we will find one of them only, leading to falsely choosen agents
	public OTFVisNet.Link findNearestLink(Point2D p) {
		OTFVisNet.Link result = null;
		Line2D line = new Line2D.Double();
		double dist = 0;
		double minDist = Double.MAX_VALUE;

		for (OTFVisNet.Link link : links.values())
			if (link != null && link.isVisible) {
				line.setLine(link.from.easting, link.from.northing, link.to.easting, link.to.northing);
				dist = Math.abs(line.ptSegDist(p));
//				double distStart = p.distance(link.from.easting,link.from.northing);
//				double distEnd = p.distance(link.to.easting, link.to.northing);
//				dist= Math.min(distEnd,distStart);
				if (dist < minDist) {
					result = link;
					minDist = dist;
				}
			}
		return result;
	}

	public void convertToDipsplayList(Plan plan) {
		// just male the nodes in a color of choice first
	}

	public Node getNode(String id) {
		return nodes.get(id);
	}

	public Link getLink(String id) {
		return links.get(id);
	}


}
