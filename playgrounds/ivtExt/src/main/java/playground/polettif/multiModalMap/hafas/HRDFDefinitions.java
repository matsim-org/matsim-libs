/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.multiModalMap.hafas;

/**
 * Definitions and categories for modes found in hafas files.
 * Assignment equivalent to GTFS.
 */
public final class HRDFDefinitions {

	public enum TransportModes {

		/** 0 - Tram, Streetcar, Light rail. Any light rail or street level system within a metropolitan area. */
		TRAM("tram"),

		/** 1 - Subway, Metro. Any underground rail system within a metropolitan area. */
		SUBWAY("subway"),

		/** 2 - Rail. Used for intercity or long-distance travel. */
		RAIL("rail"),

		/** 3 - Bus. Used for short- and long-distance bus routes. */
		BUS("bus"),

		/** 4 - Ferry. Used for short- and long-distance boat service. */
		FERRY("ferry"),

		/** 5 - Cable car. Used for street-level cable cars where the cable runs beneath the car. */
		CABLE_CAR("cablecar"),

		/** 6 - Gondola, Suspended cable car. Typically used for aerial cable cars where the car is suspended from the cable. */
		GONDOLA("gondola"),

		/** 7 - Funicular. Any rail system designed for steep inclines. */
		FUNICULAR("funicular"),

		NONE("");

		String modeName;

		private TransportModes(String modeName) {
			this.modeName = modeName;
		}
	}

	public enum Vehicles {
		T   ("T", 	TransportModes.TRAM),
		NFT ("NFT",	TransportModes.TRAM),

		M   ("M",	TransportModes.SUBWAY),

		AG  ("AG",	TransportModes.RAIL),
		BEX ("BEX",	TransportModes.RAIL),
		CNL ("CNL",	TransportModes.RAIL),
		D   ("D",	TransportModes.RAIL),
		EC  ("EC",	TransportModes.RAIL),
		EN	("EN",	TransportModes.RAIL),
		EXT	("EXT",	TransportModes.RAIL),
		GEX	("GEX",	TransportModes.RAIL),
		IC	("IC",	TransportModes.RAIL),
		ICE	("ICE",	TransportModes.RAIL),
		ICN	("ICN",	TransportModes.RAIL),
		IR	("IR",	TransportModes.RAIL),

		NZ	("NZ",	TransportModes.RAIL),
		R	("R",	TransportModes.RAIL),
		RE	("RE",	TransportModes.RAIL),
		RJ	("RJ",	TransportModes.RAIL),
		S	("S",	TransportModes.RAIL),
		SN	("SN",	TransportModes.RAIL),
		TGV	("TGV",	TransportModes.RAIL),
		VAE	("VAE",	TransportModes.RAIL),
		ZUG	("ZUG",	TransportModes.RAIL),

		BUS	("BUS",	TransportModes.BUS),
		NFB	("NFB",	TransportModes.BUS),
		TX	("TX",	TransportModes.BUS),
		KB	("KB",	TransportModes.BUS),
		NFO	("NFO",	TransportModes.BUS),
		EXB	("EXB",	TransportModes.BUS),
		NB	("NB",	TransportModes.BUS),

		BAT	("BAT",	TransportModes.FERRY),
		BAV	("BAV",	TransportModes.FERRY),
		FAE	("FAE",	TransportModes.FERRY),

		LB	("LB",	TransportModes.GONDOLA),
		GB	("GB",	TransportModes.GONDOLA),

		FUN	("FUN",	TransportModes.FUNICULAR),

		ARZ ("ARZ", TransportModes.NONE),	// Car-carrying train, Autoreisezug
		ATZ ("ATZ", TransportModes.NONE),	// Car train, Autotunnelzug
		MP	("MP", 	TransportModes.NONE),	// LeermaterialZ Personenbeförd
		SL	("SL",	TransportModes.NONE),	// Chairlift, Sesselbahn

		// todo look up initial values
		TER ("TER", TransportModes.RAIL),	// Train Express Regional
		TE2 ("TE2", TransportModes.RAIL),	// TER200
		ICB	("ICB",	TransportModes.BUS),	// intercity bus
		TRO	("TRO",	TransportModes.BUS),	// trolley bus
		RB	("RB",	TransportModes.RAIL);	// Regionalbahn


		String value;
		TransportModes mode;
		private Vehicles(String value, TransportModes mode) {
			this.value = value;
			this.mode = mode;
		}

		public TransportModes getMode() {
			return mode;
		}
	}

}