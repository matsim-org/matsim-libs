/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigReaderMatsimXml_v2.java
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

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.gbl.Gbl;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ConfigReaderMatsimXml_v2 extends MatsimXmlParser {

	public static final String XML_ROOT = "config";
	public static final String XML_GROUP = "group";
	public static final String XML_PARAM = "param";
	public static final String XML_LIST = "list";

	public static final String XML_NAME = "name";
	public static final String XML_VALUE = "value";
	
	private Config config = null;
	private Stack<ConfigGroupI> groups = null;
	private Stack<ConfigListI> lists = null;
	
	public ConfigReaderMatsimXml_v2(Config config) {
		super();
		this.config = config;
		this.groups = new Stack<ConfigGroupI>();
		this.lists = new Stack<ConfigListI>();
	}
	
	public void readFile(String filename) throws SAXException, ParserConfigurationException, IOException {
		this.parse(filename);
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (XML_GROUP.equals(name)) {
			ConfigGroupI group = null; 
			if (this.lists.empty()) {
				group = config.getGroup(atts.getValue(XML_NAME));
			} else {
				ConfigListI list = this.lists.peek();
				String att = atts.getValue(XML_NAME);
				group = list.addGroup(att);
			}
			if (group == null) {
				throw new IllegalArgumentException("group " + atts.getValue(XML_NAME) + " is unknown!");
			}
			this.groups.push(group);
		} else if (XML_PARAM.equals(name)) {
			this.groups.peek().setValue(atts.getValue(XML_NAME), atts.getValue(XML_VALUE));
		} else if (XML_LIST.equals(name)) {
			ConfigListI list = this.groups.peek().getList(atts.getValue(XML_NAME));
			this.lists.push(list);
		} else if (XML_ROOT.equals(name)) {
		} else {
			Gbl.errorMsg(this + "[tag=" + name + " not known]");
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (XML_GROUP.equals(name)) {
			this.groups.pop();
		} else if (XML_LIST.equals(name)) {
			this.lists.pop();
		}
	}

}
