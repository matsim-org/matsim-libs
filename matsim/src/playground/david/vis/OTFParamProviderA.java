/* *********************************************************************** *
 * project: org.matsim.*
 * OTFParamProviderA.java
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

package playground.david.vis;

import playground.david.vis.interfaces.OTFParamProvider;

public abstract class OTFParamProviderA implements OTFParamProvider {

	public int getIntParam(int index) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Int Params NOT provided");
	}
	public float getFloatParam(int index) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Float Params NOT provided");
	}
	public String getStringParam(int index) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("String Params NOT provided");
	}
	
	/** Default implementation of this method
	 * could most probably be done FASTER, but is safe!
	 */
	public int getIndex( String longName) {
		int result = -1;
		for (int i=0; i< getParamCount(); i++) if(getLongName(i).equals(longName))return i;
		return result;
	}
}