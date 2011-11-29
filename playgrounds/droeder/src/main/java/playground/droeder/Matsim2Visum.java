/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder;

import java.util.ArrayList;
import java.util.List;


/**
 * @author droeder
 *
 */
public class Matsim2Visum {

	private String networkFile;
	private String populationFile;
	private String areaShpFile;
	private String date;

	/**
	 * 
	 * @param date todays date dd.mm.yyyy
	 * @param networkFile
	 * @param populationFile
	 * @param areaShpFile
	 */
	public Matsim2Visum(String date, String networkFile, String populationFile, String areaShpFile){
		this.date = date;
		this.networkFile = networkFile;
		this.populationFile = populationFile;
		this.areaShpFile = areaShpFile;
	}
	
	public void run(String outDir){
		this.readData();
		this.createVisumData();
		this.writeVisumData(outDir);
	}

	// ################## read Matsim data #################
	private void readData() {
		// TODO Auto-generated method stub
		
	}

	// ############### create Visum data ###################
	private void createVisumData() {
		// TODO Auto-generated method stub
		
	}


	//################## write Data ########################
	private void writeVisumData(String outDir) {
		this.writeNetwork(outDir);
		this.writeOdMatrix(outDir);
	}

	private void writeNetwork(String outDir) {
		// TODO Auto-generated method stub
		
	}
	
	private void writeOdMatrix(String outDir) {
		// TODO Auto-generated method stub
		
	}
}

class Netzwerk{
	private String date;
	private List<NetzElement> elemente;
	
	public Netzwerk(String date){
		this.date = date;
		this.elemente = new ArrayList<NetzElement>();
	}
	
	public void addNetzElement(NetzElement e){
		this.elemente.add(e);
	}
	
	public List<NetzElement> getNetzElemente(){
		return this.elemente;
	}
}

abstract class NetzElement{
	public final static String NETZPARAMETER = "Netzparameter";
	public final static String VERKEHRSSYSTEME = "Verkehrssysteme";
	
	private String header;
	private String value = "";
	public String name;
	public NetzElement(final String name){
		this.name = name;
		this.header = this.makeHeader(name);
	}
	
	private String makeHeader(String name) {
		String header =
			"*\n"+
			"*Tabelle: " + name + "\n" +
			"*\n";
		return header;
	}
	
	public String getHeader(){
		return this.header;
	}
	
	public void setValue(String s){
		this.value = s;
	}
	
	@Override
	public String toString(){
		return (this.header + this.value);
	}
}
class NetzParameter extends NetzElement{

	/**
	 * @param name
	 */
	public NetzParameter() {
		super(NetzElement.NETZPARAMETER);
		super.setValue(this.makeValue());
	}

	private String makeValue() {
		String v = 
			"$NETZPARAMETER:NETVERSNR;NETVERSNAME;MASSSTAB;LINKSVERKEHR;KOORDDEZ;WAEHRUNGDEZSTELLEN;LANGELAENGENDEZ;KURZELAENGENDEZ;ABBT0DEZ;GESCHWDEZ;VERKETTENMAXLAENGE;VERKETTENTRENNZEICHEN;ERZMODUSNSEG;VISATTRRECHT;DEFKOORD;ABBTYPSTD;STRECKENORIENTIERUNGSBERECHNUNGSART" + "\n" +
			"1,210;;1,000;0;4;2;3;2;0;0;255;,;1;INVISIBLE;;ONLYFROMANGLE;EIGHT" + "\n";
		return v;
	}

	
}

class VerkehrsSysteme extends NetzElement{

	public List<Verkehrssystem> systeme;
	
	public VerkehrsSysteme() {
		super(NetzElement.VERKEHRSSYSTEME);
		this.systeme = new ArrayList<Verkehrssystem>();
	}
	
	public void addSystem(Verkehrssystem s){
		this.systeme.add(s);
		String temp = "";
		for(Verkehrssystem sys: this.systeme){
			temp+= sys.toString() + "\n"; 
		}
		super.setValue(temp);
	}

}
class Verkehrssystem{
	private String code;
	private Object name;
	private String type;
	private String pkwEinh;
	
	public Verkehrssystem(String code, String name, VsysTyp typ, Double pkwEinh){
		this.code = code;
		this.name = name;
		this.type = typ.type;
		this.pkwEinh = String.valueOf(pkwEinh).replace(".", ",");
	}
	@Override
	public String toString(){
		return (code + ";" + name + ";" + type + ";" + pkwEinh);  
	}
}

enum VsysTyp{
	OV("OV"), OVFuss("OVFuss"), IV("IV");
	public String type;
	private VsysTyp(String type) {
		this.type = type;
	}
}

class NachfrageSegment{
	
}

