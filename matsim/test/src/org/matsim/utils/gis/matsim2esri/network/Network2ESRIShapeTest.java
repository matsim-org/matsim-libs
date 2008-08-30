/* *********************************************************************** *
 * project: org.matsim.*
 * Network2ESRIShapeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.gis.matsim2esri.network;

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Network2ESRIShapeTest extends MatsimTestCase  {

	
	//TODO [GL] - find a way to compare *.dbf files since simple checksum tests are not applicable here. - 08/30/2008 gl
	public void testPolygonCapacityShape() {
		String netFileName = "test/scenarios/berlin/network.xml.gz";
		String outputFileP = getOutputDirectory() + "./network.shp";
		String ref = getInputDirectory() + "./network.shp";
		
		
		Gbl.createConfig(null);
		Gbl.getConfig().global().setCoordinateSystem("DHDN_GK4");

		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);

		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(network);
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(0.001);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Network2ESRIShape(network,outputFileP, builder).write();
		
		System.out.println("calculating *.shp file checksums...");
		long checksum1 = CRCChecksum.getCRCFromFile(ref);;
		long checksum2 = CRCChecksum.getCRCFromGZFile(outputFileP);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals(checksum1, checksum2);
		

	}
	
	public void testPolygonLanesShape() {
		String netFileName = "test/scenarios/berlin/network.xml.gz";
		String outputFileP = getOutputDirectory() + "./network.shp";
		String ref = getInputDirectory() + "./network.shp";
		
		
		Gbl.createConfig(null);
		Gbl.getConfig().global().setCoordinateSystem("DHDN_GK4");

		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);

		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(network);
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(1);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Network2ESRIShape(network,outputFileP, builder).write();
		
		System.out.println("calculating *.shp file checksums...");
		long checksum1 = CRCChecksum.getCRCFromFile(ref);;
		long checksum2 = CRCChecksum.getCRCFromGZFile(outputFileP);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals(checksum1, checksum2);
	}
	
	public void testPolygonFreespeedShape() {
		String netFileName = "test/scenarios/berlin/network.xml.gz";
		String outputFileP = getOutputDirectory() + "./network.shp";
		String ref = getInputDirectory() + "./network.shp";
		
		
		Gbl.createConfig(null);
		Gbl.getConfig().global().setCoordinateSystem("DHDN_GK4");

		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);

		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(network);
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(1);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Network2ESRIShape(network,outputFileP, builder).write();
		
		System.out.println("calculating *.shp file checksums...");
		long checksum1 = CRCChecksum.getCRCFromFile(ref);;
		long checksum2 = CRCChecksum.getCRCFromGZFile(outputFileP);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals(checksum1, checksum2);
	}
	
	public void testLineStringShape() {
		String netFileName = "test/scenarios/berlin/network.xml.gz";
		String outputFileShp = getOutputDirectory() + "./network.shp";
//		String outputFileDbf = getOutputDirectory() + "./network.dbf";
		String refShp = getInputDirectory() + "./network.shp";
//		String refDbf = getInputDirectory() + "./network.dbf";
		
		
		Gbl.createConfig(null);
		Gbl.getConfig().global().setCoordinateSystem("DHDN_GK4");

		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);

		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(network);
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(1);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Network2ESRIShape(network,outputFileShp, builder).write();
		
		System.out.println("calculating *.shp file checksums...");
		long checksum1 = CRCChecksum.getCRCFromFile(refShp);;
		long checksum2 = CRCChecksum.getCRCFromGZFile(outputFileShp);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals(checksum1, checksum2);
		
//		System.out.println("calculating *.dbf file checksums...");
//		checksum1 = CRCChecksum.getCRCFromFile(refDbf);;
//		checksum2 = CRCChecksum.getCRCFromGZFile(outputFileDbf);
//		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
//		assertEquals(checksum1, checksum2);
	}
}
