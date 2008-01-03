/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigTest.java
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

package playground.marcel.config;

import org.matsim.gbl.Gbl;
import org.matsim.utils.misc.Time;

import playground.marcel.config.groups.GenericConfigGroup;

public class ConfigTest {

	public void run(final String filename) {

		// this line is only needed that we can call Gbl.writeTime, which uses (the old) GBL.OUTPUT_TIME_FORMAT
		Gbl.createConfig(null);

		// out new config!
		Config config = new Config();
		// Gbl.setConfig(config); // later, we should set it like this to Gbl

		// write default settings
		ConfigWriterMatsimXml_v2 writer = new ConfigWriterMatsimXml_v2(config);
		writer.useCompression(false);
		writer.writeFile("test/marcel/defaultconfig.out.xml");
//		writer.writeStream(new PrintWriter(System.out));

		// prepare for reading custom group
		GenericConfigGroup myGroup = new GenericConfigGroup("genericgroup");
		config.addGroup("genericgroup", myGroup);

		// read config
		try {
			ConfigReaderMatsimXml_v2 configreader = new ConfigReaderMatsimXml_v2(config);
//			configreader.setValidating(false);
			configreader.readFile(filename);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// write the current config settings
		writer.writeFile("test/marcel/testconfig.out.xml");
//		writer.writeStream(new PrintWriter(System.out));


		// make some simple tests
		String inNet1 = config.network().getInputFile();
		String inNet2 = config.getGroup("network").getValue("inputFile");
		if (!inNet1.equals(inNet2)) {
			System.err.println("got different values for inputFile");
		}

		// how to access complex config settings with specialized groups, e.g. scoring-settings
		System.out.println(config.scoring().getBrainExpBeta());
		System.out.println(config.scoring().getActivity("work").getClosingTime());
		System.out.println(Time.writeTime(config.scoring().getActivity("work").getClosingTime()));
		System.out.println(config.scoring().getActivity("inexistant") == null ? "activity 'inexistant' does not exist, correct!" : "error, activity 'inexistant' should not exist, but it seems it does.");


		// how to access complex config settings with the generic group
		System.out.println(config.getGroup("genericgroup").getValue("one"));

		System.out.println(myGroup.getValue("two"));

		System.out.println(myGroup.getList("three").getGroup("two").getValue("two"));

		ConfigListI list = myGroup.getList("four");
		System.out.println(list == null ? "list four does not exist" : "error, list four should not exist but does!");

		ConfigGroupI entry = myGroup.getList("three").getGroup("four");
		System.out.println(entry == null ? "entry three.four does not exist" : "error, entry three.four should not exist but does!");

		String value = myGroup.getList("three").getGroup("three").getList("two").getGroup("one").getValue("one");
		System.out.println(value == null ? "error, value 3.3.2.1.1 does not exist" : "value 3.3.2.1.1 exists!");

	}

	public static void main(final String[] args) {
		if (args.length != 1) {
			System.err.println("usage: ConfigTest config.xml");
			System.exit(1);
		}

		ConfigTest test = new ConfigTest();
		test.run(args[0]);
	}

}
