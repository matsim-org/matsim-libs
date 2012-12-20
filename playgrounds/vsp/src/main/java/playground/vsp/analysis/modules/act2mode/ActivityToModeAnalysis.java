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
package playground.vsp.analysis.modules.act2mode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author droeder
 *
 */
public class ActivityToModeAnalysis extends AbstractAnalyisModule {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(ActivityToModeAnalysis.class);
	private Network net;
	private ActivityToModeAnalysisHandler handler;
	private HashMap<Integer, Set<Feature>> departureSlotFeatures;
	private HashMap<Integer, Set<Feature>> arrivalSlotFeatures;
	private int slotSize;
	private final String targetCoordinateSystem;

	/**
	 * a class to create shapefiles per timeSlot for activities mapped to the first mainmode after/before the activity
	 * @param sc, the scenario (it has to contain facilities!!!)
	 * @param personsOfInterest, might be null, than all persons are processed
	 * @param slotSize, timeSlotSize in seconds
	 */
	public ActivityToModeAnalysis(Scenario sc, Set<Id> personsOfInterest, int slotSize, String targetCoordinateSystem) {
		super(ActivityToModeAnalysis.class.getSimpleName());
		this.net = sc.getNetwork();
		this.handler = new ActivityToModeAnalysisHandler(this.net, personsOfInterest, ((ScenarioImpl)sc).getActivityFacilities());
		this.slotSize = slotSize;
		this.targetCoordinateSystem = targetCoordinateSystem;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {

	}

	@Override
	public void postProcessData() {
		// creature featureType
		AttributeType[] attribs = new AttributeType[3];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null, null, MGC.getCRS(this.targetCoordinateSystem));
		attribs[1] = AttributeTypeFactory.newAttributeType("ActType", String.class);
		attribs[2] = AttributeTypeFactory.newAttributeType("Mode", String.class);
		FeatureType featureType = null ;
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attribs, "activities");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		//create features for departure
		this.departureSlotFeatures = new HashMap<Integer, Set<Feature>>();
		for(ActivityToMode atm : this.handler.getDepartures()){
			createFeatureAndAdd(atm, this.departureSlotFeatures, featureType);
		}
		
		//create features for arrivals
		this.arrivalSlotFeatures = new HashMap<Integer, Set<Feature>>();
		for(ActivityToMode atm : this.handler.getArrivals()){
			createFeatureAndAdd(atm, this.arrivalSlotFeatures, featureType);
		}

	}

	/**
	 * @param atm
	 * @param departureSlotFeatures2
	 * @param featureType 
	 */
	private void createFeatureAndAdd(ActivityToMode atm, 
			HashMap<Integer, Set<Feature>> slotFeatures, FeatureType featureType) {
		Integer slice = (int) (atm.getTime() / this.slotSize);
		Set<Feature> temp = slotFeatures.get(slice);
		if(temp == null) {
			temp = new HashSet<Feature>();
		}
		Object[] featureAttribs = new Object[3];
		featureAttribs[0] = new GeometryFactory().createPoint(new Coordinate(atm.getCoord().getX(), atm.getCoord().getY(), 0.));
		featureAttribs[1] = atm.getActType();
		featureAttribs[2] = atm.getMode();
		Feature f;
		try {
			f = featureType.create(featureAttribs);
			temp.add(f);
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		slotFeatures.put(slice, temp);
	}

	@Override
	public void writeResults(String outputFolder) {
		for(Entry<Integer, Set<Feature>> e: this.departureSlotFeatures.entrySet()){
			ShapeFileWriter.writeGeometries(e.getValue(), outputFolder + "departure_" + e.getKey().toString()  + ".shp");
		}
		for(Entry<Integer, Set<Feature>> e: this.arrivalSlotFeatures.entrySet()){
			ShapeFileWriter.writeGeometries(e.getValue(), outputFolder + "arrival_" + e.getKey().toString()  + ".shp");
		}
	}
}

