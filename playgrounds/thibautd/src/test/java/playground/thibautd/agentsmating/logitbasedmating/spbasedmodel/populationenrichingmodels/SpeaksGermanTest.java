/* *********************************************************************** *
 * project: org.matsim.*
 * SpeaksGermanTest.java
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
package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel.populationenrichingmodels;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests the good behaviour of the german language "model"
 * @author thibautd
 */
public class SpeaksGermanTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private String id2langFile;
	private Map<Id, Boolean> speaksGermanMap;

	/**
	 * Creates the files to use to test input.
	 * @throws IOException 
	 */
	@Before
	public void init() throws IOException {
		String directory = utils.getClassInputDirectory();
		(new File( directory )).mkdirs();
		id2langFile = directory + "/file.txt";

		Map<Id, Boolean> map = new HashMap<Id, Boolean>();

		map.put( new IdImpl( 1 ) , true );
		map.put( new IdImpl( 2 ) , false );
		map.put( new IdImpl( 3 ) , false );
		map.put( new IdImpl( 4 ) , true );
		map.put( new IdImpl( 5 ) , false );
		map.put( new IdImpl( 6 ) , true );
		map.put( new IdImpl( 7 ) , false );
		map.put( new IdImpl( 8 ) , true );

		speaksGermanMap = Collections.unmodifiableMap( map );

		BufferedWriter writer = IOUtils.getBufferedWriter( id2langFile );

		Random rand = new Random( 1984 );
		for (Map.Entry<Id, Boolean> entry : map.entrySet()) {
			String lang = entry.getValue() ? "g" : (rand.nextBoolean() ? "f" : "i" );
			writer.write( entry.getKey() +"\t" + lang );
			writer.newLine();
		}
		writer.close();
	}

	@Test
	public void testSize() {
		SpeaksGermanModel model = new SpeaksGermanModel( id2langFile );

		Assert.assertEquals(
				"did not retrieve as many items as put in the file",
				speaksGermanMap.size(),
				model.size());
	}

	/**
	 * tests that data is imported properly from file
	 */
	@Test
	public void importTest() {
		SpeaksGermanModel model = new SpeaksGermanModel( id2langFile );

		int count = 0;
		for (Map.Entry<Id, Boolean> entry : speaksGermanMap.entrySet()) {
			count++;
			boolean modelValue;

			try {
				modelValue = model.speaksGerman( entry.getKey() );
			}
			catch (IllegalArgumentException e) {
				Assert.fail( "while reading "+count+"th value got exception "+e.getClass().getSimpleName()+" saying: "+e.getMessage() );
				return;
			}

			Assert.assertTrue(
					"the value retrieved from the file is not the value put in the file!",
					entry.getValue().equals( modelValue ) );
		}
	}
}

