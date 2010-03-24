/* *********************************************************************** *
 * project: org.matsim.*
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

import java.awt.geom.Point2D;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFVisQSimFeature;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.InfoTextContainer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

import com.sun.opengl.util.BufferUtil;

/**
 * QueryAgentPTBus draws certain public transport related informations.
 *
 */
public class QueryAgentPTBus extends AbstractQuery {

	public static class Result implements OTFQueryResult {

		private static final long serialVersionUID = 1L;

		private final List<String> allIds;
		private float[] vertex = null;
		private transient FloatBuffer vert;
		private boolean calcOffset = true;

		public Result(final List<String> allIds) {
			this.allIds = allIds;
		}

		public void draw(OTFDrawer drawer) {
			if(drawer instanceof OTFOGLDrawer) {
				draw((OTFOGLDrawer)drawer);
			}
		}

		private void draw(OTFOGLDrawer drawer) {
			if(this.vertex == null) return;

			OGLAgentPointLayer layer = (OGLAgentPointLayer) drawer.getActGraph().getLayer(AgentPointDrawer.class);

			if( this.calcOffset == true) {
				float east = (float)drawer.getQuad().offsetEast;
				float north = (float)drawer.getQuad().offsetNorth;

				this.calcOffset = false;
				for(int i = 0; i < this.vertex.length; i+=2) {
					this.vertex[i] -=east;
					this.vertex[i+1] -= north;
				}
				this.vert = BufferUtil.copyFloatBuffer(FloatBuffer.wrap(this.vertex));

			}

			GL gl = drawer.getGL();
			gl.glEnable(GL.GL_BLEND);
			gl.glColor4d(0.6, 0.0,0.2,.2);

			gl.glEnable(GL.GL_LINE_SMOOTH);
			gl.glEnableClientState (GL.GL_VERTEX_ARRAY);
			gl.glLineWidth(1.f*OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth());
			gl.glVertexPointer (2, GL.GL_FLOAT, 0, this.vert);
			gl.glDrawArrays (GL.GL_LINE_STRIP, 0, this.vertex.length/2);
			gl.glDisableClientState (GL.GL_VERTEX_ARRAY);
			gl.glDisable(GL.GL_LINE_SMOOTH);

			for(String id : allIds) {
				Point2D.Double pos = layer.getAgentCoords(id.toCharArray());
				if (pos == null) continue;

				//System.out.println("POS: " + pos.x + ", " + pos.y);
				gl.glColor4f(0.2f, 0.4f, 0.4f, 0.5f);//Blue
				gl.glLineWidth(2);
				gl.glBegin(GL.GL_LINE_STRIP);
				gl.glVertex3d((float)pos.x + 50, (float)pos.y + 50,0);
				gl.glVertex3d((float)pos.x +250, (float)pos.y +250,0);
				gl.glEnd();
				drawCircle(gl, (float)pos.x, (float)pos.y, 100.f);
				InfoTextContainer.showTextOnce("Bus " + id, (float)pos.x+ 250, (float)pos.y+ 250, -0.0007f);

			}
			gl.glDisable(GL.GL_BLEND);

		}

		@Override
		public void remove() {

		}

		@Override
		public boolean isAlive() {
			return false;
		}

	}

	private static final long serialVersionUID = -8415337571576184768L;

	private String agentId;
	private Result result;
	private final List<String> allIds = new LinkedList<String>();

	private Network net = null;


	@Override
	public void setId(String id) {
		this.agentId = id;
	}

	private float[] buildRoute(Plan plan) {
		List<Id> drivenLinks = new LinkedList<Id> ();

		List<PlanElement> actslegs = plan.getPlanElements();
		for (PlanElement pe : actslegs) {
			if (pe instanceof Activity) {
				// handle act
				Activity act = (Activity)pe;
				drivenLinks.add(act.getLinkId());
			} else if (pe instanceof Leg) {
				// handle leg
				Leg leg = (Leg) pe;
				//if (!leg.getMode().equals("car")) continue;
				for (Id linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
					drivenLinks.add(linkId);
				}
			}
		}

		if(drivenLinks.size() == 0) return null;

		// convert this to drawable info
		float[] vertex = null;
		vertex = new float[drivenLinks.size()*2];
		int pos = 0;
		for(Id linkId : drivenLinks) {
			Link link = this.net.getLinks().get(linkId);
			Node node = link.getFromNode();
			vertex[pos++] = (float)node.getCoord().getX();
			vertex[pos++] = (float)node.getCoord().getY();
		}
		return vertex;
	}

	@Override
	public void installQuery(OTFVisQSimFeature queueSimulation, EventsManager events, OTFServerQuad2 quad) {
		this.net = queueSimulation.getQueueSimulation().getNetwork().getNetworkLayer();
		this.result = new Result(this.allIds);
		String prefix = agentId + "-";
		for(Person person : queueSimulation.getQueueSimulation().getScenario().getPopulation().getPersons().values()) {
			if(person.getId().toString().startsWith(prefix, 0)) allIds.add(person.getId().toString());
		}
		if (allIds.size()==0) return;
		Plan plan = queueSimulation.getQueueSimulation().getScenario().getPopulation().getPersons().get(new IdImpl(allIds.get(0))).getSelectedPlan();
		this.result.vertex = buildRoute(plan);
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



	@Override
	public Type getType() {
		return OTFQuery.Type.AGENT;
	}

	@Override
	public OTFQueryResult query() {
		return result;
	}

}