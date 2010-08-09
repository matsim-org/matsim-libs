/* *********************************************************************** *
 * project: org.matsim.*
 * DgTestDoubleMvi
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
package playground.dgrether.tests;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgTestDoubleMvi {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String file1 = DgPaths.RUNBASE + "run749/it.100/749.100.Zurich.otfvis.mvi";
		String file2 = DgPaths.RUNBASE + "run749/it.2000/749.2000.Zurich.otfvis.mvi";
		String[] a = {file1, file2};
//		OTFDoubleMVI.main(a);
		throw new UnsupportedOperationException("did not compile on 9/aug/2010, so I commented it out.  kai") ;
	}

}
