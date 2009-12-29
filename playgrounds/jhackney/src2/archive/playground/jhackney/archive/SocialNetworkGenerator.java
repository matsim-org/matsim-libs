/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkGenerator.java
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

package playground.jhackney.deprecated;

import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.population.Population;

import playground.jhackney.socialnet.SocialNetwork;

public class SocialNetworkGenerator {
	private static String sNAlgorithmName_;
	
	public SocialNetwork generateSocialNetwork(Population plans) {
		SocialNetwork net;
		// Check parameters and call the right algorithm (which implements
		// SocialNetwork)
		sNAlgorithmName_ = Gbl.getConfig().socnetmodule().getSocNetAlgo();

		if (sNAlgorithmName_.equals("random")) {
			System.out.println("Setting up the " + sNAlgorithmName_
					+ " algorithm.");
			net = new RandomSocialNetwork(plans);
			
		} else if (sNAlgorithmName_.equals("wattssmallworld")) {
			System.out.println("Setting up the " + sNAlgorithmName_
					+ " algorithm.");
			net = new WattsSocialNetwork(plans);
			
		} else if (sNAlgorithmName_.equals("jingirnew")) {
			System.out.println("Setting up the " + sNAlgorithmName_
					+ " algorithm.");
			net = new JGNSocialNetwork(plans);
			
		} else if (sNAlgorithmName_.equals("empty")) {
			System.out.println("Setting up the " + sNAlgorithmName_
					+ " algorithm.");
			net = new EmptySocialNetwork(plans);
			
		} else {
			Gbl.errorMsg(" "+this.getClass()
							+ ".run(). Social Network Algorithm > "
							+ sNAlgorithmName_
							+ " < is not known. Poor choice of input parameter in module "
							+ SocNetConfigGroup.GROUP_NAME
							+ ". Check spelling or choose from: random, wattssmallworld, jingirnew, empty");
			net = null;
		}
		return net;
	}
}
