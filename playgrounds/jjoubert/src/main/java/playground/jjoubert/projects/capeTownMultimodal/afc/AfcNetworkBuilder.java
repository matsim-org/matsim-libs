/* *********************************************************************** *
 * project: org.matsim.*
 * AfcNetworkBuilder.java
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

/**
 * 
 */
package playground.jjoubert.projects.capeTownMultimodal.afc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;

import edu.uci.ics.jung.graph.util.Pair;
import playground.southafrica.freight.digicore.algorithms.complexNetwork.DigicoreNetworkWriter;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreNetwork;
import playground.southafrica.utilities.Header;

/**
 * @author jwjoubert
 *
 */
public class AfcNetworkBuilder {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AfcNetworkBuilder.class.toString(), args);
		
		String trips = args[0];
		String network = args[1];
		String gephiNetwork = args[2];
		
		buildNetwork(trips, network, gephiNetwork);
		
		Header.printFooter();
	}
	

	private static void buildNetwork(String trips, String network, String gephiNetwork){
		DigicoreNetwork dn = new DigicoreNetwork();
		Map<Id<ActivityFacility>, DigicoreActivity> map = new HashMap<Id<ActivityFacility>, DigicoreActivity>();
		
		BufferedReader br = IOUtils.getBufferedReader(trips);
		Counter counter = new Counter("  lines # ");
		try{
			String line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				
				String o = sa[1];
				double ox = Double.parseDouble(sa[2]);
				double oy = Double.parseDouble(sa[3]);
				DigicoreActivity dao = new DigicoreActivity("MyCiTi", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
				dao.setFacilityId(Id.create(o, ActivityFacility.class));
				dao.setCoord(CoordUtils.createCoord(ox, oy));

				String d = sa[8];
				double dx = Double.parseDouble(sa[9]);
				double dy = Double.parseDouble(sa[10]);
				DigicoreActivity daf = new DigicoreActivity("MyCiTi", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
				daf.setFacilityId(Id.create(d, ActivityFacility.class));
				daf.setCoord(CoordUtils.createCoord(dx, dy));
				dn.addArc(dao, daf);
				
				/* Add the facilities if they do not exist yet. */
				if(!map.containsKey(dao.getFacilityId())){
					map.put(dao.getFacilityId(), dao);
				}
				if(!map.containsKey(daf.getFacilityId())){
					map.put(daf.getFacilityId(), daf);
				}
				
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + trips);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + trips);
			}
		}
		counter.printCounter();

		try {
			new DigicoreNetworkWriter(dn).writeNetwork(network, true);
			new DigicoreNetworkWriter(dn);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + network);
		}
		
		/* Write Csv file. */
		BufferedWriter bw = IOUtils.getBufferedWriter(gephiNetwork);
		try{
			bw.write("Source;Target;weight");
			bw.newLine();
			
			Collection<Pair<Id<ActivityFacility>>> edges = dn.getEdges();
			for(Pair<Id<ActivityFacility>> edge : edges){
				Id<ActivityFacility> oId = edge.getFirst();
				Id<ActivityFacility> dId = edge.getSecond();
				DigicoreActivity o = map.get(oId);
				DigicoreActivity d = map.get(dId);
				
				double weight = dn.getEdgeWeight(oId, dId);
//				bw.write(String.format("%s,%s,%.0f,%.6f,%.6f,%.6f,%.6f\n", 
//						oId.toString(), 
//						dId.toString(), 
//						weight,
//						o.getCoord().getX(), o.getCoord().getY(),
//						d.getCoord().getX(), d.getCoord().getY()));
				bw.write(String.format("%s;%s;%.0f\n", 
						oId.toString(), 
						dId.toString(), 
						weight));
//				bw.write(String.format("%s;%s\n", 
//						oId.toString(), 
//						dId.toString()));
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + gephiNetwork);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + gephiNetwork);
			}
		}

	}
	

}
