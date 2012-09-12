/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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

/**
 * 
 */
package playground.tnicolai.matsim4opus.xmlvalidation;

/**
 * @author thomas
 *
 */
import java.io.*;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import org.xml.sax.*;

public class XSDValidation
{
   public static void main( String[] args ) throws Exception
   {
      if( args.length != 2 ) {
         System.out.println( "Bitte XSD-Schema und XML-Dokument angeben." );
         return;
      }
      System.out.println( args[0] + " + " + args[1] );
      XSDValidation.validate( args[0], args[1] );
    }

   public static void validate( String xsdSchema, String xmlDokument ) throws SAXException, IOException
   {
      SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
      Schema        schema = schemaFactory.newSchema( new File( xsdSchema ) );
      Validator     validator = schema.newValidator();
      validator.setErrorHandler( new XsdValidationLoggingErrorHandler() );
      validator.validate( new StreamSource( new File( xmlDokument ) ) );
   }
}

class XsdValidationLoggingErrorHandler implements ErrorHandler
{
   public void warning( SAXParseException ex ) throws SAXException
   {
      System.out.println( "Warnung: " + ex.getMessage() );
   }

   public void error( SAXParseException ex ) throws SAXException
   {
      System.out.println( "Fehler: " + ex.getMessage() );
   }

   public void fatalError( SAXParseException ex ) throws SAXException
   {
      System.out.println( "Fataler Fehler: " + ex.getMessage() );
   }
}