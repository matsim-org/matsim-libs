/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesReaderConvertOTs.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.anhorni.rc.signals;

import java.util.Stack;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.Attributes;


public class GTFSReader extends MatsimXmlParser {

	private final static String LINKGTFS = "linkgtfs";
	private final static String GTF = "gtf";

	private TreeMap<Id<Link>, LinkGTF> greentimefractions = new TreeMap<Id<Link>, LinkGTF>();
	private LinkGTF currlinkgtf = null;
	
	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (LINKGTFS.equals(name)) {
			startLinkGTFS(atts);
		} else if (GTF.equals(name)) {
			startGTF(atts);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (LINKGTFS.equals(name)) {
			greentimefractions.put(this.currlinkgtf.getLinkId(), this.currlinkgtf);
		} 
	}

	
	private void startLinkGTFS(final Attributes atts) {
		this.currlinkgtf = new LinkGTF(Id.create(atts.getValue("id"), Link.class));
	}
	
	private void startGTF(final Attributes atts) {
		String timeStr = atts.getValue("time");
		String parts[] = timeStr.split(":");
		int hour = Integer.parseInt(parts[0]);
		double gtf = Double.parseDouble(atts.getValue("val"));
		this.currlinkgtf.addGTF(hour, gtf);
	}
	

	/**
	 * Parses the specified facilities file. This method calls {@link #parse(String)}.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}

	public TreeMap<Id<Link>, LinkGTF> getGreentimefractions() {
		return greentimefractions;
	}
}
