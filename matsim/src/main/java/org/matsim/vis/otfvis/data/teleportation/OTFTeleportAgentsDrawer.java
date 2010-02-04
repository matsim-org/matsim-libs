/* *********************************************************************** *
 * project: org.matsim.*
 * OTFTeleportAgentsDrawer
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.vis.otfvis.data.teleportation;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;

/**
 *
 * @author dgrether
 */
public class OTFTeleportAgentsDrawer extends OTFGLDrawableImpl{

	private static final Logger log = Logger.getLogger(OTFTeleportAgentsDrawer.class);

  private Map<String, Point2D.Double> positions = new HashMap<String, Point2D.Double>();

  public OTFTeleportAgentsDrawer(){
  	log.info("acitvated OTFTeleportAgentsDrawer...");
  }

	public void onDraw(GL gl) {
		float agentSize = OTFClientControl.getInstance().getOTFVisConfig().getAgentSize();
		gl.glColor3d(0.0, 0.0, 1.0);
		double zCoord = 1.0;
		double offset = agentSize/2;
		for (Point2D.Double p : positions.values()){
			gl.glBegin(GL.GL_QUADS);
				gl.glVertex3d(p.x - offset, p.y - offset, zCoord);
				gl.glVertex3d(p.x - offset, p.y + offset, zCoord);
				gl.glVertex3d(p.x + offset, p.y + offset, zCoord);
				gl.glVertex3d(p.x + offset, p.y - offset, zCoord);
			gl.glEnd();
		}
	}

	public Map<String, Point2D.Double> getPositions(){
		return this.positions;
	}

}
