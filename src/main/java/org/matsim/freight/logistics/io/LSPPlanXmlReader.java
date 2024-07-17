/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.io;

import java.net.URL;
import java.util.Stack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.logistics.LSPConstants;
import org.matsim.freight.logistics.LSPs;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Delegates a LSPPlanXmlParser according to declared schema definition file
 *
 * @author nrichter (Niclas Richter)
 */
public final class LSPPlanXmlReader implements MatsimReader {
  private static final Logger log = LogManager.getLogger(LSPPlanXmlReader.class);
  private final LSPsPlanParser parser;

  public LSPPlanXmlReader(final LSPs lsPs, Carriers carriers) {
    System.setProperty("matsim.preferLocalDtds", "true");
    this.parser = new LSPsPlanParser(lsPs, carriers);
  }

  public void readFile(String filename) {
    try {
      this.parser.setValidating(true);
      this.parser.readFile(filename);
    } catch (Exception e) {
      log.warn("### Exception found while trying to read LSPPlan: Message: {} ; cause: {} ; class {}", e.getMessage(), e.getCause(), e.getClass());
      throw e;
    }
  }

  public void readURL(URL url) {
    try {
      this.parser.readURL(url);
    } catch (Exception e) {
      log.warn("### Exception found while trying to read LSPPlan: Message: {} ; cause: {} ; class {}", e.getMessage(), e.getCause(), e.getClass());
      if (e.getCause()
          .getMessage()
          .contains(
              "cvc-elt.1")) { // "Cannot find the declaration of element" -> exception comes most
                              // probably because no validation information was found
        log.warn("read with validation = true failed. Try it again without validation... url: {}", url.toString());
        parser.setValidating(true);
        parser.readURL(url);
      } else { // other problem: e.g. validation does not work, because of missing validation file.
        throw e;
      }
    }
  }

  private static final class LSPsPlanParser extends MatsimXmlParser {
    private final LSPs lsPs;
    private final Carriers carriers;

    private MatsimXmlParser delegate = null;

    LSPsPlanParser(LSPs lsPs, Carriers carriers) {
      super(ValidationType.XSD_ONLY);
      this.lsPs = lsPs;
      this.carriers = carriers;
    }

    public void startTag(String name, Attributes attributes, Stack<String> context) {
      if (LSPConstants.LSPS.equalsIgnoreCase(name)) {
        String str = attributes.getValue("xsi:schemaLocation");
        log.info("Found following schemaLocation in lsPs definition file: {}", str);
        if (str.contains("lspsDefinitions_v1.xsd")) {
          delegate = new LSPPlanXmlParserV1(lsPs, carriers);
        } else {
          throw new RuntimeException("no reader found for " + str);
        }
      } else {
        this.delegate.startTag(name, attributes, context);
      }
    }

    public void endTag(String name, String content, Stack<String> context) {
      this.delegate.endTag(name, content, context);
    }

    public void endDocument() {
      try {
        this.delegate.endDocument();
      } catch (SAXException var2) {
        throw new RuntimeException(var2);
      }
    }
  }
}
