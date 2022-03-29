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

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.awt.TextRenderer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.PtConstants;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryOptions;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawable;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.GLUtils;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.snapshotwriters.PositionInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.jogamp.opengl.GL2GL3.GL_QUADS;
import static org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

/**
 * For a given agentID this QueryAgentPlan draws a visual representation of the
 * agent's day.
 *
 * @author dstrippgen
 * @author michaz
 */
public class QueryAgentPlan extends AbstractQuery implements OTFQueryOptions, ItemListener {

	private static final String INCLUDE_ROUTES = "include routes";

	private static boolean includeRoutes = true ;

	private static final Logger log = Logger.getLogger(QueryAgentPlan.class);

	private Id<Person> agentId;

	private SimulationViewForQueries simulationView;

	@Override
	public void installQuery(SimulationViewForQueries simulationView) {
		this.simulationView = simulationView;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		System.err.println("itemStateChange ...") ;
		JCheckBox source = (JCheckBox)e.getItemSelectable();
		if (source.getText().equals(INCLUDE_ROUTES)) {
			includeRoutes = !includeRoutes ;
			System.err.println("changing includeRoutes") ;
		}

	}

	@Override
	public JComponent getOptionsGUI(JComponent mother) {
		JPanel com = new JPanel();
		com.setSize(500, 60);
		JCheckBox synchBox = new JCheckBox(INCLUDE_ROUTES);
		synchBox.setSelected(true);
		synchBox.addItemListener(this);
		com.add(synchBox);
		return com;
	}

	@Override
	public OTFQueryResult query() {
		MobsimAgent agent = simulationView.getMobsimAgents().get(this.agentId);
		Plan plan = simulationView.getPlan(agent);
		Result result = new Result();
		result.agentId = this.agentId.toString();
		if (plan != null) {
			for (PlanElement e : plan.getPlanElements()) {
				if (e instanceof Activity) {
					Activity act = (Activity) e;
					if ( !includeRoutes && PtConstants.TRANSIT_ACTIVITY_TYPE.equals(act.getType()) ) {
						continue ; // skip
					}
					if ( StageActivityTypeIdentifier.isStageActivity( act.getType() )
							     && OTFClientControl.getInstance().getOTFVisConfig().getColoringScheme()== ColoringScheme.infection ) {

						continue ; // skip
					}
					Coord c2 = getCoord(act);
					ActivityInfo activityInfo = new ActivityInfo((float) c2.getX(), (float) c2.getY(), getSubstring( act ) );
					if ( StageActivityTypeIdentifier.isStageActivity( act.getType() ) ) {
						activityInfo = new ActivityInfo( (float) c2.getX(), (float) c2.getY(), act.getType().replace( "interaction", "i" ) );
					}
					result.acts.add(activityInfo);
				}
			}

			if ( includeRoutes ) {
				result.buildRoute(plan, agentId, simulationView.getNetwork(), Level.ROUTES, simulationView.getScenario() );
			} else {
				result.buildRoute(plan, agentId, simulationView.getNetwork(), Level.PLANELEMENTS, simulationView.getScenario() );
			}
		} else {
			log.error("No plan found for id " + this.agentId);
		}
		
		Activity act = simulationView.getCurrentActivity(agent);
		if (act != null) {
			Coord c2 = getCoord(act);
			if ( act.getStartTime().isDefined() && simulationView.getTime() > act.getStartTime().seconds()
					     && act.getEndTime().isDefined() && simulationView.getTime() <= act.getEndTime().seconds()) {
				ActivityInfo activityInfo = new ActivityInfo((float) c2.getX(), (float) c2.getY(), getSubstring( act ) );
				activityInfo.finished = (simulationView.getTime() - act.getStartTime().seconds()) / (act.getEndTime().seconds() - act.getStartTime().seconds());
				result.acts.add(activityInfo);
			}
		}
		return result;
	}
	private static String getSubstring( Activity act ){
		String substring = act.getType();
		if ( substring.length() >3 ){
			substring = act.getType().substring( 0, 3 );
		}
		return substring;
	}

	private Coord getCoord( Activity act) {
//		Coord coord = act.getCoord();
//		if (coord == null) {
//			Link link = simulationView.getNetwork().getLinks().get(act.getLinkId());
//			if ( link==null ) {
//				log.warn("could not find link belong to linkId=" + act.getLinkId() ) ;
//			}
//			coord = link.getCoord();
//		}

		Coord coord = PopulationUtils.decideOnCoordForActivity( act, simulationView.getScenario() ) ;

		return OTFServerQuadTree.getOTFTransformation().transform(coord);
	}

	@Override
	public Type getType() {
		return OTFQuery.Type.AGENT;
	}

	@Override
	public void setId(String id) {
		this.agentId = Id.create(id, Person.class);
	}

	@Override
	public void uninstall() {
	}


	private static final SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
	private static final PositionInfo.LinkBasedBuilder builder = new PositionInfo.LinkBasedBuilder().setLinkWidthCalculator(linkWidthCalculator);

	public static class Result implements OTFQueryResult {

		private String agentId;
		private final List<Coord> vertex = new ArrayList<>();
		private final List<Color> colors = new ArrayList<>();
		private FloatBuffer vert;
		private final List<ActivityInfo> acts = new ArrayList<>();
		private ByteBuffer cols;
		private TextRenderer textRenderer;

		public Result() {
		}

		private void buildRoute(Plan plan, Id<Person> agentId, Network net, Level level, Scenario scenario) {
			List<PlanElement> planElements = plan.getPlanElements();
			if (planElements.isEmpty()) {
				return;//non-plan agents may do not have a meaningful plan to be shown
			}
			for (PlanElement planElement : planElements) {
				if (planElement instanceof Activity) {
					Activity act = (Activity) planElement;
					Coord coord = act.getCoord();
					if (coord == null) {
//						final Id<Link> linkId = act.getLinkId();
						Id<Link> linkId = PopulationUtils.decideOnLinkIdForActivity(act, scenario);
						Link link = net.getLinks().get(linkId);
						var pi = builder
								.setPersonId(agentId)
								.setLinkId(link.getId())
								.setFromCoord(link.getFromNode().getCoord())
								.setToCoord(link.getToNode().getCoord())
								.setLinkLength(link.getLength())
								.setDistanceOnLink(0.9 * link.getLength())
								.setLane(0)
								.build();


						//AgentSnapshotInfo pi = snapshotInfoFactory.createAgentSnapshotInfo(agentId, link, 0.9*link.getLength(), 0);
						coord = new Coord(pi.getEasting(), pi.getNorthing());
					}
					addCoord(coord, Color.BLUE );
				} else if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					Color color = Color.lightGray;
					String mode = leg.getMode();
					if( mode.contains( TransportMode.car ) ){
						color = Color.ORANGE;
					} else if( mode.contains( TransportMode.pt ) ){
						color = Color.YELLOW;
					} else if( mode.contains( TransportMode.walk ) ){
						color = Color.GREEN;
					} else if( mode.contains( TransportMode.drt ) ){
						color = Color.RED;
					} else if( mode.equals( TransportMode.non_network_walk ) ){
						color = Color.GREEN;
					}
					if ( leg.getRoute() instanceof NetworkRoute && level==Level.ROUTES) {
						Link startLink = net.getLinks().get(leg.getRoute().getStartLinkId());
						Coord from = startLink.getToNode().getCoord();
						addCoord(from, color);
						for (Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
							Link driven = net.getLinks().get(linkId);
							Node node = driven.getToNode();
							Coord coord = node.getCoord();
							addCoord(coord, color);
						}
						Link endLink = net.getLinks().get(leg.getRoute().getEndLinkId());
						Coord to = endLink.getToNode().getCoord();
						addCoord(to, color);
					} else {
						Link fromLink = net.getLinks().get(leg.getRoute().getStartLinkId());
						Coord from;
						if (fromLink != null) {
							from = fromLink.getToNode().getCoord();
						} else {
							from = this.vertex.get(this.vertex.size()-1);
						}
						Link toLink = net.getLinks().get(leg.getRoute().getEndLinkId());
						Coord to;
						if (toLink != null) {
							to = toLink.getToNode().getCoord();
						} else {
							to = this.vertex.get(this.vertex.size() - 1);
						}

						Coord coord = CoordUtils.getCenter(from, to);
						addCoord(from, color);
						addCoord(coord, color);
						addCoord(to, color);
					}
				}
			}

			this.vert = Buffers.newDirectFloatBuffer(vertex.size()*2);
			this.vert.rewind();
			for (Coord coord : this.vertex) {
				Coord transform = OTFServerQuadTree.getOTFTransformation().transform(coord);
				this.vert.put((float) transform.getX());
				this.vert.put((float) transform.getY());
			}
			this.cols = Buffers.newDirectByteBuffer(colors.size()*4);
			this.cols.rewind();
			for (Color color : this.colors) {
				this.cols.put((byte) color.getRed());
				this.cols.put((byte) color.getGreen());
				this.cols.put((byte) color.getBlue());
				this.cols.put((byte) 255);
			}
			this.vert.position(0);
			this.cols.position(0);
		}


		private void addCoord(Coord coord, Color col) {
			vertex.add(coord);
			colors.add(col);
		}

		@Override
		public void draw(OTFOGLDrawer drawer) {
			if (textRenderer == null) {
				textRenderer = new TextRenderer(new Font("SansSerif", Font.PLAIN, 32), true, false);
				textRenderer.setColor(new Color(50, 50, 128, 255));
			}
			GL2 gl = OTFGLAbstractDrawable.getGl();
			if (vert != null) {
				drawPlanPolyLine(gl);
			}
			drawActivityTexts();
			Point2D.Double agentCoords = drawer.getCurrentSceneGraph().getAgentPointLayer().getAgentCoords(this.agentId.toCharArray());
			if (agentCoords != null) {
//				 We know where the agent is, so we draw stuff around them.
				drawArrowFromAgentToTextLabel(agentCoords, gl);
				drawCircleAroundAgent(agentCoords, gl);
				drawLabelText(drawer, agentCoords);
			}
		}

		private void drawPlanPolyLine(GL2 gl) {
			Color color = Color.ORANGE;
			gl.glColor4d(color.getRed() / 255., color.getGreen() / 255., color.getBlue() / 255., .5);
			gl.glEnable(GL2.GL_BLEND);
			gl.glEnable(GL2.GL_LINE_SMOOTH);
			gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
			gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glLineWidth(OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth());
			gl.glColorPointer(4, GL2.GL_UNSIGNED_BYTE, 0, cols);
			gl.glVertexPointer(2, GL2.GL_FLOAT, 0, this.vert);
			gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, this.vertex.size());
			gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
			gl.glDisable(GL2.GL_LINE_SMOOTH);
			gl.glDisable(GL2.GL_BLEND);
		}

		private static void drawArrowFromAgentToTextLabel(Point2D.Double pos, GL2 gl) {
			gl.glColor4f(0.f, 0.2f, 1.f, 0.5f);// Blue
			gl.glLineWidth(2);
			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3d((float) pos.x + 50, (float) pos.y + 50, 0);
			gl.glVertex3d((float) pos.x + 250, (float) pos.y + 250, 0);
			gl.glEnd();
		}

		private static void drawCircleAroundAgent(Point2D.Double pos, GL2 gl) {
			float scale = (float) OTFClientControl.getInstance().getMainOTFDrawer().getScale();
			float size = 10.f * scale;
			GLUtils.drawCircle(gl, (float) pos.x, (float) pos.y, size);
		}

		private void drawActivityTexts() {
			GLAutoDrawable drawable = OTFClientControl.getInstance().getMainOTFDrawer().getCanvas();
			for (ActivityInfo activityEntry : this.acts ) {
				drawTextBox(drawable, textRenderer, activityEntry);
			}
		}

		private void drawTextBox(GLAutoDrawable drawable, TextRenderer textRenderer, ActivityInfo activityEntry) {
			float scale = (float) OTFClientControl.getInstance().getMainOTFDrawer().getScale();

			// The size of the whole text box, including the progress bar.
			// Multiply it by scale so that it is independent of zoom factor.

			GL2 gl = (GL2) drawable.getGL();

			GLU glu = new GLU();
			gl.glPushMatrix();
			gl.glTranslatef(activityEntry.east, activityEntry.north, 0f);
			gl.glScalef(scale, scale, 1f);

			// Origin (0,0,0) is now the activity location.
			// That's where the bottom left corner of the text will go.
			textRenderer.begin3DRendering();
			textRenderer.draw3D(activityEntry.name, 0f, 0f, 0f, 1f);
			// Only the TextRenderer knows how big the letters are.
			// We memorize it so we can then draw a widget around the text.
			Rectangle2D textBounds = textRenderer.getBounds(activityEntry.name);
			textRenderer.end3DRendering();

			gl.glPushMatrix();
			gl.glEnable(GL.GL_BLEND);
			gl.glColor4f(0.9f, 0.9f, 0.9f, 0.9f);
			float halfh = (float)textBounds.getHeight()/2;
			gl.glTranslatef(0f, halfh, 0f);

			// Origin (0,0,0) is now the left end of the center line of the text.
			// Draw the left cap.
			GLUquadric quad1 = glu.gluNewQuadric();
			glu.gluPartialDisk(quad1, 0, halfh, 12, 2, 180, 180);
			glu.gluDeleteQuadric(quad1);
			// Draw the rectangle.
			gl.glBegin(GL_QUADS);
			gl.glVertex3d(0, -halfh, 0);
			gl.glVertex3d(0, halfh, 0);
			gl.glVertex3d(textBounds.getWidth(), halfh, 0);
			gl.glVertex3d(textBounds.getWidth(), -halfh, 0);
			gl.glEnd();
			gl.glPushMatrix();
			gl.glTranslatef((float)textBounds.getWidth(), 0f, 0f);
			// Origin (0,0,0) is now the right end of the center line of the text.
			// Draw the right cap.
			GLUquadric quad2 = glu.gluNewQuadric();
			glu.gluPartialDisk(quad2, 0, halfh, 12, 2, 0, 180);
			glu.gluDeleteQuadric(quad2);
			gl.glPopMatrix();

			// Origin (0,0,0) is now again the activity location.
			// Draw the progress bar.
			if (activityEntry.finished > 0f) {
				gl.glColor4f(0.9f, 0.7f, 0.7f, 0.9f);
				gl.glBegin(GL_QUADS);
				gl.glVertex3d(0, -halfh, 0);
				gl.glVertex3d(0, -halfh -7, 0);
				gl.glVertex3d(textBounds.getWidth(), -halfh -7, 0);
				gl.glVertex3d(textBounds.getWidth(), -halfh, 0);
				gl.glEnd();
				gl.glColor4f(0.9f, 0.5f, 0.5f, 0.9f);
				gl.glBegin(GL_QUADS);
				gl.glVertex3d(0, -halfh, 0);
				gl.glVertex3d(0, -halfh -7, 0);
				gl.glVertex3d(textBounds.getWidth()*activityEntry.finished, -halfh -7, 0);
				gl.glVertex3d(textBounds.getWidth()*activityEntry.finished, -halfh, 0);
				gl.glEnd();
			}
			gl.glPopMatrix();

			textRenderer.begin3DRendering();
			textRenderer.draw3D(activityEntry.name, 0f, 0f, 0f, 1f);
			textRenderer.end3DRendering();

			gl.glPopMatrix();
			gl.glDisable(GL.GL_BLEND);
		}

		private void drawLabelText(OTFOGLDrawer drawer, Point2D.Double pos) {
			InfoText agentText = new InfoText(this.agentId, (float) pos.x + 250, (float) pos.y + 250);
			agentText.setAlpha(0.7f);
			agentText.draw(textRenderer, OTFClientControl.getInstance().getMainOTFDrawer().getCanvas(), drawer.getViewBoundsAsQuadTreeRect());
		}

		@Override
		public void remove() {

		}

		@Override
		public boolean isAlive() {
			return true;
		}

	}

	private class ActivityInfo implements Serializable {

		private static final long serialVersionUID = 1L;
		public double finished;
		float east, north;
		String name;

		ActivityInfo(float east, float north, String name) {
			this.east = east;
			this.north = north;
			this.name = name;
		}
	}

	enum Level { ROUTES, PLANELEMENTS } 



}
