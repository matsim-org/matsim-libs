/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationExportToKML.java
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

package playground.lnicolas.kml;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.utils.geometry.shared.CoordWGS84;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Feature;
import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.IconStyle;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMLWriter;
import org.matsim.utils.vis.kml.LineStyle;
import org.matsim.utils.vis.kml.NetworkLink;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Point;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.kml.KMLWriter.XMLNS;
import org.matsim.utils.vis.kml.Link.ViewRefreshMode;
import org.matsim.utils.vis.kml.fields.Color;

public class PopulationExportToKML extends PlansAlgorithm {

	private KML masterKML;
	private Document masterDocument;
	private Style networkStyle;

	TreeMap<String, KML> kmls = new TreeMap<String, KML>();

	private boolean useCompression = true;

	public String KMLFilename = null;
	public String KMLDirname = null;

	// give here the lowest facility size class that should be visualized
	private final int LOWEST_SIZE_CLASS = 1;

	private TreeMap<Integer, String> iconHREFs = new TreeMap<Integer, String>();
	private TreeMap<Integer, Integer> sizeClasses = new TreeMap<Integer, Integer>();
	private TreeMap<Integer, Double> iconScales = new TreeMap<Integer, Double>();
	private TreeMap<Integer, Double> regionSizes = new TreeMap<Integer, Double>();

	public PopulationExportToKML(String KMLDirname) {
		super();
		this.KMLFilename = KMLDirname + "population_master.kml";
		this.KMLDirname = KMLDirname;
	}

	@Override
	public void run(Plans population) {

		for (int i = 0; i <= 15; i++) {
			if (i > 10) {
				this.iconHREFs.put(i, "http://maps.google.com/mapfiles/kml/paddle/blu-stars-lv.png");
			} else if (i == 0) {
				this.iconHREFs.put(i, "http://maps.google.com/mapfiles/kml/paddle/blu-circle-lv.png");
			} else {
				this.iconHREFs.put(i, "http://maps.google.com/mapfiles/kml/paddle/" + i + "-lv.png");
			}
		}

		this.sizeClasses.put(1, 0);
		this.sizeClasses.put(2, 20);
		this.sizeClasses.put(3, 37);
		this.sizeClasses.put(4, 55);
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
			this.writePopulation(population);
			//doNetwork();

			KMLWriter myKMLWriter = new KMLWriter(this.masterKML, this.KMLFilename, XMLNS.V_21, this.useCompression);
			myKMLWriter.write();

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("    done.");

	}

	public void writeKMLStyles() throws IOException {

		Integer age;
		Integer sizeClass;

		// KML facilities styles
		Iterator<Integer> ageIt = this.iconHREFs.keySet().iterator();
		while (ageIt.hasNext()) {

			age = ageIt.next();

			Iterator<Integer> scaleIt = this.iconScales.keySet().iterator();
			while (scaleIt.hasNext()) {

				sizeClass = scaleIt.next();

				Style aStyle = new Style(new String(age + this.sizeClasses.get(sizeClass) + "Style"));
				this.masterDocument.addStyle(aStyle);
				IconStyle anIconStyle = new IconStyle(
						new Icon(new String(this.iconHREFs.get((int)Math.ceil(age.doubleValue()/10.0)))),
						null,
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

	public void writePopulation(Plans population) throws IOException {

		CoordWGS84 chCoord;
		int cnt = 0, skip = 1, age = 0, sizeClass = 0;

		System.out.println("Processing population...");
		Iterator<Person> it = population.getPersons().values().iterator();

		while(it.hasNext()) {

			final double x, y, lat, lon;
			Person p = it.next();
			Act homeActivitiy = ((Act)p.getPlans().get(0).getActsLegs().get(0));
			x = homeActivitiy.getCoord().getX();
			y = homeActivitiy.getCoord().getY();

			// data in enterprise census have x and y wrong, so we have to revert them here
			chCoord = CoordWGS84.createFromCH1903(x, y);
			lat = chCoord.getLatitude();
			lon = chCoord.getLongitude();

			age = p.getAge();

			sizeClass = 1;
			while (this.sizeClasses.get(sizeClass + 1) < age) {
				sizeClass++;
			}

			if (sizeClass >= this.LOWEST_SIZE_CLASS) {

				KML theKML = this.createFacilitiesKML(chCoord, sizeClass);
				Document theDocument = (Document) theKML.getFeature();

				// copy the style from the master KML document to this "child" KML document
				String styleId = new String(p.getSex() + this.sizeClasses.get(sizeClass) + "Style");
				if (!theDocument.containsStyle(styleId)) {
					theDocument.addStyle(this.masterKML.getFeature().getStyle(styleId));
				}

				// put the placemark in it (including the region)
				Placemark aPerson = new Placemark(
						Integer.valueOf((p.getId().toString())).toString(),
						Integer.valueOf(p.getId().toString()).toString(),
						"Age: " + age + ", sex: " + p.getSex() + ", employed: "
							+ p.getEmployed() + ", car_avail: " + p.getCarAvail(),
						Placemark.DEFAULT_ADDRESS,
						Placemark.DEFAULT_LOOK_AT,
						this.masterDocument.getStyle(styleId).getStyleUrl(),
						Placemark.DEFAULT_VISIBILITY,
						Placemark.DEFAULT_REGION,
						Feature.DEFAULT_TIME_PRIMITIVE);
				aPerson.setGeometry(new Point(lon, lat, 0));

				theDocument.addFeature(aPerson);
			}

			cnt++;
			if (cnt % skip == 0) {
				System.out.println("person count: " + cnt);
				skip *= 2;
			}

		}
		System.out.println("person count: " + cnt + " DONE.");
		System.out.println();
		System.out.println("Processing facilities...DONE.");
		System.out.println();

		System.out.println("Writing out the KMLs...");
		File masterDir = new File(this.KMLDirname);
		if (!masterDir.exists()) {
			if (!masterDir.mkdirs()) {
				Gbl.errorMsg("Error creating dir: " + masterDir.toString());
			}
		}
		Iterator<String> kmlId_it = this.kmls.keySet().iterator();

		while (kmlId_it.hasNext()) {

			String filename = kmlId_it.next();
			KML writeKML = this.kmls.get(filename);

			KMLWriter myKMLWriter = new KMLWriter(writeKML, filename,
					XMLNS.V_21, this.useCompression);
			myKMLWriter.write();
		}
		System.out.println("Writing out the KMLs...DONE.");

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

	public KML createFacilitiesKML(CoordWGS84 chCoord, int sizeClass) {

		CoordWGS84 southWestCorner, northEastCorner;

		double regionSize = 0;
		int kmlX = 0, kmlY = 0;

		File makeDir;

		// construct the KML index, which is at the same time the filename with dirextory structure

		String kmlId = new String("/Users/meisterk/results/facilitiesInRegions");

		for (int ii=4; ii >= sizeClass; ii--) {

			regionSize = this.regionSizes.get(ii);
			kmlX = (new Double(chCoord.getXCH1903()).intValue() / new Double(regionSize).intValue()) * new Double(regionSize).intValue();
			kmlY = (new Double(chCoord.getYCH1903()).intValue() / new Double(regionSize).intValue()) * new Double(regionSize).intValue();

			kmlId = kmlId.concat("/" + kmlX + "_" + kmlY);

		}

		makeDir = new File(kmlId);
		if (!makeDir.exists()) {
			if (!makeDir.mkdirs()) {
				Gbl.errorMsg("Error creating directory: " + kmlId);
			}
		}

		kmlId = kmlId.concat("/facilities_" + kmlX + "_" + kmlY + "_" + this.sizeClasses.get(sizeClass));
		if (this.useCompression) {
			kmlId = kmlId.concat(".kmz");
		} else {
			kmlId = kmlId.concat(".kml");
		}

		// if KML doesn't exist, create it
		if (!this.kmls.containsKey(kmlId)) {

			// create and store the KML
			KML newKML = new KML();
			newKML.setFeature(new Document(
					kmlId,
					kmlId,
					"Contains all facilities with number of employees > " + this.sizeClasses.get(sizeClass) +
					" of square (" + kmlX + ";" + kmlY + ") - ("+ new Double(kmlX + regionSize) + ";" + new Double(kmlY + regionSize) + ")",
					Feature.DEFAULT_ADDRESS,
					Feature.DEFAULT_LOOK_AT,
					Feature.DEFAULT_STYLE_URL,
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE));

			// create the corners
			southWestCorner = CoordWGS84.createFromCH1903(kmlX, kmlY);

			northEastCorner = CoordWGS84.createFromCH1903(kmlX + regionSize, kmlY + regionSize);

			this.kmls.put(kmlId, newKML);

			KML parentKML;
			if (sizeClass == 4) {
				parentKML = this.masterKML;
			} else {
				// recursive
				parentKML = this.createFacilitiesKML(chCoord, sizeClass + 1);
			}

			Document parentDocument = (Document) parentKML.getFeature();

			// add the Link to the parent KML (including the region)
			if (!parentDocument.containsFeature(kmlId)) {
				NetworkLink nl = new NetworkLink(kmlId, new org.matsim.utils.vis.kml.Link(kmlId, ViewRefreshMode.ON_REGION));
				parentDocument.addFeature(nl);
			}

		}

		return this.kmls.get(kmlId);

	}
}

