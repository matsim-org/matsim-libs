/* *********************************************************************** *
 * project: org.matsim.*
 * NetJComponent.java
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

package org.matsim.vis.otfvis.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.rmi.RemoteException;

import javax.swing.JComponent;

import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFDataQuadReceiver;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.OTFDataSimpleAgentReceiver;
import org.matsim.vis.otfvis.interfaces.OTFQueryHandler;
import org.matsim.vis.otfvis.opengl.gui.ValueColorizer;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

/**
 * @author david
 */
abstract class OTFSwingDrawable implements OTFDrawable, OTFDataReceiver{
	static Graphics2D g2d = null;

	@Override
	public final void draw() {
		onDraw(g2d);
	}

	abstract public void onDraw(Graphics2D g2d);

	@Override
	public void invalidate(SceneGraph graph) {
		graph.addItem(this);
	}
}

/**
 * The class implements the Component for SWING based drawing of the OTFVis.
 * This version of the OTFVis does not support all possible features implemented in the OpenGL-based version.
 *
 * @author dstrippgen
 */
public class OTFSwingDrawer extends JComponent {

	private static final Color netColor = new Color(180,180,210,128);
	
	double scale = 1;
	
	private static final long serialVersionUID = 1L;

	private static final float linkWidth = 100;

	private final int frameDefaultWidth;

	private final int frameDefaultHeight;

	private final OTFClientQuad quad;

	private transient SceneGraph sceneGraph;
	
	private OTFQueryHandler queryHandler;
	
	OTFHostControlBar hostControlBar;

	private OTFSwingDrawerContainer parentDrawer;

	// --------------- CONSTRUCTION ---------------

	public OTFSwingDrawer(OTFClientQuad quad, OTFHostControlBar hostControlBar, OTFSwingDrawerContainer parentDrawer) {
		this.quad = quad;
		this.parentDrawer = parentDrawer;
		// calculate size of frame

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double factor = screenSize.getWidth() / networkClippingWidth();
		factor = Math.min(factor, screenSize.getHeight() / networkClippingHeight());
		factor *= 0.8f;

		frameDefaultWidth = (int) Math.floor(networkClippingWidth() * factor);
		frameDefaultHeight = (int) Math.floor(networkClippingHeight() * factor);

		scale(1);
		this.hostControlBar = hostControlBar;
	}

	void scale(double factor) {
		if (factor > 0) {
			this.scale = factor;
			int scaledWidth = (int) Math.round(factor * frameDefaultWidth);
			int scaledHeight = (int) Math.round(factor * frameDefaultHeight);
			this.setPreferredSize(new Dimension(scaledWidth, scaledHeight));
		}
	}

	// -------------------- COORDINATE TRANSFORMATION --------------------

	private double networkClippingMaxEasting() {
		return quad.getMaxEasting() -quad.getMinEasting() + 1;
	}

	private double networkClippingMaxNorthing() {
		return quad.getMaxNorthing() - quad.getMinNorthing() + 1;
	}

	private double networkClippingWidth() {
		return networkClippingMaxEasting() - (0 - 1);
	}

	private double networkClippingHeight() {
		return networkClippingMaxNorthing() - (0 - 1);
	}

	
	public float getScale() {
		return (float) scale;
	}
	
	AffineTransform getBoxTransform() {

		// two original extreme coordinates ...

		double v1 = 0 - 1;
		double w1 = 0 - 1;

		double v2 = networkClippingMaxEasting();
		double w2 = networkClippingMaxNorthing();

		// ... mapped onto two extreme picture coordinates ...

		Dimension prefSize = this.getPreferredSize();

		double x1 = 0;
		double y1 = (int) prefSize.getHeight();

		double x2 = (int) prefSize.getWidth();
		double y2 = 0;

		// ... yields a simple affine transformation without shearing:

		double m00 = (x1 - x2) / (v1 - v2);
		double m02 = x1 - m00 * v1;

		double m11 = (y1 - y2) / (w1 - w2);
		double m12 = y1 - m11 * w1;

		return new AffineTransform(m00, 0.0, 0.0, m11, m02, m12);
	}

	// -------------------- PAINTING --------------------

	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		parentDrawer.mouseMan.drawElements(g2);

		boolean useAntiAliasing = true;

		if (useAntiAliasing ) {
			g2.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
		} else {
			g2.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF));
		}

		OTFSwingDrawable.g2d = g2;

		g2.setStroke(new BasicStroke(Math.round(0.05 * linkWidth)));
		g2.transform(getBoxTransform());
		sceneGraph.draw();
		if (this.queryHandler != null) {
			this.queryHandler.drawQueries(parentDrawer);
		}
	}

	public OTFClientQuad getQuad() {
		return quad;
	}
	
	public  Graphics2D getG2D() {
		return OTFSwingDrawable.g2d;
	}

	public void invalidate() {
		try {
			this.sceneGraph = quad.getSceneGraph(hostControlBar.getOTFHostControl().getSimTime(), null, parentDrawer);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	/***
	 * Drawer class for drawing simple quads
	 */
	public static class SimpleQuadDrawer extends OTFSwingDrawable implements OTFDataQuadReceiver{
		protected final Point2D.Float[] quad = new Point2D.Float[4];
		protected String id = "noId";
		private float oldResizer = 1;
		//		protected float coloridx = 0;


		Point2D.Float calcOrtho(Point2D.Float start, Point2D.Float end, int nrLanes){
			double dx = end.y - start.y;
			double dy = end.x -start.x;
			double sqr1 = Math.sqrt(dx*dx +dy*dy);
			final double cellWidth_m = nrLanes*linkWidth;

			dx = dx*cellWidth_m/sqr1;
			dy = -dy*cellWidth_m/sqr1;

			return new Point2D.Float((float)dx,(float)dy);
		}

		@Override
		public void setQuad(float startX, float startY, float endX, float endY) {
			setQuad(startX, startY,endX, endY, 1);
		}

		@Override
		public void setQuad(float startX, float startY, float endX, float endY, int nrLanes) {
			this.quad[0] = new Point2D.Float(startX, startY);
			this.quad[1] = new Point2D.Float(endX, endY);
			final Point2D.Float ortho = calcOrtho(this.quad[0], this.quad[1], nrLanes);
			this.quad[2] = new Point2D.Float(startX + ortho.x, startY + ortho.y);
			this.quad[3] = new Point2D.Float(endX + ortho.x, endY + ortho.y);
		}

		@Override
		public void setColor(float coloridx) {
			//			this.coloridx = coloridx;
		}

		@Override
		public void onDraw(Graphics2D display) {
			Polygon poly = new Polygon();
			
			float resizer = (float) ((2*OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth())/(1*linkWidth) + 0.5);
			quad[2].x = resizer/this.oldResizer * (quad[2].x - quad[0].x) + quad[0].x;
			quad[2].y = resizer/this.oldResizer * (quad[2].y - quad[0].y) + quad[0].y;
			quad[3].x = resizer/this.oldResizer * (quad[3].x - quad[1].x) + quad[1].x;
			quad[3].y = resizer/this.oldResizer * (quad[3].y - quad[1].y) + quad[1].y;
			this.oldResizer=resizer;
			
			poly.addPoint((int)(quad[0].x), (int)(quad[0].y));
			poly.addPoint((int)(quad[1].x), (int)(quad[1].y));
			poly.addPoint((int)(quad[3].x), (int)(quad[3].y));
			poly.addPoint((int)(quad[2].x), (int)(quad[2].y));
			display.setColor(netColor);
			display.fill(poly);
			
			
			// Show LinkIds
			if (OTFClientControl.getInstance().getOTFVisConfig().drawLinkIds()){
			    float idSize = 4*OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth();
			    int fontSize = (int)idSize; 
			    float middleX = (float)(0.5*this.quad[0].x + (0.5)*this.quad[3].x);
			    float middleY = (float)(0.5*this.quad[0].y + (0.5)*this.quad[3].y);
				Line2D line = new Line2D.Float(middleX, middleY, (float)(middleX + idSize),(float)(middleY + idSize));
				display.setColor(Color.blue);
				display.draw(line);
				java.awt.Font font_old = display.getFont();
				AffineTransform tx = new AffineTransform(1,0,0,-1,0,0);
				display.transform(tx);
				java.awt.Font font = new java.awt.Font("Arial Unicode MS", java.awt.Font.PLAIN, fontSize);
				display.setFont(font);
				display.drawString(this.id,(float)(middleX + 1.25*idSize),-(float)(middleY + 0.75*idSize));
				try {
					tx.invert();
				} catch (NoninvertibleTransformException e) {
					e.printStackTrace();
				}
				display.transform(tx);
				display.setFont(font_old);
			}
		    
			
			//display.setColor(Color.BLUE);
			//display.draw(poly);
		}

		@Override
		public void setId(char[] idBuffer) {
			this.id = String.valueOf(idBuffer);
		}
	}


	/***
	 * Drawer class for drawing agents
	 */
	public static class AgentDrawer extends OTFSwingDrawable implements OTFDataSimpleAgentReceiver{
		//Anything above 50km/h should be yellow!
		private final static ValueColorizer colorizer = new ValueColorizer(
				new double[] { 0.0, 30., 50.}, new Color[] {
						Color.RED, Color.YELLOW, Color.GREEN});

		protected char[] id;
		protected float startX, startY, color;
		protected int state;

		@Override
		public void setAgent(char[] id, float startX, float startY, int state, int user, float color) {
			this.id = id;
			this.startX = startX;
			this.startY = startY;
			this.color = color;
			this.state = state;
		}

		@Override
		public void setAgent( AgentSnapshotInfo agInfo ) {
			this.id = agInfo.getId().toString().toCharArray();
			this.startX = (float) agInfo.getEasting() ;
			this.startY = (float) agInfo.getNorthing() ;
			this.color = (float) agInfo.getColorValueBetweenZeroAndOne() ;
			this.state = agInfo.getAgentState().ordinal() ;
		}

		//		protected void setColor(Graphics2D display) {
		//			Color color = colorizer.getColor(0.1 + 0.9*this.color);
		//			if ((state & 1) != 0) {
		//				color = Color.lightGray;
		//			}
		//			display.setColor(color);
		//
		//		}
		//

		@Override
		public void onDraw(Graphics2D display) {
			Color color = colorizer.getColor(0.1 + 0.9*this.color);
			if ((state & 1) != 0) color = Color.lightGray;

			Point2D.Float pos = new Point2D.Float(startX, startY);
			// draw agent...
			//			final int lane = (RANDOMIZE_LANES ? (agent.hashCode()
			//			% lanes + 1) : agent.getLane());

			
			final double agentWidth = linkWidth *0.9;
			final double agentLength = agentWidth*0.9;
			float agentSize = OTFClientControl.getInstance().getOTFVisConfig().getAgentSize();
			final double offsetX = - 0.5 * agentLength;

			// there is only ONE displayvalue!
			if (state == 1 ) {
				display.setColor(Color.gray);
			} else {
				display.setColor(color);
			}
//			display.fillOval((int)Math.round(pos.x + offsetX), (int)pos.y, (int)Math.round(agentLength), (int)Math.round(agentWidth));
			display.fillOval((int)Math.round(pos.x + offsetX), (int)pos.y, (int)Math.round(agentSize), (int)Math.round(agentSize));
		}

	}


	public void clearCache() {
		if(quad != null) quad.clearCache();
	}

	public void handleClick(Double point, int mouseButton, MouseEvent e) {
		Point2D.Double origPoint = new Point2D.Double(point.x + this.quad.offsetEast, point.y + this.quad.offsetNorth);
		if(this.queryHandler != null) this.queryHandler.handleClick(this.quad.getId(), origPoint, mouseButton);
	}

	public void handleClick(Rectangle currentRect, int button) {
		Rectangle2D.Double origRect = new Rectangle2D.Double(currentRect.x + this.quad.offsetEast, currentRect.y + this.quad.offsetNorth, currentRect.width, currentRect.height);
		if(this.queryHandler != null) this.queryHandler.handleClick(this.quad.getId(), origRect, button);
	}
	
	public void setQueryHandler(OTFQueryHandler queryHandler) {
		if(queryHandler != null) this.queryHandler = queryHandler;
	}

}