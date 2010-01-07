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
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.vis.otfvis.caching.ClientDataBase;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.DrawingUtils;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.gl.InfoTextContainer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

import com.sun.opengl.util.BufferUtil;

/**
 * For a given agentID this QueryAgentPlan draws a visual representation of the agent's day.
 * 
 * @author dstrippgen
 *
 */
public class QueryAgentPlan implements OTFQuery {

  private static final Logger log = Logger.getLogger(QueryAgentPlan.class);
	
  private static final long serialVersionUID = -8415337571576184768L;

	protected String agentId;
	protected float[] vertex = null;
	private byte[] colors = null;
	private transient FloatBuffer vert;
	private List<Object> acts;
	protected InfoText agentText = null;
	private int lastActivity = -1;
	private ByteBuffer cols; 

	private boolean calcOffset = true;

	public void setId(String id) {
		this.agentId = id;
	}

	private static int countLines(Plan plan) {
		int count = 0;
		for (Object o : plan.getPlanElements()) {
			if (o instanceof ActivityImpl) {
				count++;
			} else if (o instanceof LegImpl) {
				LegImpl leg = (LegImpl)o;
				if (leg.getMode().equals(TransportMode.car)) {
					List<Link> route = ((NetworkRouteWRefs) leg.getRoute()).getLinks();
					count += route.size();
					if(route.size() != 0) count++; //add last position if there is a path
				}
			}
		}
		return count;
	}

	protected void setColor(int pos, Color col) {
		this.colors[pos*4 +0 ] = (byte)col.getRed();
		this.colors[pos*4 +1 ] = (byte)col.getGreen();
		this.colors[pos*4 +2 ] = (byte)col.getBlue();
		this.colors[pos*4 +3 ] = (byte)128;
	}

	protected void setCoord(int pos, Coord coord, Color col) {
		this.vertex[pos*2 +0 ] = (float)coord.getX();
		this.vertex[pos*2 +1 ] = (float)coord.getY();
		setColor(pos, col);
	}

	public void buildRoute(Plan plan) {
		int count = countLines(plan);
		if (count == 0) return;

		int pos = 0;
		this.vertex = new float[count*2];
		this.colors = new byte[count*4];

		Color carColor = Color.ORANGE;
		Color actColor = Color.BLUE;
		Color ptColor = Color.YELLOW;
		Color walkColor = Color.MAGENTA;
		Color otherColor = Color.PINK;

		for (Object o : plan.getPlanElements()) {
			if(o instanceof ActivityImpl) {
				Color col = actColor;
				ActivityImpl act = (ActivityImpl)o;
				Coord coord = act.getCoord();
				if (coord == null) {
				  coord = act.getLink().getCoord();
				}
				setCoord(pos++, coord, col);
			} 
			else if (o instanceof LegImpl) {
				Leg leg = (Leg) o;
				if (leg.getMode().equals(TransportMode.car)) {
					Node last = null;
					for (Link driven : ((NetworkRouteWRefs) leg.getRoute()).getLinks()) {
						Node node = driven.getFromNode();
						last = driven.getToNode();
						setCoord(pos++, node.getCoord(), carColor);
					}
					if(last != null) {
					  setCoord(pos++, last.getCoord(), carColor);
					}
				}
				else if (leg.getMode().equals(TransportMode.pt)){
				  setColor(pos-1, ptColor); 
				}
        else if (leg.getMode().equals(TransportMode.walk)){
          setColor(pos-1, walkColor); 
        }
				else {
					setColor(pos-1, otherColor); // replace act Color with pt color... here we need walk etc too
				}
			} // end leg handling
		}
	}

	public OTFQuery query(QueueNetwork net, Population plans, EventsManager events, OTFServerQuad2 quad) {
		Person person = plans.getPersons().get(new IdImpl(this.agentId));
		if (person != null) {
			Plan plan = person.getSelectedPlan();
			this.acts = new Vector<Object>();
			for (PlanElement e : plan.getPlanElements()){
			  if (e instanceof Activity){
			    Activity act = (Activity) e;
	        Coord coord = act.getCoord();
	        if (coord == null) {
	          Link link = net.getLinks().get(act.getLinkId()).getLink();
	          coord = link.getCoord();
	        }
	        this.acts.add(new MyInfoText((float) coord.getX(), (float) coord.getY(), act.getType()));
			  }
			}
			buildRoute(plan);
		}
		else {
			log.error("No person found for id " + this.agentId);
		}
		return this;
	}

	public void draw(OTFDrawer drawer) {
		if(drawer instanceof OTFOGLDrawer) {
			drawWithGLDrawer((OTFOGLDrawer)drawer);
		}
		else {
		  log.error("cannot draw query cause no OTFOGLDrawer is used!");
		}
	}

	protected void drawWithGLDrawer(OTFOGLDrawer drawer) {
		if (this.vertex != null) {
			OGLAgentPointLayer layer = (OGLAgentPointLayer) drawer.getActGraph().getLayer(AgentPointDrawer.class);
			Point2D.Double pos = tryToFindAgentPosition(layer);
			calcOffsetIfNecessary(drawer, pos);
			rewindGLBuffers();
			GL gl = drawer.getGL();
			prepare(gl);
			if (pos != null) {
				// We know where the agent is, so we draw stuff around them.
				drawArrowFromAgentToTextLabel(pos, gl);
				drawCircleAroundAgent(pos, gl);
				updateAgentTextPosition(pos);
				resetAnyOldProgressbars();
			} 
			else {
				// We don't know where the agent is, so we ask the server if
				// they are still active.
				queryAgentActivityStatus(drawer);
			}
			unPrepare(gl);
		}
	}

	private void prepare(GL gl) {
		Color color = Color.ORANGE;
		gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,.5);
		gl.glEnable(GL.GL_BLEND);
		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glEnableClientState (GL.GL_COLOR_ARRAY);
		gl.glEnableClientState (GL.GL_VERTEX_ARRAY);
		gl.glLineWidth(1.f * getLineWidth());
		gl.glColorPointer (4, GL.GL_UNSIGNED_BYTE, 0, cols);
		gl.glVertexPointer (2, GL.GL_FLOAT, 0, this.vert);
		gl.glDrawArrays (GL.GL_LINE_STRIP, 0, this.vertex.length/2);
		gl.glDisableClientState (GL.GL_VERTEX_ARRAY);
		gl.glDisableClientState (GL.GL_COLOR_ARRAY);
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
		return ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getLinkWidth();
	}

	private void drawArrowFromAgentToTextLabel(Point2D.Double pos, GL gl) {
		gl.glColor4f(0.f, 0.2f, 1.f, 0.5f);//Blue
		gl.glLineWidth(2);
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glVertex3d((float)pos.x + 50, (float)pos.y + 50,0);
		gl.glVertex3d((float)pos.x +250, (float)pos.y +250,0);
		gl.glEnd();
	}

	private void drawCircleAroundAgent(Point2D.Double pos, GL gl) {
		DrawingUtils.drawCircle(gl, (float)pos.x, (float)pos.y, 200.f);
	}

	private void resetAnyOldProgressbars() {
		if (this.lastActivity >= 0) ((InfoText)this.acts.get(this.lastActivity)).setFill(0.0f);
	}

	private void updateAgentTextPosition(Point2D.Double pos) {
		if(this.agentText != null) {
			this.agentText.setX((float)pos.x+ 250);
			this.agentText.setY((float)pos.y + 250);
		}
	}

	private void queryAgentActivityStatus(OTFOGLDrawer drawer) {
		QueryAgentActivityStatus query = new QueryAgentActivityStatus();
		query.setId(this.agentId);
		query.setNow(drawer.getActGraph().getTime());
		query = (QueryAgentActivityStatus) drawer.getQuad().doQuery(query);
		
		if ((query != null) && (query.activityNr != -1) && (query.activityNr < this.acts.size())) {
			InfoText posT = ((InfoText)this.acts.get(query.activityNr));
			posT.setColor(new Color(255,50,50,180));
			posT.setFill((float)query.finished);
			this.lastActivity = query.activityNr;
		}
	}

	private void calcOffsetIfNecessary(OTFOGLDrawer drawer, Point2D.Double pos) {
		if (this.calcOffset == true) {
			float east = (float)drawer.getQuad().offsetEast;
			float north = (float)drawer.getQuad().offsetNorth;

			this.calcOffset = false;
			for(int i = 0; i < this.vertex.length; i+=2) {
				this.vertex[i] -=east;
				this.vertex[i+1] -= north;
			}
			this.vert = BufferUtil.copyFloatBuffer(FloatBuffer.wrap(this.vertex));
			this.cols = BufferUtil.copyByteBuffer(ByteBuffer.wrap(this.colors));
			
			
			for (int i=0;i< this.acts.size(); i++) {
				MyInfoText inf = (MyInfoText)this.acts.get(i);
				this.acts.set(i,InfoTextContainer.showTextPermanent(inf.name, inf.east - east, inf.north - north, -0.001f ));
				((InfoText)this.acts.get(i)).setAlpha(0.5f);
			}

			if (pos != null) {
				this.agentText = InfoTextContainer.showTextPermanent(this.agentId, (float)pos.x, (float)pos.y, -0.0005f );
				this.agentText.setAlpha(0.7f);
			}
			onEndInit();
		}
	}

	private Point2D.Double tryToFindAgentPosition(OGLAgentPointLayer layer) {
		ClientDataBase clientDataBase = ClientDataBase.getInstance();
		Map<Id, Id> piggyBackingMap = clientDataBase.getPiggyBackingMap();
		Point2D.Double pos = getAgentPositionFromPointLayer(this.agentId, layer);
		if (pos == null) {
			// The agent visualizer doesn't know where the agent is. Presumably
			// they are in a transit vehicle.
			Id piggyBackingAgentId = piggyBackingMap.get(new IdImpl(this.agentId));
			if (piggyBackingAgentId != null) {
				pos = getAgentPositionFromPointLayer(piggyBackingAgentId.toString(), layer);
			}
		} else {
			// The agent visualizer DOES know where the agent is, so we give the
			// piggy-backing database a hint that the agent is not with someone else anymore.
			// Ugly, but OK for now.
			piggyBackingMap.put(new IdImpl(this.agentId), null);
		}
		return pos;
	}

	private Double getAgentPositionFromPointLayer(String agentIdString, OGLAgentPointLayer layer) {
		return layer.getAgentCoords(agentIdString.toCharArray());
	}
		
	public void remove() {
		// Check if we have already generated InfoText Objects, otherwise drop deleting
		if (this.calcOffset == true) return;
		if (this.acts != null) {
			for (int i=0;i< this.acts.size(); i++) {
				InfoText inf = (InfoText)this.acts.get(i);
				if(inf != null) {
				  InfoTextContainer.removeTextPermanent(inf);
				}
			}
		}
		if (this.agentText != null) {
		  InfoTextContainer.removeTextPermanent(this.agentText);
		}
	}

	public boolean isAlive() {
		return false;
	}

	public Type getType() {
		return OTFQuery.Type.AGENT;
	}

	protected void onEndInit() {
		// for derived classes
	}
	
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


}