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

package playground.johannes.gsv.synPop.mid.analysis;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.IOException;
import java.util.Map;

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.synpop.source.mid2008.generator.RowHandler;

/**
 * @author johannes
 * 
 */
public class RawPkm {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Handler h = new Handler();
		h.read("/home/johannes/gsv/germany-scenario/mid2008/raw/MiD2008_PUF_Wege.txt");

		TObjectDoubleIterator<String> it = h.pkms.iterator();
		for (int i = 0; i < h.pkms.size(); i++) {
			it.advance();
			System.out.println(String.format("%s\t%s", it.key(), it.value()));
		}

	}

	private static class Handler extends RowHandler {

		TObjectDoubleHashMap<String> pkms = new TObjectDoubleHashMap<>();

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * playground.johannes.synpop.source.mid2008.generator.RowHandler#handleRow(java.util
		 * .Map)
		 */
		@Override
		protected void handleRow(Map<String, String> attributes) {
			String mode = attributes.get(MIDKeys.LEG_MODE);
			if (mode.equalsIgnoreCase("MIV (Fahrer)") || mode.equalsIgnoreCase("MIV (Mitfahrer)")) {
				String distStr = attributes.get(MIDKeys.LEG_DISTANCE);
				if (distStr != null) {
					double d = Double.parseDouble(distStr);
					if (d < 9994) {
						d = d * 1000;
						String type = attributes.get(MIDKeys.LEG_MAIN_TYPE);
						String subtype = attributes.get(MIDKeys.LEG_SUB_TYPE);
						type = convertType(type, subtype);
						
						String gew = attributes.get("w_gew");
						double w = Double.parseDouble(gew);
						d = d * w;
						pkms.adjustOrPutValue(type, d, d);
					}
				}
			}

		}

		private String convertType(String typeId, String subtype) {
			if (typeId == null) {
				throw new NullPointerException();
			}

			if (typeId.equalsIgnoreCase("Erreichen des Arbeitsplatzes")) {
				return ActivityType.WORK;

			} else if (typeId.equalsIgnoreCase("dienstlich oder geschäftlich")) {
				return ActivityType.BUSINESS;

			} else if (typeId.equalsIgnoreCase("Erreichen der Ausbildungsstätte oder Schule")) {
				return ActivityType.EDUCATION;

			} else if (typeId.equalsIgnoreCase("Einkauf")) {
				return ActivityType.SHOP;

			} else if (typeId.equalsIgnoreCase("private Erledigungen")) {
				return "private";

			} else if (typeId.equalsIgnoreCase("Bringen oder Holen von Personen")) {
				return "pickdrop";

			} else if (typeId.equalsIgnoreCase("Freizeitaktivität")) {
				// return ActivityType.LEISURE;

				if (subtype != null) {
					if (subtype.equalsIgnoreCase("Besuch oder Treffen mit|von Freunden, Verwandten, Bekannten")) {
						return "visit";
					} else if (subtype.equalsIgnoreCase("Besuch kultureller Einrichtung (z.B. Kino, Theater, Museum")) {
						return "culture";
					} else if (subtype.equalsIgnoreCase("Besuch einer Veranstaltung (z.B. Fußballspiel, Markt)")) {
						return "culture";
					} else if (subtype.equalsIgnoreCase("Sport (selbst aktiv), Sportverein")) {
						return "sport";
					} else if (subtype.equalsIgnoreCase("Restaurant, Gaststätte, Mittagessen etc.")) {
						return "gastro";
					} else if (subtype.equalsIgnoreCase("Tagesausflug, mehrtägiger Ausflug (bis 4 Tage)")) {
						return "vacations_short";
					} else if (subtype.equalsIgnoreCase("Urlaub (ab 5 Tage)")) {
						return "vacations_long";
					}
				}
				return ActivityType.LEISURE;

			} else if (typeId.equalsIgnoreCase("nach Hause")) {
				return ActivityType.HOME;

			} else if (typeId.equalsIgnoreCase("zur Schule oder Vorschule")) {
				return ActivityType.EDUCATION;

			} else if (typeId.equalsIgnoreCase("Kindertagesstätte oder Kindergarten")) {
				return ActivityType.EDUCATION;

			} else {
				return ActivityType.MISC;
			}

		}

	}
}
