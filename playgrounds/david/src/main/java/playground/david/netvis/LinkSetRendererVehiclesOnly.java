/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSetRendererVehiclesOnly.java
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

package playground.david.netvis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import org.matsim.core.gbl.Gbl;
import org.matsim.vis.netvis.DisplayableLinkI;
import org.matsim.vis.netvis.DisplayableNetI;
import org.matsim.vis.netvis.VisConfig;
import org.matsim.vis.netvis.gui.ControlToolbar;
import org.matsim.vis.netvis.gui.NetJComponent;
import org.matsim.vis.netvis.renderers.RendererA;
import org.matsim.vis.netvis.renderers.ValueColorizer;
import org.matsim.vis.netvis.visNet.DisplayAgent;
import org.matsim.vis.netvis.visNet.DisplayLink;
import org.matsim.vis.netvis.visNet.DisplayNet;


public class LinkSetRendererVehiclesOnly<NET extends DisplayableNetI> extends RendererA {

	private static class OTFVisNet {
		
	};
	private final boolean RANDOMIZE_LANES = false;

	private final boolean RENDER_CELL_CONTOURS = true;

	private final ValueColorizer colorizer = new ValueColorizer();

	private final NET network;

	private final double laneWidth;

	private BufferedImage image;
	private final AffineTransform boxTransformOLD = new AffineTransform();
	private final AffineTransform displayTransformOLD = new AffineTransform();
	private boolean isOTF = false;

	public LinkSetRendererVehiclesOnly(VisConfig visConfig, NET network) {
		super(visConfig);
		this.network =  network;
		this.isOTF = (network instanceof OTFVisNet);
		this.laneWidth = DisplayLink.LANE_WIDTH * visConfig.getLinkWidthFactor();
	}

	private final boolean redrawLanes = false;

	public BufferedImage captureScreen(Graphics2D display) {
		Component target = getNetJComponent();
		Rectangle rect = getNetJComponent().getVisibleRect();
		Rectangle rect2 = getNetJComponent().getParent().getBounds();

		image = new BufferedImage((int)rect.getWidth(), (int)rect.getHeight()+40, BufferedImage.TYPE_INT_RGB);
		Graphics2D imdisplay = image.createGraphics();
		imdisplay.fillRect( 0, 0, (int)rect.getWidth() - 1, (int)rect.getHeight() - 1 );
		imdisplay.setTransform(display.getTransform());
		target.paint(imdisplay);

		return image;
	}

	// -------------------- RENDERING --------------------
	private void renderLink(DisplayableLinkI link, Graphics2D display, AffineTransform linkTransform) {

		final int NodeWidth = 20;
		final int lanes = link.getLanesAsInt(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
		final int cellLength_m = (int)Math.round(link.getLength_m());
		final int cellWidth_m = (int)Math.round(laneWidth * lanes);
		int cellStart_m = 0;

		display.setColor(Color.WHITE);
			if (isOTF) {
//				if (((OTFVisNet)network).lastLink == link) {
//					display.setColor(Color.lightGray);
//				}
			}

			display.fillRect(cellStart_m + NodeWidth, -cellWidth_m, cellLength_m - NodeWidth,
					cellWidth_m);

		display.setColor(Color.BLACK);
		display.drawRect(0 + NodeWidth, -cellWidth_m, cellLength_m-NodeWidth, cellWidth_m);
        if (getVisConfig().showLinkLabels()) {
        	display.setFont(display.getFont().deriveFont((float)(cellWidth_m*0.5)));
        	display.drawString(link.getDisplayText(), (int)(0.5*cellLength_m), -(int)(cellWidth_m*0.5));
        }

//		if (isOTF) {
//			if (((OTFVisNet)network).lastLink == link) {
//				display.setColor(Color.RED);
//				display.fillRect(cellStart_m, -cellWidth_m, cellLength_m,
//						cellWidth_m);
//				display.drawRect(0, -cellWidth_m, cellLength_m
//						* link.getDisplayValueCount(), cellWidth_m);
//			}
//		}

	}

	private void renderLinks(Graphics2D display,
			AffineTransform boxTransform) {

		NetJComponent comp = getNetJComponent();
		AffineTransform originalTransform = display.getTransform();

		Iterator it = null;
		if (isOTF) {
//			it = ((OTFVisNet)network).getLinks().iterator();
		} else  {
			it = ((DisplayNet)network).getLinks().values().iterator();
		}

		for (; it.hasNext();)
		{
			DisplayableLinkI link = (DisplayableLinkI) it.next();

			if (isOTF) {
//				((OTFVisNet.Link)link).setVisible(false);
			}

			if (!comp.checkLineInClip(link.getStartEasting(), link
					.getStartNorthing(), link.getEndEasting(), link
					.getEndNorthing())) {
				continue;
			}

			if (link.getStartEasting() == link.getEndEasting()
					&& link.getStartNorthing() == link.getEndNorthing())
				continue;

			AffineTransform linkTransform = new AffineTransform(
			originalTransform);
			linkTransform.concatenate(boxTransform);

			display.setTransform(linkTransform);
			Polygon poly = new Polygon();
	        double dy = link.getEndEasting() - link.getStartEasting();
	        double dx = link.getEndNorthing() - link.getStartNorthing();
	        double sqr = Math.sqrt(dx*dx +dy*dy);
			final double cellWidth_m = laneWidth * link.getLanesAsInt(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);

			dx = dx*cellWidth_m/sqr;
	        dy = -dy*cellWidth_m/sqr;

			poly.addPoint((int)(link.getStartEasting()-dx), (int)(link.getStartNorthing()-dy));
			poly.addPoint((int)(link.getStartEasting()+dx), (int)(link.getStartNorthing()+dy));
			poly.addPoint((int)(link.getEndEasting()+dx), (int)(link.getEndNorthing()+dy));
			poly.addPoint((int)(link.getEndEasting()-dx), (int)(link.getEndNorthing()-dy));
			display.setColor(Color.WHITE);
			display.fill(poly);
			display.setColor(Color.BLUE);
			display.draw(poly);
//			if (isOTF) {
//				((OTFVisNet.Link)link).setVisible(true);
//			}
//
//			AffineTransform lin2trans =  link.getLinear2PlaneTransform();
//			if (lin2trans != null) linkTransform.concatenate(lin2trans);
//			/*
//			 * (1) RENDER LINK
//			 */
//			display.setTransform(linkTransform);
//			renderLink(link, display, linkTransform);
		}
	}

	static int ii= 0;
	@Override
	protected synchronized void myRendering(Graphics2D display,
			AffineTransform boxTransform) {
		String test = getVisConfig().get("ShowAgents");
		boolean drawAgents = test == null || test.equals("true");
//		this.laneWidth = OTFVisNet.Link.laneWidth* getVisConfig().getLinkWidthFactor()/50.;

		NetJComponent comp = getNetJComponent();

		AffineTransform originalTransform = display.getTransform();

		display.setStroke(new BasicStroke(Math.round(0.05 * laneWidth)));
		display.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON));

//		if (!boxTransformOLD.equals(boxTransform) || !displayTransformOLD.equals(originalTransform)) {
//			redrawLanes = true;
//			boxTransformOLD = boxTransform;
//			displayTransformOLD =originalTransform;
//			captureScreen(display);
//			redrawLanes = false;
////			try {
////				com.sun.image.codec.jpeg.JPEGCodec.createJPEGEncoder(new FileOutputStream("c:\\myFile"+ ii++ + ".jpg")).encode(image);
////			} catch (IOException e) {
////				e.printStackTrace();
////			}
//		}

		if (redrawLanes) {
			renderLinks(display, boxTransform);
		}else {
			Gbl.startMeasurement();
			renderLinks(display, boxTransform);
			display.setTransform(new AffineTransform());
			display.drawImage(image, null, 0, 0);
			display.setTransform(originalTransform);

			Iterator it = null;
			if (isOTF) {
//				it = ((OTFVisNet)network).getLinks().iterator();
			} else  {
				it = ((DisplayNet)network).getLinks().values().iterator();
			}

			for (; it.hasNext();) {
				DisplayableLinkI link = (DisplayableLinkI) it.next();

				if (!comp.checkLineInClip(link.getStartEasting(), link
						.getStartNorthing(), link.getEndEasting(), link
						.getEndNorthing())) {
					continue;
				}

				if (link.getStartEasting() == link.getEndEasting()
						&& link.getStartNorthing() == link.getEndNorthing())
					continue;

				AffineTransform linkTransform = new AffineTransform(
						originalTransform);
				linkTransform.concatenate(boxTransform);
				AffineTransform lin2trans =  link.getLinear2PlaneTransform();
				if (lin2trans != null) linkTransform.concatenate(lin2trans);
				display.setTransform(linkTransform);
//				if (isOTF && ((OTFVisNet)network).lastLink == link) {
//					renderLink(link, display, linkTransform);
//				}

				final int lanes = link.getLanesAsInt(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
				/*
				 * (2) RENDER VEHICLES
				 *
				 * IMPORTANT: If you modify this, ensure proper rendering of angents
				 * on multi-lane links!
				 */

				 final double agentWidth = laneWidth *0.9;
				 final double agentLength = agentWidth*0.9;

				 final boolean flip = (link.getStartEasting() <= link
						 .getEndEasting());

//				 if (flip) {
//					 AffineTransform flipTransform = AffineTransform
//					 .getScaleInstance(1, -1);
//					 linkTransform.concatenate(flipTransform);
//					 display.setTransform(linkTransform);
//				 }

				 if (link.getDisplayText().equals("15880")) {
					 int i= 0;
					 i++;
				 }
				 if (link.getMovingAgents() != null && drawAgents)
					 for (Iterator agentIt = link.getMovingAgents().iterator(); agentIt
					 .hasNext();) {

						 DisplayAgent agent = (DisplayAgent) agentIt.next();

						 final int lane = (RANDOMIZE_LANES ? (agent.hashCode()
								 % lanes + 1) : agent.getLane());

						 final double  x = agent.getPosInLink_m();
						 final double offsetX = - 0.5 * agentLength;

						 final int y = (int)Math.round(agentWidth*(lane-lanes));
						//		 * ((flip ? lanes : 0) - lane));

						 // there is only ONE displayvalue!
						 if (lane < 0) {
							 display.setColor(Color.gray);
						 } else {
							 display.setColor(colorizer.getColor(0.1 + 0.9*link.getDisplayValue(0)));
						 }
						 display.fillOval((int)Math.round(x + offsetX), y, (int)Math.round(agentLength), (int)Math.round(agentWidth));
//
//							if (isOTF && ((OTFVisNet)network).selectedAgents.contains(((OTFVisNet.DisplayAgent)agent).id)) {
//								// Also: Draw a circle around the selected agent
//								display.setColor(Color.BLUE);
//								int circleWidth = (int)(2.* agentWidth);
//								display.drawArc((int)Math.round(x-circleWidth), (int)Math.round(y-circleWidth+0.5*agentWidth), 2*circleWidth, 2*circleWidth, 0, 360);
//							}

					 }
			}
		}

		display.setTransform(originalTransform);
		Gbl.printElapsedTime();
	}


	ControlToolbar toolbar = null;

	public void setControlToolbar(ControlToolbar toolbar) {
		this.toolbar = toolbar;
	}

}
