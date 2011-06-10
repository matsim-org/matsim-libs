/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusSignals2PointLayer
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.cottbus.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.signalsystems.data.SignalsData;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPoint;


/**
 * @author dgrether
 *
 */
public class DgCottbusSignals2PointLayer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ScenarioImpl sc = CottbusUtils.loadCottbusScenrio(true);
		Map<Id, Set<Id>> systemId2NodeIdsMap = DgSignalsUtils.calculateSignalizedNodesPerSystem(sc.getScenarioElement(SignalsData.class).getSignalSystemsData(), sc.getNetwork());
		String srsId = TransformationFactory.WGS84_UTM33N;
		CoordinateReferenceSystem networkSrs = MGC.getCRS(srsId);
		FeatureType ft = createMultiPointSignalSystemFeatureType(networkSrs);
		List<Feature> multiPointFeatures = new ArrayList<Feature>();
		for (Id systemId : systemId2NodeIdsMap.keySet()){
			Set<Id> nodeIds = systemId2NodeIdsMap.get(systemId);
			Coordinate[] nodeCoords = new Coordinate[nodeIds.size()];
			int i = 0;
			for (Id nodeId : nodeIds){
				Node node = sc.getNetwork().getNodes().get(nodeId);
				nodeCoords[i] = MGC.coord2Coordinate(node.getCoord());
				i++;
			}
			MultiPoint multiPoint = MGC.geoFac.createMultiPoint(nodeCoords);
			Feature feature = ft.create(new Object[]{multiPoint, systemId.toString()});
			multiPointFeatures.add(feature);
		}
		ShapeFileWriter.writeGeometries(multiPointFeatures, DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_system_shape/signal_systems.shp");
	}

	
	private static FeatureType createMultiPointSignalSystemFeatureType(CoordinateReferenceSystem crs) throws FactoryConfigurationError, SchemaException{
		AttributeType[] attrAct = new AttributeType[2];
		attrAct[0] = DefaultAttributeTypeFactory.newAttributeType("MultiPoint",MultiPoint.class, true, null, null, crs);
		attrAct[1] = AttributeTypeFactory.newAttributeType("sig_sys_id", String.class);
		FeatureType featureTypeAct = FeatureTypeBuilder.newFeatureType(attrAct, "signal_system_feature");
		return featureTypeAct;
	}

	
}
