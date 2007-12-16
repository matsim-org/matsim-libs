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

import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.vis.netvis.DisplayNetStateWriter;
import org.matsim.utils.vis.netvis.VisConfig;

/**
 * @author laemmel
 */
public class RouterNetStateWriter extends DisplayNetStateWriter {

	//holds the information of the links explored so far
	private HashMap<IdI,LinkAttribute> linkStates;

	public RouterNetStateWriter(BasicNetI network, String networkFileName, VisConfig visConfig, String filePrefix, int timeStepLength_s, int bufferSize) {
		super(network,networkFileName,visConfig,filePrefix,timeStepLength_s,bufferSize);

		this.linkStates = new HashMap<IdI,LinkAttribute>();
	}

	public boolean linkAttribExist(IdI id){
		return linkStates.containsKey(id);
	}

	public void setLinkColor(IdI id, double color) {
		LinkAttribute attrib;
		if (linkStates.containsKey(id)) {
			attrib = linkStates.get(id);
		} else {
			attrib = new LinkAttribute(id);
			linkStates.put(id, attrib);
		}
		attrib.setColor(color);
	}

	public void setLinkMsg(IdI id, String msg){
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
    protected double getLinkDisplValue(final BasicLinkI link, final int index) {
		LinkAttribute attrib;
		if (linkStates.containsKey(link.getId())) {
			attrib = linkStates.get(link.getId());
		} else {
			return 0;
		}
    	return attrib.getColor();
    }

    @Override
    protected String getLinkDisplLabel(final BasicLinkI link) {
		LinkAttribute attrib;
		if (linkStates.containsKey(link.getId())) {
			attrib = linkStates.get(link.getId());
		} else {
			return "null";
		}
		return attrib.getMsg();
    }


}
