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

package playground.andreas.bln.ana.acts2kml;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.opengis.kml._2.AbstractFeatureType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.ScreenOverlayType;
import net.opengis.kml._2.StyleType;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;

public class KMLActsWriter {

	private static final Logger log = Logger.getLogger(KMLActsWriter.class);

	private String kmzFileName;
	private String outputDirectory;

	private List<Activity> activityList;

//	private ArrayList<Link> activityLinks;

	private Network network;

	private boolean writeActivityLinks = true;
	private boolean writeActivities = true;

	private MyKmlStyleFactory styleFactory;
	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	private StyleType networkLinkStyle;
	private MyFeatureFactory networkFeatureFactory;

	private CoordinateTransformation coordinateTransform = new IdentityTransformation();

//	private Plan personsPlan;


	public KMLActsWriter(Network network, List<Activity> activityList) {
		this.network = network;
		this.activityList = activityList;
	}
	
	public KMLActsWriter(Network network, Activity activity) {
		this(network, new LinkedList<Activity>());		
		this.activityList.add(activity);
	}

	public KMLActsWriter() {
		this.activityList = new LinkedList<Activity>();
	}

	public void addActivity(Activity act){
		this.activityList.add(act);
	}
	
	public void writeFile() {

		String outputFile = this.outputDirectory + "/" + this.kmzFileName;

		ObjectFactory LocalkmlObjectFactory = new ObjectFactory();

		// create main file and document

		DocumentType mainDoc = LocalkmlObjectFactory.createDocumentType();
		mainDoc.setId("mainDoc");
		mainDoc.setOpen(Boolean.TRUE);

		KmlType mainKml = LocalkmlObjectFactory.createKmlType();
		mainKml.setAbstractFeatureGroup(LocalkmlObjectFactory.createDocument(mainDoc));

		// create a folder
		FolderType mainFolder = LocalkmlObjectFactory.createFolderType();
		mainFolder.setId("actsFolder");
		mainFolder.setName("Matsim Data");
		mainFolder.setOpen(Boolean.TRUE);
		mainDoc.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createFolder(mainFolder));

		// create the writer
		KMZWriter writer = new KMZWriter(outputFile);

		this.styleFactory = new MyKmlStyleFactory(writer, mainDoc);
		this.networkFeatureFactory = new MyFeatureFactory(this.coordinateTransform);

		try {

			// add the MATSim logo to the kml
			ScreenOverlayType logo = MatsimKMLLogo.writeMatsimKMLLogo(writer);
			mainFolder.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createScreenOverlay(logo));
			
			// add the person's activity links to the kml
			if(this.writeActivities){
				createActivityLinks();
				
				Collection<FolderType> activityFolderCollection = getActivities();
				
				for (FolderType activityFolder : activityFolderCollection) {
					if (activityFolder != null) {
						activityFolder.setVisibility(Boolean.FALSE);
						mainFolder.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createFolder(activityFolder));
					}
				}			
				
			}

			// add the person's activity links to the kml
			if(this.writeActivityLinks){
				createActivityLinks();
				FolderType activityLinksFolder = getActivityLinksFolder(this.createActivityLinks(), "Activity Links of Person " );
				if (activityLinksFolder != null) {
					activityLinksFolder.setVisibility(Boolean.FALSE);
					mainFolder.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createFolder(activityLinksFolder));
				}
			}

		} catch (IOException e) {
			Gbl.errorMsg("Cannot create kmz or logo because of: " + e.getMessage());
			e.printStackTrace();
		}
		writer.writeMainKml(mainKml);
		writer.close();
		log.info("... finished");
	}
	
	private Collection<FolderType> getActivities() throws IOException {
	
//		this.networkLinkStyle = this.styleFactory.createDefaultNetworkLinkStyle();
//		this.networkNodeStyle = this.styleFactory.createDefaultColoredNodeStyle();
		
		HashMap<String, FolderType> folderTypes = new HashMap<String, FolderType>();		
		
		for (Activity act : this.activityList) {
			
			if(folderTypes.get(act.getType()) == null){
				FolderType linkFolder = this.kmlObjectFactory.createFolderType();
				linkFolder.setName(act.getType() + " activities");
				folderTypes.put(act.getType(), linkFolder);
			}
			
			AbstractFeatureType abstractFeature = this.networkFeatureFactory.createActFeature(act, this.styleFactory.createDefaultColoredNodeStyle(act.getType()));
			folderTypes.get(act.getType()).getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark((PlacemarkType) abstractFeature));
		}
		
		return folderTypes.values();
	}

	private FolderType getActivityLinksFolder(List<Link> links, String description) throws IOException {

		if(links != null){
			this.networkLinkStyle = this.styleFactory.createDefaultNetworkLinkStyle();

			FolderType linkFolder = this.kmlObjectFactory.createFolderType();
			linkFolder.setName(description);

			for (Link link : links) {
				AbstractFeatureType abstractFeature = this.networkFeatureFactory.createLinkFeature(link, this.networkLinkStyle);
				linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder((FolderType) abstractFeature));
			}	

			return linkFolder;
		} else {
			return null;
		}
	}

	private List<Link> createActivityLinks() {
		
		List<Link> links = new LinkedList<Link>();
		
		for (Activity act : this.activityList) {
			if(act.getLinkId() != null && this.network != null){
				links.add(this.network.getLinks().get(act.getLinkId()));
			}
		}
		
		if(links.size() == 0) {
			return null;
		} else {
			return links;
		}
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
//		final String outputDirectory = "E:\\temp";
//
//		Gbl.createConfig(null);
////		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
////		new MatsimNetworkReader(network).readFile(netFilename);
//
//		List<Activity> actList = new LinkedList<Activity>();
//		Activity act = new ActivityImpl("whatever", new CoordImpl(4579260, 5841710));
//		actList.add(act);
//		act = new ActivityImpl("whatever", new CoordImpl(4579260, 5841710));
//		actList.add(act);
//		
//		Activity act2 = new ActivityImpl("whatever2", new CoordImpl(4579260, 5841710));
//		actList.add(act2);
//		
//		KMLActsWriter test = new KMLActsWriter(null, actList);
//
//		test.setCoordinateTransformation(new GK4toWGS84());
//		test.setKmzFileName(kmzFilename);
//		test.setOutputDirectory(outputDirectory);
////		test.setNetwork(network);
//
//		test.writeFile();
//
//		log.info("Done!");
//	}

}
