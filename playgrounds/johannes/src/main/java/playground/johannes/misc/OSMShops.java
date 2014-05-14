package playground.johannes.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class OSMShops {

	public static void main(String[] args) {
		

	}

	
	private static class Parser extends MatsimXmlParser {

		private List<Point> shops = new ArrayList<Point>(2000);
		
		private GeometryFactory factory = new GeometryFactory();
		
		private boolean wayActive = false;
		
		
		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			if("way".equalsIgnoreCase(name)) {
				wayActive = true;
			} else if(wayActive) {
				
			}
			if("tag".equalsIgnoreCase(name)) {
				String key = atts.getValue("k");
				if("shop".equalsIgnoreCase(key)) {
					
				}
			}
			
		}

		@Override
		public void endTag(String name, String content, Stack<String> context) {
			if("way".equalsIgnoreCase(name)) {
				wayActive = false;
			}
			
		}
		
	}
}
