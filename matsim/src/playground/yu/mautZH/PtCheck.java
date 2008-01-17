/* *********************************************************************** *
 * project: org.matsim.*
 * PtCheck.java
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

package playground.yu.mautZH;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.matsim.plans.Person;
import org.matsim.plans.algorithms.PersonAlgorithm;

/**
 * outputs the amount of public transit user or its fraction into .txt-files
 * 
 * @author ychen
 * 
 */
public class PtCheck extends PersonAlgorithm {
	// ----------------------MEMBER VARIABLES----------------------------
	/**
	 * Counter of all read persons
	 */
	private int personCnt;

	/**
	 * Counter of all persons, who use public transport
	 */
	private int ptUserCnt;

	/**
	 * internal outputStream
	 */
	private DataOutputStream out;

	// -----------------------CONSTRUCTOR--------------------------------
	/**
	 * @param fileName -
	 *            the filename of the .txt-file, in which the public transit
	 *            user amount and fraction will be saved.
	 * @throws IOException
	 */
	public PtCheck(String fileName) throws IOException {
		out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(new File(fileName))));
		System.out.println("  begins to write txt-file about pt-rate");
		out.writeBytes("Iter\tPtRate\tPtUser\n");
		personCnt = 0;
		ptUserCnt = 0;
	}

	@Override
	public void run(Person person) {
		personCnt++;
		if (person.getSelectedPlan().getType().equals("oev"))
			ptUserCnt++;
	}

	// ---------------------------GETTER------------------------------------
	public double getPtRate() {
		if (personCnt > 0)
			return (double) ptUserCnt / (double) personCnt;
		System.err.println("there is no persons gecheckt!!");
		return -1.0;
	}

	/**
	 * @return the personCnt.
	 */
	public int getPersonCnt() {
		return personCnt;
	}

	/**
	 * @return the ptUserCnt.
	 */
	public int getPtUserCnt() {
		return ptUserCnt;
	}

	// ----------------------------SETTER---------------------------------
	/**
	 * @param personCnt
	 *            The personCnt to set.
	 */
	public void setPersonCnt(int personCnt) {
		this.personCnt = personCnt;
	}

	/**
	 * @param ptUserCnt -
	 *            The ptUserCnt to set.
	 */
	public void setPtUserCnt(int ptUserCnt) {
		this.ptUserCnt = ptUserCnt;
	}

	/**
	 * writes public transit user amount and fraction into .txt-file.
	 * 
	 * @param Iter -
	 *            number of iteration
	 * @throws IOException
	 */
	public void write(int Iter) throws IOException {
		double ptRate = getPtRate();
		double ptUserCnt = getPtUserCnt();
		out.writeBytes(Iter + "\t" + ptRate + "\t" + ptUserCnt + "\n");
//		System.out.println("There are " + ptRate * 100
//				+ "% persons who use Public Transportation! " + ptUserCnt + "/"
//				+ getPersonCnt());
	}

	public void writeEnd() throws IOException {
		out.close();
	}

	/**
	 * resets number of all persons and persons, who use public transit
	 */
	public void resetCnt() {
		setPersonCnt(0);
		setPtUserCnt(0);
	}
}
