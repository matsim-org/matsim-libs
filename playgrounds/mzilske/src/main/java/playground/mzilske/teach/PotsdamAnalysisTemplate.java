/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mzilske.teach;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class PotsdamAnalysisTemplate implements Runnable {
	
	private PolylineFeatureFactory factory;
	
	private GeometryFactory geometryFactory = new GeometryFactory();

	private Scenario scenario;
	
	public static void main(String[] args) {
		PotsdamAnalysisTemplate potsdamAnalysis = new PotsdamAnalysisTemplate();
		potsdamAnalysis.run();
	}

	@Override
	public void run() {
		analyse();
		initFeatureType();
		Collection<SimpleFeature> features = createFeatures();
		ShapeFileWriter.writeGeometries(features, "output/links");
	}

	private void analyse() {
		scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("examples/equil/config.xml"));
	}

	private List<SimpleFeature> createFeatures() {
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			SimpleFeature feature = getFeature(link);
			features.add(feature);
		}
		return features;
	}
	
	
	/**
	 * Ein FeatureType ist sozusagen die Datensatzbeschreibung für das, was visualisiert werden soll.
	 * Schreiben Sie in das Array attribs hier Ihre eigenen Datenfelder dazu.
	 * 
	 */
	private void initFeatureType() {
		this.factory = new PolylineFeatureFactory.Builder().
				setCrs(MGC.getCRS(TransformationFactory.WGS84_UTM35S)).
				setName("links").
				addAttribute("ID", String.class).
				addAttribute("length", Double.class).
				
				// Hier weitere Attribute anlegen

				create();
	}
	
	
	/**
	 * 
	 * So erzeugt man ein Feature aus einem Link.
	 * Es kann um weitere Attribute erweitert werden.
	 * 
	 */
	private SimpleFeature getFeature(final Link link) {
		Coordinate[] coords = new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())};
		Object [] attribs = new Object[] {
				link.getId().toString(),
				link.getLength()
				
				// Hier die weiteren Attribute ausfüllen
		};

		return this.factory.createPolyline(coords, attribs, null);
	}
	
}
