/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.analysis.personMoney;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.ParallelEventsManager;
import org.matsim.testcases.MatsimTestUtils;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


public class PersonMoneyEventAggregatorTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Test method for {@link org.matsim.analysis.personMoney.PersonMoneyEventsCollector}.
	 */
	@Test
	void testPersonMoneyEventCollector() {


		Id<Person> passenger1 = Id.createPersonId("passenger1");
		Id<Person> passenger2 = Id.createPersonId("passenger2");
		Id<Person> passenger3 = Id.createPersonId("passenger3");

		String drt_A = "drt_A";
		String drt_B = "drt_B";
		String drt_C = "drt_C";

		ParallelEventsManager events = new ParallelEventsManager(false);

		PersonMoneyEventsAggregator aggregator = new PersonMoneyEventsAggregator(ConfigUtils.createConfig().global());
		events.addHandler(aggregator);

		final MutableDouble fare = new MutableDouble(0);
		events.addHandler(new PersonMoneyEventHandler() {
			@Override
			public void handleEvent(PersonMoneyEvent event) {
				fare.add(event.getAmount());
			}

			@Override
			public void reset(int iteration) {
			}
		});

		events.initProcessing();
		events.processEvent(new PersonMoneyEvent(3, passenger2, -2.5, "drtFare", drt_A, "discount"));
		events.processEvent(new PersonMoneyEvent(3, passenger3, -1.23, "drtFare", drt_A, "discount"));
		events.processEvent(new PersonMoneyEvent(5, passenger1, 22.3, "drtFare", drt_A, "testFee"));
		events.processEvent(new PersonMoneyEvent(1, passenger2, 4.10, "DrtFare", "DrtA", "testFee"));
		events.processEvent(new PersonMoneyEvent(3, passenger2, 0.50, "drtFare", drt_A, "testFee"));
		events.processEvent(new PersonMoneyEvent(6, passenger1, 6.50, "drtFare", drt_B, "testFee"));
		events.processEvent(new PersonMoneyEvent(2, passenger2, 9.50, "drtFare", drt_B, "testFee"));
		events.processEvent(new PersonMoneyEvent(9, passenger2, -1.50, "drtFare", drt_C, "discount"));
		events.processEvent(new PersonMoneyEvent(3, passenger3, 6.50, "drtFare", drt_C, "testFee"));
		events.processEvent(new PersonMoneyEvent(1, passenger1, -5, "drtFare", drt_C, "discount"));
		events.processEvent(new PersonMoneyEvent(7, passenger2, 2.50, "drtFare", drt_C, "testFee"));
		events.finishProcessing();
		aggregator.writeOutput(utils.getOutputDirectory() + "/PersonMoneyEventsAggregator.csv");

		CSVFormat format = CSVFormat.newFormat(';').withFirstRecordAsHeader();

		try (FileReader aggregatorCsv = new FileReader(utils.getOutputDirectory() + "/PersonMoneyEventsAggregator.csv");
			 CSVParser parser = CSVParser.parse(aggregatorCsv, format)) {

			List<CSVRecord> csvRecordList = parser.getRecords();


			List<CSVRecord> csvRecordListA = csvRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("transactionPartner")).equals(drt_A) && record.get(parser.getHeaderMap().get("purpose")).equals("drtFare")).collect(Collectors.toList());
			Assertions.assertEquals(1, csvRecordListA.size(), "Either no record or more than one record");
			Assertions.assertEquals(19.07, Double.parseDouble(csvRecordListA.get(0).get(2)),MatsimTestUtils.EPSILON,"Wrong personMoneyAmountSum for drt_A");

			List<CSVRecord> csvRecordList0 = csvRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("transactionPartner")).equals("DrtA") && record.get(parser.getHeaderMap().get("purpose")).equals("DrtFare")).collect(Collectors.toList());
			Assertions.assertEquals(1, csvRecordList0.size(), "Either no record or more than one record");
			Assertions.assertEquals(4.1, Double.parseDouble(csvRecordList0.get(0).get(2)),MatsimTestUtils.EPSILON,"Wrong personMoneyAmountSum for empty purpose");

			List<CSVRecord> csvRecordListB = csvRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("transactionPartner")).equals(drt_B) && record.get(parser.getHeaderMap().get("purpose")).equals("drtFare")).collect(Collectors.toList());
			Assertions.assertEquals(1, csvRecordListB.size(), "Either no record or more than one record");
			Assertions.assertEquals(16, Double.parseDouble(csvRecordListB.get(0).get(2)),MatsimTestUtils.EPSILON,"Wrong personMoneyAmountSum for drt_B");

			List<CSVRecord> csvRecordListC = csvRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("transactionPartner")).equals(drt_C) && record.get(parser.getHeaderMap().get("purpose")).equals("drtFare")).collect(Collectors.toList());
			Assertions.assertEquals(1, csvRecordListC.size(), "Either no record or more than one record for drt_C");
			Assertions.assertEquals(2.5, Double.parseDouble(csvRecordListC.get(0).get(2)),MatsimTestUtils.EPSILON,"Wrong personMoneyAmountSum");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
