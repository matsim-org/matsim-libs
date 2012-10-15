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

package playground.wrashid.singapore.fcl.parkopedia;

import java.util.Stack;

import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class ConvertToCsv extends MatsimXmlParser implements MatsimSomeReader {

	Double lat;
	Double lng;
	Integer num;
	String name;
	int numberOfParkingWithUnknownCapacity = 0;
	int totalNumberOfParking = 0;

	int numberOfPrices = 0;
	double priceSum = 0;
	double maxPrice = 0;
	private String separater;

	public ConvertToCsv() {
		super(false);
		System.out.println("name" + separater + "lat" + separater + "lng" + separater + "num" + separater + "price");
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {

		if (name.equalsIgnoreCase("space")) {
			lat = null;
			lng = null;
			num = null;
			this.name = null;
			numberOfPrices = 0;
			priceSum = 0;
		}

		if (name.equalsIgnoreCase("price")) {
			String hours = atts.getValue("hours");
			if (hours != null && hours.contains("Mon")) {

				String duration = atts.getValue("duration");

				double parsedAmount = Double.parseDouble(atts.getValue("amount"));
				if (duration.contains("min") || duration.contains("mins")) {
					// System.out.println(duration + " -> " + hours);
					try {
						double numberOfMins = Double.parseDouble(duration.replace("mins", "").replace("min", "").trim());

						if (numberOfMins == 1 && parsedAmount > 10) {
							parsedAmount /= 10000;
							System.out.println(duration + " -> " + hours + " -> " + atts.getValue("amount"));
						}

						double hourlyPrice = parsedAmount / numberOfMins * 60;

						if (hourlyPrice > 30) {
							//System.out.println("###" + duration + " -> " + hours + " -> " + atts.getValue("amount"));
						}

						priceSum += hourlyPrice;

						// if (priceSum>30){
						// System.out.println(duration + " -> " + hours + " -> "
						// + atts.getValue("amount"));
						// System.out.println(parsedAmount);
						// System.out.println(Float.parseFloat(atts.getValue("amount")));
						// }

						numberOfPrices++;
					} catch (Exception e) {

					}
				} else if (duration.contains("hour") || duration.contains("hours")) {
					try {
						double numberOfHours = Double.parseDouble(duration.replace("hours", "").replace("hour", "").trim());
						priceSum += parsedAmount / numberOfHours;

						numberOfPrices++;
					} catch (Exception e) {

					}
				} else {

				}

			}

		}

		/*
		 * if (name.equalsIgnoreCase("price")) { String hours =
		 * atts.getValue("hours"); System.out.println(hours + "->" +
		 * atts.getValue("amount") + "->" + atts.getValue("duration"));
		 * 
		 * if (hours.contains("Mon-Fri 8:00-18:00") ||
		 * hours.contains("Mon-Fri 8:00-17:00")){
		 * 
		 * }
		 * 
		 * }
		 */
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (name.equalsIgnoreCase("lat")) {
			lat = Double.parseDouble(content);
		}

		if (name.equalsIgnoreCase("lng")) {
			lng = Double.parseDouble(content);
		}

		if (name.equalsIgnoreCase("num")) {
			num = Integer.parseInt(content);
		}

		if (name.equalsIgnoreCase("name")) {
			this.name = content.replace("<![CDATA[", "").replace("]]>", "");
		}

		if (name.equalsIgnoreCase("space")) {
			String numPrint;
			String pricePrint;
			if (num == null) {
				numPrint = "";
				numberOfParkingWithUnknownCapacity++;
			} else {
				numPrint = Integer.toString(num);
			}

			if (numberOfPrices != 0) {
				double price = priceSum / numberOfPrices;

				if (price > maxPrice) {
					maxPrice = price;
				}

				pricePrint = Double.toString(price);
			} else {
				pricePrint = "";
			}

			separater = "$";
			System.out.println(this.name + separater + lat + separater + lng + separater + numPrint + separater + pricePrint);
			totalNumberOfParking++;
		}

	}

}
