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
package floetteroed.utilities;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;


/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class IdentifiedElementParser extends DefaultHandler {

	// -------------------- CONSTANTS --------------------

	private final String element;

	private final String idAttribute;

	// -------------------- MEMBERS --------------------

	private Map<String, Attributes> id2attrs = null;

	// -------------------- CONSTRUCTION --------------------

	public IdentifiedElementParser(final String element,
			final String idAttribute) {
		if (element == null || "".equals(element)) {
			throw new IllegalArgumentException("element is " + idAttribute
					+ ".");
		}
		this.element = element;
		this.idAttribute = idAttribute;
	}

	// -------------------- IMPLEMENTATION --------------------

	public Map<String, Attributes> readId2AttrsMap(final String fileName) {
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			final SAXParser parser = factory.newSAXParser();
			final XMLReader reader = parser.getXMLReader();
			reader.setFeature("http://apache.org/xml/features/"
					+ "nonvalidating/load-external-dtd", false);
			reader.setFeature("http://xml.org/sax/features/" + "validation",
					false);
			reader.setContentHandler(this);
			reader.parse(fileName);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this.id2attrs;
	}

	// --------------- IMPLEMENTATION OF DefaultHandler ---------------

	@Override
	public void startDocument() {
		this.id2attrs = new HashMap<String, Attributes>();
	}

	@Override
	public void startElement(final String namespaceURI, final String sName,
			final String qName, final Attributes attrs) {
		if (this.element.equals(qName)) {
			if (this.idAttribute == null) {
				this.id2attrs.put(Integer.toString(this.id2attrs.size()),
						new AttributesImpl(attrs));
			} else {
				final String id = attrs.getValue(this.idAttribute);
				final Attributes attrsCopy = new AttributesImpl(attrs);
				this.id2attrs.put(id, attrsCopy);
				// this.id2attrs.put(attrs.getValue(this.idAttribute),
				// new AttributesImpl(attrs));
			}
		}
	}
}
