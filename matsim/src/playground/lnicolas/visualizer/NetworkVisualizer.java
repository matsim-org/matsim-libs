/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkVisualizer.java
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

package playground.lnicolas.visualizer;
///*
// * $Id: NetworkVisualizer.java,v 1.2 2007/01/04 10:59:48 niclefeb Exp $
// */
//
///* *********************************************************************** *
// * project         : matsimJ
// * package         : org.matsim.playground.lnicolas.visualizer
// * file            : NetworkVisualizer.java
// *                          ---------------------                          
// * copyright       : (C) 2006 by Michael Balmer, Marcel Rieser,            *
// *                   David Strippgen, Gunnar Floetteroed, Konrad Meister,  *
// *                   Nicolas Lefebvre, Kai Nagel, Kay W. Axhausen          *
// *                   Technische Universitaet Berlin (TU-Berlin) and        *
// *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
// * email           : niclef at gmail dot com                               *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package org.matsim.playground.lnicolas.visualizer;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.TreeMap;
//
//import com.sun.j3d.utils.universe.SimpleUniverse;
//import com.sun.j3d.utils.geometry.ColorCube;
//import javax.media.j3d.BranchGroup;
//
//import org.matsim.demandmodeling.network.Link;
//import org.matsim.demandmodeling.network.Node;
//import org.matsim.demandmodeling.world.Coord;
//
//public class NetworkVisualizer {
//
//	TreeMap<Integer, GdfNode> nodes = new TreeMap<Integer, GdfNode>();
//	
//	ArrayList<GdfLink> links = new ArrayList<GdfLink>();
//	
//	double currentCarLength = 1.0;
//	double roadWidth = 1.25 * currentCarLength;
//	double roadGap = 0.25 * roadWidth;
//	
//	public GdfNode add(Node node) {
//		GdfNode n = new GdfNode(node);
//		nodes.put(node.getID(), n);
//		return n;
//	}
//	
//	public void add(int id, String label, double x, double y, int size, String color,
//			Boolean isVisible) {
//		nodes.put(id, new GdfNode(label, size, color, isVisible, x, y));
//	}
//	
//	public GdfLink add(Link link) {
//		GdfLink l = new GdfLink(link);
//		links.add(l);
//		return l;
//	}
//	
//	public void draw() {
//		SimpleUniverse universe = new SimpleUniverse();
//	    BranchGroup group = new BranchGroup();
//	    
//	    
//	}
//	
//	public GdfNode get(Node node) {
//		return nodes.get(node.getID());
//	}
//	
//	public class GdfNode {
//		
//		private String label = "";
//		private int size = 4;
//		private String color = "darkgray";
//		private Boolean labelIsVisible = false;
//		private double x = 0;
//		private double y = 0;
//		private int nodeID;
//		
//		GdfNode(Node node) {
//			super();
//			label = "n_" + node.getID();
//			nodeID = node.getID();
//			x = node.getCoord().getX();
//			y = node.getCoord().getY();
//		}
//		
//		public GdfNode(String label, int size, String color,
//				Boolean labelIsVisible, double x, double y) {
//			super();
//			this.label = label;
//			this.size = size;
//			this.color = color;
//			this.labelIsVisible = labelIsVisible;
//			this.x = x;
//			this.y = y;
//		}
//
//		/**
//		 * @param color the color to set
//		 */
//		public void setColor(String color) {
//			this.color = color;
//		}
//
//		/**
//		 * @param labelIsVisible the labelIsVisible to set
//		 */
//		public void setLabelIsVisible(Boolean labelIsVisible) {
//			this.labelIsVisible = labelIsVisible;
//		}
//
//		/**
//		 * @param size the size to set
//		 */
//		public void setSize(int size) {
//			this.size = size;
//		}
//		
//		public int compare(GdfNode other) {
//			if (nodeID < other.nodeID) {
//				return -1;
//			} else if (nodeID == other.nodeID) {
//				return 0;
//			} else {
//				return +1;
//			}
//		}
//		
//	}
//	
////	public class GdfNodeComparator implements Comparator<GdfNode> {
////
////		public int compare(GdfNode n1, GdfNode n2) {
////			return n1.compare(n2);
////		}
////	}
//	
//	public class GdfLink {
//		Boolean directed = true;
//		double width = 0.3;
//		String color = "darkgray";
//		
//		Node fromNode = null;
//		Node toNode = null;
//		
//		GdfLink(Node fromNode, Node toNode) {
//			this.fromNode = fromNode;
//			this.toNode = toNode;
//		}
//		
//		GdfLink(Link link) {
//			this(link.getFromNode(), link.getToNode());
//		}
//
//		/**
//		 * @param width the width to set
//		 */
//		public void setWidth(double width) {
//			this.width = width;
//		}
//
//		public void setColor(String color) {
//			this.color = color;
//		}
//		
//		public int compare(GdfLink other) {
//			if (fromNode.compareTo(other.fromNode) != 0) {
//				return fromNode.compareTo(other.fromNode);
//			} else if (toNode.compareTo(other.toNode) != 0) {
//				return toNode.compareTo(other.toNode) ;
//			} else if (fromNode.equals(other.fromNode)
//					&& toNode.equals(other.toNode)) {
//				return 0;
//			} else {
//				return +1;
//			}
//		}
//		
//		void draw()
//		{
//			Coord fromPos = new Coord(fromNode.getCoord());
//			
//			Coord toPos = new Coord(toNode.getCoord());
//
////			directionVector = toPos-fromPos;
////			direction = directionVector.getNormalized();
////			rightDirection = direction.getRotatedClockwise();
////			startPosition = fromPos;
////			endPosition = toPos;
////			heading = atan2(direction.getY(), direction.getX());
////			
////			double startLeftX = startPosition + roadGap * rightDirection;
////			Position startRight = startPosition + roadWidth * rightDirection;
////			Position endLeft = endPosition + roadGap * rightDirection;
////			Position endRight = endPosition + roadWidth * rightDirection;
////
////		 	glBegin(GL_QUADS);
////
////			glColor3f(GREYSHADE, GREYSHADE, GREYSHADE);
////			
////			glVertex3f( startLeft.getX(), startLeft.getY(), 0.0);
////			glVertex3f( startRight.getX(), startRight.getY(), 0.0);
////			glVertex3f( endRight.getX(), endRight.getY(), 0.0);
////			glVertex3f( endLeft.getX(), endLeft.getY(), 0.0);
////
////			glColor3f(1.0, 0.0, 0.0);
////
////			double size = 10.0;
////			glVertex3f( endPosition.getX() - size, endPosition.getY() - size, 0.0);
////			glVertex3f( endPosition.getX() + size, endPosition.getY() - size, 0.0);
////			glVertex3f( endPosition.getX() + size, endPosition.getY() + size, 0.0);
////			glVertex3f( endPosition.getX() - size, endPosition.getY() + size, 0.0);
////			
////
////		  	glEnd();
//		}
//	}
//	
////	public class GdfLinkComparator implements Comparator<GdfLink> {
////
////		public int compare(GdfLink l1, GdfLink l2) {
////			return l1.compare(l2);
////		}
////	}
//}
