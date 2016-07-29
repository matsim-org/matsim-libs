package playground.smetzler.XMLparse;

		 
	import java.io.File;
	import java.io.IOException;
	import java.util.List;
	 
	import javax.xml.parsers.ParserConfigurationException;
	import javax.xml.parsers.SAXParser;
	import javax.xml.parsers.SAXParserFactory;
	 
	import org.xml.sax.SAXException;
	 
//	import com.journaldev.xml.Employee;
	 
	public class XMLParserSAX {
	 
	    public static void main(String[] args) {
	    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
	    try {
	        SAXParser saxParser = saxParserFactory.newSAXParser();
	        MyHandler handler = new MyHandler();
	        saxParser.parse(new File("C:/Users/Ettan/13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/schlesi/map_schlesi.osm"), handler);
	        //Get Employees list
	        List<Cycleways> cycList = handler.getCycList();
	        //print employee information
	        for(Cycleways way : cycList)
	            System.out.println(way);
	    } catch (ParserConfigurationException | SAXException | IOException e) {
	        e.printStackTrace();
	    }
	    }
	 
	}
	

