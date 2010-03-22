/* *********************************************************************** *
 * project: org.matsim.*
 * DgSatellicNetworkConverter
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

import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dgrether.DgPaths;
import playground.yu.utils.qgis.MATSimNet2QGIS;


/**
 * @author dgrether
 *
 */
public class DgSatellicNetworkConverter {

  public static void main(String[] args) {
    String net = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_ab.xml";
    String netOut = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_ab.shp";
    
//    Scenario sc = new ScenarioImpl();
//    MatsimNetworkReader reader = new MatsimNetworkReader(sc);
//    reader.readFile(net);
//    String srs = "+proj=utm +zone=32 +ellps=GRS80 +units=m +no_defs";
    String srs = TransformationFactory.WGS84;
    MATSimNet2QGIS mn2q = new MATSimNet2QGIS(net, srs);
    mn2q.writeShapeFile(netOut);
    
//    NetworkWriteAsTable tableWriter = new NetworkWriteAsTable(netOut);
//    tableWriter.run((NetworkLayer) sc.getNetwork());
    
  }

}
