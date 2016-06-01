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

package playground.ikaddoura.incidents.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.StringUtils;

import playground.ikaddoura.incidents.data.TrafficItem;

/**
* @author ikaddoura
*/

public class HereMapsTrafficItemXMLReader {
	private static final Logger log = Logger.getLogger(HereMapsTrafficItemXMLReader.class);

	private List<TrafficItem> trafficItems = new ArrayList<>();
	
	public void printTrafficItems() {
		for (TrafficItem tmc : trafficItems) {
			System.out.println(tmc.toString());
		}
	}

	public List<TrafficItem> getTrafficItems() {
		return trafficItems;
	}

	public void readStream(String trafficItemXMLFile) throws XMLStreamException, FactoryConfigurationError, IOException {
		
		log.info("Reading stream from file: " + trafficItemXMLFile);
		
		String[] fileNameData = trafficItemXMLFile.split("_");
		long downloadTime = Long.valueOf(fileNameData[2]);
		
		XMLStreamReader in = null;
		
		if (trafficItemXMLFile.endsWith(".xml.gz")) {
			GZIPInputStream stream = new GZIPInputStream(new FileInputStream(new File(trafficItemXMLFile)));
			in = XMLInputFactory.newInstance().createXMLStreamReader(stream);			

		} else if (trafficItemXMLFile.endsWith(".xml")) {
			FileInputStream stream = new FileInputStream(new File(trafficItemXMLFile));
			in = XMLInputFactory.newInstance().createXMLStreamReader(stream);			

		} else {
			throw new RuntimeException("This traffic item xml reader can only read *.xml or *.xml.gz files. Aborting...");
		}
		
		this.readStream(in, downloadTime);
	}
	
	public void readStream(URL url) throws XMLStreamException, IOException {

		log.info("Reading stream from URL: " + url);

		XMLStreamReader in = XMLInputFactory.newInstance().createXMLStreamReader(url.openStream());
		this.readStream(in, System.currentTimeMillis());		
	}
	
	public void readStream(XMLStreamReader in, Long downloadTime) throws XMLStreamException, FactoryConfigurationError {
				
		TrafficItem trafficItem = null;
		boolean inLevelTrafficItem = false;
		boolean inLevelRDSTMC = false;
		boolean inLevelOrigin = false;
		boolean inLevelTo = false;
		boolean inLevelTMCAlert = false;
		boolean inLevelGeoloc = false;

		while (in.hasNext()) {

			in.next();

			if (in.isStartElement()) {

				// set the level information
				if (in.getLocalName().equals("TRAFFIC_ITEM")) {
					trafficItem = new TrafficItem(downloadTime);
					inLevelTrafficItem = true;
				} else if (in.getLocalName().equals("ORIGIN")) {
					inLevelOrigin = true;
				} else if (in.getLocalName().equals("TO")) {
					inLevelTo = true;
				} else if (in.getLocalName().equals("RDS-TMC")) {
					inLevelRDSTMC = true;
				} else if (in.getLocalName().equals("ALERTC")) {
					inLevelTMCAlert = true;
				} else if (in.getLocalName().equals("GEOLOC")) {
					inLevelGeoloc = true;
				}

				// store the level-specific data
				if (inLevelTrafficItem) {
					if (in.getLocalName().equals("TRAFFIC_ITEM_ID")) {
						trafficItem.setId(in.getElementText());
					} else if (in.getLocalName().equals("ORIGINAL_TRAFFIC_ITEM_ID")) {
						trafficItem.setOriginalId(in.getElementText());
					} else if (in.getLocalName().equals("START_TIME")) {
						trafficItem.setStartDateTime(convertDateTimeFormat(in.getElementText()));
					} else if (in.getLocalName().equals("END_TIME")) {
						trafficItem.setEndDateTime(convertDateTimeFormat(in.getElementText()));
					} else if (in.getLocalName().equals("TRAFFIC_ITEM_STATUS_SHORT_DESC")) {
						trafficItem.setStatus(in.getElementText());
					}

					if (inLevelRDSTMC) {
						
						if (inLevelOrigin) {

							if (in.getLocalName().equals("LOCATION_ID")) {
								trafficItem.getOrigin().setLocationId(in.getElementText());
							} else if (in.getLocalName().equals("LOCATION_DESC")) {
								trafficItem.getOrigin().setDescription(in.getElementText());
							} else if (in.getLocalName().equals("EBU_COUNTRY_CODE")) {
								trafficItem.getOrigin().setCountryCode(in.getElementText());
							}

						} else if (inLevelTo) {

							if (in.getLocalName().equals("LOCATION_ID")) {
								trafficItem.getTo().setLocationId(in.getElementText());
							} else if (in.getLocalName().equals("LOCATION_DESC")) {
								trafficItem.getTo().setDescription(in.getElementText());
							} else if (in.getLocalName().equals("EBU_COUNTRY_CODE")) {
								trafficItem.getTo().setCountryCode(in.getElementText());
							}

						} else if (inLevelTMCAlert) {

							if (in.getLocalName().equals("PHRASE_CODE")) {
								trafficItem.getTMCAlert().setPhraseCode(in.getElementText());
							} else if (in.getLocalName().equals("EXTENT")) {
								trafficItem.getTMCAlert().setExtent(in.getElementText());
							} else if (in.getLocalName().equals("DESCRIPTION")) {
								trafficItem.getTMCAlert().setDescription(in.getElementText());
							} else if (in.getLocalName().equals("ALERTC_DURATION")) {
								trafficItem.getTMCAlert().setAlertDuration(in.getElementText());
							} else if (in.getLocalName().equals("UPDATE_CLASS")) {
								trafficItem.getTMCAlert().setUpdateClass(in.getElementText());
							}
						}

					} else if (inLevelGeoloc) {

						if (inLevelOrigin) {
							if (in.getLocalName().equals("LATITUDE")) {
								trafficItem.getOrigin().setLatitude(in.getElementText());
							} else if (in.getLocalName().equals("LONGITUDE")) {
								trafficItem.getOrigin().setLongitude(in.getElementText());
							}
						} else if (inLevelTo) {
							if (in.getLocalName().equals("LATITUDE")) {
								trafficItem.getTo().setLatitude(in.getElementText());
							} else if (in.getLocalName().equals("LONGITUDE")) {
								trafficItem.getTo().setLongitude(in.getElementText());
							}
						}
					}

				} else {
					// ignore non-traffic items
				}

			} else if (in.isEndElement()) {
			
				if (inLevelTrafficItem) {
					if (in.getLocalName().equals("TRAFFIC_ITEM")) {
						trafficItems.add(trafficItem);
						trafficItem = null;
						inLevelTrafficItem = false;
					} else if (in.getLocalName().equals("ORIGIN")) {
						inLevelOrigin = false;
					} else if (in.getLocalName().equals("TO")) {
						inLevelTo = false;
					} else if (in.getLocalName().equals("RDS-TMC")) {
						inLevelRDSTMC = false;
					} else if (in.getLocalName().equals("ALERTC")) {
						inLevelTMCAlert = false;
					} else if (in.getLocalName().equals("GEOLOC")) {
						inLevelGeoloc = false;
					}
				}
			}
		}

		in.close();
	}
	
	private String convertDateTimeFormat(String datetimeString) {
		
		// current format: MM/DD/YYYY HH:MM:SS
		// target format: YYYY-MM-DD HH:MM:SS

		String dateTimeDelimiter = " ";
		String[] datetime = StringUtils.explode(datetimeString, dateTimeDelimiter.charAt(0));
		String dateStr = datetime[0];
		String timeStr = datetime[1];
		
		String dateDelimiter = "/";
		String[] date = StringUtils.explode(dateStr, dateDelimiter.charAt(0));
		String month = date[0];
		String day = date[1];
		String year = date[2];
		
		if (year.length() != 4) {
			
			log.warn("Expecting the traffic incidents to have the time format MM/DD/YYYY HH:MM:SS. This is something else: " + datetimeString + " Aborting...");
			year = "0";
		}
		
		String newFormat = year + "-" + month + "-" + day + " " + timeStr;
		return newFormat;
	}
	
}

