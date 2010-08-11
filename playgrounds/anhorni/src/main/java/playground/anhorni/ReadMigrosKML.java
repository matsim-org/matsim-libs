package playground.anhorni;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 

public class ReadMigrosKML {

	public static void main (String argv []){
		ReadMigrosKML reader = new ReadMigrosKML();	
		reader.read();
	}
	
	public void read() {
		
		//TODO: Namen prüfen in KML
		String [] kreise = {"Zürich","Altstetten", "Albisrieden", "Friesenberg", "Leimbach", "Wollishofen", "Witikon", "Hirzenbach", "Höngg", 
				"Affoltern", "Seebach", "Oerlikon", "Saatlen", "Schwamendingen"};
		
		String outfile = "../../matsim/output/Migros.txt";	
		
	    try {
	    	
		    	final BufferedWriter out = IOUtils.getBufferedWriter(outfile);		

	            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	            Document doc = docBuilder.parse (new File("../../matsim/input/shops/Migros_ch.xml"));

	            // normalize text representation
	            doc.getDocumentElement ().normalize ();

	            NodeList listOfPlacemarks = doc.getElementsByTagName("Placemark");
	            int totalPlacemarks = listOfPlacemarks.getLength();
	            System.out.println("Total no of Placemarks : " + totalPlacemarks);
	            
	            int id = 30000;

	            for(int s=0; s<listOfPlacemarks.getLength() ; s++){
	            	
	            	
	            	
	                Node placemarkNode = listOfPlacemarks.item(s);
	                if(placemarkNode.getNodeType() == Node.ELEMENT_NODE){
	                	
	                	String outLine = "<shop";
	                	
	                    Element placemarkElement = (Element)placemarkNode;

	                    NodeList nameList = placemarkElement.getElementsByTagName("name");
	                    Element nameElement = (Element)nameList.item(0);

	                    NodeList txtName = nameElement.getChildNodes();
	                    String[] nameEntries = ((Node)txtName.item(0)).getNodeValue().trim().split(",", -1);
	                                        
	                    String name = nameEntries[0];
	                    String loc = nameEntries[1];
	                    
	                    String[] nameSubEntries = name.split(" ", -1);
	                    String[] locSubEntries = loc.split(" ", -1);                    
	                         
	                    System.out.print("name : " + ((Node)txtName.item(0)).getNodeValue().trim());
	                    
	                    outLine += " id=" + "\"" + id +"\"";
	                    outLine += " name=" + "\"" + nameEntries[0] + "\"";
	                    outLine += " street=" +  "\"" + nameSubEntries[1];
	                    
	                    if (nameSubEntries.length == 3) {
	                    	outLine += " " + nameSubEntries[2] + "\"";
	                    }
	                    else {
	                    	outLine +=  "\"";	
	                    }
	                    
	                    outLine += " PLZ=" + "\"" + locSubEntries[1] + "\"";
	                    outLine += " city=" + "\"" + locSubEntries[2] + "\"";
	                    
	                    if (!Arrays.asList(kreise).contains(locSubEntries[2])) {
	                    	
	                    	System.out.print(locSubEntries[2]);
	                    	
	                    	continue;	                    	
	                    }
	                        

	                    NodeList coordinatesList = placemarkElement.getElementsByTagName("coordinates");
	                    Element coordinatesElement = (Element)coordinatesList.item(0);

	                    NodeList txtCoordinates = coordinatesElement.getChildNodes();
	                    System.out.println(" coordinates : " + ((Node)txtCoordinates.item(0)).getNodeValue().trim());
	                    
	                    String[] coordinateEntries = ((Node)txtCoordinates.item(0)).getNodeValue().trim().split(",", -1);
	                    String lat = coordinateEntries[1];
	                    String lng = coordinateEntries[0];
	                    
	                    outLine += " lat=" + "\"" + lat +"\"";
	                    outLine += " lan=" + "\"" + lng +"\"";
	                    
	                    outLine += " yaw=" + "\"" + "\"";
	                    outLine += " pitch=" + "\"" + "\"";
	                    
	                    outLine += "/>";
	                    out.write(outLine);	                    
	                    
	                }//end of if clause
	                
	                
	                out.newLine();
	                id++;
	                
	            }//end of for loop with s var
	            
	            out.flush();			
				out.flush();
				out.close();

	        }catch (SAXParseException err) {
		        System.out.println ("** Parsing error" + ", line " 
		             + err.getLineNumber () + ", uri " + err.getSystemId ());
		        System.out.println(" " + err.getMessage ());
	        }catch (SAXException e) {
		        Exception x = e.getException ();
		        ((x == null) ? e : x).printStackTrace ();
	        } catch (final IOException e) {
				Gbl.errorMsg(e);
	        }catch (Throwable t) {
		      	t.printStackTrace ();
		      }
	        //System.exit (0);
	    }
	}
