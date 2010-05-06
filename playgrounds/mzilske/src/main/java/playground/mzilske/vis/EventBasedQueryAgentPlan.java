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

package playground.mzilske.vis;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
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
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFVisQSimFeature;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.DrawingUtils;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.gl.InfoTextContainer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;
import org.matsim.vis.otfvis.opengl.queries.AbstractQuery;
import org.matsim.vis.otfvis.opengl.queries.QueryQueueModel;
import org.matsim.vis.snapshots.writers.PositionInfo;

import com.sun.opengl.util.BufferUtil;

/**
 * For a given agentID this QueryAgentPlan draws a visual representation of the
 * agent's day.
 *
 * @author dstrippgen
 * @author michaz
 */
public class EventBasedQueryAgentPlan extends AbstractQuery {

	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(EventBasedQueryAgentPlan.class);

	private Id agentId;

	private transient Result result;
	
	@Override
	public OTFQueryResult query() {
		result.teleportingAgentPosition = null;
		queryActivityStatus();
		return result;
	}

	private void queryActivityStatus() {
		result.activityNr = -1;
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
	public void installQuery(Scenario scenario, QueryQueueModel queueModel) {
		result = new Result();
		result.agentId = this.agentId.toString();
		Person person = scenario.getPopulation().getPersons().get(this.agentId);
		if (person != null) {
			Plan plan = person.getSelectedPlan();
			for (PlanElement e : plan.getPlanElements()) {
				if (e instanceof Activity) {
					Activity act = (Activity) e;
					Coord coord = act.getCoord();
					if (coord == null) {
						Link link = scenario.getNetwork().getLinks().get(act.getLinkId());
						coord = link.getCoord();
					}
					result.acts.add(new MyInfoText((float) coord.getX(),
							(float) coord.getY(), act.getType()));
				}
			}
			buildRoute(plan, result, agentId, scenario.getNetwork());
			result.hasPlan = true;
		} else {
			log.error("No plan found for id " + this.agentId);
		}
	}

	@Override
	public void installQuery(OTFVisQSimFeature queueSimulation, EventsManager events, OTFServerQuad2 quad) {
		throw new UnsupportedOperationException();
	}

	public static class Result implements OTFQueryResult {

		private static final Logger log = Logger.getLogger(EventBasedQueryAgentPlan.class);

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
			activityTexts = null;
			agentText = null;
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

	public static void buildRoute(Plan plan, EventBasedQueryAgentPlan.Result result, Id agentId, Network net) {
		int count = countLines(plan);
		if (count == 0)
			return;

		int pos = 0;
		result.vertex = new float[count * 2];
		result.colors = new byte[count * 4];

		Color carColor = Color.ORANGE;
		Color actColor = Color.BLUE;
		Color ptColor = Color.YELLOW;
		Color walkColor = Color.MAGENTA;
		Color otherColor = Color.PINK;

		for (Object o : plan.getPlanElements()) {
			if (o instanceof Activity) {
				Color col = actColor;
				Activity act = (Activity) o;
				Coord coord = act.getCoord();
				if (coord == null) {
					assert (net != null);
					Link link = net.getLinks().get(act.getLinkId());
					PositionInfo pi = new PositionInfo(agentId, link);
					coord = new CoordImpl(pi.getEasting(), pi.getNorthing());
				}
				setCoord(pos++, coord, col, result);
			} else if (o instanceof Leg) {
				Leg leg = (Leg) o;
				if (leg.getMode().equals(TransportMode.car)) {
					Node last = null;
					for (Id linkId : ((NetworkRoute) leg.getRoute())
							.getLinkIds()) {
						Link driven = net.getLinks().get(linkId);
						Node node = driven.getFromNode();
						last = driven.getToNode();
						setCoord(pos++, node.getCoord(), carColor, result);
					}
					if (last != null) {
						setCoord(pos++, last.getCoord(), carColor, result);
					}
				} else if (leg.getMode().equals(TransportMode.pt)) {
					setColor(pos - 1, ptColor, result);
				} else if (leg.getMode().equals(TransportMode.walk)) {
					setColor(pos - 1, walkColor, result);
				} else {
					setColor(pos - 1, otherColor, result); // replace act Color with pt
					// color... here we need
					// walk etc too
				}
			} // end leg handling
		}
	}

	private static void setColor(int pos, Color col, EventBasedQueryAgentPlan.Result result) {
		result.colors[pos * 4 + 0] = (byte) col.getRed();
		result.colors[pos * 4 + 1] = (byte) col.getGreen();
		result.colors[pos * 4 + 2] = (byte) col.getBlue();
		result.colors[pos * 4 + 3] = (byte) 128;
	}

	private static void setCoord(int pos, Coord coord, Color col, EventBasedQueryAgentPlan.Result result) {
		result.vertex[pos * 2 + 0] = (float) coord.getX();
		result.vertex[pos * 2 + 1] = (float) coord.getY();
		setColor(pos, col, result);
	}


	private static int countLines(Plan plan) {
		int count = 0;
		for (Object o : plan.getPlanElements()) {
			if (o instanceof Activity) {
				count++;
			} else if (o instanceof Leg) {
				Leg leg = (Leg) o;
				if (leg.getMode().equals(TransportMode.car)) {
					List<Id> route = ((NetworkRoute) leg.getRoute())
					.getLinkIds();
					count += route.size();
					if (route.size() != 0)
						count++; // add last position if there is a path
				}
			}
		}
		return count;
	}

}