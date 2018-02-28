/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package cemdap4wob.network;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class ReassignNetworkCapacities {
	public static void main(String[] args) {
		String origNetwork = "C:\\Users\\Joschka\\Documents\\shared-svn\\projects\\vw_rufbus\\projekt1\\scenario\\network\\versions\\networkptcc.xml";
		String newNetwork = "D:\\runs-svn\\vw_rufbus\\vw214c\\vw214c.output_network.xml.gz";
		Network oldNet = NetworkUtils.createNetwork();
		new MatsimNetworkReader(oldNet).readFile(origNetwork);

		Network newNet = NetworkUtils.createNetwork();
		new MatsimNetworkReader(newNet).readFile(newNetwork);
		int i = 0;
		for (Link l : newNet.getLinks().values()) {
			if (oldNet.getLinks().containsKey(l.getId())) {
				double oldCap = oldNet.getLinks().get(l.getId()).getCapacity();
				if (oldCap < l.getCapacity()) {
				l.setCapacity(oldCap);
				i++;
				}
			}
			if (l.getId().toString().equals("30051")){
				l.setCapacity(1320);
			}
		}
		System.out.println(i);
		new NetworkWriter(newNet).write("D:\\runs-svn\\vw_rufbus\\newnet.xml.gz");
	}
		
}
