/* *********************************************************************** *
 * project: org.matsim.*
 * PtNetworkReader.java
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.gbl.Gbl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import playground.marcel.ptnetwork.tempelements.TempFZP;
import playground.marcel.ptnetwork.tempelements.TempFZPPkt;
import playground.marcel.ptnetwork.tempelements.TempHP;
import playground.marcel.ptnetwork.tempelements.TempLink;
import playground.marcel.ptnetwork.tempelements.TempRP;
import playground.marcel.ptnetwork.tempelements.TempRoute;
import playground.marcel.ptnetwork.tempelements.TempTrip;

public class PtNetworkReader extends org.xml.sax.helpers.DefaultHandler{

	/*member variables*/
	private int idCnt = 10000;
	private String lineName = null;
	private String publicLineName = null;
	private String vType = null;
	private StringBuffer buffer = null;
	private boolean valid = false;


	public ArrayList<TempHP> hps = new ArrayList<TempHP>();
	public ArrayList<TempLink> links = new ArrayList<TempLink>();
	public ArrayList<TempRoute> routes = new ArrayList<TempRoute>();

	private TempHP tmphp = null;
	private TempRoute tmproute = null;
	private TempRP tmprp = null;
	private TempFZP tmpfzp = null;
	private TempFZPPkt tmpfzppkt = null;
	private TempTrip tmptrip = null;

	private boolean rpFlag = false;
	private boolean fzpFlag = false;
	private boolean tripFlag = false;


	public String getLineName(){
		return this.lineName;
	}

	public void setLineName(final String name){
		this.lineName=name;
	}

	public String getVType(){
		return this.vType;
	}

	public void setVType(final String vType){
		this.vType=vType;
	}

	public boolean isValid(){
		return this.valid;
	}

	public int getIdCnt(){
		return this.idCnt;
	}

	public void read (final String filename, final int idCnt) {
		this.idCnt=idCnt;
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			SAXParser saxParser = factory.newSAXParser();
			if(filename.endsWith("xml")){
				saxParser.parse(new File(filename), this);
			}
		}
		catch (SAXException e) {
			e.printStackTrace();
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startElement(final String namespaceURI,final String sName,
			final String qName,final Attributes attrs) {
		/*
		 * creating buffer for character()
		 * creating temp objects
		 * setting flags if necessary
		 * */
		this.buffer = new StringBuffer();

		if (qName.equals("Haltepunkt")) {
			this.tmphp = new TempHP();
			this.tmphp.setHp_Id(""+this.idCnt);
		}
		else if (qName.equals("Route")) {
			this.tmproute = new TempRoute();
		}
		else if (qName.equals("Routenpunkt")) {
			this.tmprp = new TempRP();
			this.rpFlag = true;
		}
		else if (qName.equals("Fahrzeitprofil")) {
			this.tmpfzp = new TempFZP();
			this.fzpFlag=true;
		}
		else if (qName.equals("Fahrzeitprofilpunkt")) {
			this.tmpfzppkt = new TempFZPPkt();
		}
		else if (qName.equals("Fahrt")) {
			this.tmptrip = new TempTrip();
			this.tripFlag = true;
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) {

		/* ****************************
		 * copy buffer to tmp
		 * add temp objects to lists and reset flags if necessary
		 * ************************************************/
		String tmp = null;
		tmp = this.buffer.toString();

		if(qName.equals("interneLiniennummer")){

			/* **********************************
			 * set lineName
			 * ***********************************/

			this.lineName=tmp;
//			System.out.println("READER: Set lineName to "+lineName);
		}
		else if(qName.equals("öffentlicheLiniennummer")){

			/* **********************************
			 * set public lineName
			 * ***********************************/

			this.publicLineName=tmp.replace("U", "");
		}


		else if (qName.equals("Meldungsnummer")){
			if(tmp.equals("0")){
				this.valid=true;
			}
		}
		else if(qName.equals("Betriebszweignummer")){
			if(tmp.equals("1")){
				this.vType="B";
			}
			else if(tmp.equals("2")){
				this.vType="U";
			}
			else if(tmp.equals("3")){
				this.vType="T";
			}
			else if(tmp.equals("4")){
				this.vType="F";
			} else{
				Gbl.errorMsg("READER: unexpected vType: " + tmp);
			}
		}

		else if(qName.equals("Haltepunktnummer")&&(this.rpFlag==false)){
			this.tmphp.oldIDs.addFirst(tmp);
		}

		else if(qName.equals("Haltepunktnummer")&&(this.rpFlag==true)){

			/* **************************************
			 * look up id in oldIDs of all HPs in List to write new ID
			 * as hp_Id of RP
			 * ********************************************/

//			for (TempHP tmphp2 : this.hps) {
//				for (String tmp2 : tmphp2.oldIDs) {
//					if(tmp2.equals(tmp)){
//						tmprp.hp_Id=tmphp2.hp_Id;
//					}
//				}
//			}
			this.tmprp.hp_Id=tmp;
			this.tmprp.hp=getHPbyOldID(tmp);
		}
		else if(qName.equals("Haltestellenbereichsnummer")){
			this.tmphp.setHb_Id(tmp);
		}
		else if(qName.equals("Fahrplanbuchname")){
			this.tmphp.setName(tmp);
		}
		else if(qName.equals("Xkoordinate")){
			if(tmp!=null){
				this.tmphp.setXCoord((Integer.parseInt(tmp))/10.0);
			}
		}
		else if(qName.equals("Ykoordinate")){
			if(tmp!=null){
				this.tmphp.setYCoord((Integer.parseInt(tmp))/10.0);
			}
		}
		else if(qName.equals("Routennummer")){
			this.tmproute.id=tmp;
		}
		else if (qName.equals("Richtung")){
			this.tmproute.direct=Integer.parseInt(tmp);
		}
		else if(qName.equals("Position")&&(this.rpFlag==true)){
			this.tmprp.pos=Integer.parseInt(tmp);
		}
		else if(qName.equals("Streckenlänge")){
			this.tmprp.length=Integer.parseInt(tmp);
		}
		else if(qName.equals("Fahrgastwechsel")&&(this.rpFlag==true)){
			if (tmp.equals("J")){
				this.tmprp.passengerChange=true;
			}
		}
		else if(qName.equals("Fahrzeitprofilnummer")&&(this.fzpFlag==true)){
			this.tmpfzp.id=tmp;
		}
		else if(qName.equals("Position")&&(this.fzpFlag==true)){

			/* ************************************************************
			 * element fzp contains only indirect information about HP
			 * look up HP-ID by checking hp_ID of RP with the same position
			 * *******************************************************/

			this.tmpfzppkt.pos=Integer.parseInt(tmp);
			TempRP tmprp2 = this.tmproute.getTempRP(this.tmpfzppkt.pos);
			this.tmpfzppkt.hp_Id=tmprp2.hp_Id;
			this.tmpfzppkt.passengerChange=tmprp2.passengerChange;
			this.tmpfzppkt.length=tmprp2.length;
			this.tmpfzppkt.hp=tmprp2.hp;
		}
		else if(qName.equals("Streckenfahrzeit")){
			this.tmpfzppkt.ttime=Integer.parseInt(tmp);
		}
		else if(qName.equals("Wartezeit")){
			this.tmpfzppkt.wtime=Integer.parseInt(tmp);
		}
		else if(qName.equals("Fahrzeitprofilnummer") && (this.tripFlag==true)){
			this.tmptrip.fzpid=tmp;
			this.tmptrip.fzp=this.tmproute.getTempFZP(tmp);
		}

//		All trips must start within 24h -> 86400 secs

		else if(qName.equals("Startzeit")&&(this.tripFlag==true)){
			this.tmptrip.deptime=Integer.parseInt(tmp)%86400;
		}
		else if(qName.equals("Fahrgastfahrt")){
			if(tmp.equals("N")){
				this.tmptrip.passengerTrip=false;
			}
			if(tmp.equals("J")){
				this.tmptrip.passengerTrip=true;
			}
		}

		else if(qName.equals("Haltepunkt")){

			/* ******************************************************
			 * check Hps for Coords
			 * if coords already exists, just add id to oldIDs
			 * if not create node with new id and old id in oldIDs, modify idcnt
			 * *****************************************************/
			boolean existed = false;
			for (TempHP tmphp2 : this.hps) {
				if (tmphp2.getCoord().equals(this.tmphp.getCoord())){
					tmphp2.oldIDs.addLast(this.tmphp.oldIDs.getFirst());
//					System.out.println("READER: Added oldID "+tmphp2.oldIDs.getLast()+" to Node with new ID "+tmphp2.hp_Id);
					existed=true;
				}
			}
			if (!existed) {

//				System.out.println("READER: Created new node "+tmphp.name+" with Id "+tmphp.hp_Id+" and old ID "+tmphp.oldIDs.getFirst());
				this.hps.add(this.tmphp);
				this.idCnt++;
			}
		}
		else if(qName.equals("Routenpunkt")){

			this.tmproute.rps.add(this.tmprp);
			this.rpFlag=false;
		}
		else if(qName.equals("Fahrzeitprofil")){
//			for(int i=0;i<tmpfzp.pkte.size();i++){
//				if (tmpfzp.pkte.get(i).passengerChange==false){
//					/*
//					 * if fzppkt is not a passenger stop, remove from route
//					 * ttime and length is added to wtime of following
//					 * */
//					if(tmpfzp.pkte.size()>(i+1)){
//						tmpfzp.pkte.get(i+1).wtime+=tmpfzp.pkte.get(i).ttime;
//						tmpfzp.pkte.get(i+1).length+=tmpfzp.pkte.get(i).length;
//					}
//					tmpfzp.pkte.remove(i);
//					i--;
//				}
//
//			}
//			for(int i=0;i<tmpfzp.pkte.size();i++){
//				if(tmpfzp.pkte.size()>(i+1)){
//					if(getHPbyOldID(tmpfzp.pkte.get(i).hp_Id).getName().equals(getHPbyOldID(tmpfzp.pkte.get(i+1).hp_Id).getName())){
//						tmpfzp.pkte.get(i+1).wtime+=tmpfzp.pkte.get(i).ttime;
//						tmpfzp.pkte.get(i+1).length+=tmpfzp.pkte.get(i).length;
//						tmpfzp.pkte.remove(i);
//						i--;
//					}
//				}
//			}
//			for(int i=0;i<tmpfzp.pkte.size();i++){
//				tmpfzp.pkte.get(i).hp_Id=getHPbyOldID(tmpfzp.pkte.get(i).hp_Id).getHp_Id()+tmproute.direct;
//			}
			this.tmpfzp.direct=this.tmproute.direct;
			this.tmpfzp.trimInValidPkte();
			this.tmproute.fzps.add(this.tmpfzp);
			this.fzpFlag=false;
		}
		else if(qName.equals("Fahrzeitprofilpunkt")){

			/* *****************************************
			 *add in position of tmpfzppkt to ArrayList pkte
			 * *****************************************/

			this.tmpfzp.pkte.add((this.tmpfzppkt.pos-1),this.tmpfzppkt);
//			System.out.println("READER: Added Node "+tmpfzppkt.hp_Id+" ("+getHPbyOldID(tmpfzppkt.hp_Id).hp_Id +") as FZPPkt "+tmpfzppkt.pos+" to FZP "+tmpfzp.id+" of route "+tmproute.id);
		}
		else if (qName.equals("Fahrt")){
			 // Adding only trips with passenger access
			if (this.tmptrip.passengerTrip) {
				this.tmproute.trips.add(this.tmptrip);
			}
			this.tripFlag = false;
		}
		else if (qName.equals("Route")){
			if(this.tmproute.direct!=0){
				this.routes.add(this.tmproute);
			}
		}

	}

	@Override
	public void characters (final char[] ch, final int start, final int length){

		this.buffer.append(new String(ch,start,length));
		/*
		 * Check current to identify current element, check flags
		 * write timetable info to temp objects
		 * */
	}

	@Override
	public void endDocument(){
			this.lineName=this.vType+"-"+this.publicLineName;
	}

	public void finishNetwork(){
		this.hps=cloneHps(this.hps);

//		setting pointer in fzpPkt to hp
//		must be done after cloning

		for (TempRoute route : this.routes) {
			for (TempFZP fzp : route.fzps) {
				for (TempFZPPkt pkt : fzp.pkte) {
					if (!(pkt.hp.equals(this.getHP(pkt.hp_Id)))) {
						pkt.hp = this.getHP(pkt.hp_Id);
					}
				}
			}
		}

//		go through route
//		NOTE: not yet optimized by using get-methods

		for (TempRoute route : this.routes) {
			for (TempTrip trip : route.trips) {
//				define trip start time
				int tmpdtime = trip.deptime;
				for (TempFZP fzp : route.fzps) {

//					identify fzp
					if ((trip.fzpid.equals(fzp.id)) && (fzp.pkte.size() > 1)) {
//							create start Link, set dtime to deptime + possible wtime

						TempLink link = new TempLink(Integer.toString(this.idCnt+1000000));

						link.fromNodeID=fzp.pkte.get(0).hp_Id;
						link.fromNode=getHP(fzp.pkte.get(0).hp_Id);

						tmpdtime+=fzp.pkte.get(0).wtime;
//						link.dtimes.add(tmpdtime);

//							in the following handle link end + possible new link beginning

						for(int i=1;i<fzp.pkte.size();i++){
							link.toNodeID=fzp.pkte.get(i).hp_Id;
							link.toNode=getHP(fzp.pkte.get(i).hp_Id);

							link.putTtime(tmpdtime, fzp.pkte.get(i).ttime);
							link.length=fzp.pkte.get(i).length;



							if (getLink(link.fromNode,link.toNode)!=null) {
								TempLink link2 = getLink(link.fromNode,link.toNode);

								link2.putTtime(tmpdtime, fzp.pkte.get(i).ttime);
//								link2.dtimes.addAll(link.dtimes);
							} else if(getLink(link.fromNodeID,link.toNodeID)==null){
								link.toNode.inLinks.add(link);
								link.fromNode.outLinks.add(link);
//									say("link created from "+link.fromNodeID+" to "+link.toNodeID);
								this.links.add(link);
								this.idCnt++;
							}
							tmpdtime+=fzp.pkte.get(i).ttime;
							if((i+1)<fzp.pkte.size()){
								link = new TempLink(Integer.toString(this.idCnt+1000000));
								link.fromNodeID=fzp.pkte.get((i)).hp_Id;
								link.fromNode=getHP(fzp.pkte.get((i)).hp_Id);
								tmpdtime+=fzp.pkte.get(i).wtime;
//								link.dtimes.add(tmpdtime);
							}
						}
					}
				}
			}
		}
		this.hps=deleteDoubleHps(this.hps);
	}

	public TempHP getHP (final String ID) {
		for (TempHP temphp : this.hps) {
			if (temphp.getHp_Id().equals(ID)){
				return temphp;
			}
		}
		return null;
	}

	public TempHP getHPbyOldID (final String Id, final int direct){
		if((direct!=1)&&(direct!=2)){
			System.out.println("READER: undefined direction tag: "+direct);
			return null;
		}
		for (TempHP temphp : this.hps) {
			for (String tmpId : temphp.oldIDs) {
				if ((tmpId.equals(Id)) && (direct==temphp.getDirect())){
					return temphp;
				}
			}
		}
		return null;
	}

	public TempHP getHPbyOldID (final String Id){
		for (TempHP temphp : this.hps) {
			for (String tmpId : temphp.oldIDs) {
				if (tmpId.equals(Id)){
					return temphp;
				}
			}
		}
		return null;
	}

	public TempFZP getFZP (final String ID){
		for (TempFZP tempfzp : this.tmproute.fzps) {
			if (tempfzp.id.equals(ID)){
				return tempfzp;
			}
		}
		return null;
	}

	public TempLink getLink(final String fromNodeID, final String toNodeID){
		TempHP fromNode = getHP(fromNodeID);
		TempHP toNode = getHP(toNodeID);
		return getLink(fromNode, toNode);
	}
	public TempLink getLink(final TempHP fromNode, final TempHP toNode){
		for (TempLink link : fromNode.outLinks) {
			if (link.toNode.equals(toNode)) {
				return link;
			}
		}
		return null;
	}

	public ArrayList<TempHP> cloneHps(final ArrayList<TempHP> hpList){
		ArrayList<TempHP> hpList2 = new ArrayList<TempHP>();
		for (TempHP hp : hpList) {
			TempHP hp2 = new TempHP(hp,true);
			hp.setHp_Id(hp.getHp_Id()+Integer.toString(hp.getDirect()));
			hp2.setHp_Id(hp2.getHp_Id()+Integer.toString(hp2.getDirect()));
			hpList2.add(hp);
			hpList2.add(hp2);
		}
		return hpList2;
	}

	public ArrayList<TempHP> deleteDoubleHps(final ArrayList<TempHP> hpList){
		ArrayList<TempHP> hpList2 = new ArrayList<TempHP>();
		for (TempHP hp : hpList) {
			if((hp.inLinks.isEmpty()==false)||(hp.outLinks.isEmpty()==false)) {
				hpList2.add(hp);
			}
		}
		return hpList2;
	}

	/*
	 * helper / debug methods
	 */

	public static void say(final String say){
		System.out.println(say);
	}

	public void printFzps(){
		for (TempRoute route : this.routes) {
			for (TempFZP fzp : route.fzps) {
				say("route "+route.id+" fzp "+fzp.id);
				for (TempFZPPkt pkt : fzp.pkte) {
					say("pkt "+pkt.pos+" HP  ("+pkt.hp_Id+")");
					if(getHPbyOldID(pkt.hp_Id)!=null){
						say(getHPbyOldID(pkt.hp_Id).getName());
					}
				}
			}
		}
	}

}
