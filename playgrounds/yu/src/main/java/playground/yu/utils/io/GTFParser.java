/* *********************************************************************** *
 * project: org.matsim.*
 * GTFParser.java
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

/**
 * 
 */
package playground.yu.utils.io;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

/**
 * @author yu
 * 
 */
public class GTFParser extends MatsimXmlParser {
	private final static String GREENTIMEFRACTIONS = "greentimefractions",
			LINKGTFS = "linkgtfs", GTF = "gtf";
	/**
	 * @param arg0
	 *            - String linkId;
	 * @param arg1
	 *            - Double time [s];
	 * @param arg2
	 *            - Double greentimefraction or the grenntime factor for
	 *            capacity of a link.
	 */
	private Map<String, HashMap<Double, Double>> linkgtfsMap;
	private String crtLinkId;
	private HashMap<Double, Double> crtLinkGTFs;

	/**
	 * 
	 */
	public GTFParser(final Map<String, HashMap<Double, Double>> gtfsMap) {
		linkgtfsMap = gtfsMap;
		setValidating(false);
	}

	@Override
	public void endTag(final String name, final String content,
			final Stack<String> context) {
		if (LINKGTFS.equals(name))
			endLinkGTFs();
	}

	@Override
	public void startTag(final String name, final Attributes atts,
			final Stack<String> context) {
		if (GREENTIMEFRACTIONS.equals(name))
			startGreentimefractions(atts);
		else if (LINKGTFS.equals(name))
			startLinkGTFs(atts);
		else if (GTF.equals(name))
			startGTF(atts);
	}

	private void endLinkGTFs() {
		linkgtfsMap.put(crtLinkId, crtLinkGTFs);
	}

	private void startGreentimefractions(final Attributes meta) {
		linkgtfsMap = new TreeMap<String, HashMap<Double, Double>>();
	}

	private void startLinkGTFs(final Attributes meta) {
		crtLinkId = meta.getValue("id");
		HashMap<Double, Double> gtfs = linkgtfsMap.get(crtLinkId);
		crtLinkGTFs = gtfs == null ? new HashMap<Double, Double>() : gtfs;
	}

	private void startGTF(final Attributes meta) {
		double gtfs = Double.parseDouble(meta.getValue("val"));
		if (gtfs < 1.0)
			crtLinkGTFs.put(Time.parseTime(meta.getValue("time")), gtfs);
	}

	/**
	 * @param filename
	 *            -The name of the gtfs(greentimefractions)-file to parse.
	 */
	public void readFile(final String filename) {
		parse(filename);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		GTFParser g = new GTFParser(
				new TreeMap<String, HashMap<Double, Double>>());
		g.readFile("../schweiz-ivtch/greentimes/ivtch.xml");
		for (String linkId : g.getLinkgtfsMap().keySet())
			System.out.println("link " + linkId + "\t" + g.getAvgGtfs(linkId));
		System.out.println("done!");
	}

	public double getAvgGtfs(final String linkId) {
		double avgGtfs = -1;
		if (linkgtfsMap != null) {
			HashMap<Double, Double> linkGtfs = linkgtfsMap.get(linkId);
			if (linkGtfs != null) {
				double d = 0;
				for (Iterator<Double> i = linkGtfs.values().iterator(); i
						.hasNext();)
					d += i.next();
				avgGtfs = d / linkGtfs.size();
			}
		}
		return avgGtfs;
	}

	public double getGtfs(final double time, final String linkId) {
		double gtfs = -1;
		if (linkgtfsMap != null) {
			HashMap<Double, Double> linkGtfs = linkgtfsMap.get(linkId);
			if (linkGtfs != null)
				gtfs = linkGtfs.get((((int) time) / 3600) * 3600.0);
		}
		return gtfs;
	}

	/**
	 * @return the linkgtfsMap
	 */
	public Map<String, HashMap<Double, Double>> getLinkgtfsMap() {
		return linkgtfsMap;
	}
}
