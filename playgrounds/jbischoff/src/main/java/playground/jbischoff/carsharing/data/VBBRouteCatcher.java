/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.jbischoff.carsharing.data;



import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author jbischoff
 *
 */
public class VBBRouteCatcher {
			
			private static SimpleDateFormat VBBDATE = new SimpleDateFormat("HH:mm:ss");
			private static SimpleDateFormat VBBDAY = new SimpleDateFormat("yyyyMMdd");
			private static SimpleDateFormat VBBTIME = new SimpleDateFormat("HH:mm");
			
			private int bestRideTime = Integer.MAX_VALUE;
			private int bestTransfers = Integer.MAX_VALUE;
			
			private boolean writeOutput = true;
			private String filename;
			
			public VBBRouteCatcher(Coord from, Coord to, long departureTime)  {
			this.writeOutput = false;
			initiate();
			
			}
			private void initiate() {
				//httpclient is extremely noisy by default
				java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
				java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
				System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
				System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
				System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
				System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
				System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");				
			}
			
			
			
			public VBBRouteCatcher(Coord from, Coord to , long departureTime,
					String fileName) {
				this.writeOutput = true;
				this.filename = fileName;
				initiate();
				run (from,to,departureTime);
			}
			public static void main(String[] args) throws IOException, Exception {
//				Hansaplatz-Friedrich-Wilhelm-Platz
//				Coord c = new CoordImpl(52.519580833333,13.359681944444);
//				Coord d = new CoordImpl(52.470137777778,13.335396944444);
				
				//Salzufer-Hoenow
				Coord c = new CoordImpl(52.5188,13.32183);
				Coord d = new CoordImpl(52.5307913,13.6303349);
				VBBRouteCatcher rc = new VBBRouteCatcher(c,d,System.currentTimeMillis(),"vbbTest.xml.gz");
			}
			private void run(Coord from, Coord to, long departureTime)  {
		          
		    		Locale locale  = new Locale("en", "UK");
		    		String pattern = "###.000000";
		    		
		    		DecimalFormat df = (DecimalFormat)    NumberFormat.getNumberInstance(locale);
		    		df.applyPattern(pattern);
		    		
		    		
		    	    // Construct data
		    		//X&Y coordinates must be exactly 8 digits, otherwise no proper result is given. They are swapped (x = long, y = lat)
		    	
		    		//Verbindungen 1-n bekommen; Laufweg, Reisezeit & Umstiege ermitteln
		    		  String text = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>"
		    				  +"<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>"
		    				  + "<ReqC accessId=\"JBischoff2486b558356fa9b81b1rzum\" ver=\"1.1\" requestId=\"7\" prod=\"SPA\" lang=\"DE\">"
		    				  + "<ConReq>"
		    				  + "<ReqT date=\""+VBBDAY.format(departureTime)+"\" time=\""+VBBTIME.format(departureTime)+"\">"
		    				  + "</ReqT>"
		    				  + "<RFlags b=\"0\" f=\"1\" >"
		    				  + "</RFlags>"
		    				  + "<Start>"
		    				  + "<Coord name=\"START\" x=\""+df.format(from.getY()).replace(".", "")+"\" y=\""+df.format(from.getX()).replace(".","")+"\" type=\"WGS84\"/>"
		    				  + "<Prod  prod=\"1111000000000000\" direct=\"0\" sleeper=\"0\" couchette=\"0\" bike=\"0\"/>"
		    				  + "</Start>"
		    				  + "<Dest>"
		    				  + "<Coord name=\"ZIEL\" x=\""+df.format(to.getY()).replace(".", "")+"\" y=\""+df.format(to.getX()).replace(".","")+"\" type=\"WGS84\"/>"
		    				  + "</Dest>"
		    				  + "</ConReq>"
		    				  + "</ReqC>";
		    		PostMethod post = new PostMethod("http://demo.hafas.de/bin/pub/vbb/extxml.exe/");
			    	post.setRequestBody(text);
			    	post.setRequestHeader(
			                "Content-type", "text/xml; charset=ISO-8859-1");
			        HttpClient httpclient = new HttpClient();
			        try {
			            
			            int result = httpclient.executeMethod(post);
			            
			            
			            // Display status code
//			            System.out.println("Response status code: " + result);
			            
			            // Display response
//			            System.out.println("Response body: ");
//			            System.out.println(post.getResponseBodyAsString());
			            
			            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			            DocumentBuilder builder = factory.newDocumentBuilder();
			            Document document = builder.parse(post.getResponseBodyAsStream());
			            
			            if (writeOutput){
			            	BufferedWriter writer =	IOUtils.getBufferedWriter(filename);
			            	Transformer transformer = TransformerFactory.newInstance().newTransformer();
			            	transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			            	//initialize StreamResult with File object to save to file
			            	StreamResult res = new StreamResult(writer);
			            	DOMSource source = new DOMSource(document);
			            	transformer.transform(source, res);
			            	writer.flush();
			            	writer.close();
			        		
			            }
			            
			            Node connectionList = document.getFirstChild().getFirstChild().getFirstChild();
			            NodeList connections =  connectionList.getChildNodes();
			            int amount = connections.getLength();
			            for (int i = 0; i<amount; i++){
			        	   Node connection = connections.item(i);
			        	   Node overview = connection.getFirstChild();;
			        	   
			        	   while (!overview.getNodeName().equals("Overview"))
			        	   { 
			        		   overview = overview.getNextSibling();
			        	   }
			        	   
			        	   System.out.println(  overview.getChildNodes().item(3).getTextContent());
			        	   int transfers = Integer.parseInt(   overview.getChildNodes().item(3).getTextContent());
			        	   String time = overview.getChildNodes().item(4).getFirstChild().getTextContent().substring(3);
			        	   System.out.println(time);
			        	   Date rideTime = VBBDATE.parse(time);
			        	   int seconds = rideTime.getHours()*3600+rideTime.getMinutes()*60+rideTime.getSeconds();
			        	   System.out.println( seconds +"s; transfers: "+ transfers);
			        	   if (seconds<this.bestRideTime){
			        		   this.bestRideTime = seconds;
			        		   this.bestTransfers = transfers;
			        	   }
			           } 
			        }
			        catch (Exception e){
			        	this.bestRideTime = -1;
			        	this.bestTransfers = -1;
			        }
			        
			        finally {
			            // Release current connection to the connection pool 
			            // once you are done
			            post.releaseConnection();
			            post.abort();
			         
			        }
			    
			        
		    	}
			public int getBestRideTime() {
				return bestRideTime;
			}
			public int getBestTransfers() {
				return bestTransfers;
			} 
	


}
