/* *********************************************************************** *
 * project: org.matsim.*
 * TrafficStateXmlWriterTest
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
package playground.vsp.energy.trafficstate;

import java.io.File;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author dgrether
 *
 */
public class TrafficStateXmlWriterTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void testWriteFile() {
		this.writeFile();
	}
	
	private final String writeFile(){
		TrafficState ts = new TrafficState();
		EdgeInfo ei = new EdgeInfo(Id.create("23", Link.class));
		TimeBin timeBin = new TimeBin(0.0, 3600.0 , 24.3);
		ei.getTimeBins().add(timeBin);
		timeBin = new TimeBin(3600.0, 7200.0 , 23.3);
		ei.getTimeBins().add(timeBin);
		ts.addEdgeInfo(ei);
		
		ei = new EdgeInfo(Id.create("42", Link.class));
		timeBin = new TimeBin(0.0, 3600.0 , 21.3);
		ei.getTimeBins().add(timeBin);
		timeBin = new TimeBin(3600.0, 7200.0 , 20.3);
		ei.getTimeBins().add(timeBin);
		ts.addEdgeInfo(ei);

		String outfile = this.testUtils.getOutputDirectory() + "test_traffic_state.xml";
		new TrafficStateXmlWriter(ts).writeFile(outfile);
		
		Assert.assertTrue(new File(outfile).exists());
		
		return outfile;
	}
	
	@Test
	public final void testReadFile(){
		String inputfile = this.writeFile();
		String outputfile = this.testUtils.getOutputDirectory() + "test_traffic_state_out.xml";
		TrafficState ts = new TrafficState();
		new TrafficStateXmlReader(ts).readFile(inputfile);
		new TrafficStateXmlWriter(ts).writeFile(outputfile);
	}
	
}
