/* *********************************************************************** *
 * project: org.matsim.*
 * DgSatellicNetworkPostProcessing
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
package playground.dgrether.prognose2025;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgPrognose2025NetworkPostProcessing {

  
  /**
   * @param args
   */
  public static void main(String[] args) {
    String netbase = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_pv";
    String net = netbase + ".xml";
    String netout = netbase + "_cleaned.xml.gz";
    
    Scenario sc = new ScenarioImpl();
    sc.getConfig().network().setInputFile(net);
    ScenarioLoader loader = new ScenarioLoaderImpl(sc);
    loader.loadScenario();
    
    NetworkCleaner cleaner = new NetworkCleaner();
    cleaner.run(sc.getNetwork());
    
    NetworkWriter writer = new NetworkWriter(sc.getNetwork());
    writer.write(netout);
    
  }
  
}
