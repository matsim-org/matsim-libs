/* *********************************************************************** *
 * project: org.matsim.*
 * DgSatellicNetworkVisualizer
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

import org.matsim.run.OTFVis;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgSatellicNetworkVisualizer {

  /**
   * @param args
   */
  public static void main(String[] args) {
    String net = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network.xml.gz";
    String[] a = {net};
    OTFVis.playNetwork(a);
    
  }

}
