/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesExportToGUESS.java
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

package playground.meisterk.facilities;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.algorithms.FacilitiesAlgorithm;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Feature;
import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.IconStyle;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMLWriter;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.LineStyle;
import org.matsim.utils.vis.kml.NetworkLink;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Point;
import org.matsim.utils.vis.kml.Region;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.kml.KMLWriter.XMLNS;
import org.matsim.utils.vis.kml.Link.ViewRefreshMode;
import org.matsim.utils.vis.kml.fields.Color;
import org.matsim.world.Location;

public class FacilitiesExportToGUESS extends FacilitiesAlgorithm {

	// config parameters
	public static final String KML21_MODULE = "kml21";
	public static final String CONFIG_OUTPUT_DIRECTORY = "outputDirectory";
	public static final String CONFIG_OUTPUT_KML_BASE_DIRECTORY = "outputKMLSwissFacilitiesInRegionsBaseDirectory";
	public static final String CONFIG_OUTPUT_KML_MASTER_FILE = "outputKMLSwissFacilitiesInRegionsMasterFile";
	public static final String CONFIG_USE_COMPRESSION = "useCompression";
	public static final String CONFIG_OUTPUT_KML_LOWEST_SIZE_CLASS = "outputKMLSwissFacilitiesInRegionsLowestSizeClass";
	public static final String CONFIG_USE_KMZ_WRITER = "useKMZWriter";
	public static final String SEP = System.getProperty("file.separator");

	private KML masterKML;
	private Document masterDocument;
	private Style networkStyle;

	TreeMap<String, KML> kmls = new TreeMap<String, KML>();

	private boolean useCompression;
	private int lowestSizeClass;
	private String baseDirectory;
	private String masterFilename;
	private boolean useKMZWriter;

	private TreeMap<String, String> iconHREFs = new TreeMap<String, String>();
	private TreeMap<Integer, Integer> sizeClasses = new TreeMap<Integer, Integer>();
	private TreeMap<Integer, Double> iconScales = new TreeMap<Integer, Double>();
	private TreeMap<Integer, Double> regionSizes = new TreeMap<Integer, Double>();

	public FacilitiesExportToGUESS() {
		super();
	}

	@Override
	public void run(Facilities facilities) {

		// initialize from config
		this.baseDirectory = Gbl.getConfig().getParam(KML21_MODULE, CONFIG_OUTPUT_KML_BASE_DIRECTORY);
		this.masterFilename = Gbl.getConfig().getParam(KML21_MODULE, CONFIG_OUTPUT_KML_MASTER_FILE);

		if (Gbl.getConfig().getParam(KML21_MODULE, CONFIG_USE_COMPRESSION).equals("true")) {
			this.useCompression = Boolean.TRUE;
		} else if(Gbl.getConfig().getParam(KML21_MODULE, CONFIG_USE_COMPRESSION).equals("false")) {
			this.useCompression = Boolean.FALSE;
		} else {
			Gbl.errorMsg(
					"Invalid value for config parameter " + CONFIG_USE_COMPRESSION +
					" in module " + KML21_MODULE +
					": \"" + Gbl.getConfig().getParam(KML21_MODULE, CONFIG_USE_COMPRESSION) + "\"");
		}

		if (Gbl.getConfig().getParam(KML21_MODULE, CONFIG_USE_KMZ_WRITER).equals("true")) {
			this.useKMZWriter = Boolean.TRUE;
		} else if(Gbl.getConfig().getParam(KML21_MODULE, CONFIG_USE_KMZ_WRITER).equals("false")) {
			this.useKMZWriter = Boolean.FALSE;
		} else {
			Gbl.errorMsg(
					"Invalid value for config parameter " + CONFIG_USE_KMZ_WRITER +
					" in module " + KML21_MODULE +
					": \"" + Gbl.getConfig().getParam(KML21_MODULE, CONFIG_USE_KMZ_WRITER) + "\"");
		}

		this.lowestSizeClass = Integer.parseInt(Gbl.getConfig().getParam(KML21_MODULE, CONFIG_OUTPUT_KML_LOWEST_SIZE_CLASS));

		// use the web references to stay platform independent
		this.iconHREFs.put("work", "http://maps.google.com/mapfiles/kml/paddle/W.png");
		this.iconHREFs.put("education", "http://maps.google.com/mapfiles/kml/paddle/E.png");
		this.iconHREFs.put("shop", "http://maps.google.com/mapfiles/kml/paddle/S.png");
		this.iconHREFs.put("leisure", "http://maps.google.com/mapfiles/kml/paddle/L.png");

		this.sizeClasses.put(1, 0);
		this.sizeClasses.put(2, 10);
		this.sizeClasses.put(3, 50);
		this.sizeClasses.put(4, 250);
		this.sizeClasses.put(5, Integer.MAX_VALUE);

		this.iconScales.put(1, Math.sqrt(1.0));
		this.iconScales.put(2, Math.sqrt(2.0));
		this.iconScales.put(3, Math.sqrt(3.0));
		this.iconScales.put(4, Math.sqrt(4.0));

		this.regionSizes.put(1, 250.0);
		this.regionSizes.put(2, 500.0);
		this.regionSizes.put(3, 1000.0);
		this.regionSizes.put(4, 2000.0);

		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		try {

			this.masterKML = new KML();
			this.masterDocument = new Document("master document");
			this.masterKML.setFeature(this.masterDocument);

			writeKMLStyles();
			//doFacilities(facilities);
			this.doFacilitiesWithNetworkLinks(facilities);
			//doNetwork();

			writeFacilities();

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("    done.");
	}

	public void writeKMLStyles() throws IOException {

		String activity;
		Integer sizeClass;

		// KML facilities styles
		Iterator<String> activityIt = this.iconHREFs.keySet().iterator();
		while (activityIt.hasNext()) {

			activity = activityIt.next();

			Iterator<Integer> scaleIt = this.iconScales.keySet().iterator();
			while (scaleIt.hasNext()) {

				sizeClass = scaleIt.next();


				Style aStyle = new Style(new String(activity + this.sizeClasses.get(sizeClass) + "Style"));
				this.masterDocument.addStyle(aStyle);
				IconStyle anIconStyle = new IconStyle(
						new Icon(new String(this.iconHREFs.get(activity))),
						Color.DEFAULT_COLOR,
						IconStyle.DEFAULT_COLOR_MODE,
						this.iconScales.get(sizeClass)
						);
				aStyle.setIconStyle(anIconStyle);

			}

		}

		// KML network link style
		this.networkStyle = new Style("networkLinkStyle");
		this.masterDocument.addStyle(this.networkStyle);
		this.networkStyle.setLineStyle(new LineStyle(new Color("7f", "ff", "00", "00"), LineStyle.DEFAULT_COLOR_MODE, 5));

	}

	public void doFacilitiesWithNetworkLinks(Facilities facilities) throws IOException {

		String actType = "work", tmpActType;
		int cnt = 0, skip = 1, workCapacity = 0, sizeClass = 0;

		System.out.println("Processing facilities...");
		Iterator<? extends Location> f_i = facilities.getLocations().values().iterator();

		CH1903LV03toWGS84 ct = new CH1903LV03toWGS84();

		while(f_i.hasNext()) {

			Facility f = (Facility)f_i.next();
			CoordI chCoord = f.getCenter();
			CoordI lonLat = ct.transform(chCoord);

			workCapacity = f.getActivity("work").getCapacity();

			sizeClass = 1;
			while (this.sizeClasses.get(sizeClass + 1) < workCapacity) {
				sizeClass++;
			}

			// if there is another activity but work, choose it
			actType = "work";
			Iterator<String> a_it = f.getActivities().keySet().iterator();
			while (a_it.hasNext()) {

				tmpActType = a_it.next();
				if (!tmpActType.equals("work")) {
					actType = tmpActType;
				}
			}

			if (sizeClass >= this.lowestSizeClass) {

				KML theKML = this.createFacilitiesKML(chCoord, sizeClass);
				Document theDocument = (Document) theKML.getFeature();

				// copy the style from the master KML document to this "child" KML document
				String styleId = new String(actType + this.sizeClasses.get(sizeClass) + "Style");
				if (!theDocument.containsStyle(styleId)) {
					theDocument.addStyle(this.masterKML.getFeature().getStyle(styleId));
				}

				// put the placemark in it (including the region)
				Placemark aFacility = new Placemark(
						f.getId().toString(),
						Placemark.DEFAULT_NAME,
						"Object id: " + f.getId().toString() + SEP + "Number of employees: " + workCapacity,
						Feature.DEFAULT_ADDRESS,
						Placemark.DEFAULT_LOOK_AT,
						this.masterDocument.getStyle(styleId).getStyleUrl(),
						Placemark.DEFAULT_VISIBILITY,
						Placemark.DEFAULT_REGION,
						Feature.DEFAULT_TIME_PRIMITIVE);
				aFacility.setGeometry(new Point(lonLat.getX(), lonLat.getY(), 0));

				theDocument.addFeature(aFacility);

			}

			cnt++;
			if (cnt % skip == 0) {
				System.out.println("facility count: " + cnt);
				skip *= 2;
			}

		}
		System.out.println("facility count: " + cnt + " DONE.");
		System.out.println();
		System.out.println("Processing facilities...DONE.");
		System.out.println();

	}

	public void writeFacilities() {

		File masterDir = new File(this.baseDirectory);
		if (!masterDir.exists()) {
			if (!masterDir.mkdirs()) {
				Gbl.errorMsg("Error creating dir: " + masterDir.toString());
			}
		}

		if (this.useKMZWriter) {

			System.out.println("Writing out the KMLs...");
			KMZWriter myKMZWriter = new KMZWriter(this.baseDirectory + SEP + "allSwissFacilitiesInOneKMZFile");

			System.out.println("  Writing the master file...");
			myKMZWriter.writeMainKml(this.masterKML);
			System.out.println("  Writing the master file...DONE.");

			System.out.println("  Writing the linked files...");
			Iterator<String> kmlId_it = this.kmls.keySet().iterator();

			while (kmlId_it.hasNext()) {

				String filename = kmlId_it.next();
				KML writeKML = this.kmls.get(filename);

				myKMZWriter.writeLinkedKml(filename, writeKML);

			}
			System.out.println("  Writing the linked files...DONE.");

			System.out.println("Writing out the KMLs...DONE.");

			myKMZWriter.close();

		}
		else
		{
			KMLWriter myKMLWriter;

			System.out.println("  Writing the master file...");
			myKMLWriter = new KMLWriter(
					this.masterKML,
					this.baseDirectory + SEP + this.masterFilename,
					XMLNS.V_21,
					this.useCompression);
			myKMLWriter.write();
			System.out.println("  Writing the master file...DONE.");

			Iterator<String> kmlId_it = this.kmls.keySet().iterator();

			System.out.println("  Writing the other files...");
			while (kmlId_it.hasNext()) {

				String filename = kmlId_it.next();
				KML writeKML = this.kmls.get(filename);

				myKMLWriter = new KMLWriter(
						writeKML,
						filename,
						XMLNS.V_21,
						this.useCompression);
				myKMLWriter.write();
			}
			System.out.println("  Writing the other files...DONE.");

			System.out.println("Writing out the KMLs...DONE.");
		}
	}

	public void doNetwork() throws IOException {

//		int cnt = 0, skip = 1;
//
//		double x = 0, y = 0;
//		double width = 10, height = 10;
//		// we need some dummy activity capacity
//		double capacity = 10;
//		CoordWGS84 lookAtCoordCH1903, fromNodeCoordCH1903, toNodeCoordCH1903;
//		String actType = "networkNode";
//
//		System.out.println("  creating network object... ");
//		NetworkLayer network = new NetworkLayer();
//		System.out.println("  done.");
//
//		System.out.println("  reading network file... ");
//		NetworkParser parser = new NetworkParser(network);
//		parser.parse();
//		System.out.println("  done.");
//
//		Iterator link_it = network.getLinks().iterator();
//
//		Link link = null;
//		Node fromNode = null;
//		Node toNode = null;
//		double fromX = 0.0, fromY = 0.0, toX = 0.0, toY = 0.0;
//		cnt = 0; skip = 1;
//		TreeSet<Integer> cutLinks = new TreeSet<Integer>();
//		TreeSet<Integer> cutNodes = new TreeSet<Integer>();
//		cutLinks.clear();
//		cutNodes.clear();
//		System.out.println("  Determining relevant links...");
//		while (link_it.hasNext()) {
//
//			link = (Link) link_it.next();
//			fromNode = link.getFromNode();
//			toNode = link.getToNode();
//			fromX = fromNode.getCoord().getX();
//			fromY = fromNode.getCoord().getY();
//			toX = fromNode.getCoord().getX();
//			toY = fromNode.getCoord().getY();
//
//			// cut to be displayed
//			if (
//					((fromX >= xMin) && (fromX < xMax) && (fromY >= yMin) && (fromY < yMax)) ||
//					((toX >= xMin) && (toX < xMax) && (toY >= yMin) && (toY < yMax))
//			) {
//
//				cutLinks.add(link.getID());
//				cutNodes.add(fromNode.getID());
//				cutNodes.add(toNode.getID());
//
//				cnt++;
//				if (cnt % skip == 0) {
//					System.out.println("\tnetwork link cnt = " + cnt);
//					skip *= 2;
//				}
//			}
//		}
//		System.out.println("\tnetwork link cnt = " + cnt + " - finished.");
//		System.out.println("  done.");
//
//		// output network nodes
//		System.out.println("  Writing network nodes...");
//		Iterator<Integer> cutNodes_it = cutNodes.iterator();
//
//		while (cutNodes_it.hasNext()) {
//
//			fromNode = network.getNode(cutNodes_it.next().toString());
//			GDFOut.write("node" + fromNode.getID() + "," +
//					fromNode.getCoord().getX() + "," +
//					fromNode.getCoord().getY() + "," +
//					width + "," +
//					height + "," +
//					actType + "," +
//					capacity); GDFOut.newLine();
//		}
//		System.out.println("  done.");
//
//		System.out.println("  Writing network links...");
//		GDFOut.write("edgedef> node1,node2,directed");
//
//		Folder networkFolder = myKMLDocument.createFolder(
//				"Network",
//				"Network",
//				"Contains the network links.",
//				Folder.DEFAULT_LOOK_AT,
//				Folder.DEFAULT_STYLE_URL,
//				Folder.DEFAULT_VISIBILITY,
//				Folder.DEFAULT_REGION);
//
//		// output network links
//
//		Iterator<Integer> cutLinks_it = cutLinks.iterator();
//
//		while (cutLinks_it.hasNext()) {
//
//			link = network.getLink(cutLinks_it.next().toString());
//			fromNode = link.getFromNode();
//			toNode = link.getToNode();
//			fromX = fromNode.getCoord().getX();
//			fromY = fromNode.getCoord().getY();
//			toX = toNode.getCoord().getX();
//			toY = toNode.getCoord().getY();
//			// revert x and y again
//			fromNodeCoordCH1903 = new CoordWGS84(new CH1903Date(fromY), new CH1903Date(fromX));
//			toNodeCoordCH1903 = new CoordWGS84(new CH1903Date(toY), new CH1903Date(toX));
//			if ((fromY < toY) && (fromX < toX)) {
//				lookAtCoordCH1903 =
//					new CoordWGS84(
//						new CH1903Date(fromY + Math.abs((toY - fromY) / 2)),
//						new CH1903Date(fromX + Math.abs((toX - fromX) / 2)
//								)
//						);
//			}
//			else if ((fromY > toY) && (fromX < toX)) {
//					lookAtCoordCH1903 =
//						new CoordWGS84(
//							new CH1903Date(toY + Math.abs((toY - fromY) / 2)),
//							new CH1903Date(fromX + Math.abs((toX - fromX) / 2)
//									)
//							);
//			}
//			else if ((fromY < toY) && (fromX > toX)) {
//				lookAtCoordCH1903 =
//					new CoordWGS84(
//							new CH1903Date(fromY + Math.abs((toY - fromY) / 2)),
//							new CH1903Date(toX + Math.abs((toX - fromX) / 2)
//									)
//							);
//			}
//			else //if ((fromY > toY) && (fromX > toX)) {
//			{
//					lookAtCoordCH1903 =
//						new CoordWGS84(
//								new CH1903Date(toY + Math.abs((toY - fromY) / 2)),
//								new CH1903Date(toX + Math.abs((toX - fromX) / 2)));
//			}
//			GDFOut.write("node" + fromNode.getID() + "," +
//					"node" + toNode.getID() + "," +
//					"true"); GDFOut.newLine();
//
//			Placemark aLink = networkFolder.createPlacemark(
//					new Integer(link.getID()).toString(),
//					new Integer(link.getID()).toString(),
//					new Integer(link.getID()).toString(),
//					Placemark.DEFAULT_LOOK_AT,
//					this.networkStyle.getStyleUrl(),
//					Placemark.DEFAULT_VISIBILITY,
//					Placemark.DEFAULT_REGION);
//
//			Point fromPoint = new Point(
//					fromNodeCoordCH1903.getLongitude(),
//					fromNodeCoordCH1903.getLatitude(),
//					0.0);
//			Point toPoint = new Point(toNodeCoordCH1903.getLongitude(), toNodeCoordCH1903.getLatitude(), 0.0);
//			LineString aLineString = aLink.createLineString(fromPoint, toPoint);
//
//
//		}
//		System.out.println("  done.");

	}

	public KML createFacilitiesKML(CoordI chCoord, int sizeClass) {

		double regionSize = 0;
		int kmlX = 0, kmlY = 0;

		File makeDir;

		// construct the KML index, which is at the same time the relative filename with directory structure

		String kmlFilename = new String(this.baseDirectory);

		for (int ii=4; ii >= sizeClass; ii--) {

			regionSize = this.regionSizes.get(ii);
			kmlX = ( ((int) chCoord.getX()) / (int)regionSize ) * (int)regionSize;
			kmlY = ( ((int) chCoord.getY()) / (int)regionSize ) * (int)regionSize;

			kmlFilename = kmlFilename.concat(SEP + kmlX + "_" + kmlY);

		}

		if (!this.useKMZWriter) {
			makeDir = new File(kmlFilename);
			if (!makeDir.exists()) {
				if (!makeDir.mkdirs()) {
					Gbl.errorMsg("Error creating directory: " + kmlFilename);
				}
			}
		}

		kmlFilename = kmlFilename.concat(SEP + "facilities_" + kmlX + "_" + kmlY + "_" + this.sizeClasses.get(sizeClass));
		if (this.useCompression) {
			kmlFilename = kmlFilename.concat(".kmz");
		} else {
			kmlFilename = kmlFilename.concat(".kml");
		}

		if (this.useKMZWriter) {
			kmlFilename = kmlFilename.substring(kmlFilename.lastIndexOf(SEP) + 1, kmlFilename.length());
		}

		// if KML doesn't exist, create it
		if (!this.kmls.containsKey(kmlFilename)) {

			// create and store the KML
			KML newKML = new KML();
			newKML.setFeature(new Document(
					kmlFilename,
					kmlFilename,
					"Contains all facilities with number of employees > " + this.sizeClasses.get(sizeClass) +
					" of square (" + kmlX + ";" + kmlY + ") - ("+ Double.toString(kmlX + regionSize) + ";" + Double.toString(kmlY + regionSize) + ")",
					Feature.DEFAULT_ADDRESS,
					Feature.DEFAULT_LOOK_AT,
					Feature.DEFAULT_STYLE_URL,
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE));

			// create the corners
			CH1903LV03toWGS84 transform = new CH1903LV03toWGS84();
			CoordI southWestCorner = transform.transform(new Coord(kmlX, kmlY));
			CoordI northEastCorner = transform.transform(new Coord(kmlX + regionSize, kmlY + regionSize));

			// create the region
			Region theRegion = new Region(
					northEastCorner.getY(),
					southWestCorner.getY(),
					southWestCorner.getX(),
					northEastCorner.getX(),
					512,
					Region.DEFAULT_MAX_LOD_PIXELS);

			this.kmls.put(kmlFilename, newKML);

			KML parentKML;
			if (sizeClass == 4) {
				parentKML = this.masterKML;
			} else {
				// recursive
				parentKML = this.createFacilitiesKML(chCoord, sizeClass + 1);
			}

			Document parentDocument = (Document) parentKML.getFeature();

			// add the Link to the parent KML (including the region)
			if (!parentDocument.containsFeature(kmlFilename)) {

				String strLink = kmlFilename.substring(
					kmlFilename.lastIndexOf(SEP, kmlFilename.lastIndexOf(SEP) - 1) + 1);

				NetworkLink nl = new NetworkLink(
						strLink,
						new org.matsim.utils.vis.kml.Link(
								strLink,
								ViewRefreshMode.ON_REGION),
								Feature.DEFAULT_NAME,
								Feature.DEFAULT_DESCRIPTION,
								Feature.DEFAULT_ADDRESS,
								Feature.DEFAULT_LOOK_AT,
								Feature.DEFAULT_STYLE_URL,
								Feature.DEFAULT_VISIBILITY,
								theRegion,
								Feature.DEFAULT_TIME_PRIMITIVE);
				parentDocument.addFeature(nl);
			}

		}

		return this.kmls.get(kmlFilename);

	}

}

