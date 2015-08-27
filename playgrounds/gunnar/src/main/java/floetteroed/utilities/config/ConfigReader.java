/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.config;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class ConfigReader extends DefaultHandler {

	// -------------------- CONSTANTS --------------------

	private static final String VALUE_ATTR = "value";

	// -------------------- MEMBERS --------------------

	private Config config = null;

	private LinkedList<String> currentKey = null;

	private String configPath = null;

	// -------------------- CONSTRUCTION --------------------

	public ConfigReader() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public Config read(final String fileName) {
		this.configPath = fileName;
		this.currentKey = new LinkedList<String>();
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			final SAXParser parser = factory.newSAXParser();
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(this);
			/*
			 * ignore the DTD declaration
			 */
			reader.setFeature("http://apache.org/xml/features/"
					+ "nonvalidating/load-external-dtd", false);
			reader.setFeature("http://xml.org/sax/features/" + "validation",
					false);
			reader.parse(fileName);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this.config;
	}

	// -------------------- OVERRIDING OF DefaultHandler --------------------

	@Override
	public void startElement(final String uri, final String lName,
			final String qName, final Attributes attrs) {
		if (this.config == null) {
			this.config = new Config(qName, this.configPath);
		}
		this.currentKey.add(qName);
		final String value = attrs.getValue(VALUE_ATTR);
		if (value != null) {
			this.config.add(new ArrayList<String>(this.currentKey), value);
		}
	}

	@Override
	public void endElement(final String uri, final String lName,
			final String qName) {
		this.currentKey.removeLast();
	}
}
