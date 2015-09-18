/* *********************************************************************** *
 * project: org.matsim.*
 * QuerySpinne.java
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

package org.matsim.vis.otfvis.opengl.queries;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryOptions;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawable;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.snapshotwriters.VisLink;
import org.matsim.vis.snapshotwriters.VisVehicle;

import com.jogamp.common.nio.Buffers;

/**
 * QuerySpinne shows a relationship network for a given link based on several options.
 * The network shown is that of the routes that that agents take over the course of
 * their day/trip.
 *
 * @author dstrippgen
 */
public class QuerySpinne extends AbstractQuery implements OTFQueryOptions, ItemListener {

	public static class Result implements OTFQueryResult {

		private transient FloatBuffer vert = null;
		private transient ByteBuffer colors =  null;
		private transient InfoText agentText = null;
		private int[] count = null;
		private boolean calcOffset = true;
		private float[] vertex = null;
		private String linkIdString = null;
		private transient OTFOGLDrawer.FastColorizer colorizer3 = null;

		@Override
		public void draw(OTFOGLDrawer drawer) {
			if(this.vertex == null) return;

			if( this.calcOffset == true) {
				float east = (float)drawer.getQuad().offsetEast;
				float north = (float)drawer.getQuad().offsetNorth;

				this.calcOffset = false;
				for(int i = 0; i < this.vertex.length; i+=2) {
					this.vertex[i] -=east;
					this.vertex[i+1] -= north;
				}

				int maxCount = 0;
				for(int i= 0;i< this.count.length; i++) if (this.count[i] > maxCount) maxCount = this.count[i];

				colorizer3 = new OTFOGLDrawer.FastColorizer(
						new double[] { 0.0, maxCount}, new Color[] {
								Color.WHITE, Color.BLUE});

				this.colors = ByteBuffer.allocateDirect(this.count.length*4*2);

				for (int i = 0; i< this.count.length; i++) {
					Color mycolor = colorizer3.getColor(this.count[i]);
					this.colors.put((byte)mycolor.getRed());
					this.colors.put((byte)mycolor.getGreen());
					this.colors.put((byte)mycolor.getBlue());
					this.colors.put((byte)120);
					this.colors.put((byte)mycolor.getRed());
					this.colors.put((byte)mycolor.getGreen());
					this.colors.put((byte)mycolor.getBlue());
					this.colors.put((byte)120);
				}

				this.vert = Buffers.copyFloatBuffer(FloatBuffer.wrap(this.vertex));
				this.agentText = new InfoText(this.linkIdString, this.vertex[0], this.vertex[1] );
				this.agentText.draw(drawer.getTextRenderer(), OTFGLAbstractDrawable.getDrawable(), drawer.getViewBoundsAsQuadTreeRect());
			}

			this.vert.position(0);
			this.colors.position(0);

			GL2 gl = OTFGLAbstractDrawable.getGl();
			Color color = Color.ORANGE;
			gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,.3);
			gl.glColor4d(1., 1.,1.,.3);
			gl.glEnable(GL2.GL_BLEND);
			gl.glEnable(GL2.GL_LINE_SMOOTH);
			gl.glEnableClientState (GL2.GL_COLOR_ARRAY);
			gl.glEnableClientState (GL2.GL_VERTEX_ARRAY);
			gl.glLineWidth(2.f*OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth());
			gl.glVertexPointer (2, GL2.GL_FLOAT, 0, this.vert);
			gl.glColorPointer (4, GL2.GL_UNSIGNED_BYTE, 0, this.colors);
			gl.glDrawArrays (GL2.GL_LINES, 0, this.vertex.length/2);
			gl.glDisableClientState (GL2.GL_VERTEX_ARRAY);
			gl.glDisableClientState (GL2.GL_COLOR_ARRAY);
			gl.glDisable(GL2.GL_LINE_SMOOTH);
			gl.glDisable(GL2.GL_BLEND);

			drawCaption(drawer);
		}

		private void drawQuad(GL2 gl, double xs, double xe, double ys, double ye, Color color) {
			gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,color.getAlpha()/255.);
			double z = 0;
			gl.glBegin(GL2.GL_QUADS);
			gl.glVertex3d(xs, ys, z);
			gl.glVertex3d(xe, ys, z);
			gl.glVertex3d(xe, ye, z);
			gl.glVertex3d(xs, ye, z);
			gl.glEnd();

		}
		private void drawCaption(OTFOGLDrawer drawer) {
			QuadTree.Rect bounds = drawer.getViewBoundsAsQuadTreeRect();

			double maxX = bounds.minX + (bounds.maxX -bounds.minX)*0.22;
			double minX = bounds.minX + (bounds.maxX -bounds.minX)*0.01;
			double maxY = bounds.minY + (bounds.maxY -bounds.minY)*0.15;
			double minY = bounds.minY + (bounds.maxY -bounds.minY)*0.01;
			GL2 gl = OTFGLAbstractDrawable.getDrawable().getGL().getGL2();
			Color color = new Color(255,255,255,200);
			gl.glEnable(GL.GL_BLEND);
			drawQuad(gl, minX, maxX, minY, maxY, color);
			double horOf = (maxY-minY)/12;
			double verOf = (maxX-minX)/12;

			int maxCount = 0;
			for(int i= 0;i< this.count.length; i++) if (this.count[i] > maxCount) maxCount = this.count[i];

			Color c1 = colorizer3.getColor(0.0);
			Color c2 = colorizer3.getColor(maxCount/2.);
			Color c3 = colorizer3.getColor(maxCount);

			double a=1,b=4,c=1,d=3;
			drawQuad(gl, minX +a*verOf, minX+b*verOf, minY+c*horOf, minY+d*horOf, c1);
			InfoText text1 = new InfoText("Count: 0" , (float)(minX+(b+1)*verOf), (float) (minY+c*horOf));
			text1.draw(drawer.getTextRenderer(), OTFGLAbstractDrawable.getDrawable(), drawer.getViewBoundsAsQuadTreeRect());
			a=1;b=4;c=5;d=7;
			drawQuad(gl, minX +a*verOf, minX+b*verOf, minY+c*horOf, minY+d*horOf, c2);
			InfoText text2 = new InfoText("Count: " + (maxCount/2) , (float)(minX+(b+1)*verOf), (float) (minY+c*horOf));
			text2.draw(drawer.getTextRenderer(), OTFGLAbstractDrawable.getDrawable(), drawer.getViewBoundsAsQuadTreeRect());
			a=1;b=4;c=9;d=11;
			drawQuad(gl, minX +a*verOf, minX+b*verOf, minY+c*horOf, minY+d*horOf, c3);
			InfoText text3 = new InfoText("Count: " + (maxCount) , (float)(minX+(b+1)*verOf), (float) (minY+c*horOf));
			text3.draw(drawer.getTextRenderer(), OTFGLAbstractDrawable.getDrawable(), drawer.getViewBoundsAsQuadTreeRect());
		}

		@Override
		public void remove() {
			
		}

		@Override
		public boolean isAlive() {
			return false;
		}
	}

	protected Id<Link> queryLinkId;
	private transient Map<Id<Link>, Integer> drivenLinks = null;
	private SimulationViewForQueries simulationView;

	private static boolean tripOnly = false;
	private static boolean nowOnly = false;

	@Override
	public void itemStateChanged(ItemEvent e) {
		JCheckBox source = (JCheckBox)e.getItemSelectable();
		if (source.getText().equals("leg only")) {
			tripOnly = !tripOnly;
		} else if (source.getText().equals("only vehicles on the link now")) {
			nowOnly = ! nowOnly;
		}

	}

	@Override
	public JComponent getOptionsGUI(JComponent mother) {
		JPanel com = new JPanel();
		com.setSize(500, 60);
		JCheckBox SynchBox = new JCheckBox("leg only");
		SynchBox.setMnemonic(KeyEvent.VK_M);
		SynchBox.setSelected(false);
		SynchBox.addItemListener(this);
		com.add(SynchBox);
		SynchBox = new JCheckBox("only vehicles on the link now");
		SynchBox.setMnemonic(KeyEvent.VK_V);
		SynchBox.setSelected(false);
		SynchBox.addItemListener(this);
		com.add(SynchBox);
		return com;
	}

	private void addLink(Id<Link> linkId) {
		Integer count = this.drivenLinks.get(linkId);
		if (count == null) this.drivenLinks.put(linkId, 1);
		else  this.drivenLinks.put(linkId, count + 1);
	}

	private List<Plan> getPersonsNOW() {
		List<Plan> actPersons = new ArrayList<Plan>();
		VisLink link = simulationView.getVisNetwork().getVisLinks().get(this.queryLinkId);
		Collection<? extends VisVehicle> vehs = link.getAllVehicles();
		for( VisVehicle veh : vehs) {
			if ( veh.getDriver() instanceof PlanAgent ) {
				Plan plan = ((PlanAgent)veh.getDriver()).getCurrentPlan() ;
				actPersons.add( plan );
			}
		}
		return actPersons;
	}

	private List<Plan> getPersons(Map<Id<Person>, Plan> plans) {
		List<Plan> actPersons = new ArrayList<Plan>();
		for (Plan plan : plans.values()) {
			List<PlanElement> actslegs = plan.getPlanElements();
			for (PlanElement pe : actslegs) {
				if (pe instanceof Activity) {
					// handle act
					Activity act = (Activity) pe;
					Id<Link> id2 = act.getLinkId();
					if(id2.equals(this.queryLinkId)) {
						actPersons.add(plan);
						break;
					}
				} else if (pe instanceof Leg) {
					// handle leg
					Leg leg = (Leg) pe;
					// just look at car routes right now
					if(!leg.getMode().equals(TransportMode.car)) continue;
					for (Id<Link> id2 : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
						if(id2.equals(this.queryLinkId) ) {
							actPersons.add(plan);
							break;
						}
					}
				}
			}
		}
		return actPersons;
	}

	private void collectLinksFromLeg(List<Plan> actPersons) {
		boolean addthis = false;
		for (Plan plan : actPersons) {
			for (PlanElement pe : plan.getPlanElements()) {
				if ( pe instanceof Leg ) {
					Leg leg = (Leg) pe ;
					Route route = leg.getRoute();
					if ( route instanceof NetworkRoute ) { // added in jun09, see below in "collectLinks". kai, jun09
						List<Id<Link>> linkIds = new ArrayList<>();
						for (Id<Link> linkId : ((NetworkRoute) route).getLinkIds() ) {
							linkIds.add(linkId);
							if(linkId.equals(this.queryLinkId) ) {
								// only if this specific route includes link, add the route
								addthis = true;
							}
						}
						if(addthis) for (Id<Link> linkId : linkIds) addLink(linkId);
						addthis = false;
					}
				}
			}
		}
	}

	protected void collectLinks(List<Plan> actPersons) {
		for (Plan plan : actPersons) {
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					addLink(act.getLinkId());
				} else if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					Route route = leg.getRoute() ;
					if (route instanceof NetworkRoute) {
						NetworkRoute nr = (NetworkRoute) route ;
						for (Id<Link> linkId : nr.getLinkIds()) {
							addLink(linkId);
						}
					}
				}
			}
		}
	}

	@Override
	public void installQuery(SimulationViewForQueries simulationView) {
		this.simulationView = simulationView;
		this.result = new Result();
		result.linkIdString = this.queryLinkId.toString();
		this.drivenLinks = new HashMap<Id<Link>, Integer>();

		List<Plan> actPersons = nowOnly ? getPersonsNOW() : getPersons(simulationView.getPlans());

		if(tripOnly) collectLinksFromLeg(actPersons);
		else collectLinks(actPersons);

		if(this.drivenLinks.size() == 0) return;

		// convert this to drawable info
		result.vertex = new float[this.drivenLinks.size()*4];
		result.count = new int[this.drivenLinks.size()];
		int pos = 0;
		for(Id<Link> linkId : this.drivenLinks.keySet()) {
			Link link = simulationView.getNetwork().getLinks().get(linkId);
			result.count[pos/4] = this.drivenLinks.get(linkId);
			Node node = link.getFromNode();
			result.vertex[pos++] = (float)node.getCoord().getX();
			result.vertex[pos++] = (float)node.getCoord().getY();
			node = link.getToNode();
			result.vertex[pos++] = (float)node.getCoord().getX();
			result.vertex[pos++] = (float)node.getCoord().getY();
		}
	}

	private Result result;

	@Override
	public Type getType() {
		return OTFQuery.Type.LINK;
	}

	@Override
	public void setId(String id) {
		this.queryLinkId = Id.create(id, Link.class);
	}

	@Override
	public OTFQueryResult query() {
		return result;
	}

}
