/* *********************************************************************** *
 * project: org.matsim.*
 * TransformCRSTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.io.IOException;

import org.geotools.referencing.CRS;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialGraph;
import org.matsim.contrib.sna.snowball.spatial.io.SampledSpatialGraphMLReader;
import org.matsim.contrib.sna.snowball.spatial.io.SampledSpatialGraphMLWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * @author illenberger
 *
 */
public class TransformCRSTask implements GraphTask<SpatialGraph> {

	private CoordinateReferenceSystem targetCRS;
	
	public TransformCRSTask(CoordinateReferenceSystem targetCRS) {
		this.targetCRS = targetCRS;
	}
	
	@Override
	public SpatialGraph apply(SpatialGraph graph) {
		CoordinateReferenceSystem sourceCRS = graph.getCoordinateReferenceSysten();
		try {
			MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
			
			for(SpatialVertex vertex : graph.getVertices()) {
				double[] points = new double[] {
						vertex.getPoint().getCoordinate().x,
						vertex.getPoint().getCoordinate().y };
				transform.transform(points, 0, points, 0, 1);
				vertex.getPoint().getCoordinate().x = points[0];
				vertex.getPoint().getCoordinate().y = points[1];
			}
		} catch (FactoryException e) {
			e.printStackTrace();
			return null;
		} catch (TransformException e) {
			e.printStackTrace();
			return null;
		}
		
		return graph;
	}

	public static void main(String[] args) throws IOException {
		TransformCRSTask task = new TransformCRSTask(CRSUtils.getCRS(Integer.parseInt(args[2])));
		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
		SampledSpatialGraph graph = reader.readGraph(args[0]);
		graph = (SampledSpatialGraph) task.apply(graph);
		SampledSpatialGraphMLWriter writer = new SampledSpatialGraphMLWriter();
		writer.write(graph, args[1]);
	}
}
