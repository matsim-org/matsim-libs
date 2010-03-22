/* *********************************************************************** *
 * project: org.matsim.*
 * DgSatellicData
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.tests.satellic;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public interface DgSatellicData {

  public final String BASEDIR = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/";
  
  public final String NETWORK = BASEDIR + "demand/network_cleaned.xml.gz";
  
  public final String EMPTY_POPULATION = BASEDIR + "demand/plans1.xml";
  
}
