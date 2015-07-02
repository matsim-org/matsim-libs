/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.gctpeds.analysis;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JSONParser {
	
	private static final Logger log = Logger.getLogger(JSONParser.class);
	
	private String file;
	private Trajectories tr;


	public JSONParser(String file, Trajectories tr) {
		this.file = file;
		this.tr = tr;
	}
	
	public void run() throws JsonParseException, IOException {
		 JsonFactory factory = new JsonFactory();
		 JsonParser parser = factory.createParser(new File(this.file));
		 
		 while (!parser.isClosed()) {
			 JsonToken tok = parser.nextToken();
			 if (tok == null) {
				 parser.close();
				 break;
			 }
			 if (JsonToken.START_OBJECT.equals(tok)){
				 handleObject(parser);
			 }
			 
//			 System.out.println(tok);
		 }
	}
	
	private void handleObject(JsonParser parser) throws JsonParseException, IOException {
		JsonToken tok = parser.nextToken();
		Id<Person> id = null;
		double time = 0,x = 0,y = 0,vx = 0, vy = 0;
		while (!JsonToken.END_OBJECT.equals(tok)) {
			String name = parser.getText();
			switch (name) {
			case "id": 
				parser.nextToken();
				id = Id.createPersonId(parser.getText());
				break;
			case "time": 
				parser.nextToken();
				String text = parser.getText();
				time = Double.parseDouble(text);
				break;
			case "x": 
				parser.nextToken();
				x = Double.parseDouble(parser.getText());
				break;
			case "y": 
				parser.nextToken();
				y = Double.parseDouble(parser.getText());
				break;
			case "vx": 
				parser.nextToken();
				vx = Double.parseDouble(parser.getText());
				break;
			case "vy": 
				parser.nextToken();
				vy = Double.parseDouble(parser.getText());
				break;
			default:
				parser.nextToken();
				log.warn("Unknown field:" + name + " with value:" + parser.getText() + ". Ignored!");
			}
			tok = parser.nextToken();
		}
		if (!id.toString().equals("-1")){
			this.tr.addCoordinate(id,time,x,y,vx,vy);
		}
	}
	public static void main(String [] args) throws JsonParseException, IOException {
		String input = "/Users/laemmel/devel/nyc/output_measurements/tr_link_842.json" ;
		Trajectories tra = new Trajectories();
		new JSONParser(input,tra).run();
		tra.dumpAsJuPedSimTrajectories(0,0,3600*16+25*60,3600*16+35*60,"/Users/laemmel/tmp/jps.test");
	}

}
