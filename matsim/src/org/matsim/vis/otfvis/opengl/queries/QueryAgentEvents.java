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

package org.matsim.vis.otfvis.opengl.queries;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.matsim.api.basic.v01.events.BasicPersonEvent;
import org.matsim.api.basic.v01.events.handler.BasicPersonEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.Events;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

import com.sun.opengl.util.BufferUtil;

/**
 * QueryAgentEvents shows a visual representation of the agents route along with the events happening
 * to the agent. The events will only show from the time the has first been selected. No past events will be shown.
 *  
 * @author dstrippgen
 *
 */
public class QueryAgentEvents extends QueryAgentPlan {

	private static final long serialVersionUID = -7388598935268835323L;

	public List<String> events = new ArrayList<String>();

	public  class MyEventsHandler implements BasicPersonEventHandler, Serializable{

		private static final long serialVersionUID = 1L;
		private final String agentId;
		public List<BasicPersonEvent> orig_events = new ArrayList<BasicPersonEvent>();
		
		public MyEventsHandler(String agentId) {
			this.agentId = agentId;
		}

		public void handleEvent(BasicPersonEvent event) {
			if(event.getPersonId().toString().equals(this.agentId)){
				orig_events.add(event);
			}
			
		}

		public void reset(int iteration) {
		}
		
		public List<String> getEvents () {
			events = new ArrayList<String>();
			for (BasicPersonEvent event : orig_events) {
				events.add(event.toString());
			}
			orig_events.clear();
			return events;
		}
	}

	private  List<InfoText> texts = null;
	//private  InfoText agentText = null;

	private boolean calcOffset = true;
	private MyEventsHandler handler = null;

	
	public OTFQuery query(QueueNetwork net, PopulationImpl plans, Events events, OTFServerQuad quad) {
		if(handler == null) {
			handler = new MyEventsHandler(agentId);
			events.addHandler(handler);
			PersonImpl person = plans.getPersons().get(new IdImpl(this.agentId));
			if (person != null) {
				PlanImpl plan = person.getSelectedPlan();
				buildRoute(plan);
			}
			this.events = handler.getEvents();
			return this;
		} else{
			return this.clone();
		}
	}

	public QueryAgentEvents clone() {
		QueryAgentEvents result = new QueryAgentEvents();
		result.handler = this.handler;
		result.vertex = this.vertex;
		result.texts = this.texts;
		result.agentText = this.agentText;
		result.agentId = this.agentId;
		result.calcOffset = this.calcOffset;
		result.events = handler.getEvents();

		return result;
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

			if (pos != null) {
				this.agentText = InfoText.showTextPermanent(this.agentId, (float)pos.x, (float)pos.y, -0.0005f );
				this.agentText.setAlpha(0.7f);
			}
			this.texts = new ArrayList<InfoText>();
			//InfoText.showText("Agent selected...");
		}

		GL gl = drawer.getGL();
		Color color = Color.ORANGE;
		gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,.5);
		FloatBuffer vert = BufferUtil.copyFloatBuffer(FloatBuffer.wrap(this.vertex));

		gl.glEnable(GL.GL_BLEND);
		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glEnableClientState (GL.GL_VERTEX_ARRAY);
		gl.glLineWidth(1.f*((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getLinkWidth());
		gl.glVertexPointer (2, GL.GL_FLOAT, 0, vert);
		gl.glDrawArrays (GL.GL_LINE_STRIP, 0, this.vertex.length/2);
		gl.glDisableClientState (GL.GL_VERTEX_ARRAY);
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

			int offset = 0;
			for(String event : events) {
				this.texts.add(InfoText.showTextPermanent(event,(float)pos.x + 150, (float)pos.y + 150 + 80*offset++,-0.0005f));
			}
			events.clear();

		}
		gl.glDisable(GL.GL_BLEND);

	}

	public void remove() {
		// Check if we have already generated InfoText Objects, otherwise drop deleting
		if (this.calcOffset == true) return;
		for (InfoText inf : this.texts) {
			if(inf != null) InfoText.removeTextPermanent(inf);
		}
		this.texts.clear();
		if (this.agentText != null) InfoText.removeTextPermanent(this.agentText);
		this.agentText = null;
	}
	
	// this must be done every time again until it is removed
	public boolean isAlive() {
		return true;
	}

	public Type getType() {
		return OTFQuery.Type.AGENT;
	}

}