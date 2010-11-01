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
public class DgPrognose2025NetworkVisualizer {

  /**
   * @param args
   */
  public static void main(String[] args) {
//    String net = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_ab.xml";
//    String net = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_ab_wgs84.xml";
    //    String net = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_bs.xml";
//    String net = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_ab_bs.xml";
//    String net = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_cleaned.xml.gz";
  String net = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_cleaned_wgs84.xml.gz";
  	String[] a = {net};
    OTFVis.playNetwork(a);
    
  }

}
