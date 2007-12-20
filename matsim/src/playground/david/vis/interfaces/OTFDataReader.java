/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDataHandler.java
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

package playground.david.vis.interfaces;

import java.io.DataInputStream;
import java.io.IOException;

import playground.david.vis.data.OTFData;

public interface OTFDataReader {
	public void readConstData(DataInputStream in) throws IOException;
	public void readDynData(DataInputStream in) throws IOException;
	public void connect(OTFData.Receiver receiver);
	public void invalidate();
}

