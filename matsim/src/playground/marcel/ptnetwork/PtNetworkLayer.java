/* *********************************************************************** *
 * project: org.matsim.*
 * PtNetworkLayer.java
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

package playground.marcel.ptnetwork;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Route;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.misc.Time;

public class PtNetworkLayer extends NetworkLayer implements LeastCostPathCalculator{

	private final static double PEDESTRIAN_SPEED = 1.5; // [m/s]
	public final static String PEDESTRIAN_TYPE = "P";

	private int CHANGE_COST_MULTIPLIER = 300;

	private final PtInternalNetwork intnet = new PtInternalNetwork();

	private QuadTree<PtNode> pedNodeQuadTree = null;
	private long dijkstraCounter = Long.MIN_VALUE;
	private int idCounter = 1000000;

	public PtNetworkLayer(){
		super();
	}
	public PtNetworkLayer(final int costMultiplier){
		super();
		this.CHANGE_COST_MULTIPLIER = costMultiplier;
	}

//	@Override TODO [MR] change to NetworkFactory when used again. Below too!
	protected Node newNode(final String id, final String x, final String y, final String type) {
		return new PtNode(id,x,y,type);
	}
//	@Override _TODO change to NetworkFactory when used again. 
	protected Link newLink(final NetworkLayer network, final String id, final Node from, final Node to,
			 final String length, final String freespeed, final String capacity, final String permlanes,
			 final String origid, final String type) {
		return new PtLink(this, id, from, to, length, freespeed, capacity, permlanes, origid, type);
	}

	public void setCostMultiplier(final int costMultiplier){
		this.CHANGE_COST_MULTIPLIER = costMultiplier;
	}


	public ArrayList<PtNode> getNearestPedNodes(final Coord coord) {
		// this method is nowhere used, and with the QuadTree could be implemented
		// much faster than it is at the moment. remove?
		ArrayList<PtNode> nearestNodes = new ArrayList<PtNode>();
		double shortestDistance = Double.MAX_VALUE;
		for (Iterator<Node> it = this.nodes.values().iterator(); it.hasNext(); ) {
			PtNode node = (PtNode)it.next();
			if (PEDESTRIAN_TYPE.equals(node.getType())) {
				double distance = node.getCoord().calcDistance(coord);
				if (distance < shortestDistance) {
					shortestDistance = distance;
					nearestNodes.clear();
					nearestNodes.add(node);
				} else if (distance == shortestDistance) {
					nearestNodes.add(node);
				}
			}
		}
		return nearestNodes;
	}

	public void buildfromBERTA(final String folder, final String coordFile, final boolean byCoord, final String netfile){
		this.intnet.buildInternalNetwork(folder,this.idCounter);

		if (byCoord) {
			this.intnet.readCoordFileByCoord(coordFile);
		} else {
			this.intnet.readCoordFileById(coordFile);
		}

		this.intnet.createHbs();
		this.idCounter=this.intnet.getIdCnt();

		if (netfile != null) {
			this.intnet.addVISUMNetwork(netfile,this.idCounter);
			this.idCounter=this.intnet.getIdCnt();
		}

		this.intnet.writeToNetworkLayer(this,this.idCounter);
		this.idCounter=this.intnet.getIdCnt();
		this.handleDeadEnds(this.getDeadEnds());
	}

	public boolean writeToVISUM(final String netfile){
		boolean written = false;
		if(!this.intnet.lines.isEmpty()){
			this.intnet.writeToVISUM(netfile);
			written=true;
		}
		return written;
	}
	public void buildfromVISUM(final String netfile,final String netfile2){
		this.intnet.parseVISUMNetwork(netfile);
		if (netfile2 != null) {
			this.intnet.addVISUMNetwork(netfile2,this.idCounter);
			this.idCounter=this.intnet.getIdCnt();
		}
		this.intnet.writeToNetworkLayer(this,this.idCounter);
		this.idCounter=this.intnet.getIdCnt();
	}

	public ArrayList<PtNode> getDeadEnds(){
		ArrayList<PtNode> deads= new ArrayList<PtNode>();
		for (Iterator<Node> it = this.nodes.values().iterator();it.hasNext();){
			PtNode node = (PtNode) it.next();
			if(PEDESTRIAN_TYPE.equals(node.getType())) {
				int totalin=0;
				int totalout=0;
				for(Iterator<? extends Link> it2 = node.getOutLinks().values().iterator();it2.hasNext();){
					PtLink link = (PtLink)it2.next();
					PtNode node2 = (PtNode) link.getToNode();
					totalin+=(node2.getInLinks().size()-1);
					totalout+=(node2.getOutLinks().size()-1);
				}
				if((totalout==0)||(totalin==0)){
					for(Iterator<? extends Link> it2 = node.getOutLinks().values().iterator();it2.hasNext();){
						PtLink link = (PtLink)it2.next();
						PtNode node2 = (PtNode) link.getToNode();
						System.out.println("found dead end in node: "+node2.getId());
						deads.add(node2);
					}
				}
			}
		}
		if(deads.size()==0){
			deads=null;
		}
		return deads;
	}

	public boolean handleDeadEnds(final ArrayList<PtNode> deads){
		boolean handled =false;
		if(deads == null){
			return handled;
		}
		for (PtNode node : deads) {
			int fixcnt=0;
			PtNode act = node;
			PtNode nxt = null;
			boolean branchDone = false; // wurde der Node schon irgendwie besser verknüpft?
			if (node.getOutLinks().size() == 2) {
				// node must be pedestrian node (Haltebereich) with only one Hp attached
				while (!branchDone) {
					// searach for the next (only one) non-pedNode which is linked to act
					for (Iterator<? extends Link> it = act.getOutLinks().values().iterator(); it.hasNext(); ) {
						PtLink link = (PtLink)it.next();
						if(link.getType()==null){
							nxt=(PtNode)link.getToNode();
						}
					}

					for(Iterator<? extends Link> it = nxt.getOutLinks().values().iterator();it.hasNext();){
						PtLink link = (PtLink)it.next();
						if(PEDESTRIAN_TYPE.equals(link.getType())){
							PtNode nxtPed = (PtNode) link.getToNode();
							if(nxtPed.getInLinks().size()>=2){
								branchDone=true;

							}
						}
					}

					this.createLink(Integer.toString((this.idCounter+1000000)), nxt.getId().toString(), act.getId().toString(), Double.toString(act.getCoord().calcDistance(nxt.getCoord())), "50", "10000", "1",Integer.toString((this.idCounter+1000000)), "P");
					this.idCounter++;
					fixcnt++;
					act=nxt;
					nxt=null;

				}
				System.out.println("fixed dead end start point in node "+node.getId()+" by adding "+fixcnt+" links downstream");
				handled = true;
			} else if(node.getInLinks().size()==2) {
				while (!branchDone) {
					for(Iterator<? extends Link> it = act.getInLinks().values().iterator();it.hasNext();){
						PtLink link = (PtLink)it.next();
						if (link.getType()==null) {
							nxt=(PtNode)link.getFromNode();
						}
					}

					for (Iterator<? extends Link> it = nxt.getOutLinks().values().iterator();it.hasNext();) {
						PtLink link = (PtLink)it.next();
						if(PEDESTRIAN_TYPE.equals(link.getType())){
							PtNode nxtPed = (PtNode) link.getToNode();
							if(nxtPed.getInLinks().size()>=2){
								branchDone=true;
							}
						}
					}

					this.createLink(Integer.toString(this.idCounter+1000000), act.getId().toString(), nxt.getId().toString(), Double.toString(act.getCoord().calcDistance(nxt.getCoord())), "50", "10000", "1",""+(this.idCounter+1000000), "P");
					this.idCounter++;
					fixcnt++;
					act=nxt;
					nxt=null;

				}
				System.out.println("fixed dead end end point in node "+node.getId()+" by adding "+fixcnt+" links uptream");
				handled = true;
			}
		}
		return handled;
	}

	public Route calcLeastCostPath(final Node fromNode, final Node toNode, final double starttime){
		Route route = dijkstraGetCheapestRoute(fromNode.getCoord(), toNode.getCoord(), starttime, 0);
		return route;
	}

	public Route dijkstraGetCheapestRoute(final CoordI fromCoord, final CoordI toCoord, final double depTime, final int searchRadius) {

		ArrayList<PtNode> depNodes = this.getPedNodesWithin(searchRadius, fromCoord);
		ArrayList<PtNode> arrNodes = this.getPedNodesWithin(searchRadius, toCoord);
		ArrayList<PtNode> reachedNodes = new ArrayList<PtNode>();
		SortedSet<PtNode> pending = new TreeSet<PtNode>(new PtNodeCostComparator());

		// initialize nodes for Dijkstra
		/* instead of initializing all the nodes in every Dijktra-call, we use a
		 * counter, which gets incremented with every call to this method. When
		 * expanding a node, we store the current counter-value in the node. Thus
		 * we can compare if the values stored in the node like actCost and actTime
		 * are current values or the values from a previous call to Dijkstra. This
		 * way, we can initialize the nodes we travel along when needed, and leave
		 * all other nodes untouched. This should get a major speedup in large
		 * networks, not sure about smaller networks. [marcel, jan07]
		 */
		if (this.dijkstraCounter == Long.MAX_VALUE) {
			/* okay, now we have to re-initialize all nodes, as our counter will
			 * "wrap around" and we could no longer be sure the counter-values are
			 * unique.         */
			for (Iterator<Node> it2 = this.nodes.values().iterator(); it2.hasNext();) {
				PtNode node = (PtNode) it2.next();
				node.dijkstraCounter = Long.MIN_VALUE;
			}
			this.dijkstraCounter = Long.MIN_VALUE;
		}
		this.dijkstraCounter++;

		// start at departure nodes
		for (PtNode actNode : depNodes) {
			actNode.actTime = (int)(depTime + (actNode.getCoord().calcDistance(fromCoord) / PEDESTRIAN_SPEED));
			actNode.actCost = actNode.actTime;
			actNode.shortestPath = null;
			actNode.dijkstraCounter = this.dijkstraCounter;

//			in the following departure nodes are already expanded
//			touched links have changecost, those must not be considered

			for (Iterator<? extends Link> it4 = actNode.getOutLinks().values().iterator(); it4.hasNext();) {
				PtLink link = (PtLink) it4.next();
				PtNode node = (PtNode) link.getToNode();
				int linktime = link.getDynTTime(actNode.actTime);

				if ((node.dijkstraCounter != this.dijkstraCounter)
						|| (node.actCost > (actNode.actCost + linktime + (link.cost* this.CHANGE_COST_MULTIPLIER)))) {
					/* If the node has a different dijkstraCounter, that means we did not
					 * yet expand this node in this call to dijkstra, so expand it.
					 * Otherwise, if the counters are the same, then compare the actCost
					 * to decide whether we should expand or not.
					 */
					pending.remove(node);
					node.actCost =actNode.actCost;
					node.actTime = actNode.actTime + linktime;
					node.shortestPath = link;
					node.dijkstraCounter = this.dijkstraCounter;
					pending.add(node);
//					 check for arrival node
					if (arrNodes.remove(actNode)) {
						reachedNodes.add(actNode);
					}

				}
			}
		}

		// Dijkstra: expand nodes
		while ((pending.size() != 0) && (arrNodes.size() != 0)) {
			PtNode actNode = pending.first();
			pending.remove(actNode);

			for (Iterator<? extends Link> it2 = actNode.getOutLinks().values().iterator(); it2.hasNext();) {
				PtLink link = (PtLink) it2.next();
				PtNode node = (PtNode) link.getToNode();
				int linktime = link.getDynTTime(actNode.actTime);

				if ((node.dijkstraCounter != this.dijkstraCounter)
						|| (node.actCost > (actNode.actCost + linktime + (link.cost* this.CHANGE_COST_MULTIPLIER)))) {
					/* If the node has a different dijkstraCounter, that means we did not
					 * yet expand this node in this call to dijkstra, so expand it.
					 * Otherwise, if the counters are the same, then compare the actCost
					 * to decide whether we should expand or not.
					 */
					pending.remove(node);
					node.actCost = actNode.actCost + linktime + (link.cost * this.CHANGE_COST_MULTIPLIER);
					node.actTime = actNode.actTime + linktime;
					node.shortestPath = link;
					node.dijkstraCounter = this.dijkstraCounter;
					pending.add(node);
				}
			}

			// check for arrival node
			if (arrNodes.remove(actNode)) {
				reachedNodes.add(actNode);
			}

		}

		if (reachedNodes.size() == 0) {
			throw new RuntimeException("No route found from coord " + fromCoord + " to coord " + toCoord + " with desired departure time  " + Time.writeTime(depTime, Time.TIMEFORMAT_HHMMSS) + ".");
		}

		// search for fastest route among arrival/reached nodes
		int minCost = Integer.MAX_VALUE;
		PtNode arrNode = null;
		for (PtNode node : reachedNodes) {
			int cost = node.actCost + (int) (node.getCoord().calcDistance(toCoord) / PEDESTRIAN_SPEED);
			if (cost < minCost) {
				minCost = cost;
				arrNode = node;
			}
		}
		int arrTime = (arrNode.actTime + (int) (arrNode.getCoord().calcDistance(toCoord) / PEDESTRIAN_SPEED));

		// create path
		ArrayList<Node> path = new ArrayList<Node>();

		// walk path backwards, starting at arrival node arrNode
		PtNode actNode = arrNode;
		while (actNode.shortestPath != null) {
			path.add(0, actNode);
			actNode = (PtNode) actNode.shortestPath.getFromNode();
		}

		Route route = new Route();
		route.setRoute(path, arrTime - depTime);

		return route;
	}

	public Route dijkstraGetCheapestRouteLogger (final Coord fromCoord, final Coord toCoord, final int depTime,final int searchRadius,final BufferedWriter out){
		Route route = null;
		int touchedNodes = 0;
		try {
			ArrayList<Node> path = new ArrayList<Node>();

			ArrayList<PtNode>depNodes = this.getPedNodesWithin(searchRadius,fromCoord);
			ArrayList<PtNode>arrNodes = this.getPedNodesWithin(searchRadius,toCoord);

			out.write(depNodes.size()+";"+arrNodes.size()+";");

			ArrayList<PtNode>reachedNodes = new ArrayList<PtNode>();

			SortedSet<PtNode> pending = new TreeSet<PtNode>(new PtNodeCostComparator());

			for (Iterator<Node> it2 = this.nodes.values().iterator();it2.hasNext();){
				PtNode node =(PtNode)it2.next();
				node.actCost=Integer.MAX_VALUE;
				node.actTime=Integer.MAX_VALUE;
				node.shortestPath=null;
			}
			pending.clear();

			for (PtNode node : depNodes) {
				node.actTime = (depTime+(int)(node.getCoord().calcDistance(fromCoord)/PEDESTRIAN_SPEED));
				node.actCost = node.actTime;
				pending.add(node);
			}

			while((pending.size()!=0)&&(arrNodes.size()!=0)){
				PtNode actNode = pending.first();
				pending.remove(actNode);

				for(Iterator<? extends Link> it2 = actNode.getOutLinks().values().iterator();it2.hasNext();){
					PtLink link = (PtLink) it2.next();
					PtNode node = (PtNode)link.getToNode();
					int linktime = link.getDynTTime(actNode.actTime);

					if(node.actCost>(actNode.actCost+linktime+link.cost)) {
						pending.remove(node); // just in case the node is already in the set
						node.actCost=actNode.actCost+linktime+link.cost;
						node.actTime=actNode.actTime+linktime;
						node.shortestPath=link;
						pending.add(node);
					}
				}

				boolean isArrNode=false;
				isArrNode = arrNodes.contains(actNode);

				if (isArrNode) {
					if (arrNodes.remove(actNode)==false) {
						Gbl.errorMsg("FATAL ERROR: Could not remove Node from ArrivalList!!");
					}
					reachedNodes.add(actNode);
				}

				touchedNodes++;
			}

			int minCost = Integer.MAX_VALUE;
			PtNode arrNode = null;
			for (PtNode node : reachedNodes) {
				int cost = node.actCost + (int)(node.getCoord().calcDistance(toCoord)/PEDESTRIAN_SPEED);
				if (cost < minCost) {
					minCost = cost;
					arrNode = node;
				}
			}
			int arrTime = (arrNode.actTime + minCost);

			path.clear();
			PtNode actNode2 = arrNode;
			path.add(0, actNode2);
			while(actNode2.shortestPath!=null){
				actNode2=(PtNode)actNode2.shortestPath.getFromNode();
				path.add(0, actNode2);
			}

//				RUN;FROM_X;FROM_Y;TO_X;TO_Y;ABSCOORDDIST;NUM_FROMNODES;NUM_TONODES;FROMNODE_ID;TO_NODEID;DEPWALKDIST;ARRWALKDIST;ROUTELEMENTS;TOUCHEDNODES;PENDINGNODES;TRAVELTIME;TRAVELCOST;CALCTIME\n");

			if(path.size()>0){
				route = new Route();
				route.setRoute(path, arrTime-depTime);
				out.write(path.get(0).getId()+";"+arrNode.getId()+";"+path.get(0).getCoord().calcDistance(fromCoord)+";"+arrNode.getCoord().calcDistance(toCoord)+";"+path.size()+";"+touchedNodes+";"+pending.size()+";"+(arrTime-depTime)+";"+(int)((arrNode.actCost+(arrNode.getCoord().calcDistance(toCoord)/PEDESTRIAN_SPEED))-depTime)+";");
			} else {
				out.write(";;;;0;"+touchedNodes+";"+pending.size()+";"+arrTime+";;");
			}
			out.flush();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return route;
	}

	/**
	 * mark route for visualizer by setting number of permlanes to 5
	 *
	 * @param route
	 */
	public void markRoute(final Route route) {
		if(route.getRoute()!=null){
			ArrayList<Node> routeNodes = route.getRoute();
			for(int i=0; i<(routeNodes.size()-1);i++){
				for(Iterator<? extends Link> it = routeNodes.get(i).getOutLinks().values().iterator();it.hasNext();){
					Link link = it.next();
					if(link.getToNode().equals(routeNodes.get(i+1))){
						link.setLanes(5);
					}
				}
			}
		}
	}

	/**
	 * Returns a list of all PtNodes of type "P" within specific radius from startPoint.
	 * If there are no nodes within, the nearest node(s) of type "P" are returned.
	 *
	 * @param searchRadius
	 * @param startPoint
	 * @return list of nearest nodes
	 */
	public ArrayList<PtNode>getPedNodesWithin(final int searchRadius, final CoordI startPoint) {

		Collection<PtNode> nodes = this.pedNodeQuadTree.get(startPoint.getX(), startPoint.getY(), searchRadius);

		if (nodes.size() == 0) {
			PtNode node = this.pedNodeQuadTree.get(startPoint.getX(), startPoint.getY());
			if (node != null) {
				nodes.add(node);
			}
		}
		ArrayList<PtNode> foundNodes = new ArrayList<PtNode>(nodes);
		return foundNodes;
	}

	@Override
	public void connect() {
		super.connect();
		buildPedQuadTree();
	}

	private void buildPedQuadTree() {
		Gbl.startMeasurement();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Node n : this.nodes.values()) {
			if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
			if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
			if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
			if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("building quad tree: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.pedNodeQuadTree = new QuadTree<PtNode>(minx, miny, maxx, maxy);
		Iterator<Node> n_it = this.nodes.values().iterator();
		while (n_it.hasNext()) {
			PtNode n = (PtNode)n_it.next();
			if (PEDESTRIAN_TYPE.equals(n.getType())) {
				this.pedNodeQuadTree.put(n.getCoord().getX(), n.getCoord().getY(), n);
			}
		}
		Gbl.printRoundTime();

	}

	private static class PtNodeCostComparator implements Comparator<PtNode>, Serializable {
		private static final long serialVersionUID = 1L;

		public PtNodeCostComparator() {
		}

		public int compare(final PtNode n1, final PtNode n2) {
			double c1 = n1.actCost;
			double c2 = n2.actCost;

			if (c1 < c2) return -1;
			if (c1 > c2) return +1;
			return n1.compareTo(n2);
		}
	}
}
