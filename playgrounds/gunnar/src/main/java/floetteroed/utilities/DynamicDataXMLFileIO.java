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

import static floetteroed.utilities.XMLHelpers.writeAttr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <K>
 *            the key type
 * 
 */
public abstract class DynamicDataXMLFileIO<K> extends DefaultHandler implements
		Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	public static final String OUTER_ELEMENT = "dynamicdata";

	public static final String SUBCLASS_ATTR = "subclass";

	public static final String STARTTIME_ATTR = "starttime";

	public static final String BINSIZE_ATTR = "binsize";

	public static final String BINCOUNT_ATTR = "bincount";

	public static final String ENTRY_ELEMENT = "entry";

	public static final String KEY_ATTR = "key";

	public static final String VALUE_ATTR = "value";

	// -------------------- MEMBER VARIABLES --------------------

	private DynamicData<K> result = null;

	// -------------------- CONSTRUCTION --------------------

	protected DynamicDataXMLFileIO() {
	}

	// -------------------- INTERFACE DEFINTION --------------------

	protected DynamicData<K> newInstance(final int startTime_s,
			final int binSize_s, final int binCnt) {
		return new DynamicData<K>(startTime_s, binSize_s, binCnt);
	}

	protected abstract String key2attrValue(final K key);

	protected abstract K attrValue2key(final String string);

	// -------------------- WRITE IMPLEMENTATION --------------------

	private String values2string(final double[] entry) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < entry.length; i++) {
			result.append(entry[i] + " ");
		}
		return result.toString();
	}

	public void write(final String filename, final DynamicData<K> dd)
			throws IOException {

		final PrintWriter writer = new PrintWriter(new File(filename));

		writer.print("<");
		writer.print(OUTER_ELEMENT);
		writer.print(" ");
		writeAttr(STARTTIME_ATTR, dd.getStartTime_s(), writer);
		writeAttr(BINSIZE_ATTR, dd.getBinSize_s(), writer);
		writeAttr(BINCOUNT_ATTR, dd.getBinCnt(), writer);
		writeAttr(SUBCLASS_ATTR, this.getClass().getName(), writer);
		writer.println(">");

		for (Map.Entry<K, double[]> entry : dd.data.entrySet()) {
			if (entry.getValue() != null) {
				writer.print("  <");
				writer.print(ENTRY_ELEMENT);
				writer.print(" ");
				writeAttr(KEY_ATTR, key2attrValue(entry.getKey()), writer);
				writeAttr(VALUE_ATTR, values2string(entry.getValue()), writer);
				writer.println("/>");
			}
		}

		writer.print("</");
		writer.print(OUTER_ELEMENT);
		writer.println(">");

		writer.flush();
		writer.close();
	}

	// -------------------- READ IMPLEMENTATION --------------------

	private double[] string2values(final String value) {
		if (value == null) {
			return null;
		}
		final double[] entryArray = new double[this.result.getBinCnt()];
		final String[] entryStrings = value.split("\\s");
		if (entryArray.length != entryStrings.length) {
			Logger.getLogger(this.getClass().getName()).warning(
					"inconsistent data dimensions," + " Skipping this entry: "
							+ value);
			return null;
		}
		for (int i = 0; i < entryArray.length; i++) {
			entryArray[i] = Double.parseDouble(entryStrings[i]);
		}
		return entryArray;
	}

	public DynamicData<K> read(final String file) {
		result = null;
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			parser.parse(file, this);
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).warning(
					"exception during file parsing: " + e.toString());
			result = null;
		}
		return result;
	}

	@Override
	public void startElement(final String uri, final String localName,
			final String qName, final Attributes attributes) {
		if (OUTER_ELEMENT.equals(qName)) {
			startOuterElement(attributes);
		} else if (ENTRY_ELEMENT.equals(qName)) {
			startEntryElement(attributes);
		}
	}

	private void startOuterElement(final Attributes attrs) {
		// final String subclass = attrs.getValue(SUBCLASS_ATTR);
		// if (!this.getClass().getName().equals(subclass)) {
		// Logger.getLogger(this.getClass().getName()).warning(
		// "file has been written by " + subclass);
		// }
		final int startTime_s = Integer
				.parseInt(attrs.getValue(STARTTIME_ATTR));
		final int binSize_s = Integer.parseInt(attrs.getValue(BINSIZE_ATTR));
		final int binCnt = Integer.parseInt(attrs.getValue(BINCOUNT_ATTR));
		this.result = newInstance(startTime_s, binSize_s, binCnt);
	}

	private void startEntryElement(Attributes attrs) {
		final K key = attrValue2key(attrs.getValue(KEY_ATTR));
		final double[] values = string2values(attrs.getValue(VALUE_ATTR));
		this.result.data.put(key, values);
	}
}
