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
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL;

import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;
import org.matsim.utils.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.utils.vis.otfvis.opengl.gl.InfoText;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

import com.sun.opengl.util.BufferUtil;

public class QueryAgentPlan implements OTFQuery {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8415337571576184768L;

	private static class MyInfoText implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		float east, north;
		String name;
		public MyInfoText(float east, float north, String name) {
			this.east = east;
			this.north = north;
			this.name = name;
		}
	}

	public final String agentID;
	private transient List<Link> drivenLinks;
	private float[] vertex = null;
	private transient FloatBuffer vert;
	private Object [] acts;
	private transient InfoText agentText = null;
	private int lastActivity = -1;

	boolean calcOffset = true;

	public QueryAgentPlan(String agentID) {
		this.agentID = agentID;
	}

	public void query(QueueNetworkLayer net, Plans plans, Events events) {
		this.drivenLinks = new LinkedList<Link> ();
		Person person = plans.getPerson(this.agentID);
		if (person == null) return;

		Plan plan = person.getSelectedPlan();

		this.acts = new Object [plan.getActsLegs().size()/2];

		for (int i=0;i< this.acts.length; i++) {
			Act act = (Act)plan.getActsLegs().get(i*2);
			Link link = net.getQueueLink(act.getLinkId()).getLink();
			Node node = link.getToNode();
			this.acts[i] = new MyInfoText( (float)node.getCoord().getX(), (float)node.getCoord().getY(), act.getType());
		}

		List actslegs = plan.getActsLegs();
		for (int i= 0; i< actslegs.size(); i++) {
			if(i%2==0) {
				// handle act
				Act act = (Act)plan.getActsLegs().get(i);
				this.drivenLinks.add(act.getLink());
			} else {
				// handle leg
				Leg leg = (Leg)actslegs.get(i);
				Link[] route = leg.getRoute().getLinkRoute();
				for (Link driven : route) {
					this.drivenLinks.add(driven);
				}
			}
		}

		if(this.drivenLinks.size() == 0) return;

		// convert this to drawable info
		this.vertex = new float[this.drivenLinks.size()*2];
		int pos = 0;
		for(Link qlink : this.drivenLinks) {
			Node node = qlink.getFromNode();
			this.vertex[pos++] = (float)node.getCoord().getX();
			this.vertex[pos++] = (float)node.getCoord().getY();
		}
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
		Point2D.Double pos = layer.getAgentCoords(this.agentID.toCharArray());

		if( this.calcOffset == true) {
			float east = (float)drawer.getQuad().offsetEast;
			float north = (float)drawer.getQuad().offsetNorth;

			this.calcOffset = false;
			for(int i = 0; i < this.vertex.length; i+=2) {
				this.vertex[i] -=east;
				this.vertex[i+1] -= north;
			}
			this.vert = BufferUtil.copyFloatBuffer(FloatBuffer.wrap(this.vertex));
			for (int i=0;i< this.acts.length; i++) {
				MyInfoText inf = (MyInfoText)this.acts[i];
				this.acts[i] = InfoText.showTextPermanent(inf.name, inf.east - east, inf.north - north, -0.001f );
				((InfoText)this.acts[i]).setAlpha(0.7f);
			}

			if (pos != null) {
				this.agentText = InfoText.showTextPermanent(this.agentID, (float)pos.x, (float)pos.y, -0.0005f );
				this.agentText.setAlpha(0.7f);
			}
			//InfoText.showText("Agent selected...");
		}

		GL gl = drawer.getGL();
		Color color = Color.ORANGE;
		gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,.5);

        gl.glEnable(GL.GL_BLEND);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glEnableClientState (GL.GL_VERTEX_ARRAY);
		gl.glLineWidth(1.f*((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getLinkWidth());
		gl.glVertexPointer (2, GL.GL_FLOAT, 0, this.vert);
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
			// reset any old progressbars
			if (this.lastActivity >= 0) ((InfoText)this.acts[this.lastActivity]).fill = 0.0f;
		} else {
			QueryAgentActivityStatus query = (QueryAgentActivityStatus) drawer.getQuad().doQuery(new QueryAgentActivityStatus(this.agentID,drawer.getActGraph().getTime()));
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
}