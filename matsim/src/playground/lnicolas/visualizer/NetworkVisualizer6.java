/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkVisualizer6.java
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
//package org.matsim.playground.lnicolas.visualizer;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.TreeMap;
//import java.awt.*;
//import java.awt.event.*;
//import java.io.File;
//import java.io.IOException;
//
//import javax.swing.*;
//
//import org.matsim.demandmodeling.network.Link;
//import org.matsim.demandmodeling.network.NetworkLayer;
//import org.matsim.demandmodeling.world.Coord;
//import org.matsim.playground.lnicolas.util.GdfExport.GdfLink;
//import org.matsim.playground.lnicolas.util.GdfExport.GdfNode;
//
//import no.geosoft.cc.graphics.*;
//
//
//
///**
// * G demo program. Demonstrates:
// *
// * <ul>
// * <li>Custom selection interaction
// * <li>Object dete 	ction features
// * <li>Style inheritance and manipulation
// * <li>Dynamic style setting
// * </ul>
// * 
// * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
// */   
//
//public class NetworkVisualizer6 extends JFrame implements GInteraction, ActionListener {
//	private JButton typeButton_;
//	private JButton   zoomButton_;
//	private JButton saveButton_;
//	private JButton moveButton_;
//
//	private GWindow   window_;
//	
//	private GStyle selectionStyle_;
//
//	private GStyle textStyle_;
//
//	private GStyle selectedTextStyle_;
//
//	private GScene scene_;
//
//	private GObject rubberBand_;
//
//	private Collection selection_;
//
//	private int x0_, y0_;
//	
//	TreeMap<Integer, GdfNode> nodes = new TreeMap<Integer, GdfNode>();
//	
//	ArrayList<GdfLink> links = new ArrayList<GdfLink>();
//
//	public NetworkVisualizer6(NetworkLayer network) {
//		this(network, new Dimension(1024, 768));
//	}
//	
//	/**
//	 * Class for creating the demo canvas and hande Swing events.
//	 */
//	public NetworkVisualizer6(NetworkLayer network, Dimension displaySize) {
//		super("G Graphics Library - Demo 6");
//		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		
//
//		selectionStyle_ = new GStyle();
//		selectionStyle_.setForegroundColor(new Color(255, 255, 150));
//		selectionStyle_.setLineWidth(2);
//
//		selectedTextStyle_ = new GStyle();
//		selectedTextStyle_.setForegroundColor(new Color(255, 255, 255));
//		selectedTextStyle_.setFont(new Font("Dialog", Font.BOLD, 14));
//
//		selection_ = null;
//
//		// Create the GUI
//		JPanel topLevel = new JPanel();
//		topLevel.setLayout(new BorderLayout());
//		getContentPane().add(topLevel);
//
//		JPanel buttonPanel = new JPanel();
//		buttonPanel.add(new JLabel("Highlight lines "));
//
//		typeButton_ = new JButton("inside");
//		typeButton_.addActionListener(this);
//		buttonPanel.add(typeButton_);
//		
//		zoomButton_ = new JButton("zoom");
//		zoomButton_.addActionListener(this);
//		buttonPanel.add(zoomButton_);
//		
//		saveButton_ = new JButton("save as gif");
//		saveButton_.addActionListener(this);
//		buttonPanel.add(saveButton_);
//		
//		moveButton_ = new JButton("move");
//		moveButton_.addActionListener(this);
//		buttonPanel.add(moveButton_);
//
//		buttonPanel.add(new JLabel(" rubberband"));
//		topLevel.add(buttonPanel, BorderLayout.NORTH);
//
//		// Create the graphic canvas
//		window_ = new GWindow();
//		topLevel.add(window_.getCanvas(), BorderLayout.CENTER);
//
//		// Create scene with default viewport and world extent settings
//		scene_ = new GScene(window_, "Scene");
//		
//		scene_.shouldWorldExtentFitViewport (false);
//		scene_.shouldZoomOnResize (false);   
//
//		rubberBand_ = new GObject("Interaction");
//		GStyle rubberBandStyle = new GStyle();
//		rubberBandStyle.setBackgroundColor(new Color(1.0f, 0.0f, 0.0f, 0.2f));
//		rubberBand_.setStyle(rubberBandStyle);
//		scene_.add(rubberBand_);
//
//		pack();
//		setSize(displaySize);
//		
//		GNetwork gNetwork = new GNetwork(scene_);
////		Iterator it = network.getNodes().iterator();
////		while (it.hasNext()) {
////			gNetwork.add((Node) it.next());
////		}
//		
//		Iterator it = network.getLinks().iterator();
//		while (it.hasNext()) {
//			/*GdfLink link = (GdfLink) */gNetwork.add((Link) it.next());
//			
////			nodes.get(link.toNode.getID()).addInLink(link);
////			nodes.get(link.fromNode.getID()).addOutLink(link);
//		}
//	    
//		scene_.add(gNetwork);
//		
////		try {
////			window_.saveAsGif(displaySize.width, displaySize.height,
////					"C:/nic/matsim/gTest.gif");
////		} catch (Exception e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		}
////		System.out.println("done");
//		
//		setVisible(true);
//
//		window_.startInteraction(this);
//	}
//
//	public void actionPerformed(ActionEvent event) {
//		if (event.getSource() == zoomButton_) {
//			window_.startInteraction (new ZoomInteraction (scene_));
//		} else if (event.getSource() == typeButton_) {
//			String text = typeButton_.getText();
//			if (text.equals("inside"))
//				typeButton_.setText("intersecting");
//			else
//				typeButton_.setText("inside");
//		} else if (event.getSource() == saveButton_) {
//			try {
//				window_.saveAsGif(new File("/var/tmp/lnicolas/routingPerformance/gTest.gif"));
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		} else if(event.getSource() == moveButton_) {
//			window_.startInteraction (this);
//		}
//	}
//
//	public void event(GScene scene, int event, int x, int y) {
//		switch (event) {
//		case GWindow.BUTTON1_DOWN:
//			x0_ = x;
//			y0_ = y;
//			rubberBand_.addSegment(new GSegment());
//			break;
//
//		case GWindow.BUTTON1_UP:
//			rubberBand_.removeSegments();
//
//			// Undo visual selection of current selection
//			if (selection_ != null) {
//				for (Iterator i = selection_.iterator(); i.hasNext();) {
//					GSegment line = (GSegment) i.next();
//					GText text = line.getText();
//					text.setStyle(textStyle_);
//					line.setStyle(null);
//				}
//			}
//
//			scene_.refresh();
//			break;
//
//		case GWindow.BUTTON1_DRAG:
//			int[] xRubber = new int[] { x0_, x, x, x0_, x0_ };
//			int[] yRubber = new int[] { y0_, y0_, y, y, y0_ };
//
//			GSegment rubberBand = rubberBand_.getSegment();
//			rubberBand.setGeometry(xRubber, yRubber);
//
//			// Undo visual selection of current selection
//			if (selection_ != null) {
//				for (Iterator i = selection_.iterator(); i.hasNext();) {
//					GSegment line = (GSegment) i.next();
//					GText text = line.getText();
//					text.setStyle(textStyle_);
//					line.setStyle(null);
//				}
//			}
//
//			if (typeButton_.getText().equals("inside"))
//				selection_ = scene_.findSegmentsInside(Math.min(x0_, x), Math
//						.min(y0_, y), Math.max(x0_, x), Math.max(y0_, y));
//			else
//				selection_ = scene_.findSegments(Math.min(x0_, x), Math.min(
//						y0_, y), Math.max(x0_, x), Math.max(y0_, y));
//
//			// Remove rubber band from selection
//			selection_.remove(rubberBand);
//
//			// Set visual appaerance of new selection
//			for (Iterator i = selection_.iterator(); i.hasNext();) {
//				GSegment line = (GSegment) i.next();
//				line.setStyle(selectionStyle_);
//				GText text = line.getText();
//				text.setStyle(selectedTextStyle_);
//			}
//
//			scene_.refresh();
//			break;
//		}
//	}
//		
//	/**
//	 * Defines the geometry and presentation for a sample
//	 * graphic object.
//	 */
//	private class GNetwork extends GObject {
//		private ArrayList<Link> links;
//		private ArrayList<GSegment> gLinks;
////		private TreeMap<Integer, Node> nodes;
//		
//		double maxX = Double.MIN_VALUE;
//		double minX = Double.MAX_VALUE;
//		double maxY = Double.MIN_VALUE;
//		double minY = Double.MAX_VALUE;
//
//		GNetwork(GScene scene) {
//			links = new ArrayList<Link>();
////			nodes = new TreeMap<Integer, Node>();
//			gLinks = new ArrayList<GSegment>();
//
//			// Add style to object itself so it is inherited by segments
//			GStyle lineStyle = new GStyle();
//			lineStyle.setForegroundColor(new Color(100, 100, 100));
//			setStyle(lineStyle);
//
//			// Text style is set on each text element
//			textStyle_ = new GStyle();
//			textStyle_.setForegroundColor(new Color(0, 0, 0));
//			textStyle_.setFont(new Font("Dialog", Font.BOLD, 14));
//
////			for (int i = 0; i < nLines; i++) {
////				lines_[i] = new GSegment();
////				addSegment(lines_[i]);
////
////				GText text = new GText(Integer.toString(i));
////				text.setStyle(textStyle_);
////				lines_[i].setText(text);
////			}
//		}
//		
////		public void add(Node node) {
////			nodes.put(node.getID(), node);
////		}
//
//		void add(Link link) {
//			links.add(link);
//			GSegment gLink = new GSegment();
//			gLinks.add(gLink);
//			addSegment(gLink);
//			
//			updateMinMaxCoords(link.getToNode().getCoord());
//			updateMinMaxCoords(link.getFromNode().getCoord());
//		}
//
//		private void updateMinMaxCoords(Coord coord) {
//			if (coord.getX() < minX) {
//				minX = coord.getX();
//			}
//			if (coord.getX() > maxX) {
//				maxX = coord.getX();
//			}
//			if (-coord.getY() < minY) {
//				minY = -coord.getY();
//			}
//			if (-coord.getY() > maxY) {
//				maxY = -coord.getY();
//			}
//		}
//
//		public void draw(Rectangle section) {
//			
//			// Viewport dimensions
//			double width = getScene().getViewport().getWidth();
//			double height = getScene().getViewport().getHeight();
//			
//			double zoomFactor = 0;
//			if (width / (section.width) > height / (section.height)) {
//				zoomFactor = height / (section.height);
//			} else {
//				zoomFactor = width / (section.width);
//			}
//			
//			GStyle textStyle = new GStyle();
//			textStyle.setFont(new Font("Dialog", Font.BOLD, 8));
//			textStyle.setForegroundColor(new Color(255, 255, 0));
//			textStyle.setBackgroundColor(new Color(100, 100, 100));
//			
//			for (int i = 0; i < links.size(); i++) {
//				gLinks.get(i).setGeometry(
//						(int)((links.get(i).getFromNode().getCoord().getX() - section.x) * zoomFactor),
//						(int)((-links.get(i).getFromNode().getCoord().getY() - section.y) * zoomFactor),
//						(int)((links.get(i).getToNode().getCoord().getX() - section.x) * zoomFactor),
//						(int)((-links.get(i).getToNode().getCoord().getY() - section.y) * zoomFactor));
//				
//				GText text = new GText (links.get(i).getFromNode().getID() + "",
//						GPosition.NORTH |
//						GPosition.STATIC);
//				text.setStyle (textStyle);
//				gLinks.get(i).addText (text);
//			}
//		}
//		
//		public void draw() {
//			draw(new Rectangle((int)minX, (int)minY, (int)(maxX - minX), (int)(maxY - minY)));
//		}
//		
////		public void draw() {
////			Node fromNode = nodes.get(2626);
////			Node toNode = nodes.get(6262);
////				
////			double sectionX;
////			double sectionY;
////			double sectionHeight;
////			double sectionWidth;
////			if (fromNode.getCoord().getX() < toNode.getCoord().getX()) {
////				sectionX = fromNode.getCoord().getX();
////				sectionWidth = toNode.getCoord().getX() - fromNode.getCoord().getX();
////			} else {
////				sectionX = toNode.getCoord().getX();
////				sectionWidth = fromNode.getCoord().getX() - toNode.getCoord().getX();
////			}
////			if (-fromNode.getCoord().getY() < -toNode.getCoord().getY()) {
////				sectionY = -fromNode.getCoord().getY();
////				sectionHeight = toNode.getCoord().getY() - fromNode.getCoord().getY();
////			} else {
////				sectionY = -toNode.getCoord().getY();
////				sectionHeight = fromNode.getCoord().getY() - toNode.getCoord().getY();
////			}
////			
////			sectionHeight = -sectionHeight;
////			
////			draw(new Rectangle((int)sectionX, (int)sectionY, (int)sectionWidth, (int)sectionHeight));
////		}
//	}
//}
