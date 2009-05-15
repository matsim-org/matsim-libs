/* *********************************************************************** *
 * project: org.matsim.*
 * NOGATypes.java
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

package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.util.List;
import java.util.Vector;

public class NOGATypes {
	
	// Exact definition of Supermarket etc.
	public String [] shopGrocery = {
			"B015211A",		// 52.11A Verbrauchermärkte 	(> 2500 m2)
			"B015211B", 	// 52.11B Grosse Supermärkte 	(1000-2499 m2)
			"B015211C",		// 52.11C Kleine Supermärkte 	(400-999 m2)
			"B015211D",		// 52.11D Grosse Geschäfte 		(100-399 m2)
			"B015211E",		// 52.11E Kleine Geschäfte 		(< 100 m2)
			"B015212A",     // 52.12A Warenhäuser
			"B015221A",     // 52.21A Detailhandel mit Obst und Gemüse
			"B015222A",     // 52.22A Detailhandel mit Fleisch und Fleischwaren
			"B015223A",     // 52.23A Detailhandel mit Fisch und Meeresfrüchten
			"B015224A",     // 52.24A Detailhandel mit Brot, Back- und Süsswaren
			"B015225A",     // 52.25A Detailhandel mit Getränken
			"B015227A",     // 52.27A Detailhandel mit Milcherzeugnissen und Eiern
			"B015227B",     // 52.27B Sonstiger Fachdetailhandel mit Nahrungsmitteln, Getränken und Tabak a.n.g. (in Verkaufsräumen)
	};
	
	public String [] shopNonGrocery = {
		"B015211A",		// 52.11A Verbrauchermärkte 		(> 2500 m2)
		"B015211B", 	// 52.11B Grosse Supermärkte 		(1000-2499 m2)
		"B015211C",		// 52.11C Kleine Supermärkte 		(400-999 m2)
		"B015211D",		// 52.11D Grosse Geschäfte 			(100-399 m2)
		"B015211E",		// 52.11E Kleine Geschäfte 			(< 100 m2)
		"B015212A",     // 52.12A Warenhäuser
		"B015212B",     // 52.12B Sonstiger Detailhandel mit Waren verschiedener Art, Hauptrichtung Nichtnahrungsmittel
//		"B015221A",     // 52.21A Detailhandel mit Obst und Gemüse
//		"B015222A",     // 52.22A Detailhandel mit Fleisch und Fleischwaren
//		"B015223A",     // 52.23A Detailhandel mit Fisch und Meeresfrüchten
//		"B015224A",     // 52.24A Detailhandel mit Brot, Back- und Süsswaren
//		"B015225A",     // 52.25A Detailhandel mit Getränken
		"B015226A",     // 52.26A Detailhandel mit Tabakwaren
//		"B015227A",     // 52.27A Detailhandel mit Milcherzeugnissen und Eiern
//		"B015227B",     // 52.27B Sonstiger Fachdetailhandel mit Nahrungsmitteln, Getränken und Tabak a.n.g. (in Verkaufsräumen)
		"B015231A",     // 52.31A Fachdetailhandel mit pharmazeutischen Produkten
		"B015232A",     // 52.32A Detailhandel mit medizinischen und orthopädischen Artikeln
		"B015233A",     // 52.33A Drogerien
		"B015233B",     // 52.33B Parfümerien und sonstiger Detailhandel mit kosmetischen Artikeln und Körperpflegemitteln
		"B015241A",     // 52.41A Detailhandel mit Textilien
		"B015242A",     // 52.42A Detailhandel mit Damenbekleidung
		"B015242B",     // 52.42B Detailhandel mit Herrenbekleidung
		"B015242C",     // 52.42C Detailhandel mit Säuglings- und Kinderbekleidung
		"B015242D",     // 52.42D Detailhandel mit Pelzwaren
		"B015242E",     // 52.42E Detailhandel mit Bekleidungszubehör und Bekleidung ohne ausgeprägten Schwerpunkt
		"B015243A",     // 52.43A Detailhandel mit Schuhen
		"B015243B",     // 52.43B Detailhandel mit Lederwaren und Reiseartikeln
		"B015244A",     // 52.44A Detailhandel mit Möbeln
		"B015244B",     // 52.44B Detailhandel mit Teppichen
		"B015244C",     // 52.44C Detailhandel mit Beleuchtungs- und Haushaltsgegenständen
		"B015245A",     // 52.45A Detailhandel mit elektrischen Haushaltsgeräten
		"B015245B",     // 52.45B Detailhandel mit Radio- und Fernsehgeräten
		"B015245C",     // 52.45C Detailhandel mit Ton- und Bildträgern
		"B015245D",     // 52.45D Detailhandel mit Musikinstrumenten
		"B015245E",     // 52.45E Detailhandel mit elektrischen Haushalts-, Radio- und Fernsehgeräten ohne ausgeprägten Schwerpunkt
		"B015246A",     // 52.46A Detailhandel mit Eisen- und Metallwaren
		"B015246B",     // 52.46B Sonstiger Detailhandel mit Metallwaren, Anstrichmitteln, Glaswaren, Bau- und Heimwerkerbedarf
		"B015247A",     // 52.47A Detailhandel mit Büchern
		"B015247B",     // 52.47B Detailhandel mit Zeitungen und Zeitschriften; Kioske
		"B015247C",     // 52.47C Detailhandel mit Schreib- und Papeteriewaren
		"B015248A",     // 52.48A Detailhandel mit Getreide, Futtermitteln und Landesprodukten
		"B015248B",     // 52.48B Detailhandel mit Blumen und Pflanzen
		"B015248C",     // 52.48C Detailhandel mit Haustieren und zoologischem Bedarf
		"B015248D",     // 52.48D Detailhandel mit Brennstoffen und Heizmaterial
		"B015248E",     // 52.48E Detailhandel mit Boden- und Wandbelägen (ohne Teppiche)
		"B015248F",     // 52.48F Detailhandel mit Brillen und anderen Sehhilfen
		"B015248G",     // 52.48G Detailhandel mit fotografischen Artikeln
		"B015248H",     // 52.48H Detailhandel mit Uhren und Schmuck
		"B015248I",     // 52.48I Detailhandel mit Büromaschinen und -einrichtungen
		"B015248J",     // 52.48J Detailhandel mit Computern und Software
		"B015248K",     // 52.48K Detailhandel mit Spielwaren
		"B015248L",     // 52.48L Detailhandel mit Fahrrädern
		"B015248M",     // 52.48M Detailhandel mit Sportartikeln
		"B015248N",     // 52.48N Detailhandel mit Geschenkartikeln und Souvenirs
		"B015248O",     // 52.48O Kunsthandel
		"B015248P",     // 52.48P Sonstiger Fachdetailhandel a.n.g. (in Verkaufsräumen)
		"B015250A",     // 52.50A Detailhandel mit Antiquitäten
		"B015250B",     // 52.50B Detailhandel mit Gebrauchtwaren a.n.g. (in Verkaufsräumen)
//		"B015261A",     // 52.61A Versandhandel
		"B015262A",     // 52.62A Detailhandel an Verkaufsständen und auf Märkten
		"B015263A",     // 52.63A Sonstiger Detailhandel (nicht in Verkaufsräumen)
		"B015271A",     // 52.71A Reparatur von Schuhen und Lederwaren
		"B015272A",     // 52.72A Reparatur von elektrischen Haushaltsgeräten
		"B015273A",     // 52.73A Reparatur von Uhren und Schmuck
		"B015274A",     // 52.74A Reparatur von sonstigen Gebrauchsgütern
	};
	
	public List<String> getGroceryTypes() {
		List<String> groceryTypes = new Vector<String>();		
		for (int i = 0; i < shopGrocery.length; i++) {
			groceryTypes.add(shopGrocery[i]);
		}
		return groceryTypes;
	}
	
	public List<String> getNonGroceryTypes() {
		List<String> nongroceryTypes = new Vector<String>();		
		for (int i = 0; i < shopNonGrocery.length; i++) {
			nongroceryTypes.add(shopNonGrocery[i]);
		}
		return nongroceryTypes;
	}
}
