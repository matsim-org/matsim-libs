/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package playground.dgrether.matsimkml;

import java.io.IOException;
import java.util.Set;

import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.utils.geometry.CoordinateTransformation;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.matsimkml.MatsimKmlStyleFactory;
import org.matsim.utils.vis.matsimkml.NetworkFeatureFactory;


/**
 * @author dgrether
 *
 */
public class KmlPlansWriter {



	private NetworkLayer network;

	private MatsimKmlStyleFactory styleFactory;

	private NetworkFeatureFactory featureFactory;

	private Style networkLinkStyle;

	private Style networkNodeStyle;

	public KmlPlansWriter(final NetworkLayer network, final CoordinateTransformation coordTransform, KMZWriter writer,  Document doc) {
		this.network = network;
		this.styleFactory = new MatsimKmlStyleFactory(writer, doc);
		this.featureFactory = new NetworkFeatureFactory(coordTransform);
	}

	public Folder getPlansFolder(Set<Plan> planSet) throws IOException {
		Folder folder = new Folder("plans");
		folder.setName("MATSIM Plans, quantity: " + planSet.size());
		this.networkLinkStyle = this.styleFactory.createDefaultNetworkLinkStyle();
		this.networkNodeStyle = this.styleFactory.createDefaultNetworkNodeStyle();
//		folder.addStyle(this.networkLinkStyle);
//		folder.addStyle(this.networkNodeStyle);
		Act act;
		Folder planFolder;
		Leg leg;
		for (Plan plan : planSet) {
			planFolder = new Folder("plan" + plan.getPerson().getId());
			planFolder.setName("Selected Plan of Person: " + plan.getPerson().getId());
			act = plan.getFirstActivity();
			do {
				planFolder.addFeature(this.featureFactory.createActFeature(act, this.networkNodeStyle));
				leg = plan.getNextLeg(act);
				planFolder.addFeature(this.featureFactory.createLegFeature(leg, this.networkLinkStyle));
				act = plan.getNextActivity(leg);
			}
			while (plan.getNextLeg(act) != null);
			folder.addFeature(planFolder);
		}

		return folder;
	}


}
