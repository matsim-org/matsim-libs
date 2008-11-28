/* *********************************************************************** *
 * project: org.matsim.*
 * LinkPainter.java
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

package playground.gregor.vis;

import java.util.HashMap;
import java.util.Iterator;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.basic.v01.BasicLink;
import org.matsim.interfaces.basic.v01.BasicNet;
import org.matsim.interfaces.basic.v01.BasicNode;
import org.matsim.utils.vis.netvis.DisplayNetStateWriter;

/**
 * Implementation of DisplayNetStateWriter
 * @author laemmel
 *
 */
public class LinkPainter extends DisplayNetStateWriter {


	private HashMap<Id,LinkAttribute> linkData = new HashMap<Id,LinkAttribute>();
	private HashMap<Id,NodeAttribute> nodeData = new HashMap<Id,NodeAttribute>();


	public LinkPainter(final BasicNet network, final String networkFileName,
    		final String filePrefix, final int timeStepLength_s, final int bufferSize) {
        super(network, networkFileName, filePrefix, timeStepLength_s, bufferSize);
    }

	public boolean linkAttribExist(Id id){
		return linkData.containsKey(id);
	}

	public void setLinkColor(Id id, double color) {
		LinkAttribute attrib;
		if (linkData.containsKey(id)) {
			attrib = linkData.get(id);
		} else {
			attrib = new LinkAttribute(id);
			linkData.put(id, attrib);
		}
		attrib.setColor(color);
	}

	public void setLinkMsg(Id id, String msg){
		LinkAttribute attrib;
		if (linkData.containsKey(id)) {
			attrib = linkData.get(id);
		} else {
			attrib = new LinkAttribute(id);
			linkData.put(id, attrib);
		}
		attrib.setMsg(msg);
	}

	public void setNodeMsg(Id id, String msg){
		NodeAttribute attrib;
		if (nodeData.containsKey(id)) {
			attrib = nodeData.get(id);
		} else {
			attrib = new NodeAttribute(id);
			nodeData.put(id, attrib);
		}
		attrib.setMsg(msg);
	}

	public void resetColor(double d) {
		Iterator it = linkData.values().iterator();
		while (it.hasNext()){
			LinkAttribute attrib = (LinkAttribute) it.next();
			if (attrib.getColor() == d)
				attrib.setColor(0);
		}

	}

	public void reset() {
		linkData.clear();

	}

	///////////////////////////////////////////////////////////////
	//NetStateWriter Stuff

    @Override
    protected double getLinkDisplValue(final BasicLink link, final int index) {
		LinkAttribute attrib;
		if (linkData.containsKey(link.getId())) {
			attrib = linkData.get(link.getId());
		} else {
//			attrib = new LinkAttribute(id);
//			linkStats.put(id, attrib);
			return 0;
		}
    	return attrib.getColor();
    }

    @Override
    public String getLinkDisplLabel(final BasicLink link) {
		LinkAttribute attrib;
		if (linkData.containsKey(link.getId())) {
			attrib = linkData.get(link.getId());
		} else {
//			attrib = new LinkAttribute(id);
//			linkStats.put(id, attrib);
			return link.getId().toString();
		}
		return attrib.getMsg();
    }



	@Override
	protected String getNodeDisplLabel(BasicNode node) {
		Id id = node.getId();
		NodeAttribute attrib;
		if (nodeData.containsKey(id)){
			attrib = nodeData.get(id);
		} else {
			return node.getId().toString();
		}
		return attrib.getMsg();
	}



	private class LinkAttribute {

		private Id id;
		private double color;
		private String msg;

		public LinkAttribute(Id id){
			this.id = id;
			this.color = 0;
			this.msg = this.id.toString();
		}

		public void setColor(double color){
			this.color = color;
		}
		public void setMsg(String msg) {
			this.msg = msg;
		}

		public double getColor(){
			return this.color;
		}
		public String getMsg(){
			return this.msg;
		}

	}

	private class NodeAttribute {

		private Id id;
		private String msg;

		public NodeAttribute(Id id){
			this.id = id;
			this.msg = this.id.toString();
		}


		public void setMsg(String msg) {
			this.msg = msg;
		}

		public String getMsg(){
			return this.msg;
		}

	}





}
