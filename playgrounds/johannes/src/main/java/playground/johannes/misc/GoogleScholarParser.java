/* *********************************************************************** *
 * project: org.matsim.*
 * GoogleScholarParser.java
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
package playground.johannes.misc;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.sna.math.DummyDiscretizer;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.sna.util.TXTWriter;

/**
 * @author illenberger
 * 
 */
public class GoogleScholarParser {

	/**
	 * @param args
	 * @throws IOException 
	 */
//	public static void main(String[] args) {
//		String input = "1 fish 2 fish red fish blue fish";
//		Scanner s = new Scanner(input);
//		s.findInLine("fish (\\w+) fish");
//		MatchResult result = s.match();
//		for (int i = 1; i <= result.groupCount(); i++)
//			System.out.println(result.group(i));
//		
//		s.findInLine("fish (\\w+) fish");
//		result = s.match();
//		for (int i = 1; i <= result.groupCount(); i++)
//			System.out.println(result.group(i));
//		s.close();
//
//	}
	
	public static void main(String[] args) throws IOException {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for(int i = 1; i < 4; i++) {
			List<String> years = getYears(String.format("%1$s%2$s.html", "/Users/jillenberger/Work/phd/literature/intro/googlesearch/leisure/page", i));
			for(String y : years) {
				stats.addValue(Double.parseDouble(y));
			}
		}
		
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new LinearDiscretizer(5), false);
		TXTWriter.writeMap(hist, "year", "n", "/Users/jillenberger/Work/phd/literature/intro/googlesearch/leisure/hist.txt");
	}
	
	private static List<String> getYears(String file) throws IOException {
		List<String> years = new ArrayList<String>(1000);
		
		String pattern = "<div class=gs_a>(.*?)</div>";
		String pattern2 = "\\d+";
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		while((line = reader.readLine()) != null) {
			Scanner s = new Scanner(line);
			String match = s.findInLine(pattern);
			while(match != null) {
			
				MatchResult result = s.match();
				for (int i = 1; i <= result.groupCount(); i++) {
					String str = result.group(i);
					
					Scanner s2 = new Scanner(str);
					String match2 = s2.findInLine(pattern2);
					while(match2 != null) {
						if(match2.length() == 4)
							years.add(match2);
						match2 = s2.findInLine(pattern2);
					}
				}
				
				match = s.findInLine(pattern);
			}
		}
		
		return years;
	}
}
