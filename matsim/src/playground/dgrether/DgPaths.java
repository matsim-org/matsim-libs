/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether;


/**
 * @author dgrether
 *
 */
public interface DgPaths {

	final String VSPSVNBASE = "/Volumes/data/work/vspSvn/";
	
	final String WSBASE = "/Volumes/data/work/svnWorkspace/";
	
	final String VSPCVSBASE = "/Volumes/data/work/cvsRep/vsp-cvs/";
	
	final String IVTCHNET = VSPSVNBASE + "studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml";
	
	final String IVTCHROADPRICING = VSPSVNBASE + "studies/schweiz-ivtch/baseCase/roadpricing/zurichCityArea/zurichCityAreaWithoutHighwaysPricingScheme.xml";
	
	final String IVTCHCOUNTS = VSPSVNBASE + "studies/schweiz-ivtch/baseCase/counts/countsIVTCH.xml";
}
