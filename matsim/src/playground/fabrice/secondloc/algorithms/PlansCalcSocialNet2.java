/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcSocialNet2.java
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

package playground.fabrice.secondloc.algorithms;

import java.util.HashMap;
import java.util.TreeMap;

import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PlansAlgorithm;

import playground.fabrice.secondloc.CoolPlace;
import playground.fabrice.secondloc.Interactions;
import playground.fabrice.secondloc.socialnet.SimpleSocEventGenerator;
import playground.fabrice.secondloc.socialnet.SocEveGenerator_ActualPlans;
import playground.fabrice.secondloc.socialnet.SocialNetz;

public class PlansCalcSocialNet2 extends PlansAlgorithm {

	SocialNetz netz;
	HashMap<Link,CoolPlace> link2cool = new HashMap<Link,CoolPlace>();
	HashMap<Activity,CoolPlace> activ2cool = new HashMap<Activity,CoolPlace>();
	
	final int max_sn_iter;

	public PlansCalcSocialNet2() {

		super();
		max_sn_iter = Integer.parseInt(Gbl.getConfig().socnetmodule().getNumIterations());

	}

	public void run(Plans plans) {


		System.out.print("building Social Network...");
		SocialNetz netz = new SocialNetz(plans);
		System.out.println("...done");
		System.out.println("Initial Number of social connections:\t"+netz.getLinks().size());
			
		hackKnowledge( plans );

		Interactions interactions = new Interactions( netz );
		
		for (int iteration = 0; iteration < max_sn_iter; iteration++) {

			
			interactions.exchangeDualSpatialInformation( 1.0 );
			
			interactions.makeFriendOfFriend( plans, 0.1 );
			
			System.out.println(" * Number of social connections:\t"+netz.getLinks().size());
			
			SimpleSocEventGenerator gen = new SimpleSocEventGenerator( "work" );
			interactions.makeFriendAtSocialEvent( gen.generate(plans), 0.1);
			
			SocEveGenerator_ActualPlans gen2 = new SocEveGenerator_ActualPlans( link2cool );
			interactions.makeFriendAtSocialEvent( gen2.generate(plans), 0.1 );
			
			System.out.println(" ** Number of social connections:\t"+netz.getLinks().size());
		}

	
	}


	
	void hackKnowledge( Plans plans ){
		
		// this lookup table Link <-> Facility will disappear when Facility will be a Location
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		
		for( Facility facility : Facilities.getSingleton().getFacilities().values() ){			
			Link link  = (Link) network.getNearestLocations( facility.getCenter(),	null ).get(0);
			
			TreeMap<String, Activity> tree = facility.getActivities();
			for( String actType : tree.keySet() ){
				Activity activity = tree.get(actType);
				CoolPlace coolplace = new CoolPlace();
				coolplace.facility = facility;
				coolplace.activity = activity;
				link2cool.put( link, coolplace);
				activ2cool.put( activity, coolplace);
			}
		}
	
		// set up the knowledge parts, will be refactored
		for( Person person : plans.getPersons().values() ){
			person.getKnowledge().hacks.init( activ2cool );
		}
	}

}
