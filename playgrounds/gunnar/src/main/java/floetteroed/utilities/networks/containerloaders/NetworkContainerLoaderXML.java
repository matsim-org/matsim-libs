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
package floetteroed.utilities.networks.containerloaders;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import floetteroed.utilities.networks.construction.NetworkContainer;


/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class NetworkContainerLoaderXML extends DefaultHandler {

	// -------------------- MEMBERS --------------------

	protected NetworkContainer container = null;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * Empty default constructor.
	 */
	public NetworkContainerLoaderXML() {
		super();
	}

	// -------------------- IMPLEMENTATION --------------------

	public NetworkContainer load(final String path) {
		this.container = null;
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			final SAXParser parser = factory.newSAXParser();
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(this);
			// ignore the DTD declaration:
			reader.setFeature("http://apache.org/xml/features/"
					+ "nonvalidating/load-external-dtd", false);
			reader.setFeature("http://xml.org/sax/features/" + "validation",
					false);
			reader.parse(path);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this.container;
	}
}
