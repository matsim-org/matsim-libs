/* *********************************************************************** *
 * project: org.matsim.*
 * MyXmlConverter.java
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

package playground.jjoubert.Utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class MyXmlConverter {
	private XStream xstream = null;
	
	public MyXmlConverter(){
		this.xstream = new XStream(new DomDriver());
	}
	
	private String convertObjectToXmlString(Object obj){
		String string = null;
		string = this.xstream.toXML(obj);
		return string;
	}
	
	public void writeObjectToFile(Object obj, String fileString){
		String xmlString = convertObjectToXmlString(obj);
		try {
			BufferedWriter xmlOutput = new BufferedWriter(new FileWriter(new File(fileString)));
			try{
				xmlOutput.write(xmlString);
			} finally{
				xmlOutput.close();
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String convertFileToXmlString(String fileString){
		StringBuilder string = new StringBuilder();
		try{
			BufferedReader br = new BufferedReader( new FileReader( new File(fileString)));
			try{
				String s;
				while( (s = br.readLine() ) != null ){
					string.append(s);
					string.append("\n");
				}
			} finally {
				br.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return string.toString();		
	}
	
	public Object readObjectFromFile(String fileString){
		Object result = null;
		result = this.xstream.fromXML( convertFileToXmlString(fileString));		
		return result;
	}
	
}
