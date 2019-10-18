/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.vsp.zzArchive.bvwpOld;

import java.io.IOException;
import java.io.Writer;

import org.matsim.core.utils.io.IOUtils;

class Html {
	void multiFmtComment( String str ) {
		System.out.println( str ) ;
		beginParagraph() ;
		write( str ) ;
		endParagraph();
	}
	
	final Writer out ;
	
	Html( String str ) {
		out = IOUtils.getBufferedWriter( str+".htm" ) ;
	}

	Html() {
		out = IOUtils.getBufferedWriter("out.htm") ;
	}

	void write(String str) {
		myWrite(str) ;
	}

	void beginHtml() {
		myWrite("<html>") ;
	}
	void endHtml() {
		myWrite("</html>") ;
		try {
			out.close() ;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void beginTableRow() {
		myWrite("   <tr>\n      <td>") ;
	}
	void beginTableMulticolumnRow() {
		myWrite("   <tr>\n      <td colspan=99>") ;
	}
	void nextTableEntry() {
		myWrite("</td>\n      <td>") ;
	}
	void endTableRow() {
		myWrite("</td>\n   </tr>\n") ;
	}

	void beginTable() {
		myWrite("<table border=\"1\">\n") ;
	}
	void endTable() {
		myWrite("</table>\n") ;
	}
	
	void bvwpTableRow( String str, double n1, double n2, double p1, double p2, double d1, double d2, double u1, double u2 ) {
		beginTableRow() ;
		write(str) ; 
		nextTableEntry() ; write(n1) ; nextTableEntry() ; write(n2) ; 
		nextTableEntry() ; write(p1) ; nextTableEntry() ; write(p2) ; 
		nextTableEntry() ; write(d1) ; nextTableEntry() ; write(d2) ; 
		nextTableEntry() ; write(u1) ; nextTableEntry() ; write(u2) ; 
		endTableRow() ;
	}
	void bvwpTableRow( String str, String n1, String n2, String p1, String p2, String d1, String d2, String u1, String u2 ) {
		beginTableRow() ;
		write(str) ; 
		nextTableEntry() ; write(n1) ; nextTableEntry() ; write(n2) ; 
		nextTableEntry() ; write(p1) ; nextTableEntry() ; write(p2) ; 
		nextTableEntry() ; write(d1) ; nextTableEntry() ; write(d2) ; 
		nextTableEntry() ; write(u1) ; nextTableEntry() ; write(u2) ; 
		endTableRow() ;
	}

	void beginBody() {
		myWrite("<body>\n") ;
	}
	void endBody() {
		myWrite("</body>\n") ;
	}

	void beginParagraph() {
		myWrite("<p>\n") ;
	}
	void endParagraph() {
		myWrite("</p>\n") ;
	}

	public void write(double dbl) {
		myWrite( Double.toString(dbl) ) ;
	}

	private void myWrite( String str ) {
		try {
			out.write( str ) ;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
