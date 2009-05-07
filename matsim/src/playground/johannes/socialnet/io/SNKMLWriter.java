/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriter.java
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
package playground.johannes.socialnet.io;

import java.io.IOException;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.StyleType;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.vis.kml.KMZWriter;

import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialTie;

/**
 * @author illenberger
 *
 */
public class SNKMLWriter<P extends BasicPerson<? extends BasicPlan<? extends BasicPlanElement>>> {

//	private static ObjectFactory kmlObjectFactory = new ObjectFactory();
	
//	private static DocumentType mainDoc;
	
	private CoordinateTransformation transformation;
	
	private SNKMLObjectDescriptor<Ego<P>> vertexDescriptor;
	
	public void setCoordinateTransformation(CoordinateTransformation transformation) {
		this.transformation = transformation;
	}
	
	public void write(SocialNetwork<P> socialnet, SNKMLObjectStyle<Ego<P>, P> vertexStyle, SNKMLObjectStyle<SocialTie, P> edgeStyle, String filename) {
		try {
		ObjectFactory objectFactory = new ObjectFactory();
		KMZWriter kmzWriter = new KMZWriter(filename);
		
		DocumentType document = objectFactory.createDocumentType();
		
		
		
		LinkType iconLink = objectFactory.createLinkType();
		iconLink.setHref("node.png");
		kmzWriter.addNonKMLFile(MatsimResource.getAsInputStream("icon18.png"), "node.png");
		
		FolderType graphFolder = objectFactory.createFolderType();
		graphFolder.setName("Social Network");
		/*
		 * write vertices
		 */
		if(vertexStyle != null) {
			/*
			 * get vertex styles
			 */
			for(StyleType styleType : vertexStyle.getObjectStyle(socialnet, iconLink)) {
				document.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(styleType));
			}
			/*
			 * create vertex folder
			 */
			FolderType vertexFolder = objectFactory.createFolderType();
			vertexFolder.setName("Vertices");
			
			for(Ego<P> e : socialnet.getVertices()) {
				/*
				 * create a point geometry
				 */
				PointType point = objectFactory.createPointType();
				Coord coord = e.getCoord();
				if(transformation != null)
					coord = transformation.transform(e.getCoord());
				point.getCoordinates().add(String.format("%1$s,%2$s", Double.toString(coord.getX()), Double.toString(coord.getY())));
				/*
				 * create placemark
				 */
				PlacemarkType placemark = objectFactory.createPlacemarkType();
				
				placemark.setAbstractGeometryGroup(objectFactory.createPoint(point));
				placemark.setStyleUrl(vertexStyle.getObjectSytleId(e));
				if(vertexDescriptor != null) {
					placemark.setDescription(vertexDescriptor.getDescription(e));
					placemark.setName(vertexDescriptor.getName(e));
				}
				/*
				 * add placemark to vertex folder
				 */
				vertexFolder.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemark));
			}
			
			graphFolder.getAbstractFeatureGroup().add(objectFactory.createFolder(vertexFolder));
		}
		
		if(edgeStyle != null) {
			throw new UnsupportedOperationException("Not implemented yet!");
		}
		
		document.getAbstractFeatureGroup().add(objectFactory.createFolder(graphFolder));
		
		KmlType kml = objectFactory.createKmlType();
		kml.setAbstractFeatureGroup(objectFactory.createDocument(document));
		
		kmzWriter.writeMainKml(kml);
		kmzWriter.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
//	public static void main(String[] args) throws IOException {
//		SocialNetwork<Person> socialnet = SNGraphMLReader.loadFromConfig("/Users/fearonni/vsp-work/work/socialnets/data/ivt2006/config.xml", "/Users/fearonni/vsp-work/work/socialnets/data/ivt2006/ivt2006.graphml");
//		final String kmzFilename = "/Users/fearonni/Desktop/network.kmz";
//		
//		CH1903LV03toWGS84 transform = new CH1903LV03toWGS84();
////		NetworkLayer network = new NetworkLayer();
////		new MatsimNetworkReader(network).readFile("test/scenarios/equil/network.xml");
////		
//		ObjectFactory kmlObjectFactory = new ObjectFactory();
//		KMZWriter kmzWriter = new KMZWriter(kmzFilename);
//		
//		KmlType mainKml = kmlObjectFactory.createKmlType();
//		mainDoc = kmlObjectFactory.createDocumentType();
//		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
//		
////		KmlNetworkWriter kmlNetworkWriter = new KmlNetworkWriter(network, new AtlantisToWGS84(), kmzWriter, mainDoc);
//	
//		FolderType graphFolder = kmlObjectFactory.createFolderType();
//		graphFolder.setName("My social network");
//		
//		StyleType egoStyle = kmlObjectFactory.createStyleType();
//		egoStyle.setId("egostyle");
//		LinkType iconLink = kmlObjectFactory.createLinkType();
//		iconLink.setHref("node.png");
//		kmzWriter.addNonKMLFile(MatsimResource.getAsInputStream("icon18.png"), "node.png");
//		IconStyleType iStyle = kmlObjectFactory.createIconStyleType();
//		iStyle.setIcon(iconLink);
//		iStyle.setColor(MatsimKmlStyleFactory.MATSIMRED);
//		iStyle.setScale(0.5);
//		egoStyle.setIconStyle(iStyle);
//		mainDoc.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(egoStyle));
//		
//		TIntObjectHashMap<String> styleMap = getStyleTypes(socialnet);
//		
//		FolderType egoFolder = kmlObjectFactory.createFolderType();
//		egoFolder.setName("Egos");
//		for(Ego e : socialnet.getVertices()) {
//			if(e.getEdges().size() > 1) {
//			PlacemarkType placemark = kmlObjectFactory.createPlacemarkType();
//			placemark.setName(e.getPerson().getId().toString());
//			PointType point = kmlObjectFactory.createPointType();
//			Coord coord = transform.transform(e.getCoord());
//			point.getCoordinates().add(String.format("%1$s,%2$s", Double.toString(coord.getX()), Double.toString(coord.getY())));
//			placemark.setAbstractGeometryGroup(kmlObjectFactory.createPoint(point));
//			placemark.setStyleUrl(styleMap.get(e.getEdges().size()));
////			placemark.setStyleUrl(egoStyle.getId());
//			placemark.setDescription("k=" + Integer.toString(e.getEdges().size()));
//			
//			egoFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(placemark));
//			}
//		}
//		graphFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(egoFolder));
//		
//		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(graphFolder));
//		
//		kmzWriter.writeMainKml(mainKml);
//		kmzWriter.close();
//	}
//	
//	public static TIntObjectHashMap<String> getStyleTypes(SocialNetwork<Person> socialnet) {
//		double[] degrees = GraphStatistics.getDegreeDistribution(socialnet).absoluteDistribution().keys();
//		double k_min = StatUtils.min(degrees);
//		double k_max = StatUtils.max(degrees);
//		TIntObjectHashMap<String> styleMap = new TIntObjectHashMap<String>();
//		
//		for(double k : degrees) {
//			StyleType type = kmlObjectFactory.createStyleType();
//			type.setId("stylek" + (int)k);
//			LinkType iconLink = kmlObjectFactory.createLinkType();
//			iconLink.setHref("node.png");
////			kmzWriter.addNonKMLFile(MatsimResource.getAsInputStream("icon18.png"), "node.png");
//			IconStyleType iStyle = kmlObjectFactory.createIconStyleType();
//			iStyle.setIcon(iconLink);
//			Color color = ColorUtils.getHeatmapColor((k - k_min) / (k_max - k_min));
//			iStyle.setColor(new byte[]{(byte)color.getAlpha(), (byte)color.getBlue(), (byte)color.getGreen(), (byte)color.getRed()});
//			iStyle.setScale(0.5);
//			type.setIconStyle(iStyle);
//			mainDoc.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(type));
//			
//			styleMap.put((int)k, type.getId());
//		}
//		return styleMap;
//	}
}
