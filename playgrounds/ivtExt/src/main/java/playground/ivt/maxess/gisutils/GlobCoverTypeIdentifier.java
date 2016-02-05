/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.maxess.gisutils;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.operation.Mosaic;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.util.Arrays;

/**
 * @author thibautd
 */
public class GlobCoverTypeIdentifier {
	private final CoordinateTransformation innerToRasterTransformation;
	private final GridCoverage2D grid;

	public enum LandCover { vegetation, bare, water, no_data, artificial; }

	public GlobCoverTypeIdentifier(
			final String targetCrs,
			final String globCoverTiffFile ) {
		this.grid = readTiff( globCoverTiffFile );

		this.innerToRasterTransformation =
				TransformationFactory.getCoordinateTransformation(
						targetCrs,
						grid.getCoordinateReferenceSystem2D().toWKT() );
	}

	public LandCover getLandCover( final Coord coord ) {
		final Coord dataCrsCoord = innerToRasterTransformation.transform( coord );
		final int[] value = grid.evaluate(
				(DirectPosition) new DirectPosition2D(
					dataCrsCoord.getX(),
					dataCrsCoord.getY() ),
				new int[ 1 ] );

		//Value	Label	Red	Green	Blue
		switch ( value[ 0 ] ) {
			case 11:	//Post-flooding or irrigated croplands (or aquatic)	170	240	240
			case 14:	//Rainfed croplands	255	255	100
			case 20:	//Mosaic cropland (50-70%) / vegetation (grassland/shrubland/forest) (20-50%)	220	240	100
			case 30:	//Mosaic vegetation (grassland/shrubland/forest) (50-70%) / cropland (20-50%) 	205	205	102
			case 40:	//Closed to open (>15%) broadleaved evergreen or semi-deciduous forest (>5m)	0	100	0
			case 50:	//Closed (>40%) broadleaved deciduous forest (>5m)	0	160	0
			case 60:	//Open (15-40%) broadleaved deciduous forest/woodland (>5m)	170	200	0
			case 70:	//Closed (>40%) needleleaved evergreen forest (>5m)	0	60	0
			case 90:	//Open (15-40%) needleleaved deciduous or evergreen forest (>5m)	40	100	0
			case 100:	//Closed to open (>15%) mixed broadleaved and needleleaved forest (>5m)	120	130	0
			case 110:	//Mosaic forest or shrubland (50-70%) / grassland (20-50%)	140	160	0
			case 120:	//Mosaic grassland (50-70%) / forest or shrubland (20-50%) 	190	150	0
			case 130:	//Closed to open (>15%) (broadleaved or needleleaved, evergreen or deciduous) shrubland (<5m)	150	100	0
			case 140:	//Closed to open (>15%) herbaceous vegetation (grassland, savannas or lichens/mosses)	255	180	50
			case 150:	//Sparse (<15%) vegetation	255	235	175
			case 160:	//Closed to open (>15%) broadleaved forest regularly flooded (semi-permanently or temporarily) - Fresh or brackish water	0	120	90
			case 170:	//Closed (>40%) broadleaved forest or shrubland permanently flooded - Saline or brackish water	0	150	120
			case 180:	//Closed to open (>15%) grassland or woody vegetation on regularly flooded or waterlogged soil - Fresh, brackish or saline water	0	220	130
				return LandCover.vegetation;
			case 190:	//Artificial surfaces and associated areas (Urban areas >50%)	195	20	0
				return LandCover.artificial;
			case 200:	//Bare areas	255	245	215
			case 220:	//Permanent snow and ice	255	255	255
				return LandCover.bare;
			case 210:	//Water bodies	0	70	200
				return LandCover.water;
			case 230:	//No data (burnt areas, clouds,…)	0	0	0
				return LandCover.no_data;
			default:
				throw new IllegalArgumentException( "unknown code "+value[ 0 ] );
		}
	}

	private static GridCoverage2D readTiff( String file ) {
		try {
			final GeoTiffReader reader = new GeoTiffReader( new File( file ) );
			// could probably be limited in area, but could not really understand how
			return reader.read( null );
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}
}
