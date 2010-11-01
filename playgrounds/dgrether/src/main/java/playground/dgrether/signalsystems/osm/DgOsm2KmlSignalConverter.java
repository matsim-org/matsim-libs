/* *********************************************************************** *
 * project: org.matsim.*
 * DgOsmSignalConverter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.osm;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;

import org.apache.log4j.Logger;
import org.matsim.vis.kml.KMZWriter;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.filter.v0_6.TagFilter;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgOsm2KmlSignalConverter {
	
	private static final Logger log = Logger.getLogger(DgOsm2KmlSignalConverter.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = DgPaths.WORKBASE + "research/daten/berlin/berlin.osm";
//		filename = "/home/dgrether/Desktop/map.osm";
		
		String outfile = DgPaths.WORKBASE + "research/daten/berlin/berlin.osm.signals.kmz";
//		outfile = "/home/dgrether/Desktop/map.signals.kmz";
		
		Set<String> emptyKeys = Collections.emptySet();
		Map<String, Set<String>> emptyKVs = Collections.emptyMap();
		
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		tagKeyValues.put("highway", new HashSet<String>(Arrays.asList("traffic_signals")));
		
		
		TagFilter tagFilterWays = new TagFilter("reject-ways", emptyKeys, emptyKVs);
		TagFilter tagFilterRelations = new TagFilter("reject-relations", emptyKeys, emptyKVs);
		
		TagFilter tagFilter = new TagFilter("accept-node", emptyKeys, tagKeyValues);
		
		Osm2Kml osm2Kml = new Osm2Kml();
		
//		FastXmlReader reader = new FastXmlReader(new File(filename ), true, CompressionMethod.BZip2);
		FastXmlReader reader = new FastXmlReader(new File(filename ), true, CompressionMethod.None);

		
		
		reader.setSink(tagFilterRelations);
		tagFilterRelations.setSink(tagFilterWays);
		tagFilterWays.setSink(tagFilter);
		tagFilter.setSink(osm2Kml);
		
		reader.run();
		
		KmlType kml = osm2Kml.getKmlType();
		KMZWriter writer = new KMZWriter(outfile);
		writer.writeMainKml(kml);
		writer.close();
		log.info("done!");
	}

}


class Osm2Kml implements Sink{

	private static final Logger log = Logger.getLogger(Osm2Kml.class);
	
	private ObjectFactory kmlObjectFactory = new ObjectFactory();

	private KmlType mainKml;

	private DocumentType mainDoc;

	private FolderType mainFolder;
	
	Osm2Kml(){
		this.mainKml = kmlObjectFactory.createKmlType();
		this.mainDoc = kmlObjectFactory.createDocumentType();
		this.mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
		this.mainFolder = kmlObjectFactory.createFolderType();
		this.mainFolder.setName("Signals from osm");
		this.mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(this.mainFolder));

	}
	
	public KmlType getKmlType() {
		return mainKml;
	}

	@Override
	public void process(EntityContainer c) {
		if (c.getEntity() instanceof Node){
			Node n = (Node) c.getEntity();
//		log.debug(n);
			PlacemarkType p = this.kmlObjectFactory.createPlacemarkType();
			p.setName(Long.toString(n.getId()));
			
			PointType point = this.kmlObjectFactory.createPointType();
			point.getCoordinates().add(n.getLongitude() + "," + n.getLatitude() + ",0.0");
			p.setAbstractGeometryGroup(this.kmlObjectFactory.createPoint(point));
			StringBuffer desc = new StringBuffer();
			for (Tag t : c.getEntity().getTags()){
				desc.append("<p>");
				desc.append(t.getKey());
				desc.append(" ");
				desc.append(t.getValue());
				desc.append("</p>");
			}
			p.setDescription(desc.toString());
			
			JAXBElement<PlacemarkType> placemark = this.kmlObjectFactory.createPlacemark(p);
			this.mainFolder.getAbstractFeatureGroup().add(placemark);
		}
	}

	@Override
	public void complete() {

	}

	@Override
	public void release() {
		
	}
	
}