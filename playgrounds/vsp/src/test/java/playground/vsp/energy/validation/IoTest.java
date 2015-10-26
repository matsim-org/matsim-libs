/* *********************************************************************** *
 * project: org.matsim.*
 * IoTest
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
package playground.vsp.energy.validation;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.energy.validation.ObjectFactory;
import playground.vsp.energy.validation.PoiInfo;
import playground.vsp.energy.validation.PoiTimeInfo;
import playground.vsp.energy.validation.ValidationInfoReader;
import playground.vsp.energy.validation.ValidationInfoWriter;
import playground.vsp.energy.validation.ValidationInformation;


/**
 * @author dgrether
 *
 */
public class IoTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public final void testOutput() {
		ValidationInformation list = new ValidationInformation();
		PoiInfo poiInfo = new PoiInfo();
		poiInfo.setPoiID("test1");
		poiInfo.setMaximumCapacity(5.);
		list.add(poiInfo);
		PoiTimeInfo poiTimeInfo = new PoiTimeInfo();
		GregorianCalendar cal = this.getCalendar4Hour(6);
		poiTimeInfo.setStartTime(cal);
		cal = this.getCalendar4Hour(7);
		poiTimeInfo.setEndTime(cal);
		poiTimeInfo.setUsedCapacity(6.);
		poiInfo.getPoiTimeInfos().add(poiTimeInfo);
		poiTimeInfo = new PoiTimeInfo();
		poiTimeInfo.setStartTime(this.getCalendar4Hour(7));
		poiTimeInfo.setEndTime(this.getCalendar4Hour(8));
		poiTimeInfo.setUsedCapacity(4.);
		poiInfo.getPoiTimeInfos().add(poiTimeInfo);
		
		String outfile = this.testUtils.getOutputDirectory() +  "erValidationInfo.xml";
		
		System.err.println(outfile);
		System.err.println(new File("file").getAbsolutePath());
		
		new ValidationInfoWriter(list).writeFile(outfile);
	}

	@Test
	public final void testInput() {
		String input  = this.testUtils.getPackageInputDirectory() + "erValidationInfo.xml";
		
		ValidationInformation list = new ValidationInfoReader().readFile(input);
		Assert.assertNotNull(list);
		Assert.assertEquals(1, list.size());
		PoiInfo poiInfo = list.get(0);
		Assert.assertEquals("test1", poiInfo.getPoiID());
		Assert.assertNotNull(poiInfo.getPoiTimeInfos());
		Assert.assertEquals(5.0, poiInfo.getMaximumCapacity());
	}
	
	
	private GregorianCalendar getCalendar4Hour(int hour){
		TimeZone zone = TimeZone.getTimeZone("GMT+00:00");
		GregorianCalendar cal = new GregorianCalendar(2012,03,16,00,00,00);
		cal.setTimeZone(zone);
		cal.set(Calendar.HOUR, hour);
		return cal;
	}
	
	@Test
	public final void generateSchema() throws Exception {
		JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		SchemaOutputResolver schemaResolver = new SchemaOutputResolver() {
			private Map<String, StreamResult> resultMap = new HashMap<String, StreamResult>();
			@Override
			public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
				StringWriter writer = new StringWriter();
				StreamResult result = new StreamResult(writer);
				result.setSystemId(suggestedFileName);
				resultMap.put(namespaceUri, result);
				return result;
			}
			
			@Override
			public String toString(){
				StringBuilder sb = new StringBuilder();
				for (Entry<String, StreamResult> e : this.resultMap.entrySet()){
					sb.append("Namespace URI: ");
					sb.append(e.getKey());
					sb.append("\n");
					sb.append(e.getValue().getWriter().toString());
					sb.append("\n");
				}
				return sb.toString();
			}
		};
		
		ctx.generateSchema(schemaResolver);
		System.out.println(schemaResolver.toString());
		
	}
	
}
