/*
 * Created on Mar 19, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.parser;

/**
 * @author henkel
 */
public final class ParseException extends Exception {

	ParseException(int line, int column, String encountered, String expected){
		super(""+line+":"+column+": encountered '"+encountered+"', expected '"+expected+"'.");	
	}
	

}
