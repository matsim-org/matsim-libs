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
import java.awt.geom.Point2D.Double;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFVisQSimFeature;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.teleportation.TeleportationVisData;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.DrawingUtils;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.gl.InfoTextContainer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

import com.sun.opengl.util.BufferUtil;

/**
 * For a given agentID this QueryAgentPlan draws a visual representation of the
 * agent's day.
 *
 * @author dstrippgen
 * @author michaz
 */
public class QueryAgentPlan extends AbstractQuery {

	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(QueryAgentPlan.class);

	private Id agentId;

	private transient Result result;
	private transient Map<Id, TeleportationVisData> visTeleportationData;
	private transient QNetwork net;
	private transient OTFVisQSimFeature queueSimulation;


	@Override
	public OTFQueryResult query() {
		TeleportationVisData teleportationVisData = visTeleportationData.get(agentId);
		if (teleportationVisData != null) {
			double x = teleportationVisData.getX() - OTFServerQuad2.offsetEast;
			double y = teleportationVisData.getY() - OTFServerQuad2.offsetNorth;
			result.teleportingAgentPosition = new Point2D.Double(x, y);
			log.debug("Agent teleporting: "+x+" "+y);
		} else {
			result.teleportingAgentPosition = null;
		}
		queryActivityStatus();
		return result;
	}

	private void queryActivityStatus() {
		Integer currentActivityNumber = queueSimulation.getCurrentActivityNumbers().get(this.agentId);
		if (currentActivityNumber != null) {
			result.activityFinished = 0;
			result.activityNr = currentActivityNumber/2;
			log.debug("Agent is in activity " + result.activityNr);
		} else {
			result.activityNr = -1;
		}
	}

	@Override
	public Type getType() {
		return OTFQuery.Type.AGENT;
	}

	@Override
	public void setId(String id) {
		this.agentId = new IdImpl(id);
	}

	@Override
	public void installQuery(OTFVisQSimFeature queueSimulation, EventsManager events, OTFServerQuad2 quad) {
		this.queueSimulation = queueSimulation;
		this.net = queueSimulation.getQueueSimulation().getNetwork();
		this.visTeleportationData = queueSimulation.getVisTeleportationData();
		result = new Result();
		result.agentId = this.agentId.toString();
		Person person = queueSimulation.getQueueSimulation().getPopulation().getPersons().get(this.agentId);
		if (person != null) {
			Plan plan = person.getSelectedPlan();
			for (PlanElement e : plan.getPlanElements()) {
				if (e instanceof Activity) {
					Activity act = (Activity) e;
					Coord coord = act.getCoord();
					if (coord == null) {
						Link link = net.getLinks().get(act.getLinkId())
								.getLink();
						coord = link.getCoord();
					}
					result.acts.add(new MyInfoText((float) coord.getX(),
							(float) coord.getY(), act.getType()));
				}
			}
			QueryAgentUtils.buildRoute(plan, result, agentId, net.getNetworkLayer());
			result.hasPlan = true;
		} else {
			log.error("No plan found for id " + this.agentId);
		}
	}

	public static class Result implements OTFQueryResult {

		private static final Logger log = Logger.getLogger(QueryAgentPlan.class);

		private static final long serialVersionUID = -8415337571576184768L;

		/*package*/ String agentId;
		/*package*/ boolean hasPlan = false;
		/*package*/ Point2D.Double teleportingAgentPosition = null;
		protected float[] vertex = null;
		protected byte[] colors = null;
		private transient FloatBuffer vert;
		/*package*/ List<MyInfoText> acts = new ArrayList<MyInfoText>();
		private transient List<InfoText> activityTexts;
		protected transient InfoText agentText = null;
		private ByteBuffer cols;
		int activityNr = -1;
		double activityFinished = 0;

		private boolean calcOffset = true;

		public void draw(OTFDrawer drawer) {
			if (drawer instanceof OTFOGLDrawer) {
				drawWithGLDrawer((OTFOGLDrawer) drawer);
			} else {
				log.error("cannot draw query cause no OTFOGLDrawer is used!");
			}
		}

		protected void drawWithGLDrawer(OTFOGLDrawer drawer) {
			GL gl = drawer.getGL();
			if (hasPlan) {
				calcOffsetIfNecessary(drawer);
				rewindGLBuffers();
				prepare(gl);
				createActivityTextsIfNecessary(drawer);
			}
			OGLAgentPointLayer layer = (OGLAgentPointLayer) drawer.getActGraph().getLayer(AgentPointDrawer.class);
			Point2D.Double pos = tryToFindAgentPosition(layer);
			if (pos == null) {
				pos = teleportingAgentPosition;
			}
			if (pos != null) {
				// We know where the agent is, so we draw stuff around them.
				drawArrowFromAgentToTextLabel(pos, gl);
				drawCircleAroundAgent(pos, gl);
				createLabelTextIfNecessary(pos);
				updateAgentTextPosition(pos);
			} else {
				fillActivityLabel();
			}
			unPrepare(gl);
		}

		private Point2D.Double tryToFindAgentPosition(OGLAgentPointLayer layer) {
			Point2D.Double pos = getAgentPositionFromPointLayer(this.agentId,
					layer);
			return pos;
		}

		private void prepare(GL gl) {
			Color color = Color.ORANGE;
			gl.glColor4d(color.getRed() / 255., color.getGreen() / 255., color
					.getBlue() / 255., .5);
			gl.glEnable(GL.GL_BLEND);
			gl.glEnable(GL.GL_LINE_SMOOTH);
			gl.glEnableClientState(GL.GL_COLOR_ARRAY);
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glLineWidth(1.f * getLineWidth());
			gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, cols);
			gl.glVertexPointer(2, GL.GL_FLOAT, 0, this.vert);
			gl.glDrawArrays(GL.GL_LINE_STRIP, 0, this.vertex.length / 2);
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL.GL_COLOR_ARRAY);
			gl.glDisable(GL.GL_LINE_SMOOTH);
		}

		private void unPrepare(GL gl) {
			gl.glDisable(GL.GL_BLEND);
		}

		private void rewindGLBuffers() {
			vert.position(0);
			cols.position(0);
		}

		private float getLineWidth() {
			return OTFClientControl.getInstance().getOTFVisConfig()
					.getLinkWidth();
		}

		private void drawArrowFromAgentToTextLabel(Point2D.Double pos, GL gl) {
			gl.glColor4f(0.f, 0.2f, 1.f, 0.5f);// Blue
			gl.glLineWidth(2);
			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3d((float) pos.x + 50, (float) pos.y + 50, 0);
			gl.glVertex3d((float) pos.x + 250, (float) pos.y + 250, 0);
			gl.glEnd();
		}

		private void drawCircleAroundAgent(Point2D.Double pos, GL gl) {
			DrawingUtils.drawCircle(gl, (float) pos.x, (float) pos.y, 200.f);
		}

		private void updateAgentTextPosition(Point2D.Double pos) {
			if (this.agentText != null) {
				this.agentText.setX((float) pos.x + 250);
				this.agentText.setY((float) pos.y + 250);
			}
		}

		private Double getAgentPositionFromPointLayer(String agentIdString,
				OGLAgentPointLayer layer) {
			return layer.getAgentCoords(agentIdString.toCharArray());
		}

		private void fillActivityLabel() {
			if ((activityNr != -1) && (activityNr < this.acts.size())) {
				InfoText posT = this.activityTexts.get(activityNr);
				posT.setColor(new Color(255, 50, 50, 180));
				posT.setFill((float) this.activityFinished);
			}
		}

		private void calcOffsetIfNecessary(OTFOGLDrawer drawer) {
			if (this.calcOffset == true) {
				this.calcOffset = false;
				for (int i = 0; i < this.vertex.length; i += 2) {
					this.vertex[i] -= (float) drawer.getQuad().offsetEast;
					this.vertex[i + 1] -= (float) drawer.getQuad().offsetNorth;
				}
				this.vert = BufferUtil.copyFloatBuffer(FloatBuffer
						.wrap(this.vertex));
				this.cols = BufferUtil.copyByteBuffer(ByteBuffer
						.wrap(this.colors));
			}
		}

		private void createActivityTextsIfNecessary(OTFOGLDrawer drawer) {
			if (activityTexts == null)  {
				activityTexts = new ArrayList<InfoText>();
				for (MyInfoText activityEntry : this.acts ) {
					InfoText activityText = InfoTextContainer.showTextPermanent(
							activityEntry.name, activityEntry.east - (float) drawer.getQuad().offsetEast, activityEntry.north - (float) drawer.getQuad().offsetNorth,
							-0.001f);
					activityText.setAlpha(0.5f);
					this.activityTexts.add(activityText);
				}
			}
		}

		private void createLabelTextIfNecessary(Point2D.Double pos) {
			if (this.agentText == null) {
				this.agentText = InfoTextContainer.showTextPermanent(
						this.agentId, (float) pos.x, (float) pos.y,
						-0.0005f);
				this.agentText.setAlpha(0.7f);
			}
		}

		@Override
		public void remove() {
			if (this.activityTexts != null) {
				for (InfoText inf : this.activityTexts) {
					InfoTextContainer.removeTextPermanent(inf);
				}
			}
			if (this.agentText != null) {
				InfoTextContainer.removeTextPermanent(this.agentText);
			}
		}

		@Override
		public boolean isAlive() {
			return true;
		}

	}

	public static class MyInfoText implements Serializable {

		private static final long serialVersionUID = 1L;
		float east, north;
		String name;

		public MyInfoText(float east, float north, String name) {
			this.east = east;
			this.north = north;
			this.name = name;
		}
	}

}