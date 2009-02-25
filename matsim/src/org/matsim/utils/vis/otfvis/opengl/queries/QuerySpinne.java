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

package org.matsim.utils.vis.otfvis.opengl.queries;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Population;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;
import org.matsim.utils.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.utils.vis.otfvis.opengl.gl.InfoText;

import com.sun.opengl.util.BufferUtil;

public class QuerySpinne implements OTFQuery {

	private static final long serialVersionUID = -749787121253826794L;
	protected Id linkId;
	private transient Map<Link,Integer> drivenLinks = null;
	private float[] vertex = null;
	private int[] count = null;
	private boolean calcOffset = true;
	private transient FloatBuffer vert;
//	private transient FloatBuffer cnt;
	private transient ByteBuffer colors =  null;

	private void addLink(Link driven) {
		Integer count = this.drivenLinks.get(driven);
		if (count == null) this.drivenLinks.put(driven, 1);
		else  this.drivenLinks.put(driven, count + 1);
	}

	protected List<Plan> getPersons(Population plans, QueueNetwork net) {
		List<Plan> actPersons = new ArrayList<Plan>();

		for (Person person : plans.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			List actslegs = plan.getActsLegs();
			for (int i= 0; i< actslegs.size(); i++) {
				if( i%2 == 0) {
					// handle act
					Act act = (Act)plan.getActsLegs().get(i);
					Id id2 = act.getLink().getId();
					if(id2.equals(this.linkId)) {
						actPersons.add(plan);
						break;
					}
				} else {
					// handle leg
					Leg leg = (Leg)actslegs.get(i);
					for (Link link : ((CarRoute) leg.getRoute()).getLinks()) {
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
	
	public void query(QueueNetwork net, Population plans, Events events, OTFServerQuad quad) {
		this.drivenLinks = new HashMap<Link,Integer> ();
//		QueueLink link = net.getQueueLink(this.linkId);
//		String start = link.getLink().getFromNode().getId().toString();
//		String end = link.getLink().getToNode().getId().toString();
		
		List<Plan> actPersons = getPersons(plans, net);

		for (Plan plan : actPersons) {
			List actslegs = plan.getActsLegs();
			for (int i= 0; i< actslegs.size(); i++) {
				if( i%2 == 0) {
					// handle act
					Act act = (Act)plan.getActsLegs().get(i);
					addLink(act.getLink());
				} else {
					// handle leg
					Leg leg = (Leg)actslegs.get(i);
					for (Link link : ((CarRoute) leg.getRoute()).getLinks()) {
							addLink(link);
					}
				}

			}

		}
		if(this.drivenLinks.size() == 0) return;

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
			int maxCount = 0;
			for(int i = 0; i < this.vertex.length; i+=2) {
				this.vertex[i] -=east;
				this.vertex[i+1] -= north;
			}

			for(int i= 0;i< this.count.length; i++) if (this.count[i] > maxCount) maxCount = this.count[i];

			OTFOGLDrawer.FastColorizer colorizer3 = new OTFOGLDrawer.FastColorizer(
					new double[] { 0.0, maxCount}, new Color[] {
							Color.YELLOW, Color.RED});

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
