/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.parking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingSearch.ca.matlabInfra.Agent;

public class CreateScenario {
	private ScenarioImpl scenario = null;	
	private double sideLength = 500.0;
	private double spacing = 100.0;
	
	private final static Logger log = Logger.getLogger(CreateScenario.class);
	
//	public static void main(final String[] args) {
//		CreateScenario creator = new CreateScenario();	
//		creator.run(args[0]);			
//		log.info("Scenario creation finished \n ----------------------------------------------------");
//	}
//	
//	public void run(String path) {
//		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(this.scenario.getNetwork());
//		this.addNodes(networkFactory);
//		this.addLinks(networkFactory);
//		this.write(path);
//	}
//						
//	private void addLinks(NetworkFactoryImpl networkFactory) {		
//		int linkCnt = 0;				
//		int stepsPerSide = (int)(sideLength / spacing);
//		
//		for (int i = 0; i <= stepsPerSide ; i++) {
//			for (int j = 0; j <= stepsPerSide; j++) {
//				Id<Node> fromNodeId = Id.create(Integer.toString(i * (stepsPerSide + 1) + j), Node.class);
//							
//				if (j > 0) {
//					// create backward link
//					Id<Node> toNodeId = Id.create(Integer.toString(i * (stepsPerSide + 1) + j - 1), Node.class);
//					
//					Link l0 = networkFactory.createLink(Id.create(Integer.toString(linkCnt), Link.class), fromNodeId, toNodeId);			
//					l0.setLength(((CoordImpl)scenario.getNetwork().getNodes().get(fromNodeId).getCoord()).calcDistance(
//							scenario.getNetwork().getNodes().get(toNodeId).getCoord()));
//					this.scenario.getNetwork().addLink(l0);
//					linkCnt++;			
//					
//					Link l1 = networkFactory.createLink(Id.create(Integer.toString(linkCnt), Link.class), toNodeId, fromNodeId);
//					l1.setLength(((CoordImpl)scenario.getNetwork().getNodes().get(toNodeId).getCoord()).calcDistance(
//							scenario.getNetwork().getNodes().get(fromNodeId).getCoord()));
//					this.scenario.getNetwork().addLink(l1);
//					linkCnt++;
//				}				
//				
//				if (i > 0) {
//					// create downward link
//					Id<Node> toNodeId = Id.create(Integer.toString((i - 1) * (stepsPerSide + 1) + j), Node.class);
//					
//					Link l0 = networkFactory.createLink(Id.create(Integer.toString(linkCnt), Link.class), fromNodeId, toNodeId);
//					l0.setLength(((CoordImpl)scenario.getNetwork().getNodes().get(fromNodeId).getCoord()).calcDistance(
//							scenario.getNetwork().getNodes().get(toNodeId).getCoord()));
//					this.scenario.getNetwork().addLink(l0);
//					linkCnt++;
//															
//					Link l1 = networkFactory.createLink(Id.create(Integer.toString(linkCnt), Link.class), toNodeId, fromNodeId);
//					l1.setLength(((CoordImpl)scenario.getNetwork().getNodes().get(fromNodeId).getCoord()).calcDistance(
//							scenario.getNetwork().getNodes().get(toNodeId).getCoord()));
//					this.scenario.getNetwork().addLink(l1);
//					linkCnt++;
//				}				
//			}
//		}
//		Link l0 = networkFactory.createLink(Id.create(Integer.toString(linkCnt), Link.class), Id.create(-1, Node.class), Id.create(0, Node.class));			
//		l0.setLength(((CoordImpl)scenario.getNetwork().getNodes().get(Id.create(-1, Node.class)).getCoord()).calcDistance(
//				scenario.getNetwork().getNodes().get(Id.create(0, Node.class)).getCoord()));
//		this.scenario.getNetwork().addLink(l0);	
//		linkCnt++;
//		
//		Link l1 = networkFactory.createLink(Id.create(Integer.toString(linkCnt), Link.class), Id.create(0, Node.class), Id.create(-1, Node.class));			
//		l1.setLength(((CoordImpl)scenario.getNetwork().getNodes().get(Id.create(0, Node.class)).getCoord()).calcDistance(
//				scenario.getNetwork().getNodes().get(Id.create(-1, Node.class)).getCoord()));
//		this.scenario.getNetwork().addLink(l1);
//		linkCnt++;
//		
//		int n = (stepsPerSide + 1) * (stepsPerSide + 1) - 1;
//		Link l2 = networkFactory.createLink(Id.create(Integer.toString(linkCnt), Link.class), Id.create(n, Node.class), Id.create(9999999, Node.class));			
//		l2.setLength(((CoordImpl)scenario.getNetwork().getNodes().get(Id.create(n, Node.class)).getCoord()).calcDistance(
//				scenario.getNetwork().getNodes().get(Id.create(9999999, Node.class)).getCoord()));
//		this.scenario.getNetwork().addLink(l2);
//		linkCnt++;
//		
//		Link l3 = networkFactory.createLink(Id.create(Integer.toString(linkCnt), Link.class), Id.create(9999999, Node.class), Id.create(n, Node.class));			
//		l3.setLength(((CoordImpl)scenario.getNetwork().getNodes().get(Id.create(9999999, Node.class)).getCoord()).calcDistance(
//				scenario.getNetwork().getNodes().get(Id.create(n, Node.class)).getCoord()));
//		this.scenario.getNetwork().addLink(l3);
//		
//		
//		log.info("Created " + linkCnt + " links");
//	}
//				
//	private void addNodes(NetworkFactoryImpl networkFactory) {
//		int nodeCnt = 0;
//		int stepsPerSide = (int)(sideLength/ spacing);
//		for (int i = 0; i <= stepsPerSide ; i++) {
//			for (int j = 0; j <= stepsPerSide; j++) {
//				Node n = networkFactory.createNode(Id.create(Integer.toString(nodeCnt), Node.class), new CoordImpl(i * spacing, j * spacing));
//				this.scenario.getNetwork().addNode(n);
//				nodeCnt++;
//			}
//		}
//		Node nbegin = networkFactory.createNode(Id.create(Integer.toString(-1), Node.class), new CoordImpl(-100.0, -100.0));
//		this.scenario.getNetwork().addNode(nbegin);
//		
//		Node nend = networkFactory.createNode(Id.create(Integer.toString(9999999), Node.class), new CoordImpl(sideLength + 100.0, sideLength + 100.0));
//		this.scenario.getNetwork().addNode(nend);
//		
//		log.info("Created " + nodeCnt + " nodes");
//	}
//	
//	public void write(String path) {
//		this.writeNetwork(path);
//		this.writePopulation(path + "/population.xml");
//		this.writeParkingLots(path + "/parkingLots.xml");		
//	}
//	
//	private void writeNetwork(String path) {
//		this.writeLinks(path + "/links.xml");
//		this.writeNodes(path + "/nodes.xml");
//	}
//	
//	private void writePopulation(String file) {
//		LinkedList<Agent> agents = new LinkedList<Agent>();	
//		
//		
//		int cnt = -1;
//		for (int i = 1; i <= 100; i++) {
//			// Agent(Id id, double tripStartTime, String routeTo, String actType, double actDur, String routeAway)
//			
//			// agent from bottom left
//			cnt++;
//			List<Integer> routeTo = new Vector<Integer>();
//			routeTo.add(-1);
//			routeTo.add(0);
//			int stepsPerSide = (int)(sideLength/ spacing);
//			for (int ii = 1; ii <= stepsPerSide / 2; ii++) {
//				routeTo.add((ii - 1) * (stepsPerSide + 1) + ii);
//				routeTo.add((ii * (stepsPerSide + 1) + ii));
//			}
//			String routeToString = routeTo.toString();
//			routeToString = StringUtils.remove(routeToString, ',' );
//			routeToString = StringUtils.remove(routeToString, '[' );
//			routeToString = StringUtils.remove(routeToString, ']' );
//			
//			List<Integer> routeAway = new Vector<Integer>();
//			routeAway.add(9999999); 
//			for (int ii = stepsPerSide; ii > stepsPerSide / 2; ii--) {
//				routeAway.add(ii * (stepsPerSide + 1) + ii);
//				routeAway.add((ii - 1) * (stepsPerSide + 1) + ii) ;
//			}
//			routeAway.add((stepsPerSide / 2 * (stepsPerSide + 1) + stepsPerSide / 2)); 
//			
//			Collections.reverse(routeAway);
//			String routeAwayString = routeAway.toString();
//			routeAwayString = StringUtils.remove(routeAwayString, ',' );
//			routeAwayString = StringUtils.remove(routeAwayString, '[' );
//			routeAwayString = StringUtils.remove(routeAwayString, ']' );
// 
//			Agent agent0 = new Agent(Id.create(cnt, Agent.class), cnt * 5, routeToString, "s", 30.0 * 60.0, routeAwayString);	
//			agents.add(agent0);
//			
//			
//			// agent from bottom left
//			cnt++;
//			routeTo = new Vector<Integer>();
//			routeTo.add(-1);
//			routeTo.add(0);
//			for (int ii = 1; ii <= stepsPerSide / 2; ii++) {
//				routeTo.add((ii - 1) * (stepsPerSide + 1) + ii);
//				routeTo.add((ii * (stepsPerSide + 1) + ii));
//			}
//			Collections.reverse(routeTo);
//			routeToString = routeTo.toString();
//			routeToString = StringUtils.remove(routeToString, ',' );
//			routeToString = StringUtils.remove(routeToString, '[' );
//			routeToString = StringUtils.remove(routeToString, ']' );
//			
//			routeAway = new Vector<Integer>();
//			routeAway.add(9999999); 
//			for (int ii = stepsPerSide; ii > stepsPerSide / 2; ii--) {
//				routeAway.add(ii * (stepsPerSide + 1) + ii);
//				routeAway.add((ii - 1) * (stepsPerSide + 1) + ii) ;
//			}
//			routeAway.add((stepsPerSide / 2 * (stepsPerSide + 1) + stepsPerSide / 2)); 
//			
//			routeAwayString = routeAway.toString();
//			routeAwayString = StringUtils.remove(routeAwayString, ',' );
//			routeAwayString = StringUtils.remove(routeAwayString, '[' );
//			routeAwayString = StringUtils.remove(routeAwayString, ']' );
// 
//			Agent agent1 = new Agent(Id.create(cnt, Agent.class), cnt * 5.0, routeAwayString, "s", 30.0 * 60.0, routeToString);	
//			agents.add(agent1);
//			
//						
//			// transit agent
//			// ...
//		}		
//		ArrayList<String> list = new ArrayList<String>();
//		list.add("<agents>");
//		for (Agent agent : agents) {
//			list.add(agent.getXMLString(scenario.getNetwork()));
//		}
//		list.add("</agents>");
//		GeneralLib.writeList(list, file);
//	}
//	
//	private void writeParkingLots(String file) {
//		ArrayList<String> list = new ArrayList<String>();
//		list.add("<parkinglots>");
//		long capacity = 4;		
//		int cnt = 0;
//		for (Link link : this.scenario.getNetwork().getLinks().values()) {	
//			// only use every 2nd link
//			if (cnt % 2 == 0) {
//				list.add(this.getParkingString("sp-" + cnt, link.getCoord().getX(), link.getCoord().getY(), capacity));
//			}
//			cnt++;
//		}				
//		list.add("</parkinglots>");
//		GeneralLib.writeList(list, file);
//	}
//	
//	private String getParkingString(String id, double x, double y, double capacity) {
//		StringBuffer stringBuffer = new StringBuffer();
//		stringBuffer.append("\t<parkinglot>\n");
//		stringBuffer.append("\t\t<id>" + id + "</id>\n");
//		stringBuffer.append("\t\t<x>" + x + "</x>\n");
//		stringBuffer.append("\t\t<y>" + y + "</y>\n");
//		stringBuffer.append("\t\t<size>" + capacity + "</size>\n");
//		stringBuffer.append("\t</parkinglot>\n");
//		return stringBuffer.toString();
//	}
//				
//	private void writeLinks(String file) {		
//		ArrayList<String> list = new ArrayList<String>();
//		list.add("<links>");
//		
//		for (Link link : this.scenario.getNetwork().getLinks().values()) {	
//			StringBuffer stringBuffer = new StringBuffer();
//			stringBuffer.append("\t<link>\n");
//			stringBuffer.append("\t\t<id>" + link.getId() + "</id>\n");
//			stringBuffer.append("\t\t<fromNode>" + link.getFromNode().getId() + "</fromNode>\n");
//			stringBuffer.append("\t\t<toNode>" + link.getToNode().getId() + "</toNode>\n");
//			stringBuffer.append("\t</link>\n");
//			list.add(stringBuffer.toString());
//		}
//		list.add("</links>\n");
//		GeneralLib.writeList(list, file);
//	}
//	
//	private void writeNodes(String file) {
//		ArrayList<String> list = new ArrayList<String>();
//		list.add("<nodes>");		
//		for (Node node : this.scenario.getNetwork().getNodes().values()) {
//			StringBuffer stringBuffer = new StringBuffer();
//			stringBuffer.append("\t<node>\n");
//			stringBuffer.append("\t\t<id>" + node.getId() + "</id>\n");
//			stringBuffer.append("\t\t<x>" + node.getCoord().getX() + "</x>\n");
//			stringBuffer.append("\t\t<y>" + node.getCoord().getY() + "</y>\n");
//			stringBuffer.append("\t</node>\n");
//			list.add(stringBuffer.toString());
//		}
//		list.add("</nodes>\n");
//		GeneralLib.writeList(list, file);
//	}
}
