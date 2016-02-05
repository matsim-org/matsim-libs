/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.cadyts.utils;

import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.cadyts.pt.CadytsPtIT;
import org.matsim.testcases.MatsimTestUtils;

import cadyts.utilities.io.tabularFileParser.TabularFileParser;

public class CalibrationStatReaderTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testReader() throws IOException {
		TabularFileParser tabularFileParser = new TabularFileParser();
		URL url = CadytsPtIT.class.getClassLoader().getResource(this.utils.getInputDirectory() + "calibration-stats.txt");
		String calibStatFile = url.getFile(); // hack to get the file loaded from classpath, which is not directly supported by Cadyts
		CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
		tabularFileParser.parse(calibStatFile, calibrationStatReader);
		CalibrationStatReader.StatisticsData statData6= calibrationStatReader.getCalStatMap().get(Integer.valueOf(6));
		Assert.assertEquals("differrent Count_ll", "-1.546875", statData6.getCount_ll() );
		Assert.assertEquals("differrent Count_ll_pred_err",  "9.917082938182276E-8" , statData6.getCount_ll_pred_err() );
		Assert.assertEquals("differrent Link_lambda_avg", "0.0013507168476099964", statData6.getLink_lambda_avg() );
		Assert.assertEquals("differrent Link_lambda_max", "0.031434867572002166" , statData6.getLink_lambda_max() );
		Assert.assertEquals("differrent Link_lambda_min", "0.0", statData6.getLink_lambda_min() );
		Assert.assertEquals("differrent Link_lambda_stddev", "0.0058320747961925256" , statData6.getLink_lambda_stddev());
		Assert.assertEquals("differrent P2p_ll", "--" , statData6.getP2p_ll());
		Assert.assertEquals("differrent Plan_lambda_avg", "0.04322293912351989", statData6.getPlan_lambda_avg() );
		Assert.assertEquals("differrent Plan_lambda_max", "0.04715229919344063" , statData6.getPlan_lambda_max() );
		Assert.assertEquals("differrent Plan_lambda_min", "0.03929357905359915" , statData6.getPlan_lambda_min() );
		Assert.assertEquals("differrent Plan_lambda_stddev", "0.004200662608832472" , statData6.getPlan_lambda_stddev());
		Assert.assertEquals("differrent Total_ll", "-1.546875", statData6.getTotal_ll() );
	}
}
