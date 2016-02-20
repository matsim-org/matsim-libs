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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawable;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;

import com.jogamp.common.nio.Buffers;

/**
 * QueryAgentPTBus draws certain public transport related informations.
 */
public class QueryAgentPTBus extends AbstractQuery {

	public static class Result implements OTFQueryResult {

		private final List<String> allIds;
		private float[] vertex = null;
		private transient FloatBuffer vert;
		private boolean calcOffset = true;

		public Result(final List<String> allIds) {
			this.allIds = allIds;
		}

		@Override
		public void draw(OTFOGLDrawer drawer) {
			if(this.vertex == null) return;

			OGLAgentPointLayer layer = drawer.getCurrentSceneGraph().getAgentPointLayer();

			if( this.calcOffset == true) {
				float east = (float)drawer.getQuad().offsetEast;
				float north = (float)drawer.getQuad().offsetNorth;

				this.calcOffset = false;
				for(int i = 0; i < this.vertex.length; i+=2) {
					this.vertex[i] -=east;
					this.vertex[i+1] -= north;
				}
				this.vert = Buffers.copyFloatBuffer(FloatBuffer.wrap(this.vertex));

			}

			GL2 gl = OTFGLAbstractDrawable.getGl().getGL2();
			gl.glEnable(GL.GL_BLEND);
			gl.glColor4d(0.6, 0.0,0.2,.2);

			gl.glEnable(GL.GL_LINE_SMOOTH);
			gl.glEnableClientState (GL2.GL_VERTEX_ARRAY);
			gl.glLineWidth(1.f*OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth());
			gl.glVertexPointer (2, GL.GL_FLOAT, 0, this.vert);
			gl.glDrawArrays (GL2.GL_LINE_STRIP, 0, this.vertex.length/2);
			gl.glDisableClientState (GL2.GL_VERTEX_ARRAY);
			gl.glDisable(GL2.GL_LINE_SMOOTH);

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
				InfoText text = new InfoText("Bus " + id, (float)pos.x+ 250, (float)pos.y+ 250);
				text.draw(drawer.getTextRenderer(), OTFGLAbstractDrawable.getDrawable(), drawer.getViewBoundsAsQuadTreeRect());

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

	private String agentId;
	private Result result;
	private final List<String> allIds = new LinkedList<String>();

	private Network net = null;


	@Override
	public void setId(String id) {
		this.agentId = id;
	}

	private float[] buildRoute(Plan plan) {
		List<Id<Link>> drivenLinks = new LinkedList<Id<Link>> ();

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
				for (Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
					drivenLinks.add(linkId);
				}
			}
		}

		if(drivenLinks.size() == 0) return null;

		// convert this to drawable info
		float[] vertex = new float[drivenLinks.size()*2];
		int pos = 0;
		for(Id<Link> linkId : drivenLinks) {
			Link link = this.net.getLinks().get(linkId);
			Node node = link.getFromNode();
			vertex[pos++] = (float)node.getCoord().getX();
			vertex[pos++] = (float)node.getCoord().getY();
		}
		return vertex;
	}

	@Override
	public void installQuery(SimulationViewForQueries simulationView) {
		this.net = simulationView.getNetwork();
		this.result = new Result(this.allIds);
		String prefix = agentId + "-";
		for(Id<Person> planId : simulationView.getPlans().keySet()) {
			if(planId.toString().startsWith(prefix, 0)) allIds.add(planId.toString());
		}
		if (allIds.size()==0) return;
		Plan plan = simulationView.getPlans().get(Id.create(allIds.get(0), Person.class));
		this.result.vertex = buildRoute(plan);
	}


	public static void drawCircle(GL2 gl, float x, float y, float size) {
		float w = 40;

		gl.glLineWidth(2);
		gl.glEnable(GL2.GL_LINE_SMOOTH);
		gl.glBegin(GL2.GL_LINE_STRIP);
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