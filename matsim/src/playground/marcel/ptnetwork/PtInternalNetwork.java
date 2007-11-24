/* *********************************************************************** *
 * project: org.matsim.*
 * PtInternalNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.marcel.ptnetwork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.utils.misc.QuadTree;
import org.matsim.utils.geometry.shared.Coord;

import playground.marcel.ptnetwork.tempelements.TempFZP;
import playground.marcel.ptnetwork.tempelements.TempFZPPkt;
import playground.marcel.ptnetwork.tempelements.TempHP;
import playground.marcel.ptnetwork.tempelements.TempHb;
import playground.marcel.ptnetwork.tempelements.TempLine;
import playground.marcel.ptnetwork.tempelements.TempLink;
import playground.marcel.ptnetwork.tempelements.TempRP;
import playground.marcel.ptnetwork.tempelements.TempRoute;
import playground.marcel.ptnetwork.tempelements.TempTrip;

public class PtInternalNetwork {

	private static final String VTYPE_BUS = "B";
	private static final String VTYPE_TRAM = "T";
	private static final String VTYPE_UBAHN = "U";
	private static final String VTYPE_FAEHRE = "F";

	private static final int INITIAL_CHANGE_COST = 1;

	private int idCnt=1000;


	TreeMap<String, TempLine> lines = new TreeMap<String, TempLine>();
	ArrayList<TempHb> hbs = new ArrayList<TempHb>();
	ArrayList<TempHP> pedNodes = new ArrayList<TempHP>();


	public int getIdCnt(){
		return this.idCnt;
	}

	/**
	 * method searches given folder for xml files in BERTA format
	 * and builds an internal network
	 * NOTE: hbs must be build separately
	 * @param folder directory containing BERTA XML-files
	 * @param idCount
	 */
	public void buildInternalNetwork(final String folder, final int idCount) {
		this.idCnt=idCount;

		System.out.println("INTERNALNET: Reading Network from XMLs in Folder: "+folder);

		this.lines.clear();

		File dir = new File(folder);
		String[] files = dir.list();

		for (int i=0;i< files.length;i++){

			File lineXML = new File(dir.getAbsoluteFile(), files[i]);

			PtNetworkReader reader = new PtNetworkReader();

//			1 file contains data for 1 line
//			thus after reading and validating a file the line constructor is called

			try{
				reader.read(lineXML.getAbsolutePath(),this.idCnt);
			}catch(Exception e){
				e.printStackTrace();
			}

			if (!reader.isValid()) {
				System.out.println("INTERNALNET: lineData of " + reader.getLineName() + " is invalid!");
			} else if (reader.hps.size() < 1) {
				System.out.println("INTERNALNET: lineData of " + reader.getLineName() + " has no Haltepunkte!");
			} else if (reader.getVType().equals(VTYPE_FAEHRE)) {
				System.out.println("INTERNALNET: lineData of " + reader.getLineName() + " has VType = F!");
			} else {
				reader.finishNetwork();
				TempLine line = new TempLine(reader);
				this.lines.put(line.getName(), line);
				this.idCnt=reader.getIdCnt();
				System.out.println("INTERNALNET: Correctly read: "+line.getName());
			}
		}
		System.out.println("INTERNALNET: Reading Network DONE!");
	}

	/**
	 * Method writes the internalnetwork data to a VISUM *.net file
	 * @param fileName name of generated VISUM file
	 */
	public void writeToVISUM(final String fileName){
		System.out.println("INTERNALNET: Writing to Visum-File: "+fileName);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(fileName));
			out.write("$VISION\n*Technische Universit�t Berlin\n*25.05.2006\n*\n*\n*Tabelle:Versionsblock\n$VERSION:VERSNR;FILETYPE;LANGUAGE;UNIT\n3.000;Net;D;KM\n\n");
			out.write("*\n*\n*Tabelle: Notizblock\n$INFO:INDEX;TEXT\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Netzparameter\n$NETZPARAMETER:NETVERSNR;NETVERSNAME;MASSSTAB;LINKSVERKEHR;KOORDDEZ;LANGELAENGENDEZ;KURZELAENGENDEZ;GESCHWDEZ;DEZTRENNER;ERZMODUSNSEG;VISATTRRECHT\n1.000;;1.000;0;4;3;0;0;.;1;INVISIBLE\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Kalendertage\n$KALENDERPERIODE:NR;CODE;NAME;TYP;GUELTIGVON;GUELTIGBIS\n1;;;KEINKALENDER;07.11.2005;07.11.2005\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Verkehrstage\n$VERKEHRSTAG:NR;CODE;NAME;VONSYS;HFAKSTDKOST;HFAKANGEBOT;VTAGE\n1;t�gl.;t�glich;0;365.000;365.000;1\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Ferientage\n$FERIENTAGE:NR;CODE;NAME;GUELTIGBIS;GUELTIGVON;VTAGE\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("POI-Kategorien\n$POIKATEGORIE:NR;CODE;NAME;KOMMENTAR;OBERKATNR\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Benutzerdefinierte Attribute\n$USERATTDEF:OBJID;ATTID;CODE;NAME;DATENTYP;SPALTENSUMME;SPALTENMITTEL;SPALTENMINMAX;WERTMIN;WERTMAX;WERTSTANDARD;STRINGWERTSTANDARD;KOMMENTAR;MAXSTRINGLAENGE;NACHKOMMASTELLEN;DUENN;ACCESSRIGHT\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Definition befristeter Attribute\n$BEFRISTETEATTRIBUTE:OBJID;ATTID\n\n");
			out.write("*\n*\n*Tabelle: ");


			//			add s-Bahn

			out.write("Verkehrssysteme\n$VSYS:CODE;NAME;PKWE;TYP\nB;Bus;1.000;OV\nF;Fuss;1.000;OVFuss\nU;U-Bahn;1.000;OV\nT;Tram;1.000;OV\nS;S-Bahn;1.000;OV\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Modi\n$MODUS:CODE;NAME;VSYSSET;CGLADTYP;CGSTDVSYS;CGPRODTYP\nX;�V;B,F,U,T,S;; ;\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Nachfragesegmente\n$NACHFRAGESEGMENT:CODE;NAME;MODUS;BGRAD;HRFAKAP;HRFAKAH\nX;�V;X;1.000;1.000;365.000\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Fahrzeugeinheiten\n$FZGEINHEIT:NR;CODE;NAME;VSYSSET;TRIEBFZG;SITZPL;GESAMTPL;KOSTENSATZSTDSERVICE;KOSTENSATZSTDLEER;KOSTENSATZKMSERVICE;KOSTENSATZKMLEER;KOSTENSATZFZGEINHEIT;BEZUGSZEIT\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Fahrzeugkombinationen\n$FZGKOMB:NR;CODE;NAME;KOSTENSATZSTDSERVICE;KOSTENSATZSTDLEER;KOSTENSATZKMSERVICE;KOSTENSATZKMLEER\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("FzgEinheit zu FzgKomb\n$FZGEINHEITZUFZGKOMB:FZGKOMBNR;FZGEINHEITNR;ANZFZGEINH\n\n");
			out.write("*\n*\n*Tabelle: ");

//			add readfromdata method

			out.write("Richtungen\n$RICHTUNG:NR;CODE;NAME\n1;1;Hinrichtung\n2;2;R�ckrichtung\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Punkte\n$PUNKT:ID;XKOORD;YKOORD\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Kanten\n$KANTE:ID;VONPUNKTID;NACHPUNKTID\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Zwischenpunkte\n$ZWISCHENPUNKT:KANTEID;INDEX;XKOORD;YKOORD\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Teilfl�chen\n$TEILFLAECHE:ID\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Teilfl�chenelemente\n$TEILFLAECHENELEMENT:TFLAECHEID;INDEX;KANTEID;RICHTUNG\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Fl�chen\n$FLAECHE:ID\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Fl�chenelemente\n$FLAECHENELEMENT:FLAECHEID;TFLAECHEID;ENKLAVE\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Oberknoten\n$OBERKNOTEN:NR;CODE;NAME;ZWERT1;ZWERT2;ZWERT3\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Knoten\n$KNOTEN:NR;CODE;NAME;TYPNR;STEUERTYP;XKOORD;YKOORD;OBERKNOTNR;ZWERT1;ZWERT2;ZWERT3;T0IV;KAPSTDIV;FSDEF;LOS;ISTCBD;SNEAKERS;OPTLSA;LOSMWZ;MAXUMLZEITOPT;MINUMLZEITOPT;PHFVOLADJ\n");
			for (TempLine tmpline : this.lines.values()) {
				for (TempHP tmphp : tmpline.hps) {
					int vt=9;
					if(tmpline.getVType().equals(VTYPE_BUS)){
						vt=1;
					}else if(tmpline.getVType().equals(VTYPE_UBAHN)){
						vt=2;
					}else if(tmpline.getVType().equals(VTYPE_TRAM)){
						vt=3;
					}else if(tmpline.getVType().equals(VTYPE_FAEHRE)){
						vt=4;
					}
					out.write(tmphp.getHp_Id()+";"+tmpline.getName()+";"+tmphp.getName()+";"+vt+";0;"+tmphp.getCoord().getX()+";"+tmphp.getCoord().getY()+";0;0;0;1;0s;1000;0;;0;0.000;1;0s;1.000;1.000;1.000\n");
				}
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Oberbezirke\n$OBERBEZIRK:NR;CODE;NAME;TYPNR;XKOORD;YKOORD;FLAECHEID;ZWERT1;ZWERT2;ZWERT3\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Bezirke\n$BEZIRK:NR;CODE;NAME;TYPNR;XKOORD;YKOORD;FLAECHEID;ANTEIL_Q(IV);ANTEIL_Q(OV);ANTEIL_Z(IV);ANTEIL_Z(OV);ZWERT1;ZWERT2;ZWERT3;OBEZNR\n\n");
			out.write("*\n*\n*Tabelle: ");
			out.write("Streckentypen\n$STRECKENTYP:NR;NAME;RANG;VSYSSET;ANZFAHRSTREIFEN;KAPIV;V0IV;VMINIV;KOSTENSATZ1-OEVSYS(B);KOSTENSATZ2-OEVSYS(B);KOSTENSATZ3-OEVSYS(B);VSTD-OEVSYS(U);VSTD-OEVSYS(T);VSTD-OEVSYS(B);VSTD-OEVSYS(F)\n");
			for(int i=0;i<100;i++){
				out.write(""+i+";;1;B,F,T,U,S;1;99999;50.000;0.000;0.000;0.000;0.000;70.000;50.000;30.000;4.000\n");
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Strecken\n$STRECKE:NR;VONKNOTNR;NACHKNOTNR;NAME;PLANNR;TYPNR;VSYSSET;LAENGE;TMODELSPEZIAL;ANZFAHRSTREIFEN;KAPIV;V0IV;T-OEVSYS(B);T-OEVSYS(F);T-OEVSYS(T);T-OEVSYS(U);ZWERT1;ZWERT2;ZWERT3;ZWERT-VSYS(B);ZWERT-VSYS(F);ZWERT-VSYS(U);ZWERT-VSYS(T);KOSTENSATZ1-OEVSYS(B);KOSTENSATZ2-OEVSYS(B);KOSTENSATZ3-OEVSYS(B);ANZTARIFP-VSYS(B);EWSTYP;EWSKLASSE;EINWOHNER;BAUHOEHE;BAUART;ABSTANDBAU;GEHWEGBREITE;ZIELGEHWEGBREITE;RADWEGBREITE;ZIELRADWEGBREITE;ANTEILLKW;STEIGUNG;KURVIGKEIT;FLAECHENTYP;LAERMIMMISHOEHE;BALKENTEXTAN;BALKENTEXTRELPOS;TRAFFIXARRIVALTYPE\n");
			for (TempLine tmpline : this.lines.values()) {
				for (TempLink tmplink : tmpline.links) {
					int vt=9;
					if(tmpline.getVType().equals(VTYPE_BUS)){
						vt=1;
					}else if(tmpline.getVType().equals(VTYPE_UBAHN)){
						vt=2;
					}else if(tmpline.getVType().equals(VTYPE_TRAM)){
						vt=3;
					}else if(tmpline.getVType().equals(VTYPE_FAEHRE)){
						vt=4;
					}
					out.write(tmplink.linkID+";"+tmplink.fromNodeID+";"+tmplink.toNodeID+";;0;"+vt+";"+tmpline.getVType()+";"+tmplink.length+";0;1;99999;50.000;0s;0s;0s;0s;0;0;0;0;0;0;0;0.000;0.000;0.000;0;111;3;0;0.000;0;0.000;0.000;0;0.000;0;0;0;0;1;2.250;1;0.500;3\n");
				}
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Haltestellen\n$HALTESTELLE:NR;CODE;NAME;TYPNR;XKOORD;YKOORD;ZWERT1;ZWERT2;ZWERT3\n");
			for (TempHb hb : this.hbs) {
				out.write(hb.ID+";;"+hb.name+";;"+hb.coord.getX()+";"+hb.coord.getY()+";0;0;0\n");
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Haltestellenbereiche\n$HALTESTELLENBEREICH:NR;HSTNR;CODE;KNOTNR;NAME;TYPNR;XKOORD;YKOORD;ZWERT1;ZWERT2;ZWERT3\n");
			for (TempHb hb : this.hbs) {
				out.write(hb.ID+";"+hb.ID+";;;"+hb.name+";;"+hb.coord.getX()+";"+hb.coord.getY()+";0;0;0\n");
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Haltepunkte\n$HALTEPUNKT:NR;HSTBERNR;CODE;NAME;GERICHTET;TYPNR;VSYSSET;STDHALTEZEIT;KNOTNR;VONKNOTNR;STRNR;RELPOS;ZWERT1;ZWERT2;ZWERT3;KOSTENSATZ1;KOSTENSATZ2;KOSTENSATZ3\n");
			for (TempLine tmpline : this.lines.values()) {
				for (TempHP tmphp : tmpline.hps) {
					if(tmphp.getHb_Id()!=null){
						int vt=9;
						if(tmpline.getVType().equals(VTYPE_BUS)){
							vt=1;
						}else if(tmpline.getVType().equals(VTYPE_UBAHN)){
							vt=2;
						}else if(tmpline.getVType().equals(VTYPE_TRAM)){
							vt=3;
						}else if(tmpline.getVType().equals(VTYPE_FAEHRE)){
							vt=4;
						}
						out.write(tmphp.getHp_Id()+";"+tmphp.getHb_Id()+";;"+tmphp.getName()+";1;"+vt+";"+tmpline.getVType()+";0s;"+tmphp.getHp_Id()+";;;0.000;0;0;0;0.000;0.000;0.000\n");
					}
				}
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Linien\n$LINIE:NAME;VSYSCODE;FZGKOMBNR;BETREIBERNR;OLINNAME;ZWERT1;ZWERT2;ZWERT3\n");
			for (TempLine tmpline : this.lines.values()) {
				out.write(tmpline.getName()+";"+tmpline.getVType()+";0;0;;0;0;0\n");
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Linienrouten\n$LINIENROUTE:LINNAME;NAME;RICHTUNGCODE;ISTRINGLINIE;ZWERT1;ZWERT2;ZWERT3\n");
			for (TempLine tmpline : this.lines.values()) {
				for (TempRoute route : tmpline.routes) {
					out.write(tmpline.getName()+";"+route.id+";"+route.direct+";0;0;0;0\n");
				}
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Linienrouten-Verl�ufe\n$LINIENROUTENELEMENT:LINNAME;LINROUTENAME;RICHTUNGCODE;INDEX;KNOTNR;HPUNKTNR;ISTROUTENPUNKT;NACHLAENGE;HALTLAENGE;ZWERT\n");
			for (TempLine tmpline : this.lines.values()) {
				out.flush();
				for (TempRoute route : tmpline.routes) {
					TempFZP fzp = route.fzps.get(0);
					for(int i=0; i<fzp.pkte.size();i++){
							out.write(tmpline.getName()+";"+route.id+";"+route.direct+";"+(i+1)+";"+fzp.pkte.get(i).hp_Id+";"+fzp.pkte.get(i).hp_Id+";1;0.000;0.000;0\n");
						}
				}
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Fahrzeitprofile\n$FAHRZEITPROFIL:LINNAME;LINROUTENAME;RICHTUNGCODE;NAME;FZGKOMBNR;ISTDYN;REFELEMINDEX;FIXREFABF\n");
			for (TempLine tmpline : this.lines.values()) {
				out.flush();
				for (TempRoute route : tmpline.routes) {
					for (TempFZP fzp : route.fzps) {
						out.write(tmpline.getName()+";"+route.id+";"+route.direct+";"+fzp.id+";0;0;0;1\n");
					}
				}
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Fahrzeitprofil-Verl�ufe\n$FAHRZEITPROFILELEMENT:LINNAME;LINROUTENAME;RICHTUNGCODE;FZPROFILNAME;INDEX;AUS;EIN;ANKUNFT;ABFAHRT;LRELEMINDEX;ZWERT\n");
			for (TempLine tmpline : this.lines.values()) {
				out.flush();
				for (TempRoute route : tmpline.routes) {
					for (TempFZP fzp : route.fzps) {
						out.write(tmpline.getName()+";"+route.id+";"+route.direct+";"+fzp.id+";1;0;1;00:00:00;00:00:00;1;0\n");
						int ttime=0;
						for (int i = 1; i < (fzp.pkte.size() - 1); i++) {
							ttime += fzp.pkte.get(i).ttime;
							out.write(tmpline.getName() + ";" + route.id + ";" + route.direct + ";"
									+ fzp.id + ";" + (i + 1) + ";1;1;"
									+ Gbl.writeTime(ttime, Gbl.TIMEFORMAT_HHMMSS) + ";"
									+ Gbl.writeTime(fzp.pkte.get(i).wtime + ttime, Gbl.TIMEFORMAT_HHMMSS)
									+ ";" + (i + 1) + ";0\n");
							ttime += fzp.pkte.get(i).wtime;
						}
						ttime+=fzp.pkte.get(fzp.pkte.size()-1).ttime;
						if (fzp.pkte.size() == 1) {
							out.write(tmpline.getName() + ";" + route.id + ";" + route.direct + ";"
									+ fzp.id + ";2;1;0;"
									+ Gbl.writeTime(ttime, Gbl.TIMEFORMAT_HHMMSS) + ";"
									+ Gbl.writeTime(fzp.pkte.get(fzp.pkte.size() - 1).wtime + ttime, Gbl.TIMEFORMAT_HHMMSS) + ";"
									+ (fzp.pkte.size()) + ";0\n");
						} else {
							out.write(tmpline.getName() + ";" + route.id + ";" + route.direct + ";"
									+ fzp.id + ";" + fzp.pkte.size() + ";1;0;"
									+ Gbl.writeTime(ttime, Gbl.TIMEFORMAT_HHMMSS) + ";"
									+ Gbl.writeTime(fzp.pkte.get(fzp.pkte.size() - 1).wtime + ttime, Gbl.TIMEFORMAT_HHMMSS) + ";"
									+ (fzp.pkte.size()) + ";0\n");
						}
					}
				}
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Servicefahrten\n$FZGFAHRT:NR;NAME;ABFAHRT;LINNAME;LINROUTENAME;RICHTUNGCODE;FZPROFILNAME;BETREIBERNR;VONFZPELEMINDEX;NACHFZPELEMINDEX;SZENARIOAKTIV;ZWERT1;ZWERT2;ZWERT3\n");
			long tripcnt = 1;
			for (TempLine tmpline : this.lines.values()) {
				out.flush();
				for (TempRoute route : tmpline.routes) {
					for (TempTrip trip : route.trips) {
						if (trip.fzp != null) {
							if(trip.fzp.pkte.size()==1){
								out.write(tripcnt+";;"+Gbl.writeTime(trip.deptime, Gbl.TIMEFORMAT_HHMMSS)+";"+tmpline.getName()+";"+route.id+";"+route.direct+";"+trip.fzp.id+";0;1;2;1;0;0;0\n");
							}else{
								out.write(tripcnt+";;"+Gbl.writeTime(trip.deptime, Gbl.TIMEFORMAT_HHMMSS)+";"+tmpline.getName()+";"+route.id+";"+route.direct+";"+trip.fzp.id+";0;1;"+trip.fzp.pkte.size()+";1;0;0;0\n");
							}
							trip.id=tripcnt;
							tripcnt++;
						}

					}
				}
			}
			out.write("\n*\n*\n*Tabelle: ");
			out.write("Servicefahrtabschnitte\n$FZGFAHRTABSCHNITT:FZGFAHRTNR;NR;VTAGNR;FZGKOMBNR;VORBZEIT;NACHBZEIT;VONFZPELEMINDEX;NACHFZPELEMINDEX\n");
			for (TempLine tmpline : this.lines.values()) {
				out.flush();
				for (TempRoute route : tmpline.routes) {
					for (TempTrip trip : route.trips) {
						if(trip.fzp!=null){
							if(trip.fzp.pkte.size()==1){
								out.write(trip.id+";1;1;0;0s;0s;1;2\n");
							}else{
								out.write(trip.id+";1;1;0;0s;0s;1;"+trip.fzp.pkte.size()+"\n");
							}
						}

					}
				}
			}
		} catch(IOException e){
				e.printStackTrace();
		}
		try {
			if (out != null) out.close();
		} catch(IOException e1){
			e1.printStackTrace();
		}

		System.out.println("INTERNALNET: Writing to Visum-File DONE!");

	}

	/**
	 * method reads a specified VISUM *.net file
	 * NOTE: VISUM-file must be generated with writeToVISUM() to work properly
	 * @param netfile name of VISUM-file
	 */
	public void parseVISUMNetwork(final String netfile) {
		/* _TODO split this huge method into several smaller, e.g.:
		 * parseVisumNodes();
		 * parseVisumLinks();
		 * parseVisumHbs();
		 * parseVisumHps();
		 * parseVisumRoutes();
		 * etc.
		 */
		System.out.println("INTERNALNET: Reading Network from Visum-File: "+netfile);
		String tmp;
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(netfile));

			while((tmp=rdr.readLine()) !=null){

				if(tmp.startsWith("$KNOTEN:NR;")){
					System.out.println("parsing nodes,lines");

					while(!("".equals(tmp=rdr.readLine()))){

						String[] data = tmp.split(";");
						if(getTempLine(data[1])!=null){
							TempLine line =getTempLine(data[1]);
							TempHP hp =new TempHP();
							hp.line=line;
							hp.setHp_Id(data[0]);
							hp.setName(data[2]);
							hp.setCoord(Double.parseDouble(data[5]), Double.parseDouble(data[6]));
							hp.setDirect(data[0].charAt(data[0].length()-1));
							line.hps.add(hp);
						}else{
							TempLine line = new TempLine();
							line.setName(data[1]);
							line.setVType(line.getName().substring(0, 1));
							TempHP hp =new TempHP();
							hp.setHp_Id(data[0]);
							hp.setName(data[2]);
							hp.setCoord(Double.parseDouble(data[5]), Double.parseDouble(data[6]));
							hp.setDirect(data[0].charAt(data[0].length()-1));
							hp.line=line;
							line.hps.add(hp);
							this.lines.put(line.getName(), line);
						}
					}
					System.out.println("nodes done");
				}

				if(tmp != null && tmp.startsWith("$STRECKE:NR;")){
					System.out.println("parsing links");
					while(!("".equals(tmp=rdr.readLine()))){

						String[] data = tmp.split(";");
						TempLink link = new TempLink(data[0]);
						link.fromNodeID=data[1];
						link.toNodeID=data[2];
						String[] l=data[7].split("\\.");
						link.length=Integer.parseInt(l[0]);
						for (TempLine line : this.lines.values()) {
							TempHP hp1;
							TempHP hp2;
							if(((hp1=line.getHP(link.fromNodeID))!=null)&&((hp2=line.getHP(link.toNodeID))!=null)){

								hp1.outLinks.add(link);
								hp2.inLinks.add(link);
								link.fromNode=hp1;
								link.toNode=hp2;
								link.line=line;
								line.links.add(link);
							}
						}
					}
					System.out.println("links done");
				}
				if(tmp != null && tmp.startsWith("$HALTESTELLENBEREICH:NR;")){
					System.out.println("parsing hbs");
					while (!("".equals(tmp=rdr.readLine()))) {

						String[] data = tmp.split(";");
						TempHb hb = new TempHb(data[0],data[6],data[7]);
						hb.name=data[4];
						this.hbs.add(hb);
					}
					System.out.println("hbs done");
				}
				if(tmp != null && tmp.startsWith("$HALTEPUNKT:NR;")){
					System.out.println("referencing hps to hbs");
					while (!("".equals(tmp=rdr.readLine()))) {

						String[] data = tmp.split(";");
						for (TempLine line : this.lines.values()) {
							TempHb hb;
							TempHP hp;
							if(((hp=line.getHP(data[0]))!=null)&&((hb=getTempHb(data[1]))!=null)){
									hp.hb=hb;
									hp.setHb_Id(hb.ID);
									hb.hps.add(hp);

							}
						}
					}
					System.out.println("referencing done");

				}

				if(tmp != null && tmp.startsWith("$LINIENROUTE:LINNAME;")){
					System.out.println("parsing routes");
					while (!("".equals(tmp=rdr.readLine()))) {
						String[] data = tmp.split(";");
						TempLine line;
						if((line=getTempLine(data[0]))!=null){
								TempRoute route = new TempRoute(data[1],Integer.parseInt(data[2]));
								route.line=line;
								line.routes.add(route);
						}
					}
					System.out.println("routes done");
				}

				if(tmp != null && tmp.startsWith("$LINIENROUTENELEMENT:LINNAME;")){
					System.out.println("parsing route elements");
					while (!("".equals(tmp=rdr.readLine()))) {
						String[] data = tmp.split(";");
						TempLine line;
						TempRoute route;
						if(((line=getTempLine(data[0]))!=null)){
							if((route=line.getTempRoute(data[1]))!=null){
								if(route.direct==Integer.parseInt(data[2])){
									TempRP rp = new TempRP((Integer.parseInt(data[3])-1),data[4]);
									rp.hp=line.getHP(rp.hp_Id);
									route.rps.add(rp);

								}
							}
						}

					}
					System.out.println("route elements done");
				}

				if(tmp != null && tmp.startsWith("$FAHRZEITPROFILELEMENT:LINNAME;")){
					System.out.println("parsing fzp");
					while (!("".equals(tmp=rdr.readLine()))) {

						String[] data = tmp.split(";");
						int data7time = (int)Gbl.parseTime(data[7]);
						int data8time = (int)Gbl.parseTime(data[8]);

						TempLine line;
						TempRoute route;
						TempFZP fzp;
						if (((line=getTempLine(data[0]))!=null)
								&& ((route=line.getTempRoute(data[1]))!=null)
								&& (route.direct==Integer.parseInt(data[2]))
								) {
							if ((fzp=route.getTempFZP(data[3]))!=null) {
								TempFZPPkt pkt = new TempFZPPkt();
								pkt.pos=(Integer.parseInt(data[4]))-1;
								pkt.wtime= data8time - data7time;
								pkt.routetime = data8time;

								pkt.ttime = data7time - fzp.pkte.get(pkt.pos-1).routetime;
								TempRP rp;
								if((rp=route.getTempRP(pkt.pos))!=null){
									pkt.hp_Id=rp.hp_Id;
									pkt.hp=rp.hp;
								}
								fzp.pkte.add(pkt.pos,pkt);
							} else {
								fzp = new TempFZP(data[3]);
								TempFZPPkt pkt = new TempFZPPkt();
								pkt.pos=0;
								pkt.wtime= data8time - data7time;
								pkt.routetime = data8time;
								pkt.ttime=0;

								TempRP rp;
								if((rp=route.getTempRP(pkt.pos))!=null){
									pkt.hp_Id=rp.hp_Id;
									pkt.hp=rp.hp;
								}
								fzp.pkte.add(pkt.pos,pkt);

								route.fzps.add(fzp);
							}
						}
					}
					System.out.println("fzps done");
				}
				if(tmp != null && tmp.startsWith("$FZGFAHRT:NR;")){
					System.out.println("parsing trips");
					while (!("".equals(tmp=rdr.readLine()))) {
						String[] data = tmp.split(";");
						TempLine line = this.lines.get(data[3]);
						for (TempRoute route : line.routes) {
							if ((route.id.equals(data[4]))
									&& (route.direct==Integer.parseInt(data[5]))) {
								TempTrip trip = new TempTrip(Long.parseLong(data[0]), (int)Gbl.parseTime(data[2]),data[6]);
								route.trips.add(trip);
								trip.fzp=route.getTempFZP(trip.fzpid);
							}
						}
					}
				System.out.println("trips done");
				}
			}
		} catch (FileNotFoundException e) {
			Gbl.errorMsg(e);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		} finally {
			if (rdr != null) {
				try { rdr.close(); } catch (IOException ignored) {}
			}
		}

		System.out.println("building links");
		for (TempLine line : this.lines.values()) {
			for (TempRoute route : line.routes) {
				for (TempTrip trip : route.trips) {
					int tempdtime=trip.deptime;
					for (TempFZP fzp : route.fzps) {
						if (fzp.id.equals(trip.fzpid)){
							for (int i=0; i<fzp.pkte.size(); i++){
								tempdtime+=(fzp.pkte.get(i).ttime+fzp.pkte.get(i).wtime);
								if (fzp.pkte.size()>(i+1)) {
									for (TempLink link : line.links) {
										if (link.fromNodeID.equals(fzp.pkte.get(i).hp_Id)
												&& (link.toNodeID.equals(fzp.pkte.get(i+1).hp_Id))
												){
											link.departures.put(Integer.valueOf(tempdtime), Integer.valueOf(fzp.pkte.get(i).ttime));
										}
									}
								}
							}
						}
					}
				}
			}
		}
		System.out.println("building links done");

		System.out.println("INTERNALNET: Reading from Visum-File DONE!");

	}

	/**
	 * method reads a VISUM *.net file and adds it to existing internal network data
	 * @param netfile name of VISUM-file
	 * @param idCount
	 */
	public void addVISUMNetwork(final String netfile, final int idCount) {
		this.idCnt=idCount;

		System.out.println("INTERNALNET: Adding Visum-Network  from "+netfile+" to existing Network");

		ArrayList<TempHP> temphps = new ArrayList<TempHP>();
		ArrayList<TempLink> templinks = new ArrayList<TempLink>();

		String tmp;
		try {
			BufferedReader rdr = new BufferedReader(new FileReader(netfile));

			while((tmp=rdr.readLine()) != null){

				if(tmp.startsWith("$KNOTEN:NR;")){
					System.out.println("parsing nodes");
					String[] data1 = tmp.split(";");

					int colname = -1;
					int colx = -1;
					int coly = -1;

					for(int i = 0; i<data1.length;i++){
						if(data1[i].equals("NAME")){
							colname=i;
						}
						if(data1[i].equals("XKOORD")){
							colx=i;
						}
						if(data1[i].equals("YKOORD")){
							coly=i;
						}

					}

					while (!("".equals(tmp=rdr.readLine()))) {

						String[] data = tmp.split(";");


						TempHP hp =new TempHP();
						hp.setHp_Id("temp");
						hp.setName(data[colname]);
						hp.setCoord(Double.parseDouble(data[colx]), Double.parseDouble(data[coly]));
						hp.oldIDs.add(data[0]);
						temphps.add(hp);

					}
					System.out.println("nodes done");
				}

				if(tmp.startsWith("$HALTESTELLENBEREICH:NR;")){
					System.out.println("parsing hbs");
					String[] data1 = tmp.split(";");

					int colname = -1;
					int colx = -1;
					int coly = -1;

					for(int i = 0; i<data1.length;i++){
						if(data1[i].equals("NAME")){
							colname=i;
						}
						if(data1[i].equals("XKOORD")){
							colx=i;
						}
						if(data1[i].equals("YKOORD")){
							coly=i;
						}

					}
					while (!("".equals(tmp=rdr.readLine()))) {
						String[] data = tmp.split(";");
						TempHb hb = new TempHb(data[0].substring(0, (data[0].length()-1)),data[colx],data[coly]);
						hb.name=data[colname];
						boolean hbexists = false;
						for (TempHb hb1 : this.hbs) {
							if(hb1.ID.equals(hb.ID)){
								hbexists = true;
							}
						}
						if(!hbexists){
							this.hbs.add(hb);
						}

					}
					System.out.println("hbs done");
				}

				if(tmp.startsWith("$HALTEPUNKT:NR;")){
					System.out.println("referencing hps to hbs");
					String[] data1 = tmp.split(";");

					int colhbnr = -1;

					for(int i = 0; i<data1.length;i++){
						if(data1[i].equals("HSTBERNR")){
							colhbnr=i;
						}
					}
					while (!("".equals(tmp=rdr.readLine()))) {

						String[] data = tmp.split(";");

						for (TempHP hp : temphps) {
							if(hp.hasOldId(data[0])){
								hp.setHb_Id(data[colhbnr].substring(0,(data[colhbnr].length()-1)));
							}
						}
					}

					temphps=cloneHps(temphps);

					System.out.println("referencing done");
				}


				if (tmp.startsWith("$STRECKE:NR;")) {
					System.out.println("parsing links");
					String[] data1 = tmp.split(";");

					int colfrom = -1;
					int colto = -1;
					int collength = -1;

					for(int i = 0; i<data1.length;i++){
						if(data1[i].equals("VONKNOTNR")){
							colfrom=i;
						}
						if(data1[i].equals("NACHKNOTNR")){
							colto=i;
						}
						if(data1[i].equals("LAENGE")){
							collength=i;
						}

					}
					while (!("".equals(tmp=rdr.readLine()))) {
						String[] data = tmp.split(";");
						TempLink link = new TempLink(data[0]);
						for (TempHP hp : temphps) {
							if (hp.hasOldId(data[colfrom])) {
								link.fromNodeID=hp.getHp_Id();
								hp.outLinks.add(link);
							}
							if (hp.hasOldId(data[colto])) {
								link.toNodeID=hp.getHp_Id();
								hp.inLinks.add(link);
							}
						}
						String[] l=data[collength].split("\\.");
						link.length=Integer.parseInt(l[0]);
						templinks.add(link);
					}
					System.out.println("links done");
				}

				if(tmp.startsWith("$LINIE:NAME;")){

					System.out.println("parsing lines");
					String[] data1 = tmp.split(";");

					int colVtype = -1;


					for(int i = 0; i<data1.length;i++){
						if(data1[i].equals("VSYSCODE")){
							colVtype=i;
						}


					}
					while (!("".equals(tmp=rdr.readLine()))) {

						String[] data = tmp.split(";");
						TempLine line = new TempLine(data[0],data[colVtype]);
						this.lines.put(line.getName(), line);
					}
					System.out.println("lines done");
				}

				if(tmp.startsWith("$LINIENROUTE:LINNAME;")){
					System.out.println("parsing routes");
					String[] data1 = tmp.split(";");

					int colname = -1;
					int coldir = -1;


					for(int i = 0; i<data1.length;i++){
						if(data1[i].equals("NAME")){
							colname=i;
						}
						if(data1[i].equals("RICHTUNGCODE")){
							coldir=i;
						}


					}
					while (!("".equals(tmp=rdr.readLine()))) {

						String[] data = tmp.split(";");
						TempLine line = this.lines.get(data[0]);
						TempRoute route = new TempRoute(data[colname], Integer.parseInt(data[coldir]));
						line.routes.add(route);
					}
					System.out.println("routes done");
				}

				if(tmp.startsWith("$LINIENROUTENELEMENT:LINNAME;")){

					System.out.println("parsing routeelements");

					String[] data1 = tmp.split(";");
					int colroutename = -1;
					int coldir = -1;

					int colhpid = -1;
					int colisrp = -1;
					int collength = -1;

					for(int i = 0; i<data1.length;i++){
						if(data1[i].equals("LINROUTENAME")){
							colroutename=i;
						}
						if(data1[i].equals("RICHTUNGCODE")){
							coldir=i;
						}

						if(data1[i].equals("HPUNKTNR")){
							colhpid=i;
						}
						if(data1[i].equals("ISTROUTENPUNKT")){
							colisrp=i;
						}
						if(data1[i].equals("NACHLAENGE")){
							collength=i;
						}

					}
					while (!("".equals(tmp=rdr.readLine()))) {

						String[] data = tmp.split(";");

						TempLine line = this.lines.get(data[0]);
						for (TempRoute route : line.routes) {
							if (route.id.equals(data[colroutename]) && (route.direct==Integer.parseInt(data[coldir])) &&(Integer.parseInt(data[colisrp])==1)) {
								TempRP rp = new TempRP(route.rps.size(),data[colhpid]);
								rp.length=(int)(Double.parseDouble(data[collength])*1000);
//								check first if hp exists in line!
								boolean hpexists = false;
								for(TempHP hp : line.hps){
									if((hp.hasOldId(rp.hp_Id))&&(hp.getDirect()==route.direct)){
										hpexists = true;
										rp.hp=hp;
										rp.hp_Id=hp.getHp_Id();
										route.rps.add(route.rps.size(),rp);
									}
								}
								if(!hpexists){
									for (TempHP hp : temphps) {
										if((hp.hasOldId(rp.hp_Id))&&(hp.getDirect()==route.direct)){


											TempHP finalhp = new TempHP(hp);

											finalhp.setHp_Id(""+this.idCnt+""+finalhp.getDirect());
											this.idCnt++;
											rp.hp_Id=finalhp.getHp_Id();

											route.rps.add(route.rps.size(),rp);
											line.hps.add(finalhp);

										}
									}
								}
							}
						}
					}
					System.out.println("route elements done");
				}

				if(tmp.startsWith("$FAHRZEITPROFILELEMENT:LINNAME;")){
					System.out.println("parsing fzp");
					String[] data1 = tmp.split(";");

					int colroutename = -1;
					int coldir = -1;
					int colfzpid = -1;
					int colindex = -1;

					for(int i = 0; i<data1.length;i++){
						if(data1[i].equals("LINROUTENAME")){
							colroutename=i;
						}
						if(data1[i].equals("RICHTUNGCODE")){
							coldir=i;
						}
						if(data1[i].equals("FZPROFILNAME")){
							colfzpid=i;
						}
						if(data1[i].equals("INDEX")){
							colindex=i;
						}

					}
					while (!("".equals(tmp=rdr.readLine()))) {

						String[] data = tmp.split(";");


						int dir = Integer.parseInt(data[coldir]);
						int arrivaltime = (int)Gbl.parseTime(data[7]);
						int departuretime = (int)Gbl.parseTime(data[8]);
						int data9 = Integer.parseInt(data[9]);
						TempLine line = this.lines.get(data[0]);
						for (TempRoute route : line.routes) {
							if((route.id.equals(data[colroutename])) && (route.direct == dir)){
								boolean fzpexists=false;
								for (TempFZP fzp : route.fzps) {
									if(fzp.id.equals(data[colfzpid])){
										fzpexists=true;
										TempFZPPkt pkt = new TempFZPPkt();
										pkt.pos = (Integer.parseInt(data[colindex])-1);
										pkt.wtime = departuretime - arrivaltime;
										pkt.routetime = departuretime;

										pkt.ttime = arrivaltime - fzp.pkte.get(pkt.pos-1).routetime;

										TempRP rp =route.rps.get(pkt.pos);
										pkt.hp_Id=rp.hp_Id;
										pkt.length=rp.length;
										fzp.pkte.add(pkt.pos,pkt);
									}
								}
								if(fzpexists==false){
									TempFZP fzp = new TempFZP(data[3]);
									TempFZPPkt pkt = new TempFZPPkt();
									pkt.pos=0;
									pkt.wtime = departuretime - arrivaltime;
									pkt.routetime = departuretime;
									pkt.ttime=0;

									for (TempRP rp : route.rps) {
										if (rp.pos == data9 - 1) {
											pkt.hp_Id=rp.hp_Id;
										}
									}
									fzp.pkte.add(pkt.pos,pkt);
									route.fzps.add(fzp);
								}
							}
						}
					}
					System.out.println("fzps done");
				}
				if(tmp.startsWith("$FZGFAHRT:NR;")){
					System.out.println("parsing trips");
					while (!("".equals(tmp=rdr.readLine()))) {

						String[] data = tmp.split(";");
						TempLine line = this.lines.get(data[3]);
						for (TempRoute route : line.routes) {
							if ((route.id.equals(data[4])) && (route.direct==Integer.parseInt(data[5]))) {
								TempTrip trip = new TempTrip(Long.parseLong(data[0]), (int)Gbl.parseTime(data[2]), data[6]);
								route.trips.add(trip);
								int tempdtime=trip.deptime;
								for (TempFZP fzp : route.fzps) {

									if (fzp.id.equals(trip.fzpid) && (fzp.pkte.size() > 1)) {
//													create start Link, set dtime to deptime + posible wtime
										TempLink link = new TempLink(Integer.toString(this.idCnt+100000));

										link.fromNodeID=fzp.pkte.get(0).hp_Id;

										tempdtime+=fzp.pkte.get(0).wtime;
										for(int i=1;i<fzp.pkte.size();i++){
											link.toNodeID=fzp.pkte.get(i).hp_Id;
											link.putTtime(tempdtime, fzp.pkte.get(i).ttime);
											link.length = fzp.pkte.get(i).length;

											if(line.getLink(link.fromNodeID,link.toNodeID)!=null){
												TempLink link2 = line.getLink(link.fromNodeID,link.toNodeID);
												link2.putTtime(tempdtime, fzp.pkte.get(i).ttime);
											} else if(line.getLink(link.fromNodeID,link.toNodeID)==null){
												line.getHP(link.toNodeID).inLinks.add(link);
												line.getHP(link.fromNodeID).outLinks.add(link);
												line.links.add(link);
												this.idCnt++;
											}
											tempdtime+=fzp.pkte.get(i).ttime;
											if((i+1)<fzp.pkte.size()){

												link = new TempLink(Integer.toString(this.idCnt+100000));
												link.fromNodeID=fzp.pkte.get((i)).hp_Id;

												line.getHP(link.fromNodeID).outLinks.add(link);
												tempdtime+=fzp.pkte.get(i).wtime;
											}
										}
									}
								}
							}
						}
					}
					System.out.println("trips done");
				}
			}
			rdr.close();
		} catch(FileNotFoundException e) {
			Gbl.errorMsg(e);
		} catch(IOException e) {
			Gbl.errorMsg(e);
		}

		System.out.println("INTERNALNET: Adding Visum-Network DONE!");
		this.printnet();
	}

	/**
	 * changes all coords to matching coords in specified file
	 * @param coordFile A file containing a List of GK-Coords referenced by exact existing NodeIds
	 */
	public void readCoordFileById(final String coordFile) {
		BufferedReader rdr = null;
		try {
			String tmp;
			rdr = new BufferedReader(new FileReader(coordFile));

			tmp = rdr.readLine();
			if (tmp == null) {
				System.err.println("the file seems to be empty.");
				rdr.close();
				return;
			}

			// read until we find the header for our table
			boolean headerFound = false;
			tmp = rdr.readLine();
			while (!headerFound && tmp != null) {
				if (tmp.startsWith("$KNOTEN:NR;")) {
					headerFound = true;
				} else {
					tmp = rdr.readLine();
				}
			}
			if (!headerFound) {
				System.err.println("did not find line containing header of table, beginning with '$KNOTEN:NR;'");
				rdr.close();
				return;
			}

			System.out.println("parsing nodes");
			while (!("".equals(tmp = rdr.readLine()))) {
				String[] data=tmp.split(";");
				double newX = Double.parseDouble(data[8]);
				double newY = Double.parseDouble(data[9]);

				TempLine line = this.lines.get(data[1]);
				boolean found =false;
				for (TempHP hp : line.hps) {
					if(hp.getHp_Id().equals(data[0])){
						hp.getCoord().setXY(newX, newY);
						found=true;
					}

				}
				if (!found) {
					System.out.println("Could not find hp "+data[0]);
				}
			}
			System.out.println("done.");
		} catch (FileNotFoundException e) {
			Gbl.errorMsg(e);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		} finally {
			if (rdr != null) {
				try { rdr.close(); } catch (IOException ignored) {}
			}
		}
	}

	/**
	 * changes all coordinates to matching coordinates in specified file
	 * @param coordFile Path of a file containing a list of GK-Coords referenced by exact existing SOLDNER-Coords
	 */
	public void readCoordFileByCoord(final String coordFile) {
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(coordFile));
			String tmp = rdr.readLine();
			if (tmp == null) {
				System.err.println("the file seems to be empty.");
				rdr.close();
				return;
			}

			// read until we find the header for our table
			boolean headerFound = false;
			tmp = rdr.readLine();
			while (!headerFound && tmp != null) {
				if (tmp.startsWith("$KNOTEN:NR;")) {
					headerFound = true;
				} else {
					tmp = rdr.readLine();
				}
			}
			if (!headerFound) {
				System.err.println("did not find line containing header of table, beginning with '$KNOTEN:NR;'");
				rdr.close();
				return;
			}

			System.out.println("parsing nodes");
			// first parse all the old/new node-coordinates and store them in simple lists
			// search for max-extend of old coordinates to generate later a QuadTree out of it.
			double minX = Double.POSITIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY;
			double maxY = Double.NEGATIVE_INFINITY;
			ArrayList<Coord> newCoords = new ArrayList<Coord>();
			ArrayList<Coord> oldCoords = new ArrayList<Coord>();
			while ((tmp = rdr.readLine()) != null && tmp.trim().equals("") == false) {
				String[] data = tmp.split(";");
				double newX = Double.parseDouble(data[1]);
				double newY = Double.parseDouble(data[2]);
				double oldX = Double.parseDouble(data[3]);
				double oldY = Double.parseDouble(data[4]);
				Coord newCoord = new Coord(newX, newY);
				Coord oldCoord = new Coord(oldX, oldY);
				newCoords.add(newCoord);
				oldCoords.add(oldCoord);
				if (oldX < minX) { minX = oldX; }
				if (oldY < minY) { minY = oldY; }
				if (oldX > maxX) { maxX = oldX; }
				if (oldY > maxY) { maxY = oldY; }
			}
			// now generate the quadtree: oldX/oldY -> newCoord
			QuadTree<Coord> quadTree = new QuadTree<Coord>(minX, minY, maxX, maxY);
			for (int i = 0, max = oldCoords.size(); i < max; i++) {
				Coord oldCoord = oldCoords.get(i);
				Coord newCoord = newCoords.get(i);
				quadTree.put(oldCoord.getX(), oldCoord.getY(), newCoord);
			}
			System.out.println("replacing coords");
			for (TempLine line : this.lines.values()) {
				for (TempHP hp : line.hps) {
					Coord newCoord = quadTree.get(hp.getCoord().getX(), hp.getCoord().getY());
					hp.setCoord(newCoord.getX(), newCoord.getY());
				}
			}
			System.out.println("done.");
		} catch (FileNotFoundException e) {
			Gbl.errorMsg(e);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		} finally {
			if (rdr != null) {
				try { rdr.close(); } catch (IOException ignored) {}
			}
		}
	}

	/**
	 * Writes the loaded InternalNetwork on specified PtNetworklayer
	 * @param layer PtNetworklayer to write on
	 * @param idCount
	 */
	public void writeToNetworkLayer(final PtNetworkLayer layer, final int idCount){

		this.idCnt=idCount;
//		first create pedestrian nodes on hbs

		for (TempHb hb : this.hbs) {
			TempHP pednode = new TempHP();

			pednode.setHp_Id(hb.ID);
			pednode.setCoord(hb.coord.getX(), hb.coord.getY());
			pednode.setHb_Id("ped");

			for (TempLine line : this.lines.values()) {
				for (TempHP hp : line.hps) {

					if(hp.getHb_Id()!=null){

//						every hp is linked with links of type "P" to refernced hb / pednode

						if (hp.getHb_Id().equals(pednode.getHp_Id())){
							TempLink link = new TempLink(""+(this.idCnt+1000000));
							this.idCnt++;
							TempLink link2 = new TempLink(""+(this.idCnt+1000000));
							this.idCnt++;
							link.fromNodeID=hp.getHp_Id();
							link.toNodeID=pednode.getHp_Id();
							link2.fromNodeID=pednode.getHp_Id();
							link2.toNodeID=hp.getHp_Id();

							link.type="P";
							link2.type="P";

							link.length=pednode.getCoord().calcDistance(hp.getCoord());
							link2.length=pednode.getCoord().calcDistance(hp.getCoord());

							link2.cost = INITIAL_CHANGE_COST;
							// Zugangslinks zu Haltebereichen als extra Funktion

							line.links.add(link);
							line.links.add(link2);
						}
					}
				}
			}
			this.pedNodes.add(pednode);
		}

		for (TempHP pednode : this.pedNodes) {
			layer.createNode(pednode.getHp_Id(), Double.toString(pednode.getCoord().getX()), Double.toString(pednode.getCoord().getY()), "P");
		}

		for (TempLine line : this.lines.values()) {
			for (TempHP hp : line.hps) {
				if(hp.getHb_Id()!=null){
					layer.createNode(hp.getHp_Id(),Double.toString(hp.getCoord().getX()),Double.toString(hp.getCoord().getY()),null);
				}
			}
			for (TempLink link : line.links) {
				if (link.type != null) {
					PtLink l = (PtLink) layer.createLink(link.linkID, link.fromNodeID, link.toNodeID,
							Double.toString(link.length), "50", "10000", "1", link.linkID, link.type);
					if (link.type.equals("P")) {
						l.cost = link.cost;
					}

				} else {
					PtLink l = (PtLink) layer.createLink(link.linkID, link.fromNodeID, link.toNodeID,
							Double.toString(link.length), "50", "10000", "1", link.linkID, null);
					l.setDepartures(link.departures);

					if(l.getLength()<=0.0){
						l.setLength(l.getFromNode().getCoord().calcDistance(l.getToNode().getCoord()));
					}
				}
			}
		}
		layer.connect();
	}

	/**
	 * creates hbs on average coords of corresponding hps
	 */
	public void createHbs() {
		for (TempLine line : this.lines.values()) {
			for (TempHP hp : line.hps) {
				TempHb hb = requestHb(hp.getHb_Id(),hp.getName());
				hb.hps.add(hp);
			}
		}
		for (TempHb hb : this.hbs) {
			hb.calcCoords();
		}
	}

	/**
	 * prints numbers of internal network elements to the console
	 */
	public void printnet() {
		int linecnt=0;
		int routecnt=0;
		int linkcnt=0;
		int hpcnt=0;
		int fzpcnt=0;
		int tripcnt=0;
		hpcnt += this.pedNodes.size();
		for (TempLine line : this.lines.values()) {
			linecnt++;
			routecnt+=line.routes.size();
			linkcnt+=line.links.size();
			hpcnt+=line.hps.size();

			for (TempRoute route : line.routes) {
				tripcnt+=route.trips.size();
				fzpcnt+=route.fzps.size();
			}
		}
		System.out.println(linecnt+" lines, "+routecnt+" routes, "+fzpcnt+" fzps, "+tripcnt+" trips, "+linkcnt+" links, "+hpcnt+" hps, "+this.hbs.size()+" hbs.");

	}

	/**
	 * @param name line name
	 * @return line with specified name, null if no line exists with this name
	 */
	public TempLine getTempLine(final String name){
		return this.lines.get(name);
	}

	/**
	 * @param id id of requested hb
	 * @param name name of requested hb
	 * @return hb with specified id, if does not exist: a new hb is created with specified id and name
	 */
	public TempHb requestHb(final String id, final String name){
		for (TempHb hb : this.hbs) {
			if (id.equals(hb.ID)) {
				return hb;
			}
		}
		// we did not find a matching hbs, create one
		TempHb hb = new TempHb(id);
		hb.name=name;
		this.hbs.add(hb);
		return hb;
	}

	/**
	 * @param id id of requested hb
	 * @return hb with specified id, if does not exist: null
	 */
	public TempHb getTempHb(final String id){
		for (TempHb hb : this.hbs) {
			if(hb.ID.equals(id)){
				return hb;
			}
		}
		return null;
	}
	public TempHP getTempHP(final String id){
		TempHP hp = null;
		for (TempLine line : this.lines.values()) {

			TempHP hp2=line.getHP(id);
			if (hp2 != null){
				hp=hp2;
			}
		}

		return hp;
	}

	/**
	 * @param hpList list of hps to be cloned
	 * @return list with both original and deepcopied hps with opposite direction tag
	 */
	public ArrayList<TempHP> cloneHps(final ArrayList<TempHP> hpList){
		ArrayList<TempHP> hpList2 = new ArrayList<TempHP>();
		for (TempHP hp : hpList) {
			TempHP hp2 = new TempHP(hp, true);
			hp.setHp_Id(hp.getHp_Id() + Integer.toString(hp.getDirect()));
			hp2.setHp_Id(hp2.getHp_Id() + Integer.toString(hp2.getDirect()));
			hpList2.add(hp);
			hpList2.add(hp2);
		}
		return hpList2;
	}

}
