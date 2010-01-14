/* *********************************************************************** *
 * project: org.matsim.*
 * BusOTFVis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.test;

import java.awt.Color;

import org.matsim.core.gbl.MatsimResource;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;

/**
 * @author yu
 * 
 */
public class OGLVehPointLayer extends OGLAgentPointLayer {

	public class VehDrawer extends AgentPointDrawer {
		public final AgentArrayDrawer carDrawer = new AgentArrayDrawer() {
			@Override
			protected void setTexture() {
				this.texture = OTFOGLDrawer.createTexture(MatsimResource
						.getAsInputStream("car.png"));
			}
		};
		public final AgentArrayDrawer busDrawer = new AgentArrayDrawer() {
			@Override
			protected void setTexture() {
				this.texture = OTFOGLDrawer.createTexture(MatsimResource
						.getAsInputStream("bus.png"));
			}
		};

		public void setAgent(char[] id, float startX, float startY, int state,
				int user, float color) {
			if (id[0] == 'p' && id[1] == 't')
				busDrawer.addAgent(id, startX, startY, colorizer
						.getColor(color), true);
			else
				carDrawer.addAgent(id, startX, startY, colorizer
						.getColor(color), true);
		}

		public void drawAll() {
			this.carDrawer.draw();
			this.busDrawer.draw();
		}
	}

	private final VehDrawer vehdrawer = new VehDrawer();

	private static OTFOGLDrawer.FastColorizer colorizer = new OTFOGLDrawer.FastColorizer(
			new double[] { 0.0, 30., 50. }, new Color[] { Color.RED,
					Color.YELLOW, Color.GREEN });

	@Override
	public OTFDataReceiver newInstance(Class<? extends OTFDataReceiver> clazz) throws InstantiationException,
			IllegalAccessException {
		return vehdrawer;
	}

	@Override
	public void draw() {
		super.draw();
		vehdrawer.drawAll();
	}
}
