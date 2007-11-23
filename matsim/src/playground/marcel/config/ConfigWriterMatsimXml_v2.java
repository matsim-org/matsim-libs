/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigWriterMatsimXml_v2.java
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
import java.io.Writer;

import org.matsim.writer.MatsimXmlWriter;

public class ConfigWriterMatsimXml_v2 extends MatsimXmlWriter {

	private Config config = null;
	
	
	public ConfigWriterMatsimXml_v2(Config config) {
		this.config = config;
	}
	
	public void writeFile(String filename) {
		try {
			openFile(filename);
			writeXmlHead();
			write();
			close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void writeStream(Writer writer) {
		try {
			this.writer = writer;
			write();
			this.writer.flush();
			this.writer = null;
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void write() throws IOException {
		this.writer.write("<config>" + NL);
		for (String name : config.getGroupNames()) {
			ConfigGroupI group = config.getGroup(name);
			this.writer.write("\t<group name=\"" + group.getName() + "\">" + NL);
			writeGroupBody(group, "\t\t");
			this.writer.write("\t</group>" + NL + NL);
		}
		this.writer.write("</config>" + NL);
	}
	
	private void writeGroupBody(ConfigGroupI group, String indent) throws IOException {
		for (String param : group.paramKeySet()) {
			this.writer.write(indent + "<param name=\"" + param + "\" value=\"" + group.getValue(param) + "\" />" + NL);
		}
		for (String listname : group.listKeySet()) {
			this.writer.write(indent + "<list name=\"" + listname + "\">" + NL);
			writeListBody(group.getList(listname), indent + "\t");
			this.writer.write(indent + "</list>" + NL);
		}
	}
	
	private void writeListBody(ConfigListI list, String indent) throws IOException {
		for (String entryname : list.keySet()) {
			ConfigGroupI entry = list.getGroup(entryname);
			this.writer.write(indent + "<entry name=\"" + entryname + "\">" + NL);
			this.writeGroupBody(entry, indent + "\t");
			this.writer.write(indent + "</entry>" + NL);
		}
	}


}
