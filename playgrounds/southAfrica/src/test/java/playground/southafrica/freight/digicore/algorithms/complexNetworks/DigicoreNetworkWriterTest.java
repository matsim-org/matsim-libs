/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreNetworkWriterTest.java
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

package playground.southafrica.freight.digicore.algorithms.complexNetworks;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

import playground.southafrica.freight.digicore.algorithms.complexNetwork.DigicoreNetworkWriter;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreNetwork;

public class DigicoreNetworkWriterTest {
	private static final Logger log = Logger.getLogger( DigicoreNetworkWriterTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testWriteNetwork() {
		DigicoreNetwork dn = buildSmallNetwork();
		DigicoreNetworkWriter dnw = new DigicoreNetworkWriter(dn);
		
		/* Check that writing and overwriting of output is correct. */
		try {
			dnw.writeNetwork(utils.getOutputDirectory() + "network.txt.gz");
		} catch (IOException e) {
			Assert.fail("Should be able to write the first file without IOException.");
		}
		try {
			dnw.writeNetwork(utils.getOutputDirectory() + "network.txt.gz");
			Assert.fail("Should not be able to overwrite the file.");
		} catch (IOException e) {
			/* Pass. */
		}
		try {
			dnw.writeNetwork(utils.getOutputDirectory() + "network.txt.gz", false);
			Assert.fail("Should not be able to overwrite the file.");
		} catch (IOException e) {
			/* Pass. */
		}
		try {
			dnw.writeNetwork(utils.getOutputDirectory() + "network.txt.gz", true);
		} catch (IOException e) {
			Assert.fail("Should overwrite the file without IOException.");
		}
		
		/* Now check the content of the file. */
		BufferedReader br = IOUtils.getBufferedReader(utils.getOutputDirectory() + "network.txt.gz");
		try {
			String line = br.readLine();
			log .warn( line );
			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("NODES"));
			line = br.readLine();
			log .warn( line );
			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("NodeId,Long,Lat"));
			// ---
//			line = br.readLine();
//			log .warn( line );
//			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("3,1.0000,1.0000"));
//			line = br.readLine();
//			log .warn( line );
//			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("2,0.0000,1.0000"));
//			line = br.readLine();
//			log .warn( line );
//			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("1,0.0000,0.0000"));
//			line = br.readLine();
//			log .warn( line );
//			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("4,1.0000,0.0000"));
			//
			// The underlying HashMap, provided by the jung graph library, does not guarantee ordering.  Thus modifying the test
			// to accept arbitrary orders. kai, feb'16
			//
			for ( int ii=0 ; ii<4 ; ii++ ) {
				line = br.readLine() ;
				log .warn( line );
				boolean problem = true ;
				if ( line.equalsIgnoreCase("3,1.0000,1.0000") ) problem = false ; 
				if ( line.equalsIgnoreCase("2,0.0000,1.0000") ) problem = false ; 
				if ( line.equalsIgnoreCase("1,0.0000,0.0000") ) problem = false ; 
				if ( line.equalsIgnoreCase("4,1.0000,0.0000") ) problem = false ;
				Assert.assertFalse( problem ) ;
			}
			// ---
			line = br.readLine();
			log .warn( line );
			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("ARCS"));
			line = br.readLine();
			log .warn( line );
			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("From_Id,To_Id,From_Type,To_Type,Weight"));
			// ---
//			line = br.readLine();
//			log .warn( line );
//			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("1,2,test,test,1"));
//			line = br.readLine();
//			log .warn( line );
//			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("3,1,test,test,1"));
//			line = br.readLine();
//			log .warn( line );
//			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("1,3,test,test,2"));
//			line = br.readLine();
//			log .warn( line );
//			Assert.assertTrue("Wrong line.", line.equalsIgnoreCase("4,1,test,test,3"));
			//
			for ( int ii=0 ; ii<4 ; ii++ ) {
				line = br.readLine();
				log .warn( line );
				boolean problem = true ;
				if ( line.equalsIgnoreCase("1,2,test,test,1") ) problem = false ;
				if ( line.equalsIgnoreCase("3,1,test,test,1") ) problem = false ;
				if ( line.equalsIgnoreCase("1,3,test,test,2") ) problem = false ;
				if ( line.equalsIgnoreCase("4,1,test,test,3") ) problem = false ;
				Assert.assertFalse( problem );
			}
			//
			// ---
		} catch (IOException e) {
			Assert.fail("Should not fail reading the file.");
		}
	}

	
	/**
	 * the following little graph is used:
	 * 
	 *  2      -------> 3
	 *  ^     /       /
	 *  |   w:2      /
	 *  |   /      w:1
	 * w:1 /       /
	 *  | /       /
	 *  |/<-------
	 *  1 <--- w:3 ---- 4
	 */
	@Test
	@Ignore
	public void testWriteGraphML(){
		DigicoreNetworkWriter dnw = new DigicoreNetworkWriter(buildSmallNetwork());
		String fGraphML = utils.getOutputDirectory() + "graphML.graphML";
//		dnw.writeGraphML(fGraphML, "test", "today");
	}
	
	/**
	 * Just test if no exceptions are thrown when writing to file.
	 */
	@Test
	public void testWriteGraphOrderToFile(){
		
		DigicoreNetworkWriter dnw = new DigicoreNetworkWriter(buildSmallNetwork());
		try {
			dnw.writeGraphOrderToFile(utils.getOutputDirectory() + "testOrder.csv");
		} catch (IOException e1) {
			Assert.fail("Should not throw an exception.");
		}
	}
	


	/**
	 * the following little graph is used:
	 * 
	 *  2      -------> 3
	 *  ^     /       /
	 *  |   w:2      /
	 *  |   /      w:1
	 * w:1 /       /
	 *  | /       /
	 *  |/<-------
	 *  1 <--- w:3 ---- 4
	 */
	private static DigicoreNetwork buildSmallNetwork(){
		DigicoreNetwork dn = new DigicoreNetwork();

		DigicoreActivity da1 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da1.setCoord(new Coord(0.0, 0.0));
		da1.setFacilityId(Id.create(1, ActivityFacility.class));	
		
		DigicoreActivity da2 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new Coord(0.0, 1.0));
		da2.setFacilityId(Id.create(2, ActivityFacility.class));
		
		DigicoreActivity da3 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da3.setCoord(new Coord(1.0, 1.0));
		da3.setFacilityId(Id.create(3, ActivityFacility.class));
		
		DigicoreActivity da4 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da4.setCoord(new Coord(1.0, 0.0));
		da4.setFacilityId(Id.create(4, ActivityFacility.class));

		dn.addArc(da1, da2);
		dn.addArc(da1, da3);
		dn.addArc(da1, da3);
		dn.addArc(da3, da1);
		dn.addArc(da4, da1);
		dn.addArc(da4, da1);
		dn.addArc(da4, da1);

		return dn;
	}


}

