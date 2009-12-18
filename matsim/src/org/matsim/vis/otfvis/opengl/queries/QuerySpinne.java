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

import javax.media.opengl.GL;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.ptproject.qsim.QueueVehicle;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryOptions;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.InfoText;

import com.sun.opengl.util.BufferUtil;

/**
 * QuerySpinne shows a relationship network for a given link based on several options.
 * The network shown is that of the routes that that agents take over the course of 
 * their day/trip.
 * 
 * @author dstrippgen
 *
 */
public class QuerySpinne implements OTFQuery, OTFQueryOptions, ItemListener {

	private static final long serialVersionUID = -749787121253826794L;
	protected Id linkId;
	private transient Map<Link,Integer> drivenLinks = null;
	private float[] vertex = null;
	private int[] count = null;
	private boolean calcOffset = true;
	private static boolean tripOnly = false;
	private static boolean nowOnly = false;
	private transient FloatBuffer vert;
//	private transient FloatBuffer cnt;
	private transient ByteBuffer colors =  null;
	private transient OTFOGLDrawer.FastColorizer colorizer3;

	public void itemStateChanged(ItemEvent e) {
		JCheckBox source = (JCheckBox)e.getItemSelectable();
		if (source.getText().equals("leg only")) {
			tripOnly = !tripOnly;
		} else if (source.getText().equals("only vehicles on the link now")) {
			nowOnly = ! nowOnly;
		}	
		
	}
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
	
	private void addLink(Link driven) {
		Integer count = this.drivenLinks.get(driven);
		if (count == null) this.drivenLinks.put(driven, 1);
		else  this.drivenLinks.put(driven, count + 1);
	}

	protected List<Plan> getPersonsNOW(Population plans, QueueNetwork net) {
		List<Plan> actPersons = new ArrayList<Plan>();
		QueueLink link = net.getLinks().get(linkId);
		Collection<QueueVehicle> vehs = link.getAllVehicles();
		for( QueueVehicle veh : vehs) actPersons.add(veh.getDriver().getPerson().getSelectedPlan());
		
		return actPersons;
	}

	protected List<Plan> getPersons(Population plans, QueueNetwork net) {
		List<Plan> actPersons = new ArrayList<Plan>();

		for (Person person : plans.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			List actslegs = plan.getPlanElements();
			for (int i= 0; i< actslegs.size(); i++) {
				if( i%2 == 0) {
					// handle act
					ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
					Id id2 = act.getLink().getId();
					if(id2.equals(this.linkId)) {
						actPersons.add(plan);
						break;
					}
				} else {
					// handle leg
					LegImpl leg = (LegImpl)actslegs.get(i);
					// just look at car routes right now
					if(leg.getMode() != TransportMode.car) continue;
					for (Link link : ((NetworkRouteWRefs) leg.getRoute()).getLinks()) {
						Id id2 = link.getId();
						if(id2.equals(this.linkId) ) {
							actPersons.add(plan);
							break;
						}
					}
				}
			}
		}
		return actPersons;
	}
	
	protected void collectLinksFromTrip(List<Plan> actPersons) {
		// TODO kai Despite the name, this collects links from the "leg", not from the trip.  kai, jun09
		boolean addthis = false;
		for (Plan plan : actPersons) {
			for (PlanElement pe : plan.getPlanElements()) {

//				if( i%2 == 0) {
//					// handle act
//					Activity act = (Activity)plan.getPlanElements().get(i);
//					Id id2 = act.getLink().getId();
//					if(id2.equals(this.linkId)) {
//						// only if act is ON the link add +1 to linkcounter
//						addLink(act.getLink());
//						addthis = true;
//					}
// I don't think that it is very plausible to include this, and since it makes the code longer, I removed it. kai, jun09				
				
				if ( pe instanceof LegImpl ) {
					LegImpl leg = (LegImpl) pe ;
					RouteWRefs route = leg.getRoute();
					if ( route instanceof NetworkRouteWRefs ) { // added in jun09, see below in "collectLinks". kai, jun09
						List<Link> links = new ArrayList<Link>();
						for (Link link : ((NetworkRouteWRefs) route).getLinks() ) {
							links.add(link);
							if(link.getId().equals(this.linkId) ) {
								// only if this specific route includes link, add the route
								addthis = true;
							}
						}
						if(addthis) for (Link link : links) addLink(link);
						addthis = false;
					}
				}

			}

		}
	}

	protected void collectLinks(List<Plan> actPersons) {
		for (Plan plan : actPersons) {
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					addLink(act.getLink());
				} else if (pe instanceof LegImpl) {
					LegImpl leg = (LegImpl) pe;

//					for (Link link : ((NetworkRoute) leg.getRoute()).getLinks()) {
					/* I regularly got an exception with the above line:
					 * Exception in thread "AWT-EventQueue-0" java.lang.ClassCastException: org.matsim.core.population.routes.GenericRouteImpl
					 *	at org.matsim.vis.otfvis.opengl.queries.QuerySpinne.collectLinksFromTrip(QuerySpinne.java:XXX)
					 *  ...
					 * I assume that it comes from the fact that some routes are not network routes (but, say, pt routes).
					 * So I included the instanceof check (see below).  Should be done in ALL the queries.  kai, jun09
					 */

					RouteWRefs route = leg.getRoute() ;
					if ( route instanceof NetworkRouteWRefs ) {
						NetworkRouteWRefs nr = (NetworkRouteWRefs) route ;
						for (Link link : nr.getLinks() ) {
							addLink(link);
						}
					}
				}
			}
		}
	}
	
	public OTFQuery query(QueueNetwork net, Population plans, EventsManager events, OTFServerQuad2 quad) {
		this.drivenLinks = new HashMap<Link,Integer> ();
//		QueueLink link = net.getQueueLink(this.linkId);
//		String start = link.getLink().getFromNode().getId().toString();
//		String end = link.getLink().getToNode().getId().toString();
		
		List<Plan> actPersons = nowOnly ? getPersonsNOW(plans, net) : getPersons(plans, net);

		if(tripOnly) collectLinksFromTrip(actPersons);
		else collectLinks(actPersons);
		
		if(this.drivenLinks.size() == 0) return this;

		// convert this to drawable info
		this.vertex = new float[this.drivenLinks.size()*4];
		this.count = new int[this.drivenLinks.size()];
		int pos = 0;
		for(Link qlink : this.drivenLinks.keySet()) {
			this.count[pos/4] = this.drivenLinks.get(qlink);
			Node node = qlink.getFromNode();
			this.vertex[pos++] = (float)node.getCoord().getX();
			this.vertex[pos++] = (float)node.getCoord().getY();
			node = qlink.getToNode();
			this.vertex[pos++] = (float)node.getCoord().getX();
			this.vertex[pos++] = (float)node.getCoord().getY();
		}
		return this;
	}

	public void draw(OTFDrawer drawer) {
		if(drawer instanceof OTFOGLDrawer) {
			draw((OTFOGLDrawer)drawer);
		}
	}

	private transient InfoText agentText = null;

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

			this.vert = BufferUtil.copyFloatBuffer(FloatBuffer.wrap(this.vertex));
			this.agentText = InfoText.showTextPermanent(this.linkId.toString(), this.vertex[0], this.vertex[1], -0.0005f );
	}

		this.vert.position(0);
		this.colors.position(0);

		GL gl = drawer.getGL();
		Color color = Color.ORANGE;
		gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,.3);
		gl.glColor4d(1., 1.,1.,.3);
		gl.glEnable(GL.GL_BLEND);
		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glEnableClientState (GL.GL_COLOR_ARRAY);
		gl.glEnableClientState (GL.GL_VERTEX_ARRAY);
		gl.glLineWidth(2.f*((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getLinkWidth());
		gl.glVertexPointer (2, GL.GL_FLOAT, 0, this.vert);
		gl.glColorPointer (4, GL.GL_UNSIGNED_BYTE, 0, this.colors);
		gl.glDrawArrays (GL.GL_LINES, 0, this.vertex.length/2);
		gl.glDisableClientState (GL.GL_VERTEX_ARRAY);
		gl.glDisableClientState (GL.GL_COLOR_ARRAY);
		gl.glDisable(GL.GL_LINE_SMOOTH);
		gl.glDisable(GL.GL_BLEND);
		
		drawCaption(drawer);
	}

	private void drawQuad(GL gl, double xs, double xe, double ys, double ye, Color color) {
		gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,color.getAlpha()/255.);
		double z = 0;
		gl.glBegin(GL.GL_QUADS);
		gl.glVertex3d(xs, ys, z);
		gl.glVertex3d(xe, ys, z);
		gl.glVertex3d(xe, ye, z);
		gl.glVertex3d(xs, ye, z);
		gl.glEnd();
		
	}
	private void drawCaption(OTFOGLDrawer drawer) {
		QuadTree.Rect bounds = drawer.getViewBounds();
		
		double maxX = bounds.minX + (bounds.maxX -bounds.minX)*0.22;
		double minX = bounds.minX + (bounds.maxX -bounds.minX)*0.01;
		double maxY = bounds.minY + (bounds.maxY -bounds.minY)*0.15;
		double minY = bounds.minY + (bounds.maxY -bounds.minY)*0.01;
		GL gl = drawer.getGL();
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
		InfoText.showTextOnce ("Count: 0" , (float)(minX+(b+1)*verOf), (float) (minY+c*horOf), (float) horOf*.07f);
		a=1;b=4;c=5;d=7;
		drawQuad(gl, minX +a*verOf, minX+b*verOf, minY+c*horOf, minY+d*horOf, c2);
		InfoText.showTextOnce ("Count: " + (maxCount/2) , (float)(minX+(b+1)*verOf), (float) (minY+c*horOf), (float) horOf*.07f);
		a=1;b=4;c=9;d=11;
		drawQuad(gl, minX +a*verOf, minX+b*verOf, minY+c*horOf, minY+d*horOf, c3);
		InfoText.showTextOnce ("Count: " + (maxCount) , (float)(minX+(b+1)*verOf), (float) (minY+c*horOf), (float) horOf*.07f);
	}
	public void remove() {
		if (this.agentText != null) InfoText.removeTextPermanent(this.agentText);
	}
	
	public boolean isAlive() {
		return false;
	}
	public Type getType() {
		return OTFQuery.Type.LINK;
	}

	public void setId(String id) {
		this.linkId = new IdImpl(id);
	}



}
