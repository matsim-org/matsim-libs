/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package ft.cemdap4H.cemdapPreProcessing;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class WVIZensusData {

	/**
	 * 
	 */
	public WVIZensusData(String zensusfile) {
		readZensusfile(zensusfile);
		fillWRS();
	}

	WeightedRandomSelection<String> m_0_5 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> m_6_9 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> m_10_17 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> m_18_44 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> m_45_64 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> m_65_74 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> m_75 = new WeightedRandomSelection<>();

	WeightedRandomSelection<String> f_0_5 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> f_6_9 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> f_10_17 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> f_18_44 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> f_45_64 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> f_65_74 = new WeightedRandomSelection<>();
	WeightedRandomSelection<String> f_75 = new WeightedRandomSelection<>();

	private List<WVIZensusDataElement> zdes = new ArrayList<>();

	public String getZoneForPerson(int gender, int age) {
		if (age <= 5) {
			if (gender == 1) {
				return f_0_5.select();
			} else
				return m_0_5.select();
		} else if (age <= 9) {
			if (gender == 1) {
				return f_6_9.select();
			} else
				return m_6_9.select();
		} else if (age <= 17) {
			if (gender == 1) {
				return f_10_17.select();
			} else
				return m_10_17.select();
		} else if (age <= 44) {
			if (gender == 1) {
				return f_18_44.select();
			} else
				return m_18_44.select();
		} else if (age <= 64) {
			if (gender == 1) {
				return f_45_64.select();
			} else
				return m_45_64.select();
		} else if (age <= 74) {
			if (gender == 1) {
				return f_65_74.select();
			} else
				return m_65_74.select();
		} else {
			if (gender == 1) {
				return f_75.select();
			} else
				return m_75.select();
		}

	}

	/**
	 * @param zensusfile
	 */
	private void readZensusfile(String zensusfile) {
		TabularFileParserConfig config = new TabularFileParserConfig();
		config.setFileName(zensusfile);
		config.setDelimiterRegex(";");
		TabularFileParser parser = new TabularFileParser();
		parser.parse(config, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {
				WVIZensusDataElement e = new WVIZensusDataElement();
				if (row.length!=23) throw new RuntimeException("invalid line:"+row);
				e.zone = row[0];
				e.m_0_5 = Integer.parseInt(row[2]);
				e.f_0_5 = Integer.parseInt(row[3]);
				
				e.m_6_9 = Integer.parseInt(row[5]);
				e.f_6_9 = Integer.parseInt(row[6]);
				
				e.m_10_17 = Integer.parseInt(row[8]);
				e.f_10_17 = Integer.parseInt(row[9]);
				
				e.m_18_44= Integer.parseInt(row[11]);
				e.f_18_44 = Integer.parseInt(row[12]);
				
				e.m_45_64 = Integer.parseInt(row[14]);
				e.f_45_64 = Integer.parseInt(row[15]);
				
				e.m_65_75 = Integer.parseInt(row[17]);
				e.f_65_75 = Integer.parseInt(row[18]);
				
				e.m_75 = Integer.parseInt(row[20]);
				e.f_75 = Integer.parseInt(row[21]);
				zdes.add(e);
			}
		});
		
	}

	private void fillWRS() {
		for (WVIZensusDataElement b : zdes) {
			m_0_5.add(b.zone, b.m_0_5);
			m_6_9.add(b.zone, b.m_6_9);
			m_10_17.add(b.zone, b.m_10_17);
			m_18_44.add(b.zone, b.m_18_44);
			m_45_64.add(b.zone, b.m_45_64);
			m_65_74.add(b.zone, b.m_65_75);
			m_75.add(b.zone, b.m_75);

			f_0_5.add(b.zone, b.f_0_5);
			f_6_9.add(b.zone, b.f_6_9);
			f_10_17.add(b.zone, b.f_10_17);
			f_18_44.add(b.zone, b.f_18_44);
			f_45_64.add(b.zone, b.f_45_64);
			f_65_74.add(b.zone, b.f_65_75);
			f_75.add(b.zone, b.f_75);

		}

	}

	class WVIZensusDataElement {

		String zone;
		int m_0_5;
		int m_6_9;
		int m_10_17;
		int m_18_44;
		int m_45_64;
		int m_65_75;
		int m_75;

		int f_0_5;
		int f_6_9;
		int f_10_17;
		int f_18_44;
		int f_45_64;
		int f_65_75;
		int f_75;

	}

}
