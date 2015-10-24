/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.analysis;

import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.socialnetworks.gis.io.ZoneLayerSHP;

import java.io.IOException;

/**
 * @author johannes
 *
 */
public class ComparePopDen {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		ZoneLayer<Double> obs = ZoneLayerSHP.read("/home/johannes/gsv/synpop/data/gis/nuts/pop.nuts3.shp", "value");
		ZoneLayer<Double> sim = ZoneLayerSHP.read("/home/johannes/gsv/synpop/output/popden.shp", "value");
		
		double sum = 0;
		for(Zone<Double> zone : obs.getZones()) {
			sum += zone.getAttribute();
		}
		
		for(Zone<Double> zone : obs.getZones()) {
			zone.setAttribute(zone.getAttribute() / sum);
		}
		
		double errSum = 0;
		double count = 0;
		for(Zone<Double> zone : obs.getZones()) {
//			zone.getGeometry().c
			Zone<Double> simZone = sim.getZone(zone.getGeometry().getCentroid());
			
			double relerr = (zone.getAttribute() - simZone.getAttribute())/zone.getAttribute();
			errSum += Math.abs(relerr);
			simZone.setAttribute(Math.min(1, Math.abs(relerr)));
			count++;
		}
		
		System.out.println("avr rel err = " + errSum/count);
		ZoneLayerSHP.write(sim, "/home/johannes/gsv/synpop/output/popden.err.shp");
	}

}
