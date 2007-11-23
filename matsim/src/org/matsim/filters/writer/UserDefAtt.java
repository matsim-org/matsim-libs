/* *********************************************************************** *
 * project: org.matsim.*
 * UserDefAtt.java
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

package org.matsim.filters.writer;

/**
 * @author ychen
 */
public class UserDefAtt implements Comparable<UserDefAtt> {
	/**
	 * this enumeration type contains options, whether the attribut is to
	 * document sparsely: 1- sparsely, "duenn"; 0- not sparsely, "dick". (we use
	 * now the germen version of "VISUM 9.3")
	 */
	public enum SparseDocu {
		duenn(1), dick(0);
		private final int nr;

		SparseDocu(int nr) {
			this.nr = nr;
		}

		/**
		 * Returns a String object representing the member variable "nr" of this
		 * enum constant. The argument is converted to signed decimal
		 * representation and returned as a string.
		 *
		 * @return a string representation of the member variable "nr" of this
		 *         enum constant.
		 */
		@Override
		public String toString() {
			return Integer.toString(this.nr);
		}
	};

	/**
	 * this enumeration type contains 7 types for the attribut, which user of
	 * "VISUM9.3" can choose.
	 */
	public enum DatenTyp {
		Bool("Bool"), Filename("Filename"), Int("Int"), Double("Double"), ShortLength(
				"ShortLength"), LongLength("LongLength"), Text("Text"), Duration(
				"Duration"), Timepoint("Timepoint");
		DatenTyp(String datenTypName) {
		}
	};

	/* -------------------------MEMBER VARIABLES--------------------------- */
	private String OBJID;

	private String ATTID;

	private String CODE;

	private String NAME;

	private DatenTyp DATENTYP;

	private int NACHKOMMASTELLEN;

	private SparseDocu DUENN;

	private String pattern = "##0";

	/*-----------------------CONSTRUCTOR-------------------------------*/
	/**
	 * @param objid
	 * @param attid
	 * @param code
	 * @param name
	 * @param datentyp
	 * @param nachKommaStellen
	 * @param duenn
	 */
	public UserDefAtt(String objid, String attid, String code, String name,
			DatenTyp datentyp, int nachKommaStellen, SparseDocu duenn) {
		this.OBJID = objid;
		this.ATTID = attid;
		this.CODE = code;
		this.NAME = name;
		this.DATENTYP = datentyp;
		this.NACHKOMMASTELLEN = nachKommaStellen;
		this.DUENN = duenn;
		// attID = uda.getATTID();
		// datenTyp = uda.getDATENTYP();
		// nachKommaStellen = uda.getNACHKOMMASTELLEN();
		if (this.NACHKOMMASTELLEN > 0)
			setPattern();
	}

	/* -----------------------------GETTER------------------------------*/
	 /** @return Returns the aTTID.
	 * @uml.property name="aTTID"
	 */
	public String getATTID() {
		return this.ATTID;
	}

	/**
	 * Returns a DatenTyp of a UserDefAtt
	 *
	 * @return a DatenTyp of a UserDefAtt
	 * @uml.property name="dATENTYP"
	 */
	public DatenTyp getDATENTYP() {
		return this.DATENTYP;
	}

	/**
	 * Returns the object-ID in "UserDefAtt". e.g. "NODE", "LINK" etc.
	 *
	 * @return object-ID in "UserDefAtt". e.g. "NODE", "LINK" etc.
	 * @uml.property name="oBJID"
	 */
	public String getOBJID() {
		return this.OBJID;
	}

	/**
	 * @return Returns the nACHKOMMASTELLEN.
	 * @uml.property name="nACHKOMMASTELLEN"
	 */
	public int getNACHKOMMASTELLEN() {
		return this.NACHKOMMASTELLEN;
	}

	/* ------------------------------OVERRIDE METHODS-------------------------*/
	@Override
	public String toString() {
		return this.OBJID + ";" + this.ATTID + ";" + this.CODE + ";" + this.NAME + ";" + this.DATENTYP
				+ ";" + this.NACHKOMMASTELLEN + ";" + this.DUENN + "\n";
	}

	/** make UserDefAtt-object comparable
	 * @param arg0 - an UserDefAtt-object
	 * @return the result of the comparing between ATTIDs of 2 UserDefAtt-objects
	 */
	public int compareTo(UserDefAtt arg0) {
		return this.ATTID.compareTo(arg0.ATTID);
	}

	/**
	 * @return the pattern.
	 */
	public String getPattern() {
		return this.pattern;
	}

	/* ----------------------------SETTER---------------------*/
	 /** sets the pattern of "Benutzerdefiniert Attribut"
	 * @param pattern Pattern of a "Benutzerdefiniertes Attribut"
	 * TODO I'm not sure that this function really does what it should do... -marcel
	 */
	public void setPattern() {
		StringBuffer sb = new StringBuffer(this.pattern);
		sb.append(".");
		for (int ii = 0; ii < this.NACHKOMMASTELLEN; ii++)
			sb.append("0");
		this.pattern = sb.toString();
	}
}
