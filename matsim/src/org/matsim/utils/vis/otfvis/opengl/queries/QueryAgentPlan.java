/* *********************************************************************** *
 * project: org.matsim.*
 * QueryAgentPlan.java
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
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;

import javax.media.opengl.GL;

import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.BasicLeg.Mode;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;
import org.matsim.utils.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.utils.vis.otfvis.opengl.gl.InfoText;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

import com.sun.opengl.util.BufferUtil;

public class QueryAgentPlan implements OTFQuery {

	private static final long serialVersionUID = -8415337571576184768L;

	private static class MyInfoText implements Serializable{

		private static final long serialVersionUID = 1L;
		float east, north;
		String name;
		public MyInfoText(float east, float north, String name) {
			this.east = east;
			this.north = north;
			this.name = name;
		}
	}

	protected String agentId;
	private float[] vertex = null;
	private byte[] colors = null;
	private transient FloatBuffer vert;
	private Object [] acts;
	private transient InfoText agentText = null;
	private int lastActivity = -1;
	private ByteBuffer cols; 

	private boolean calcOffset = true;

	public void setId(String id) {
		this.agentId = id;
	}

	private static int countLines(Plan plan) {
		int count = 0;
		List actslegs = plan.getActsLegs();

		for (int i= 0; i< actslegs.size(); i++) {
			if(i%2==0) {
				// handle act
				count++;
			} else {
				// handle leg 
				Leg leg = (Leg)actslegs.get(i);

				if (leg.getMode().equals(Mode.car)) {
					List<Link> route = ((CarRoute) leg.getRoute()).getLinks();
					count += route.size();
					if(route.size() != 0) count++; //add last position if there is a path
				}
			}
		}
		return count;
	}

	protected void setCol(int pos, Color col) {
		this.colors[pos*4 +0 ] = (byte)col.getRed();
		this.colors[pos*4 +1 ] = (byte)col.getGreen();
		this.colors[pos*4 +2 ] = (byte)col.getBlue();
		this.colors[pos*4 +3 ] = (byte)128;
	}

	protected void setCoord(int pos, Coord coord, Color col) {
		this.vertex[pos*2 +0 ] = (float)coord.getX();
		this.vertex[pos*2 +1 ] = (float)coord.getY();
		setCol(pos, col);
	}

	public void buildRoute(Plan plan) {
		int count = countLines(plan);
		if(count == 0) return;

		int pos = 0;
		this.vertex = new float[count*2];
		this.colors = new byte[count*4]; //BufferUtil.newByteBuffer(count*4);

		Color carColor = Color.ORANGE;
		Color actColor = Color.BLUE;
		Color ptColor = Color.RED;

		List actslegs = plan.getActsLegs();
		for (int i= 0; i< actslegs.size(); i++) {
			if(i%2==0) {
				// handle act
				Color col = actColor;
				Act act = (Act)plan.getActsLegs().get(i);
				Coord coord = act.getCoord();
				if (coord == null) coord = act.getLink().getCenter();
				setCoord(pos++, coord, col);
			} else {
				// handle leg 
				Leg leg = (Leg)actslegs.get(i);

				if (leg.getMode().equals(Mode.car)) {
					Node last = null;
					for (Link driven : ((CarRoute) leg.getRoute()).getLinks()) {
						Node node = driven.getFromNode();
						last = driven.getToNode();
						setCoord(pos++, node.getCoord(), carColor);
					}
					if(last != null) setCoord(pos++, last.getCoord(), carColor);
				} else {
					setCol(pos-1, ptColor); // replace act Color with pt color... here we need walk etc too
				}
			}
		}
	}

	public void query(QueueNetwork net, Population plans, Events events, OTFServerQuad quad) {
		Person person = plans.getPerson(this.agentId);
		if (person == null) return;

		Plan plan = person.getSelectedPlan();

		this.acts = new Object [plan.getActsLegs().size()/2];

		for (int i=0;i< this.acts.length; i++) {
			Act act = (Act)plan.getActsLegs().get(i*2);
			Coord coord = act.getCoord();
			if (coord == null) coord = act.getLink().getCenter();
			this.acts[i] = new MyInfoText( (float)coord.getX(), (float)coord.getY(), act.getType());
		}

		buildRoute(plan);

	}

	public void draw(OTFDrawer drawer) {
		if(drawer instanceof OTFOGLDrawer) {
			draw((OTFOGLDrawer)drawer);
		}
	}

	public static void drawCircle(GL gl, float x, float y, float size) {
		float w = 40;

		gl.glLineWidth(2);
		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glBegin(GL.GL_LINE_STRIP);
		for (float f = 0; f < w;) {
			gl.glVertex3d(Math.cos(f)*size + x, Math.sin(f)*size + y,0);
			f += (2*Math.PI/w);
		}
		gl.glEnd();
		gl.glDisable(GL.GL_LINE_SMOOTH);
	}

	public void draw(OTFOGLDrawer drawer) {
		if(this.vertex == null) return;

		OGLAgentPointLayer layer = (OGLAgentPointLayer) drawer.getActGraph().getLayer(AgentPointDrawer.class);
		Point2D.Double pos = layer.getAgentCoords(this.agentId.toCharArray());

		if( this.calcOffset == true) {
			float east = (float)drawer.getQuad().offsetEast;
			float north = (float)drawer.getQuad().offsetNorth;

			this.calcOffset = false;
			for(int i = 0; i < this.vertex.length; i+=2) {
				this.vertex[i] -=east;
				this.vertex[i+1] -= north;
			}
			this.vert = BufferUtil.copyFloatBuffer(FloatBuffer.wrap(this.vertex));
			this.cols = BufferUtil.copyByteBuffer(ByteBuffer.wrap(this.colors));
			for (int i=0;i< this.acts.length; i++) {
				MyInfoText inf = (MyInfoText)this.acts[i];
				this.acts[i] = InfoText.showTextPermanent(inf.name, inf.east - east, inf.north - north, -0.001f );
				((InfoText)this.acts[i]).setAlpha(0.5f);
			}

			if (pos != null) {
				this.agentText = InfoText.showTextPermanent(this.agentId, (float)pos.x, (float)pos.y, -0.0005f );
				this.agentText.setAlpha(0.7f);
			}
			//InfoText.showText("Agent selected...");
		}

		GL gl = drawer.getGL();
		Color color = Color.ORANGE;
		gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,.5);

		gl.glEnable(GL.GL_BLEND);
		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glEnableClientState (GL.GL_COLOR_ARRAY);
		gl.glEnableClientState (GL.GL_VERTEX_ARRAY);
		vert.position(0);
		cols.position(0);
		gl.glLineWidth(1.f*((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getLinkWidth());
		gl.glColorPointer (4, GL.GL_UNSIGNED_BYTE, 0, cols);
		gl.glVertexPointer (2, GL.GL_FLOAT, 0, this.vert);
		gl.glDrawArrays (GL.GL_LINE_STRIP, 0, this.vertex.length/2);
		gl.glDisableClientState (GL.GL_VERTEX_ARRAY);
		gl.glDisableClientState (GL.GL_COLOR_ARRAY);
		gl.glDisable(GL.GL_LINE_SMOOTH);
		if (pos != null) {
			//System.out.println("POS: " + pos.x + ", " + pos.y);
			gl.glColor4f(0.f, 0.2f, 1.f, 0.5f);//Blue
			gl.glLineWidth(2);
			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3d((float)pos.x + 50, (float)pos.y + 50,0);
			gl.glVertex3d((float)pos.x +250, (float)pos.y +250,0);
			gl.glEnd();
			drawCircle(gl, (float)pos.x, (float)pos.y, 200.f);
			if(this.agentText != null) {
				this.agentText.x = (float)pos.x+ 250;
				this.agentText.y = (float)pos.y + 250;
			}
			// reset any old progressbars
			if (this.lastActivity >= 0) ((InfoText)this.acts[this.lastActivity]).fill = 0.0f;
		} else {
			QueryAgentActivityStatus query = new QueryAgentActivityStatus();
			query.setId(this.agentId);
			query.setNow(drawer.getActGraph().getTime());
			query = (QueryAgentActivityStatus) drawer.getQuad().doQuery(query);
			if ((query != null) && (query.activityNr != -1) && (query.activityNr < this.acts.length)) {
				InfoText posT = ((InfoText)this.acts[query.activityNr]);
				posT.color = new Color(255,50,50,180);
				// draw progressline underneath
				posT.fill = (float)query.finished;
				this.lastActivity = query.activityNr;
			}
		}

		gl.glDisable(GL.GL_BLEND);

	}

	public void remove() {
		// Check if we have already generated InfoText Objects, otherwise drop deleting
		if (this.calcOffset == true) return;
		if (this.acts != null) {
			for (int i=0;i< this.acts.length; i++) {
				InfoText inf = (InfoText)this.acts[i];
				if(inf != null) InfoText.removeTextPermanent(inf);
			}
		}
		if (this.agentText != null) InfoText.removeTextPermanent(this.agentText);
	}

	public boolean isAlive() {
		return false;
	}

	public Type getType() {
		return OTFQuery.Type.AGENT;
	}

}