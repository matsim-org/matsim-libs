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

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.model.SignalSystem;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPoint;


/**
 * Writes the Cottbus signal systems to a shape file containing points at signalized nodes.
 * 
 * @author dgrether
 *
 */
public class DgCottbusSignals2PointLayerShape {

	public static void main(String[] args) throws Exception {
		MutableScenario sc = CottbusUtils.loadCottbusScenrio(true);
		Map<Id<SignalSystem>, Set<Id<Node>>> systemId2NodeIdsMap = DgSignalsUtils.calculateSignalizedNodesPerSystem(((SignalsData) sc.getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalSystemsData(), sc.getNetwork());
		String srsId = TransformationFactory.WGS84_UTM33N;
		CoordinateReferenceSystem networkSrs = MGC.getCRS(srsId);
		SimpleFeatureBuilder builder = createMultiPointSignalSystemFeatureBuilder(networkSrs);
		List<SimpleFeature> multiPointFeatures = new ArrayList<SimpleFeature>();
		for (Id<SignalSystem> systemId : systemId2NodeIdsMap.keySet()){
			Set<Id<Node>> nodeIds = systemId2NodeIdsMap.get(systemId);
			Coordinate[] nodeCoords = new Coordinate[nodeIds.size()];
			int i = 0;
			for (Id<Node> nodeId : nodeIds){
				Node node = sc.getNetwork().getNodes().get(nodeId);
				nodeCoords[i] = MGC.coord2Coordinate(node.getCoord());
				i++;
			}
			MultiPoint multiPoint = MGC.geoFac.createMultiPoint(nodeCoords);
			SimpleFeature feature = builder.buildFeature(null, new Object[]{multiPoint, systemId.toString()});
			multiPointFeatures.add(feature);
		}
		ShapeFileWriter.writeGeometries(multiPointFeatures, DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/shape_files/signal_systems/signal_systems_no_13.shp");
	}

	private static SimpleFeatureBuilder createMultiPointSignalSystemFeatureBuilder(CoordinateReferenceSystem crs) {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(crs);
		b.setName("signal_system_feature");
		b.add("location", MultiPoint.class);
		b.add("sig_sys_id", String.class);
		return new SimpleFeatureBuilder(b.buildFeatureType());
	}
	
}
