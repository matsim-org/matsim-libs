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

package playground.yu.ivtch;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.plans.Person;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.utils.io.IOUtils;

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
	private int carUserCnt;
	/**
	 * Counter of all persons, who use public transport
	 */
	private int ptUserCnt;

	/**
	 * internal outputStream
	 */
	private BufferedWriter out;

	// -----------------------CONSTRUCTOR--------------------------------
	/**
	 * @param fileName -
	 *            the filename of the .txt-file, in which the public transit
	 *            user amount and fraction will be saved.
	 * @throws IOException
	 */
	public PtCheck(String fileName) throws IOException {
		out = IOUtils.getBufferedWriter(fileName);
		System.out.println("-->begins to write txt-file about pt-rate");
		out.write("Iter\tPersons\tPtRate\tPtUser\tCarUser\n");
		out.flush();
		personCnt = 0;
		ptUserCnt = 0;
		carUserCnt = 0;
	}

	@Override
	public void run(Person person) {
		personCnt++;
		String planType = person.getSelectedPlan().getType();
		if (planType != null) {
			if (planType.equals("pt")) {
				ptUserCnt++;
			} else if (planType.equals("car")) {
				carUserCnt++;
			}
		}
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
		out.write(Iter + "\t" + personCnt + "\t" + getPtRate() + "\t"
				+ getPtUserCnt() + "\t" + carUserCnt + "\n");
		// System.out.println("There are " + ptRate * 100
		// + "% persons who use Public Transportation! " + ptUserCnt + "/"
		// + getPersonCnt());
		out.flush();
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
		carUserCnt = 0;
	}
}
