/* *********************************************************************** *
 * project: org.matsim.*
 * OGLSimpleBackgroundLayer.java
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

package playground.gregor.snapshots.writers;

import java.util.ArrayList;
import java.util.List;

import org.matsim.vis.otfvis.caching.SceneLayer;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.opengl.drawer.AbstractBackgroundDrawer;


/**
 * OGLSimpleBackgroundLayer collects all items that are rendered as a background behind the network.
 * That could be satellite images or such.
 * 
 * @author dstrippgen
 *
 */
public class OGLSimpleBackgroundLayer implements SceneLayer {

	private double offsetEast;
	private double offsetNorth;
	private final static List<AbstractBackgroundDrawer> items = new ArrayList<AbstractBackgroundDrawer>();

	@Override
	public void init(boolean initConstData) {
		this.offsetEast = offsetEast;
		this.offsetNorth = offsetNorth;
	}

	@Override
	public void addItem(final OTFDataReceiver item) {
		final AbstractBackgroundDrawer drawer = (AbstractBackgroundDrawer)item;
		items.add(drawer);
	}

	public static void addPersistentItem(final AbstractBackgroundDrawer drawer) {
		items.add(drawer);
	}

	
	
	@Override
	public void glInit() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void draw() {
		for(final AbstractBackgroundDrawer item : items) {
			item.setOffset(offsetEast, offsetNorth);
			item.draw();
		}
	}

	@Override
	public int getDrawOrder() {
		return 0;
	}

	@Override
	public void finish() {
	}

	@Override
	public OTFDataReceiver newInstanceOf(Class<? extends OTFDataReceiver> clazz) {
		try {
			return clazz.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
