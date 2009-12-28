/* *********************************************************************** *
 * project: org.matsim.*
 * OSMPatchPaser.java
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author yu
 * 
 */
public class OSMPatchPaser extends MatsimXmlParser {
	// private static enum status {
	// upgrade, degrade
	// }

	private Set<String> upgradeLinks, degradeLinks, shell;
	// private status patch_status;
	private final static String
	// OSM = "OSM",
			UPGRADE = "upgrade",
			DEGRADE = "degrade", LINK = "link";

	public OSMPatchPaser() {
		super(false);
		upgradeLinks = new HashSet<String>();
		degradeLinks = new HashSet<String>();
		setValidating(false);
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		// TODO Auto-generated method stub

	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (LINK.equals(name)) {
			startLink(atts);
		} else if (UPGRADE.equals(name)) {
			shell = upgradeLinks;
		} else if (DEGRADE.equals(name)) {
			shell = degradeLinks;
		}
	}

	private void startLink(final Attributes meta) {
		shell.add(meta.getValue("id"));
	}

	public void readFile(final String filename) {
		try {
			parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the upgradeLinks
	 */
	public Set<String> getUpgradeLinks() {
		return upgradeLinks;
	}

	/**
	 * @return the degradeLinks
	 */
	public Set<String> getDegradeLinks() {
		return degradeLinks;
	}

	public static void main(String[] args) {
		OSMPatchPaser osmP = new OSMPatchPaser();
		osmP.readFile("test/yu/utils/osmpatch.xml");
		Set<String> links = osmP.getUpgradeLinks();
		int i = 0;
		System.out.println(i + "\tupgrade");
		for (String linkId : links) {
			i++;
			System.out.println(i + "\t" + linkId);
		}
		links = osmP.getDegradeLinks();
		i++;
		System.out.println(i + "\tdegrade");
		for (String linkId : links) {
			i++;
			System.out.println(i + "\t" + linkId);
		}
		System.out.println(i++ + "\tdone!");
	}
}
