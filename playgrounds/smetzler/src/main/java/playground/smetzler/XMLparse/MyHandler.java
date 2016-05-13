package playground.smetzler.XMLparse;

//example from: http://www.journaldev.com/1198/java-sax-parser-example-tutorial-to-parse-xml-to-list-of-objects
	
	import java.util.ArrayList;
	import java.util.List;
	 
	import org.xml.sax.Attributes;
	import org.xml.sax.SAXException;
	import org.xml.sax.helpers.DefaultHandler;
	 
//	import com.journaldev.xml.Employee;
	 
	 
	public class MyHandler extends DefaultHandler {
	 
	    //List to hold way object
	    private List<Cycleways> cycList = null;
	    private Cycleways cyc = null;
	 
	 
	    //getter method for way list
	    public List<Cycleways> getCycList() {
	        return cycList;
	    }
	 
	 
	    boolean bCycleway= false;
	    boolean bCyclewaySurface = false;
	 
	 
	    @Override
	    public void startElement(String uri, String localName, String qName, Attributes attributes)
	            throws SAXException {
	 
	        if (qName.equalsIgnoreCase("way")) {
	            //create a new way and put it in Map
	            String id = attributes.getValue("id");
	            //initialize way object and set id attribute
	            cyc = new Cycleways();
	            cyc.setId(Integer.parseInt(id));
	            
	            System.out.println(attributes);
	            System.out.println(localName);
	           
	            //initialize list
	            if (cycList == null)
	            	cycList = new ArrayList<>();
	        } else if (qName.equalsIgnoreCase("cycleway")) {
	            //set boolean values for fields, will be used in setting way variables
	            bCycleway = true;
		        } else if (qName.equalsIgnoreCase("surface")) {
	        	bCyclewaySurface = true;
	        }
	    }
	 
	 
	    @Override
	    public void endElement(String uri, String localName, String qName) throws SAXException {
	        if (qName.equalsIgnoreCase("way")) {
	            //add way object to list
	            cycList.add(cyc);
	        }
	    }
	 
	 
	    @Override
	    public void characters(char ch[], int start, int length) throws SAXException {
	 
	        if (bCycleway) {
	            //age element, set Employee age
	            cyc.setCyclewaytype(new String(ch, start, length));
	            System.out.println("HEE");
	            bCycleway = false;
	        } else if (bCyclewaySurface) {
	            cyc.setCyclewaySurface(new String(ch, start, length));
	            bCyclewaySurface = false;
	        } 
	    }
	}
	

