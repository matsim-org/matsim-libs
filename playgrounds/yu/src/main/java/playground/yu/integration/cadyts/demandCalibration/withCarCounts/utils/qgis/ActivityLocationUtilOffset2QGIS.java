/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityLocationUtilOffset2QGIS.java
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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.Grid2Graph;
import playground.yu.utils.qgis.X2QGIS;

public class ActivityLocationUtilOffset2QGIS implements X2QGIS {
	/**
	 * this class is only a copy of
	 * <class>playground.gregor.shapeFileToMATSim.ShapeFileWriter</class> Gregor
	 * Laemmel's
	 * 
	 * @author ychen
	 * 
	 */
	public static class ShapeFileWriter2 {
		@SuppressWarnings("deprecation")
		public static void writeGeometries(final Collection<Feature> features,
				final String filename) throws IOException, FactoryException,
				SchemaException {
			ShapefileDataStore datastore = new ShapefileDataStore(new File(
					filename).toURI().toURL());
			FeatureType ft = features.iterator().next().getFeatureType();
			datastore.createSchema(ft);
			((FeatureStore) datastore.getFeatureSource(ft.getTypeName()))
					.addFeatures(DataUtilities.reader(features));
		}
	}

	private CoordinateReferenceSystem crs = null;
	private Grid2Graph g2g = null;

	public ActivityLocationUtilOffset2QGIS(Scenario scenario,
			String coordRefSys, double gridSideLength_m,
			Map<Coord, Tuple<Integer, Double>> gridUtilOffsetMap) {
		crs = MGC.getCRS(coordRefSys);
		g2g = new Grid2Graph(crs, 1000d/* [m] */, gridUtilOffsetMap);
	}

	/**
	 * @param ShapeFilename
	 *            where the shapefile will be saved
	 */
	public void writeShapeFile(final String ShapeFilename) {
		try {
			ShapeFileWriter2.writeGeometries(g2g.getFeatures(), ShapeFilename);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
	}

	/**
	 * transfers additional parameters to Graph
	 * 
	 * @param paraName
	 * @param clazz
	 * @param parameters
	 */
	public void addParameter(final String paraName, final Class<?> clazz,
			final Map<Id, ?> parameters) {
		g2g.addParameter(paraName, clazz, parameters);
	}
}
