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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import cadyts.utilities.io.tabularFileParser.TabularFileParser;

public class CalibrationStatReaderTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testReader() throws IOException {
		TabularFileParser tabularFileParser = new TabularFileParser();
		String calibStatFile = this.utils.getInputDirectory() + "calibration-stats.txt";
		CalibrationStatReader calibrationStatReader = new CalibrationStatReader();
		tabularFileParser.parse(calibStatFile, calibrationStatReader);
		CalibrationStatReader.StatisticsData statData6= calibrationStatReader.getCalStatMap().get(Integer.valueOf(6));
		Assertions.assertEquals("-1.546875", statData6.getCount_ll(), "different Count_ll" );
		Assertions.assertEquals("9.917082938182276E-8" , statData6.getCount_ll_pred_err(), "different Count_ll_pred_err" );
		Assertions.assertEquals("0.0013507168476099964", statData6.getLink_lambda_avg(), "different Link_lambda_avg" );
		Assertions.assertEquals("0.031434867572002166" , statData6.getLink_lambda_max(), "different Link_lambda_max" );
		Assertions.assertEquals("0.0", statData6.getLink_lambda_min(), "different Link_lambda_min" );
		Assertions.assertEquals("0.0058320747961925256" , statData6.getLink_lambda_stddev(), "different Link_lambda_stddev");
		Assertions.assertEquals("--" , statData6.getP2p_ll(), "different P2p_ll");
		Assertions.assertEquals("0.04322293912351989", statData6.getPlan_lambda_avg(), "different Plan_lambda_avg" );
		Assertions.assertEquals("0.04715229919344063" , statData6.getPlan_lambda_max(), "different Plan_lambda_max" );
		Assertions.assertEquals("0.03929357905359915" , statData6.getPlan_lambda_min(), "different Plan_lambda_min" );
		Assertions.assertEquals("0.004200662608832472" , statData6.getPlan_lambda_stddev(), "different Plan_lambda_stddev");
		Assertions.assertEquals("-1.546875", statData6.getTotal_ll(), "different Total_ll" );
	}
}
