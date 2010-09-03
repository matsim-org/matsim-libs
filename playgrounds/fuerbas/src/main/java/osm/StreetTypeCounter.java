package osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;



public class StreetTypeCounter {
	
	private final Map<String, OsmWay> ways = new HashMap<String, OsmWay>();

	private String mot = new String("motorway");
	private String mot_link = new String("motorway_link");
	private String trunk = new String("trunk");
	private String trunk_link = new String("trunk_link");
	private String prim = new String("primary");
	private String prim_link = new String("primary_link");
	private String sec = new String("secondary");
	private String ter = new String("tertiary");
	private String min = new String("minor");
	private String unclass = new String("unclassified");
	private String liv = new String("living_street");
	private String res = new String ("residential");

	private int mot_count = 0;
	private int mot_l_count = 0;
	private int trunk_count = 0;
	private int trunk_l_count = 0;
	private int prim_count = 0;
	private int prim_l_count = 0;
	private int sec_count = 0;
	private int ter_count = 0;
	private int min_count = 0;
	private int unclass_count = 0;
	private int liv_count = 0;
	private int res_count = 0;
	
	
//	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
//		StreetTypeCounter counter = new StreetTypeCounter();
//		counter.parse(args[0]);
//	}
		
	
	public void countStreetType(){
		for (OsmWay way : this.ways.values()) {
			String streetType = way.tags.get("highway");
			if (way.tags.get("highway")!=null) {
				if (streetType.equals(liv)) liv_count++;
				else if (streetType.equals(res)) res_count++;
				else if (streetType.equals(mot)) mot_count++;
				else if (streetType.equals(mot_link)) mot_l_count++;
				else if (streetType.equals(trunk)) trunk_count++;
				else if (streetType.equals(trunk_link)) trunk_l_count++;
				else if (streetType.equals(prim)) prim_count++;
				else if (streetType.equals(prim_link)) prim_l_count++;
				else if (streetType.equals(sec)) sec_count++;
				else if (streetType.equals(ter)) ter_count++;
				else if (streetType.equals(min)) min_count++;
				else if (streetType.equals(unclass)) unclass_count++;
				else ;
				}	else ;			
			} 
	}
	
	
	public void parse(final String osmFilename) throws SAXException, ParserConfigurationException, IOException {
		OsmXmlParser parser = new OsmXmlParser(this.ways);
		parser.parse(osmFilename);
		countStreetType();
		System.out.println("Anzahl der Motorways: "+mot_count);
		System.out.println("Anzahl der Motorway Links: "+mot_l_count);
		System.out.println("Anzahl der Trunks: "+trunk_count);
		System.out.println("Anzahl der Trunks Links: "+trunk_l_count);
		System.out.println("Anzahl der Primary: "+prim_count);
		System.out.println("Anzahl der Primary Links: "+prim_l_count);
		System.out.println("Anzahl der Secondary: "+sec_count);
		System.out.println("Anzahl der Tertiary: "+ter_count);
		System.out.println("Anzahl der Minor: "+min_count);
		System.out.println("Anzahl der Unclassified: "+unclass_count);
		System.out.println("Anzahl der Living Streets: "+liv_count);
		System.out.println("Anzahl der Residential Streets: "+res_count);
	}
	
	
	
	
	private static class OsmWay {
		public final long id;
		public final List<String> nodes = new ArrayList<String>();
		public final Map<String, String> tags = new HashMap<String, String>();
		public int hierarchy;

		public OsmWay(final long id) {
			this.id = id;
		}
	}

	private static class OsmXmlParser extends MatsimXmlParser {

		private OsmWay currentWay = null;
		private final Map<String, OsmWay> ways;

		public OsmXmlParser(final Map<String, OsmWay> ways) {
			super();
			this.ways = ways;
			this.setValidating(false);
		}

		
		@Override
		public void endTag(String name, String content, Stack<String> context) {
			if ("way".equals(name)) {
				this.currentWay = null;
			}		
		}

		
		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {		
			if ("way".equals(name)) {
				this.currentWay = new OsmWay(Long.parseLong(atts.getValue("id")));
				this.ways.put(atts.getValue("id"), this.currentWay);
			} else if ("nd".equals(name)) {
				if (this.currentWay != null) {
					this.currentWay.nodes.add(atts.getValue("ref"));
				}
			} else if ("tag".equals(name)) {
				if (this.currentWay != null) {
					this.currentWay.tags.put(atts.getValue("k"), atts.getValue("v"));
				}
			}
		}

	}
}
