package playground.johannes.misc;

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.johannes.sna.util.TXTWriter;

public class OSMSummary {

	public static void main(String[] args) throws IOException {
		OSMParser parser = new OSMParser();
		parser.setValidating(false);
		parser.parse("/home/johannes/gsv/osm/germany-latest.osm");
		
		TXTWriter.writeMap(parser.keys, "keys", "counts", "/home/johannes/gsv/osm/germany-latest-keys.txt");
		
		for(Entry<String, TObjectDoubleHashMap<String>> entry : parser.attributes.entrySet()) {
			String key = entry.getKey();
			TObjectDoubleHashMap<String> values = entry.getValue();
			
			TXTWriter.writeMap(values, "keys", "counts", "/home/johannes/gsv/osm/" + key +".txt");
		}

	}

	private static class OSMParser extends MatsimXmlParser {

		private Map<String, TObjectDoubleHashMap<String>> attributes = new HashMap<String, TObjectDoubleHashMap<String>>();
		
		private TObjectDoubleHashMap<String> keys = new TObjectDoubleHashMap<String>();
		
		private boolean wayActive = false;
		
		private int tags = 0;
		
		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			if("way".equalsIgnoreCase(name)) {
				tags++;
				wayActive = true;
			} else {
				if(wayActive)  {
					if("tag".equalsIgnoreCase(name)) {
						String key = atts.getValue("k");
						
						keys.adjustOrPutValue(key, 1, 1);
						
						if(key.equalsIgnoreCase("building")) addValueForKey("building", atts);
						if(key.equalsIgnoreCase("landuse")) addValueForKey("landuse", atts);
						if(key.equalsIgnoreCase("natural")) addValueForKey("natural", atts);
						if(key.equalsIgnoreCase("amenity")) addValueForKey("amenity", atts);
						if(key.equalsIgnoreCase("leisure")) addValueForKey("leisure", atts);
						if(key.equalsIgnoreCase("sport")) addValueForKey("sport", atts);
						if(key.equalsIgnoreCase("shop")) addValueForKey("shop", atts);
						if(key.equalsIgnoreCase("tourism")) addValueForKey("tourism", atts);
						
					}
				}
			}
			
		}
		
		private void addValueForKey(String key, Attributes atts) {
			TObjectDoubleHashMap<String> values = attributes.get(key);
			if (values == null) {
				values = new TObjectDoubleHashMap<String>();
				attributes.put(key, values);
			}
			values.adjustOrPutValue(atts.getValue("v"), 1, 1);
		}

		@Override
		public void endTag(String name, String content, Stack<String> context) {
			if("way".equalsIgnoreCase(name)) {
				wayActive = false;
			}
			
		}
		
	}
}
