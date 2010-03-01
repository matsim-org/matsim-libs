/* *********************************************************************** *
 * project: org.matsim.*
 * MyKMLNetWriterTest.java
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

package playground.andreas.bln.ana.plans2kml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import net.opengis.kml._2.AbstractFeatureType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.ScreenOverlayType;
import net.opengis.kml._2.StyleType;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;

public class KMLPersonPlanWriter {

	private static final Logger log = Logger.getLogger(KMLPersonPlanWriter.class);

	private String kmzFileName;
	private String outputDirectory;

	private Person person;

	private ArrayList<Link> activityLinks;

	private Network network;

	private boolean writeActivityLinks = true;
	private boolean writeFullPlan = true;

	private MyKmlStyleFactory styleFactory;
	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	private StyleType networkLinkStyle;
	private MyFeatureFactory networkFeatureFactory;
	private StyleType networkNodeStyle;

	private CoordinateTransformation coordinateTransform = new IdentityTransformation();

	private Plan personsPlan;


	public KMLPersonPlanWriter(Network network, Person person) {
		this.network = network;
		this.person = person;

		this.personsPlan = this.person.getSelectedPlan();
	}

	public KMLPersonPlanWriter() {
		// Should never be used
	}

	public void writeFile() {

		String outputFile;

		if (this.kmzFileName == null || this.kmzFileName.equals("")) {
			outputFile = this.outputDirectory + "/" + this.person.getId() + ".kmz";
		} else {
			outputFile = this.outputDirectory + "/" + this.kmzFileName;
		}

		ObjectFactory LocalkmlObjectFactory = new ObjectFactory();

		// create main file and document

		DocumentType mainDoc = LocalkmlObjectFactory.createDocumentType();
		mainDoc.setId("mainDoc");
		mainDoc.setOpen(Boolean.TRUE);

		KmlType mainKml = LocalkmlObjectFactory.createKmlType();
		mainKml.setAbstractFeatureGroup(LocalkmlObjectFactory.createDocument(mainDoc));

		// create a folder
		FolderType mainFolder = LocalkmlObjectFactory.createFolderType();
		mainFolder.setId("2dnetworklinksfolder");
		mainFolder.setName("Matsim Data");
		mainFolder.setOpen(Boolean.TRUE);
		mainDoc.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createFolder(mainFolder));

		// create the writer
		KMZWriter writer = new KMZWriter(outputFile);

		this.styleFactory = new MyKmlStyleFactory(writer, mainDoc);
		this.networkFeatureFactory = new MyFeatureFactory(this.coordinateTransform, this.network);

		try {

			// add the MATSim logo to the kml
			ScreenOverlayType logo = MatsimKMLLogo.writeMatsimKMLLogo(writer);
			mainFolder.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createScreenOverlay(logo));

			// add the person's activity links to the kml
			if(this.writeActivityLinks){
				createActivityLinks();
				FolderType activityFolder = getActivityLinksFolder(this.activityLinks, "Activity Links of Person " + this.person.getId());
				if (activityFolder != null) {
					activityFolder.setVisibility(Boolean.FALSE);
					mainFolder.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createFolder(activityFolder));
				}
			}

			// write the person's full plan
			if(this.writeFullPlan){
				FolderType activityFolder = getFullPlan();
				if (activityFolder != null) {
					activityFolder.setOpen(Boolean.TRUE);
					mainFolder.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createFolder(activityFolder));
				}
			}

		} catch (IOException e) {
			Gbl.errorMsg("Cannot create kmz or logo because of: " + e.getMessage());
			e.printStackTrace();
		}
		writer.writeMainKml(mainKml);
		writer.close();
		log.info("... wrote agent " + this.person.getId());
	}

	private FolderType getFullPlan() throws IOException {

		this.networkLinkStyle = this.styleFactory.createDefaultNetworkLinkStyle();
		this.networkNodeStyle = this.styleFactory.createDefaultNetworkNodeStyle();

		FolderType linkFolder = this.kmlObjectFactory.createFolderType();
		linkFolder.setName("Full Plan of " + this.person.getId());

		Coord fromCoords = null;

		for (Iterator<PlanElement> iterator = this.personsPlan.getPlanElements().iterator(); iterator.hasNext();) {
			PlanElement planElement = iterator.next();

			if (planElement instanceof Activity) {

				ActivityImpl act = (ActivityImpl) planElement;
				fromCoords = act.getCoord();

				AbstractFeatureType abstractFeature = this.networkFeatureFactory.createActFeature(act, this.networkNodeStyle);
				linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark((PlacemarkType) abstractFeature));
			}

			if (planElement instanceof LegImpl) {

				LegImpl leg = (LegImpl) planElement;

				if (leg.getMode() == TransportMode.car) {

					ArrayList<Id> tempLinkList = getLinkIdsOfCarLeg(leg);

					FolderType routeLinksFolder = this.kmlObjectFactory.createFolderType();
					double dist = (leg.getRoute() instanceof NetworkRoute ? RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), this.network) : Double.NaN);
					routeLinksFolder.setName(leg.getMode().toString() + " mode, dur: " + Time.writeTime(leg.getTravelTime()) + ", dist: " + dist);

					if(tempLinkList.size() != 0){
						routeLinksFolder.setDescription("see attached route");
						for (Id linkId : tempLinkList) {
							Link link = this.network.getLinks().get(linkId);
							AbstractFeatureType abstractFeature = this.networkFeatureFactory.createCarLinkFeature(link,	this.styleFactory.createCarNetworkLinkStyle());
							routeLinksFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder((FolderType) abstractFeature));
						}
					} else {
						routeLinksFolder.setDescription("sorry no route found");
					}

					linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(routeLinksFolder));

				} else if (leg.getMode() == TransportMode.pt) {

					if (iterator.hasNext()) {

						Coord toCoords = null;

						for (Iterator<PlanElement> tempIterator = this.personsPlan.getPlanElements().iterator(); tempIterator.hasNext();) {
							PlanElement tempPlanElement = tempIterator.next();
							if (tempPlanElement == planElement) {
								toCoords = ((ActivityImpl) tempIterator.next()).getCoord();
							}

						}

						AbstractFeatureType abstractFeature;

						if(((GenericRouteImpl) leg.getRoute()).getRouteDescription().contains("===R")){
							abstractFeature = this.networkFeatureFactory.createPTLinkFeature(fromCoords, toCoords, leg, this.styleFactory.createDBNetworkLinkStyle());
						} else if (((GenericRouteImpl) leg.getRoute()).getRouteDescription().contains("===S")){
							abstractFeature = this.networkFeatureFactory.createPTLinkFeature(fromCoords, toCoords, leg, this.styleFactory.createSBAHNNetworkLinkStyle());
						} else if (((GenericRouteImpl) leg.getRoute()).getRouteDescription().contains("----M")){
							abstractFeature = this.networkFeatureFactory.createPTLinkFeature(fromCoords, toCoords, leg, this.styleFactory.createMetroBusTramNetworkLinkStyle());
						} else if (((GenericRouteImpl) leg.getRoute()).getRouteDescription().contains("BVU----")){
							abstractFeature = this.networkFeatureFactory.createPTLinkFeature(fromCoords, toCoords, leg, this.styleFactory.createSubWayNetworkLinkStyle());
						} else if (((GenericRouteImpl) leg.getRoute()).getRouteDescription().contains("BVT----")){
							abstractFeature = this.networkFeatureFactory.createPTLinkFeature(fromCoords, toCoords, leg, this.styleFactory.createTramNetworkLinkStyle());
						} else if (((GenericRouteImpl) leg.getRoute()).getRouteDescription().contains("BVB----")){
							abstractFeature = this.networkFeatureFactory.createPTLinkFeature(fromCoords, toCoords, leg, this.styleFactory.createBusNetworkLinkStyle());
						} else {
							abstractFeature = this.networkFeatureFactory.createPTLinkFeature(fromCoords, toCoords, leg, this.styleFactory.createDefaultNetworkLinkStyle());
						}


						abstractFeature.setDescription(((GenericRouteImpl) leg.getRoute()).getRouteDescription());
						linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder((FolderType) abstractFeature));
					}

					} else if (leg.getMode() == TransportMode.walk || leg.getMode() == TransportMode.bike || leg.getMode() == TransportMode.undefined) {

						if (iterator.hasNext()) {

							Coord toCoords = null;

							for (Iterator<PlanElement> tempIterator = this.personsPlan.getPlanElements().iterator(); tempIterator.hasNext();) {
								PlanElement tempPlanElement = tempIterator.next();
								if (tempPlanElement == planElement) {
									toCoords = ((ActivityImpl) tempIterator.next()).getCoord();
								}

							}

							AbstractFeatureType abstractFeature = this.networkFeatureFactory.createWalkLinkFeature(fromCoords, toCoords, leg, this.styleFactory.createWalkNetworkLinkStyle());
//							abstractFeature.setDescription(((GenericRouteImpl) leg.getRoute()).getRouteDescription());
							linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark((PlacemarkType) abstractFeature));
						}
					} else {
						log.warn(leg.getMode() + " - leg type not handled");
					}
			}
		}
		return linkFolder;
	}

	private FolderType getActivityLinksFolder(ArrayList<Link> links, String description) throws IOException {

		this.networkLinkStyle = this.styleFactory.createDefaultNetworkLinkStyle();
		this.networkNodeStyle = this.styleFactory.createDefaultNetworkNodeStyle();

		FolderType linkFolder = this.kmlObjectFactory.createFolderType();
		linkFolder.setName(description);

		for (Link link : links) {
			AbstractFeatureType abstractFeature = this.networkFeatureFactory.createLinkFeature(link, this.networkLinkStyle);
			linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder((FolderType) abstractFeature));
		}

		return linkFolder;
	}

	private void createActivityLinks() {

		this.activityLinks = new ArrayList<Link>();

		if (this.person != null) {
			Plan selectedPlan = this.person.getSelectedPlan();
			if (selectedPlan != null) {
				for (PlanElement planElement : selectedPlan.getPlanElements()) {
					if (planElement instanceof Activity) {
						Activity act = (Activity) planElement;
						this.activityLinks.add(this.network.getLinks().get(act.getLinkId()));
					}
				}
			}
		}
	}

	private ArrayList<Id> getLinkIdsOfCarLeg(LegImpl leg) {

		ArrayList<Id> linkIds = new ArrayList<Id>();
			if (leg.getMode() == TransportMode.car) {

				if (leg.getRoute() != null) {
					LinkNetworkRouteImpl tempRoute = (LinkNetworkRouteImpl) leg.getRoute();
					for (Id linkId : tempRoute.getLinkIds()) {
						linkIds.add(linkId);
					}
				}

			} else { log.error("You gave me a non car leg. Can't handle this one."); }
		return linkIds;
	}

	/*
	 * Getters & Setters
	 */

	public void setWriteActivityLinks(boolean value) {
		this.writeActivityLinks = value;
	}

	public void setCoordinateTransformation(CoordinateTransformation coordinateTransform) {
		this.coordinateTransform = coordinateTransform;
	}

	public CoordinateTransformation getCoordinateTransformation() {
		return this.coordinateTransform;
	}

	public void setOutputDirectory(String directory) {
		this.outputDirectory = directory;
	}

	public String getOutputDirectory() {
		return this.outputDirectory;
	}

	public void setKmzFileName(String name) {
		this.kmzFileName = name;
	}

	public String getKmzFileName() {
		return this.kmzFileName;
	}

//	public static void main(String[] args) {
//		final String netFilename = "E:\\oev-test\\output\\network.multimodal.xml";
//		final String kmzFilename = "test.kmz";
//		final String outputDirectory = "E:\\oev-test\\GE";
//
//		Gbl.createConfig(null);
//		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
//		new MatsimNetworkReader(network).readFile(netFilename);
//
//		KMLPersonPlanWriter test = new KMLPersonPlanWriter();
//
//		test.setKmzFileName(kmzFilename);
//		test.setOutputDirectory(outputDirectory);
////		test.setNetwork(network);
//
//		test.writeFile();
//
//		log.info("Done!");
//	}

}
