/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.wagonSim.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraCountry;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraDirection;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraLink;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraLinkOwner;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraLinkType;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraNode;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraNodeCluster;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraTrack;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * @author balmermi @ Senozon AG
 * @since 2013-07-04
 *
 */
public class NEMOInfraParser extends MatsimXmlParser {

	//////////////////////////////////////////////////////////////////////
	// XML Entities
	//////////////////////////////////////////////////////////////////////
	
	private static final String NEMOINFRA = "NEMOInfra";
//		private static final String ABBIEGEWIDERSTANDSTYPEN = "AbbiegewiderstandsTypen";
//			private static final String ABBIEGEWIDERSTANDSTYP = "AbbiegewiderstandsTyp";
//		private static final String ENGPASSBEREICHE = "Engpassbereiche";
//			private static final String ENGPASSBEREICH = "Engpassbereich";
//		private static final String ENGPASSINTERVALLE = "Engpassintervalle";
//			private static final String ENGPASSINTERVALL = "Engpassintervall";
//		private static final String ENGPASSZUSCHLAEGE = "Engpasszuschlaege";
//		private static final String KANTEVARLIST = "KanteVarList";
//			private static final String KANTEVAR = "KanteVar";
//		private static final String KANTEVARALIST = "KanteVarAList";
//			private static final String KANTEVARA = "KanteVarA";
//		private static final String KANTEVARBLIST = "KanteVarBList";
//			private static final String KANTEVARB = "KanteVarB";
//		private static final String KANTEVARCLIST = "KanteVarCList";
//			private static final String KANTEVARC = "KanteVarC";
//		private static final String LADEMASSLIST = "LademassList";
//			private static final String LADEMASS = "Lademass";
//		private static final String LAENDER = "Laender";
			private static final String LAND = "Land";
//		private static final String STRECKENKATEGORIEN = "Streckenkategorien";
			private static final String STRECKENKATEGORIE = "Streckenkategorie";
//		private static final String STRECKENKLASSEN = "Streckenklassen";
//			private static final String STRECKENKLASSE = "Streckenklasse";
//		private static final String STRECKENKSTLIST = "StreckenKSTList";
			private static final String STRECKENKST = "StreckenKST";
//		private static final String STRECKESEPLIST = "StreckeSEPList";
//			private static final String STRECKESEP = "StreckeSEP";
//		private static final String TRAKTIONSWECHSELTYPEN = "TraktionswechselTypen";
//			private static final String TRAKTIONSWECHSELTYP = "TraktionswechselTyp";
//		private static final String ZEITSCHEIBEN = "Zeitscheiben";
//			private static final String ZEITSCHEIBE = "Zeitscheibe";
//		private static final String ZUGGATTUNGEN = "Zuggattungen";
//			private static final String ZUGGATTUNG = "Zuggattung";
//		private static final String ZUGTYPEN = "Zugtypen";
//			private static final String ZUGTYP = "Zugtyp";
//		private static final String ZUGZAHLINTERVALLE = "Zugzahlintervalle";
//			private static final String ZUGZAHLINTERVALL = "Zugzahlintervall";
//		private static final String KNOTENLIST = "KnotenList";
			private static final String KNOTEN = "Knoten";
//		private static final String KANTEN = "Kanten";
			private static final String KANTE = "Kante";
//		private static final String GLEISE = "Gleise";
			private static final String GLEIS = "Gleis";
//		private static final String FAHRTRICHTUNGEN = "Fahrtrichtungen";
			private static final String FAHRTRICHTUNG = "Fahrtrichtung";
//		private static final String ABBIEGEWIDERSTAENDE = "Abbiegewiderstaende";
//			private static final String ABBIEGEWIDERSTAND = "Abbiegewiderstand";
//		private static final String TRAKTIONSWECHSELLIST = "TraktionswechselList";
//		private static final String GRENZKNOTENFOLGEN = "GrenzKnotenfolgen";
//		private static final String STUNDEN = "Stunden";
//			private static final String STUNDE = "Stunde";
//		private static final String MODELLZUEGE = "Modellzuege";
//			private static final String MODELLZUG = "Modellzug";
//		private static final String MODELLZUGVERFUEGBARKEITLIST = "ModellzugverfuegbarkeitList";
//			private static final String MODELLZUGVERFUEGBARKEIT = "Modellzugverfuegbarkeit";
//		private static final String MODELLZUGVORGABELIST = "ModellzugvorgabeList";
//			private static final String MODELLZUGVORGABE = "Modellzugvorgabe";
//		private static final String VERKEHRSARTEN = "Verkehrsarten";
//			private static final String VERKEHRSART = "Verkehrsart";
//		private static final String MARKTSEGMENTE = "Marktsegmente";
//			private static final String MARKTSEGMENT = "Marktsegment";
//		private static final String ZUGAUSWERTUNGSTYPEN = "Zugauswertungstypen";
//			private static final String ZUGAUSWERTUNGSTYP = "Zugauswertungstyp";
//		private static final String ZUGKLASSEN = "Zugklassen";
//			private static final String ZUGKLASSE = "Zugklasse";

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	private final NEMOInfraDataContainer dataContainer;

	private final Set<String> ignoredEntities = new HashSet<String>();
	private final Map<String,Set<String>> ignoredAtts = new HashMap<String, Set<String>>();

	private static final Logger log = Logger.getLogger(NEMOInfraParser.class);
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NEMOInfraParser(NEMOInfraDataContainer dataContainer) {
		super(false);
		this.dataContainer = dataContainer;
	}

	//////////////////////////////////////////////////////////////////////

	/**
	 * @param validateXml
	 */
	public NEMOInfraParser(NEMOInfraDataContainer dataContainer, boolean validateXml) {
		super(validateXml);
		this.dataContainer = dataContainer;
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	private final void updateIgnoredAttributes(final String entity, final Attributes atts, final String [] usedAtts) {
		Set<String> tmpAtts = new HashSet<String>();
		for (int i=0; i<usedAtts.length; i++) { tmpAtts.add(usedAtts[i]); }
		for (int i=0; i<atts.getLength(); i++) {
			if (!tmpAtts.contains(atts.getQName(i))) {
				if (!ignoredAtts.containsKey(entity)) { ignoredAtts.put(entity,new HashSet<String>()); }
				if (!ignoredAtts.get(entity).contains(atts.getQName(i))) {
					ignoredAtts.get(entity).add(atts.getQName(i));
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	
	final Boolean stringToBoolean(String string) {
		if (string.equalsIgnoreCase(Boolean.TRUE.toString()) || string.equalsIgnoreCase(Boolean.FALSE.toString())) {
			return Boolean.valueOf(string);
		}
		return null;
	}

	//////////////////////////////////////////////////////////////////////

	private static final String LAND_ID_LAND = "id_Land";
	private static final String LAND_BEZEICHNUNG = "bezeichnung";
	private static final String LAND_INLAND = "inland";
	
	private final void startLand(final Attributes atts) {
		updateIgnoredAttributes(LAND,atts,new String []{ LAND_ID_LAND, LAND_BEZEICHNUNG, LAND_INLAND });

		int id_Land = Integer.parseInt(atts.getValue(LAND_ID_LAND));
		String bezeichnung = atts.getValue(LAND_BEZEICHNUNG);
		boolean inland = stringToBoolean(atts.getValue(LAND_INLAND));
		
		NEMOInfraCountry country = new NEMOInfraCountry(id_Land);
		country.name = bezeichnung;
		country.isHomeCountry = inland;
		
		if (dataContainer.countries.put(country.id,country) != null) {
			throw new RuntimeException("At Entity '"+LAND+"': A country with id="+id_Land+" exists at least twice. Bailing out.");
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	private static final String KNOTEN_BSCODE = "bscode";
	private static final String KNOTEN_NAME = "name";
	private static final String KNOTEN_XKOORD = "xkoord";
	private static final String KNOTEN_YKOORD = "ykoord";
	private static final String KNOTEN_LAND = "land";
	private static final String KNOTEN_BAHNHOF = "bahnhof";
	private static final String KNOTEN_GUELTIG = "gueltig";
	private static final String KNOTEN_CLUSTER = "cluster";
	
	private final void startKnoten(final Attributes atts) {
		updateIgnoredAttributes(KNOTEN,atts,new String []{ KNOTEN_BSCODE, KNOTEN_NAME, KNOTEN_XKOORD, KNOTEN_YKOORD, KNOTEN_LAND, KNOTEN_BAHNHOF, KNOTEN_GUELTIG, KNOTEN_CLUSTER });

		String bscode = atts.getValue(KNOTEN_BSCODE);
		String name = atts.getValue(KNOTEN_NAME);
		int xkoord = Integer.parseInt(atts.getValue(KNOTEN_XKOORD));
		int ykoord = Integer.parseInt(atts.getValue(KNOTEN_YKOORD));
		int land = Integer.parseInt(atts.getValue(KNOTEN_LAND));
		boolean bahnhof = stringToBoolean(atts.getValue(KNOTEN_BAHNHOF));
		boolean gueltig = stringToBoolean(atts.getValue(KNOTEN_GUELTIG));
		String cluster = atts.getValue(KNOTEN_CLUSTER);
		if (cluster.equals("")) { cluster = "restCluster"; } // assign all node without a cluster to a restCluster (much better that call it "")
		
		NEMOInfraNode node = new NEMOInfraNode(bscode);
		node.name = name;
		node.coord = new Coord((double) xkoord, (double) ykoord);
		node.countryId = Integer.toString(land);
		node.isStation = bahnhof;
		node.isValid = gueltig;
		node.clusterId = cluster;

		if (dataContainer.nodes.put(node.id,node) != null) {
			throw new RuntimeException("At Entity '"+KNOTEN+"': A node with id="+bscode+" exists at least twice. Bailing out.");
		}

		NEMOInfraNodeCluster nodeCluster = dataContainer.nodeClusters.get(node.clusterId);
		if (nodeCluster == null) { 
			nodeCluster = new NEMOInfraNodeCluster(cluster);
			dataContainer.nodeClusters.put(nodeCluster.id,nodeCluster);
		}
		nodeCluster.nodeIds.add(node.id);
	}

	//////////////////////////////////////////////////////////////////////

	private static final String STRECKENKATEGORIE_ID_STRECKENKATEGORIE = "id_Streckenkategorie";
	private static final String STRECKENKATEGORIE_BEZEICHNUNG = "bezeichnung";
	private static final String STRECKENKATEGORIE_VFAKTOR = "vFaktor";
	
	private final void startStreckenkategorie(final Attributes atts) {
		updateIgnoredAttributes(STRECKENKATEGORIE,atts,new String []{ STRECKENKATEGORIE_ID_STRECKENKATEGORIE, STRECKENKATEGORIE_BEZEICHNUNG, STRECKENKATEGORIE_VFAKTOR });

		int id_Streckenkategorie = Integer.parseInt(atts.getValue(STRECKENKATEGORIE_ID_STRECKENKATEGORIE));
		double velocity = Double.parseDouble(atts.getValue(STRECKENKATEGORIE_BEZEICHNUNG).split(" ")[0])/3.6; // format: "[0-9]+ km / h"; extract number and convert to m/s
		double vFaktor = Double.parseDouble(atts.getValue(STRECKENKATEGORIE_VFAKTOR));
		
		NEMOInfraLinkType linkType = new NEMOInfraLinkType(id_Streckenkategorie);
		linkType.velocity = velocity;
		linkType.vFactor = vFaktor;
		
		if (dataContainer.linkTypes.put(linkType.id,linkType) != null) {
			throw new RuntimeException("At Entity '"+STRECKENKATEGORIE+"': A link type with id="+id_Streckenkategorie+" exists at least twice. Bailing out.");
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	private static final String STRECKENKST_ID_STRECKENKST = "id_StreckenKST";
	private static final String STRECKENKST_BEZEICHNUNG = "bezeichnung";
	
	private final void startStreckenKST(final Attributes atts) {
		updateIgnoredAttributes(STRECKENKST,atts,new String []{ STRECKENKST_ID_STRECKENKST, STRECKENKST_BEZEICHNUNG });

		int id_StreckenKST = Integer.parseInt(atts.getValue(STRECKENKST_ID_STRECKENKST));
		String owner = atts.getValue(STRECKENKST_BEZEICHNUNG); // describes the owner of the rail link
		
		NEMOInfraLinkOwner linkOwner = new NEMOInfraLinkOwner(id_StreckenKST);
		linkOwner.owner = owner;
		
		if (dataContainer.linkOwners.put(linkOwner.id,linkOwner) != null) {
			throw new RuntimeException("At Entity '"+STRECKENKST+"': A link ownership with id="+id_StreckenKST+" exists at least twice. Bailing out.");
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	private static final String KANTE_ID_KANTE = "id_Kante";
	private static final String KANTE_VONKNOTEN = "vonKnoten";
	private static final String KANTE_NACHKNOTEN = "nachKnoten";
	private static final String KANTE_STRECKESIMU = "streckeSIMU";
	private static final String KANTE_LAENGE = "laenge";
	private static final String KANTE_ZWEIGLEISIG = "zweigleisig";
	private static final String KANTE_GLOBALBEREICH = "globalbereich";
	private static final String KANTE_STRECKENKATEGORIE = "streckenkategorie";
	private static final String KANTE_STRECKENKST = "streckenKST";
	private static final String KANTE_GESPERRT = "gesperrt";
	private static final String KANTE_GUELTIG = "gueltig";
	private static final String KANTE_ZUGLAENGE_MAX = "zuglaenge_max";

	private final void startKante(final Attributes atts) {
		updateIgnoredAttributes(KANTE,atts,new String []{ KANTE_ID_KANTE, KANTE_VONKNOTEN, KANTE_NACHKNOTEN, KANTE_STRECKESIMU, KANTE_LAENGE, KANTE_ZWEIGLEISIG, KANTE_GLOBALBEREICH, KANTE_STRECKENKATEGORIE, KANTE_STRECKENKST, KANTE_GESPERRT, KANTE_GUELTIG, KANTE_ZUGLAENGE_MAX });

		int id_Kante = Integer.parseInt(atts.getValue(KANTE_ID_KANTE));
		String vonKnoten = atts.getValue(KANTE_VONKNOTEN);
		String nachKnoten = atts.getValue(KANTE_NACHKNOTEN);
		int streckeSIMU = Integer.parseInt(atts.getValue(KANTE_STRECKESIMU));
		if ((streckeSIMU != 0) && (streckeSIMU != 1)) {
			throw new RuntimeException("At Entity '"+KANTE+"': Attribute "+KANTE_STRECKESIMU+"="+streckeSIMU+" not allowed. Bailing out.");
		}
		int laenge = Integer.parseInt(atts.getValue(KANTE_LAENGE));
		if (laenge <= 0) {
			throw new RuntimeException("At Entity '"+KANTE+"': Attribute "+KANTE_LAENGE+"="+streckeSIMU+" not allowed (must be greater than zero). Bailing out.");
		}
		boolean zweigleisig = stringToBoolean(atts.getValue(KANTE_ZWEIGLEISIG));
		boolean globalbereich = stringToBoolean(atts.getValue(KANTE_GLOBALBEREICH));
		int streckenkategorie = Integer.parseInt(atts.getValue(KANTE_STRECKENKATEGORIE));
		int streckenKST = Integer.parseInt(atts.getValue(KANTE_STRECKENKST));
		boolean gesperrt = stringToBoolean(atts.getValue(KANTE_GESPERRT));
		boolean gueltig = stringToBoolean(atts.getValue(KANTE_GUELTIG));
		int zuglaenge_max = Integer.parseInt(atts.getValue(KANTE_ZUGLAENGE_MAX));
		if (zuglaenge_max <= 0) {
			throw new RuntimeException("At Entity '"+KANTE+"': Attribute "+KANTE_ZUGLAENGE_MAX+"="+zuglaenge_max+" not allowed (must be greater than zero). Bailing out.");
		}
		
		NEMOInfraLink link = new NEMOInfraLink(id_Kante);
		link.fromNodeId = Id.create(vonKnoten, Node.class);
		link.toNodeId = Id.create(nachKnoten, Node.class);
		if (streckeSIMU == 0) { link.isSimuLink = false; } else { link.isSimuLink = true; }
		link.length = (double)laenge;
		link.hasTwoTracks = zweigleisig;
		link.isGlobal = globalbereich;
		link.typeId = Integer.toString(streckenkategorie);
		link.ownerId = Integer.toString(streckenKST);
		link.isClosed = gesperrt;
		link.isValid = gueltig;
		link.maxTrainLength = (double)zuglaenge_max;
		
		if (dataContainer.links.put(link.id,link) != null) {
			throw new RuntimeException("At Entity '"+KANTE+"': A link id="+id_Kante+" exists at least twice. Bailing out.");
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	private static final String GLEIS_KANTE = "kante";
	private static final String GLEIS_GLEISNR = "gleisnr";

	private final void startGleis(final Attributes atts) {
		updateIgnoredAttributes(GLEIS,atts,new String []{ GLEIS_KANTE, GLEIS_GLEISNR });

		int kante = Integer.parseInt(atts.getValue(GLEIS_KANTE));
		boolean gleisnr = stringToBoolean(atts.getValue(GLEIS_GLEISNR));
		
		NEMOInfraTrack track = new NEMOInfraTrack(kante,gleisnr);
		
		if (dataContainer.tracks.put(track.id,track) != null) {
			throw new RuntimeException("At Entity '"+GLEIS+"': A track with linkId-trackNr-tuple=("+kante+","+gleisnr+") exists at least twice. Bailing out.");
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	private static final String FAHRTRICHTUNG_KANTE = "kante";
	private static final String FAHRTRICHTUNG_GLEISNR = "gleisnr";
	private static final String FAHRTRICHTUNG_RICHTUNG = "richtung";

	private final void startFahrtrichtung(final Attributes atts) {
		updateIgnoredAttributes(FAHRTRICHTUNG,atts,new String []{ FAHRTRICHTUNG_KANTE, FAHRTRICHTUNG_GLEISNR, FAHRTRICHTUNG_RICHTUNG });

		int kante = Integer.parseInt(atts.getValue(FAHRTRICHTUNG_KANTE));
		boolean gleisnr = stringToBoolean(atts.getValue(FAHRTRICHTUNG_GLEISNR));
		boolean richtung = stringToBoolean(atts.getValue(FAHRTRICHTUNG_RICHTUNG));
		
		NEMOInfraDirection direction = new NEMOInfraDirection(kante,gleisnr,richtung);
		
		if (dataContainer.directions.put(direction.id, direction) != null) {
			throw new RuntimeException("At Entity '"+FAHRTRICHTUNG+"': A traffic direction with linkId-trackNr-direction-triple=("+kante+","+gleisnr+","+richtung+") exists at least twice. Bailing out.");
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// interface implementations
	//////////////////////////////////////////////////////////////////////
	
	/* (non-Javadoc)
	 * @see org.matsim.core.utils.io.MatsimXmlParser#startTag(java.lang.String, org.xml.sax.Attributes, java.util.Stack)
	 */
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (LAND.equals(name)) { startLand(atts); }
		else if (KNOTEN.equals(name)) { startKnoten(atts); }
		else if (STRECKENKATEGORIE.equals(name)) { startStreckenkategorie(atts); }
		else if (STRECKENKST.equals(name)) { startStreckenKST(atts); }
		else if (KANTE.equals(name)) { startKante(atts); }
		else if (GLEIS.equals(name)) { startGleis(atts); }
		else if (FAHRTRICHTUNG.equals(name)) { startFahrtrichtung(atts); }
		else {
			if (!ignoredEntities.contains(name)) { ignoredEntities.add(name); }
		}
	}

	//////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.matsim.core.utils.io.MatsimXmlParser#endTag(java.lang.String, java.lang.String, java.util.Stack)
	 */
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (NEMOINFRA.equals(name)) {
			log.info("--- START: list of ignored entities ---");
			for (String entity : ignoredEntities) { log.info(entity); }
			log.info("--- END:   list of ignored entities ---");
			ignoredEntities.clear();
			log.info("--- START: list of ignored attributes ---");
			for (Entry<String,Set<String>> e : ignoredAtts.entrySet()) { log.info(e.getKey()+": "+e.getValue().toString()); }
			log.info("--- END:   list of ignored attributes ---");
			ignoredAtts.clear();
			
			boolean isValid = true;
			boolean isValidTmp = true;
			log.info("--- START: validation ---");
			isValidTmp = dataContainer.validateCountries(); log.info("countries: isValid="+isValidTmp); if (!isValidTmp) { isValid = false; }
			isValidTmp = dataContainer.validateLinkTypes(); log.info("link types: isValid="+isValidTmp); if (!isValidTmp) { isValid = false; }
			isValidTmp = dataContainer.validateLinkOwners(); log.info("link owners: isValid="+isValidTmp); if (!isValidTmp) { isValid = false; }
			isValidTmp = dataContainer.validateNodeClusters(); log.info("node clusters: isValid="+isValidTmp); if (!isValidTmp) { isValid = false; }
			isValidTmp = dataContainer.validateNodes(); log.info("nodes: isValid="+isValidTmp); if (!isValidTmp) { isValid = false; }
			isValidTmp = dataContainer.validateLinks(); log.info("links: isValid="+isValidTmp); if (!isValidTmp) { isValid = false; }
			isValidTmp = dataContainer.validateTracks(); log.info("tracks: isValid="+isValidTmp); if (!isValidTmp) { isValid = false; }
			isValidTmp = dataContainer.validateDirections(); log.info("directions: isValid="+isValidTmp); if (!isValidTmp) { isValid = false; }
			log.info("--- End:   validation ---");
			
			if (!isValid) { throw new RuntimeException(NEMOINFRA+" data validation failed. Bailing out."); }
		}
	}
}
