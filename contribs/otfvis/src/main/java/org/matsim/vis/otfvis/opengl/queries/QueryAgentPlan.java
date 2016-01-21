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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryOptions;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawable;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.DrawingUtils;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import com.jogamp.common.nio.Buffers;

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

	private transient Result result;

	private SimulationViewForQueries simulationView;

	@Override
	public void installQuery(SimulationViewForQueries simulationView1) {
		this.simulationView = simulationView1;
		Network net = simulationView1.getNetwork();
		Plan plan = simulationView1.getPlans().get(this.agentId);
		result = new Result();
		result.agentId = this.agentId.toString();
		if (plan != null) {
			simulationView1.addTrackedAgent(this.agentId);
			fillResult(net, plan);
		} else {
			log.error("No plan found for id " + this.agentId);
		}
	}

	private void fillResult(Network net, Plan plan) {
		for (PlanElement e : plan.getPlanElements()) {
			if (e instanceof Activity) {
				Activity act = (Activity) e;
				if ( !includeRoutes && PtConstants.TRANSIT_ACTIVITY_TYPE.equals(act.getType()) ) {
					continue ; // skip
				}
				Coord coord = act.getCoord();
				if (coord == null) {
					Link link = net.getLinks().get(act.getLinkId());
					coord = link.getCoord();
				}
				Coord c2 = OTFServerQuadTree.getOTFTransformation().transform(coord);
				
				String txt = act.getType();
				// there used to be a "cake" diagram, showing which fraction of the activity duration was spent.  That somehow
				// is gone.  Outputting the activity end time instead.  kai, nov'15
				// But somehow the below does not work; it generates some exception.  Thus commented out again. kai, nov'15
//				 txt += "-"+Time.writeTime(act.getEndTime(), ':');
//				if ( act.getEndTime()== Time.UNDEFINED_TIME ) {
//					txt = act.getType() ;
//				}
				result.acts.add(new MyInfoText((float) c2.getX(), (float) c2.getY(), txt ) ) ;
			}
		}

		if ( includeRoutes ) {
			result.buildRoute(plan, agentId, net, Level.ROUTES ); 
		} else {
			result.buildRoute(plan, agentId, net, Level.PLANELEMENTS); 
		}
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
		return result;
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
		simulationView.removeTrackedAgent(this.agentId);
	}


	public static class Result implements OTFQueryResult {

		private String agentId;
		private List<Coord> vertex = new ArrayList<Coord>();
		private List<Color> colors = new ArrayList<Color>();
		private FloatBuffer vert;
		private List<MyInfoText> acts = new ArrayList<MyInfoText>();
		private List<InfoText> activityTexts;
		private InfoText agentText = null;
		private ByteBuffer cols;


		private void buildRoute(Plan plan, Id<Person> agentId, Network net, Level level) {
			Color carColor = Color.ORANGE;
			Color actColor = Color.BLUE;
			Color ptColor = Color.YELLOW;
			Color walkColor = Color.MAGENTA;
			Color otherColor = Color.PINK;
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity act = (Activity) planElement;
					Coord coord = act.getCoord();
					if (coord == null) {
						Link link = net.getLinks().get(act.getLinkId());
						AgentSnapshotInfo pi = snapshotInfoFactory.createAgentSnapshotInfo(agentId, link, 0.9*link.getLength(), 0);
						coord = new Coord(pi.getEasting(), pi.getNorthing());
					}
					addCoord(coord, actColor);
				} else if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					if ( leg.getRoute() instanceof NetworkRoute && level==Level.ROUTES) {
						Link startLink = net.getLinks().get(((NetworkRoute) leg.getRoute()).getStartLinkId());
						Coord from = startLink.getToNode().getCoord();
						addCoord(from, carColor);
						for (Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
							Link driven = net.getLinks().get(linkId);
							Node node = driven.getToNode();
							Coord coord = node.getCoord();
							addCoord(coord, carColor);
						}
						Link endLink = net.getLinks().get(((NetworkRoute) leg.getRoute()).getEndLinkId());
						Coord to = endLink.getToNode().getCoord();
						addCoord(to, carColor);
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
							to = this.vertex.get(this.vertex.size()-1);
						}
												
						Coord coord = CoordUtils.getCenter(from, to);
						if (leg.getMode().equals(TransportMode.car)) {
							addCoord(from, carColor);
							addCoord(coord, carColor);
							addCoord(to, carColor);
						} else if (leg.getMode().equals(TransportMode.pt)) {
							addCoord(from, ptColor);
							addCoord(coord, ptColor);
							addCoord(to, ptColor);
						} else if (leg.getMode().equals(TransportMode.walk)) {
							addCoord(from, walkColor);
							addCoord(coord, walkColor);
							addCoord(to, walkColor);
						} else {
							addCoord(from, otherColor); 
							addCoord(coord, otherColor);
							addCoord(to, otherColor);
						}
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
				this.cols.put((byte) 128);
			}
			this.vert.position(0);
			this.cols.position(0);
		}


		private void addCoord(Coord coord, Color col) {
			vertex.add(coord);
			colors.add(col);
//			log.info( " east: " + coord.getX() + " north: " +coord.getY() );
			// really slow!
		}

		@Override
		public void draw(OTFOGLDrawer drawer) {
			GL2 gl = OTFGLAbstractDrawable.getGl();
			if (vert != null) {
				drawPlanPoly(gl);
			}
			createActivityTextsIfNecessary(drawer);
			OGLAgentPointLayer layer = drawer.getCurrentSceneGraph().getAgentPointLayer();
			Point2D.Double pos = tryToFindAgentPosition(layer);
			if (pos != null) {
				// We know where the agent is, so we draw stuff around them.
				drawArrowFromAgentToTextLabel(pos, gl);
				drawCircleAroundAgent(pos, gl);
				createLabelTextIfNecessary(drawer, pos);

			} 
			unPrepare(gl);
		}

		private Point2D.Double tryToFindAgentPosition(OGLAgentPointLayer layer) {
			Point2D.Double pos = getAgentPositionFromPointLayer(this.agentId, layer);
			return pos;
		}

		private void drawPlanPoly(GL2 gl) {
			Color color = Color.ORANGE;
			gl.glColor4d(color.getRed() / 255., color.getGreen() / 255., color
					.getBlue() / 255., .5);
			gl.glEnable(GL2.GL_BLEND);
			gl.glEnable(GL2.GL_LINE_SMOOTH);
			gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
			gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glLineWidth(1.f * getLineWidth());
			gl.glColorPointer(4, GL2.GL_UNSIGNED_BYTE, 0, cols);
			gl.glVertexPointer(2, GL2.GL_FLOAT, 0, this.vert);
			gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, this.vertex.size());
			gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
			gl.glDisable(GL2.GL_LINE_SMOOTH);
		}

		private static void unPrepare(GL2 gl) {
			gl.glDisable(GL2.GL_BLEND);
		}

		private static float getLineWidth() {
			return OTFClientControl.getInstance().getOTFVisConfig()
					.getLinkWidth();
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
			DrawingUtils.drawCircle(gl, (float) pos.x, (float) pos.y, 200.f);
		}

		private static Double getAgentPositionFromPointLayer(String agentIdString,
				OGLAgentPointLayer layer) {
			return layer.getAgentCoords(agentIdString.toCharArray());
		}

		private void createActivityTextsIfNecessary(OTFOGLDrawer drawer) {
			activityTexts = new ArrayList<InfoText>();
			for (MyInfoText activityEntry : this.acts ) {
				InfoText activityText = new InfoText(
						activityEntry.name, activityEntry.east, activityEntry.north);
				activityText.setAlpha(0.5f);
				activityText.draw(drawer.getTextRenderer(), OTFGLAbstractDrawable.getDrawable(), drawer.getViewBoundsAsQuadTreeRect());
				this.activityTexts.add(activityText);
			}
		}

		private void createLabelTextIfNecessary(OTFOGLDrawer drawer, Point2D.Double pos) {
			this.agentText = new InfoText(
					this.agentId, (float) pos.x + 250, (float) pos.y + 250);
			this.agentText.setAlpha(0.7f);
			this.agentText.draw(drawer.getTextRenderer(), OTFGLAbstractDrawable.getDrawable(), drawer.getViewBoundsAsQuadTreeRect());
		}

		@Override
		public void remove() {

		}

		@Override
		public boolean isAlive() {
			return true;
		}

	}

	private class MyInfoText implements Serializable {

		private static final long serialVersionUID = 1L;
		float east, north;
		String name;

		public MyInfoText(float east, float north, String name) {
			this.east = east;
			this.north = north;
			this.name = name;
		}
	}


	private static SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
	private static AgentSnapshotInfoFactory snapshotInfoFactory = new AgentSnapshotInfoFactory(linkWidthCalculator);

	enum Level { ROUTES, PLANELEMENTS } 



}