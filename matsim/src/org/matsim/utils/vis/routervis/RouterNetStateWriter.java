/* *********************************************************************** *
 * project: org.matsim.*
 * RouterNetStateWriter.java
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

package org.matsim.utils.vis.routervis;

import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicNet;
import org.matsim.utils.vis.netvis.DisplayNetStateWriter;
import org.matsim.utils.vis.netvis.VisConfig;

/**
 * @author laemmel
 */
public class RouterNetStateWriter extends DisplayNetStateWriter {

	//holds the information of the links explored so far
	private HashMap<Id,LinkAttribute> linkStates;

	public RouterNetStateWriter(BasicNet network, String networkFileName, VisConfig visConfig, String filePrefix, int timeStepLength_s, int bufferSize) {
		super(network,networkFileName,visConfig,filePrefix,timeStepLength_s,bufferSize);

		this.linkStates = new HashMap<Id,LinkAttribute>();
	}

	public boolean linkAttribExist(Id id){
		return linkStates.containsKey(id);
	}

	public void setLinkColor(Id id, double color) {
		LinkAttribute attrib;
		if (linkStates.containsKey(id)) {
			attrib = linkStates.get(id);
		} else {
			attrib = new LinkAttribute(id);
			linkStates.put(id, attrib);
		}
		attrib.setColor(color);
	}

	public void setLinkMsg(Id id, String msg){
		LinkAttribute attrib;
		if (linkStates.containsKey(id)) {
			attrib = linkStates.get(id);
		} else {
			attrib = new LinkAttribute(id);
			linkStates.put(id, attrib);
		}
		attrib.setMsg(msg);
	}


	public void reset() {
		linkStates.clear();

	}

	///////////////////////////////////////////////////////////////
	//NetStateWriter Stuff
	///////////////////////////////////////////////////////////////
	@Override
	protected double getLinkDisplValue(final BasicLink link, final int index) {
		LinkAttribute attrib = linkStates.get(link.getId());
		if (attrib == null) {
			return 0;
		}
		return attrib.getColor();
	}

	@Override
	protected String getLinkDisplLabel(final BasicLink link) {
		LinkAttribute attrib = linkStates.get(link.getId());
		if (attrib == null) {
			return "null";
		}
		return attrib.getMsg();
	}


}
