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

import net.opengis.kml._2.AbstractFeatureType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.StyleType;

import org.apache.log4j.Logger;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.utils.geometry.CoordinateTransformation;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.MatsimKmlStyleFactory;
import org.matsim.utils.vis.kml.NetworkFeatureFactory;


/**
 * @author dgrether
 *
 */
public class KmlPlansWriter {

	private static final Logger log = Logger.getLogger(KmlPlansWriter.class);

	private NetworkLayer network;

	private MatsimKmlStyleFactory styleFactory;

	private NetworkFeatureFactory featureFactory;

	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	
	private StyleType networkLinkStyle;

	private StyleType networkNodeStyle;

	public KmlPlansWriter(final NetworkLayer network, final CoordinateTransformation coordTransform, KMZWriter writer, DocumentType doc) {
		this.network = network;
		this.styleFactory = new MatsimKmlStyleFactory(writer, doc);
		this.featureFactory = new NetworkFeatureFactory(coordTransform);
	}

	public FolderType getPlansFolder(Set<Plan> planSet) throws IOException {
		
		FolderType folder = this.kmlObjectFactory.createFolderType();
		
		folder.setName("MATSIM Plans, quantity: " + planSet.size());
		this.networkLinkStyle = this.styleFactory.createDefaultNetworkLinkStyle();
		this.networkNodeStyle = this.styleFactory.createDefaultNetworkNodeStyle();
//		folder.addStyle(this.networkLinkStyle);
//		folder.addStyle(this.networkNodeStyle);
		Act act;
		FolderType planFolder;
		Leg leg;
		AbstractFeatureType abstractFeature;
		for (Plan plan : planSet) {
			planFolder = kmlObjectFactory.createFolderType();
			planFolder.setName("Selected Plan of Person: " + plan.getPerson().getId());
			act = plan.getFirstActivity();
			do {
				abstractFeature = this.featureFactory.createActFeature(act, this.networkNodeStyle);
				if (abstractFeature.getClass().equals(PlacemarkType.class)) {
					planFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark((PlacemarkType) abstractFeature)); 
				} else {
					log.warn("Not yet implemented: Adding act KML features of type " + abstractFeature.getClass());
				}
				
				leg = plan.getNextLeg(act);
				abstractFeature = this.featureFactory.createLegFeature(leg, this.networkLinkStyle);
				if (abstractFeature.getClass().equals(FolderType.class)) {
					planFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder((FolderType) abstractFeature));
				} else {
					log.warn("Not yet implemented: Adding leg KML features of type " + abstractFeature.getClass());
				}

				act = plan.getNextActivity(leg);
			}
			while (plan.getNextLeg(act) != null);
			folder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(planFolder));
		}
		
		return folder;
		
	}

}
