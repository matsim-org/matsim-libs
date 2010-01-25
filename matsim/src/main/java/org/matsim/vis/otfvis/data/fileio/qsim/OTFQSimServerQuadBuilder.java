/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQSimServerQuadBuilder
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
package org.matsim.vis.otfvis.data.fileio.qsim;

import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFServerQuadBuilder;


/**
 * @author dgrether
 *
 */
public class OTFQSimServerQuadBuilder implements OTFServerQuadBuilder {
	
	private QNetwork network;

	public OTFQSimServerQuadBuilder(QNetwork network){
		this.network = network;
	}
	
	public OTFServerQuad2 createAndInitOTFServerQuad(OTFConnectionManager connect){
		OTFQSimServerQuad quad = new OTFQSimServerQuad(this.network);
		quad.initQuadTree(connect);
		return quad;
	}
	
	
	

}
