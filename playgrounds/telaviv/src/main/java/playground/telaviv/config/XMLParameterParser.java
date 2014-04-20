package playground.telaviv.config;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads an xml file and extracts parameter.
 * 
 * @author cdobler
 */
public class XMLParameterParser {
	
	private final static Logger log = Logger.getLogger(XMLParameterParser.class);
	
	public static final String PARAMETER = "parameter";
	public static final String VALUE = "value";

	/*
	 * Returns a map with the values read from the parsed files.
	 * 
	 * The case of the key strings are ignored, i.e. "key" equals "KEY"!
	 */
	public Map<String, String> parseFile(String file) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			Handler handler = new Handler();
			saxParser.parse(file, handler);
			return handler.map;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final class Handler extends DefaultHandler {
		
		private final SortedMap<String, String> map;
		
		public Handler() {
			this.map = new TreeMap<String, String>(new StringComparator());
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			
			if (PARAMETER.equalsIgnoreCase(qName)) return;
			
			String value = attributes.getValue(VALUE);
			if (map.containsKey(qName)) throw new RuntimeException("Duplicated key " + qName + " was found!");
			else log.info("Found value '" + value + "' for key '" + qName + "'.");
			map.put(qName, value);
		}
	}

	/*
	 * Ignores case when comparing string. I.e. "key" equals "KEY".
	 */
	private static class StringComparator implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			if (o1.equalsIgnoreCase(o2)) return 0;
			else return o1.compareTo(o2);
		}
	}
}