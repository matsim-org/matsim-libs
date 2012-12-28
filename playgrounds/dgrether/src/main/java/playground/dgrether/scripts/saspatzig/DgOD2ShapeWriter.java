/* *********************************************************************** *
 * project: org.matsim.*
 * DgOD2ShapeWriter
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
package playground.dgrether.scripts.saspatzig;

import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.dgrether.DgPaths;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;


/**
 * @author dgrether
 *
 */
public class DgOD2ShapeWriter {

	/**
	 * @param args
	 * @throws SchemaException 
	 * @throws FactoryConfigurationError 
	 * @throws FactoryException 
	 * @throws TransformException 
	 * @throws IllegalAttributeException 
	 */
	public static void main(String[] args) throws SchemaException, FactoryException, TransformException, IllegalArgumentException {
		//### bvg project demand 
		String popFile = DgPaths.REPOS +  "shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/scenario/plans.times.xml.gz";
		CoordinateReferenceSystem popCrs = MGC.getCRS(TransformationFactory.DHDN_GK4);
//		String popFile = DgPaths.REPOS +  "shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-1.0sample/scenario/plans.times.xml.gz";
		String lkwIdPart = "lkw";
		String wvIdPart = "wv";
		boolean acceptAll = false;
		String outFeatureFilename = DgPaths.REPOS +  "shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/scenario/rev554-bvg00-0.1sample_od_shape/plans.times.";
//		String outFeatureFilename = DgPaths.REPOS +  "shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-1.0sample/scenario/rev554-bvg00-0.1sample_od_shape/plans.times."+ idPart +".shp";
		//### end bvg project demand 
		
		//### satellic project demand 
		popFile = DgPaths.REPOS +  "shared-svn/studies/countries/de/berlin_prognose_2025/bb_gv_10pct.xml.gz";
		popCrs = MGC.getCRS(TransformationFactory.WGS84);
		acceptAll = true;
		outFeatureFilename = DgPaths.REPOS +  "shared-svn/studies/countries/de/berlin_prognose_2025/bb.gv.10pct.xml.";
	//### end satellic project demand 
		
		
		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile(networkFile);
		config.plans().setInputFile(popFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		CoordinateReferenceSystem targetCrs = MGC.getCRS(TransformationFactory.WGS84);
		MathTransform transformation = CRS.findMathTransform(popCrs, targetCrs, true);
		SimpleFeatureBuilder mulitPointFt = createMultiPointActFeatureBuilder(targetCrs);
		List<SimpleFeature> multiPointFeatures = new ArrayList<SimpleFeature>();
		SimpleFeatureBuilder pointFt = createPointActFeatureBuilder(targetCrs);
		List<SimpleFeature> pointFeatures = new ArrayList<SimpleFeature>();

		Population pop = scenario.getPopulation();
		for (Person person : pop.getPersons().values()){
			if (acceptAll || person.getId().toString().contains(lkwIdPart) || person.getId().toString().contains(wvIdPart)){
				Activity act1 = (Activity) person.getSelectedPlan().getPlanElements().get(0);
				Activity act2 = (Activity) person.getSelectedPlan().getPlanElements().get(2);
				Coordinate a1c = MGC.coord2Coordinate(act1.getCoord());
				Coordinate a2c = MGC.coord2Coordinate(act2.getCoord());
				a1c = JTS.transform(a1c, a1c, transformation);
				a2c = JTS.transform(a2c, a2c, transformation);
				MultiPoint multiPoint = MGC.geoFac.createMultiPoint(new Coordinate[]{a1c, a2c});
				SimpleFeature feature = mulitPointFt.buildFeature(null, new Object[]{multiPoint, person.getId(), act1.getType(), act1.getStartTime(), act1.getEndTime(), act2.getStartTime(), act2.getEndTime()});
				multiPointFeatures.add(feature);
				
				Point point = MGC.geoFac.createPoint(a1c);
				feature = pointFt.buildFeature(null, new Object[] {point, person.getId(), act1.getType() , act1.getStartTime(), act1.getEndTime()});
				pointFeatures.add(feature);
				
				point = MGC.geoFac.createPoint(a2c);
				feature = pointFt.buildFeature(null, new Object[] {point, person.getId(), act2.getType() , act2.getStartTime(), act2.getEndTime()});
				pointFeatures.add(feature);
			}
		}
		
		ShapeFileWriter.writeGeometries(multiPointFeatures, outFeatureFilename + "multipoint.features.shp");
		ShapeFileWriter.writeGeometries(pointFeatures, outFeatureFilename + "point.features.shp");
	}

	private static SimpleFeatureBuilder createMultiPointActFeatureBuilder(CoordinateReferenceSystem crs) {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(crs);
		b.setName("multi_activity");
		b.add("MultiPoint",MultiPoint.class);
		b.add("person_id", String.class);
		b.add("activity_type", String.class);
		b.add("act1_start_time", Double.class);
		b.add("act1_end_time", Double.class);
		b.add("act2_start_time", Double.class);
		b.add("act2_end_time", Double.class);
		return new SimpleFeatureBuilder(b.buildFeatureType());
	}

	private static SimpleFeatureBuilder createPointActFeatureBuilder(CoordinateReferenceSystem crs) {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(crs);
		b.setName("activity");
		b.add("Point", Point.class);
		b.add("person_id", String.class);
		b.add("activity_type", String.class);
		b.add("act_start_time", Double.class);
		b.add("act_end_time", Double.class);
		return new SimpleFeatureBuilder(b.buildFeatureType());
	}

}
