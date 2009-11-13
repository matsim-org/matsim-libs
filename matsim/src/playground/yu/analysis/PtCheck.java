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

package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * outputs the amount of public transit user or its fraction into .txt-files
 * 
 * @author ychen
 * 
 */
public class PtCheck extends AbstractPersonAlgorithm {
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
	 * @param fileName
	 *            - the filename of the .txt-file, in which the public transit
	 *            user amount and fraction will be saved.
	 * @throws IOException
	 */
	public PtCheck(String fileName) throws IOException {
		this.out = IOUtils.getBufferedWriter(fileName);
		System.out.println("-->begins to write txt-file about pt-rate");
		this.out.write("Iter\tPersons\tPtRate\tPtUser\tCarUser\n");
		this.out.flush();
		this.personCnt = 0;
		this.ptUserCnt = 0;
		this.carUserCnt = 0;
	}

	public PtCheck() {
		this.personCnt = 0;
		this.ptUserCnt = 0;
		this.carUserCnt = 0;
	}

	@Override
	public void run(Person person) {
		this.personCnt++;
		// Plan.Type planType = person.getSelectedPlan().getType();
		Plan selectedPlan = person.getSelectedPlan();
		if (
		// (planType != null) && (Plan.Type.UNDEFINED != planType)
		!PlanModeJudger.useUndefined(selectedPlan)) {
			if (
			// planType.equals(Plan.Type.PT)
			PlanModeJudger.usePt(selectedPlan)) {
				this.ptUserCnt++;
			} else if (
			// planType.equals(Plan.Type.CAR)
			PlanModeJudger.useCar(selectedPlan)) {
				this.carUserCnt++;
			}
		}
	}

	// ---------------------------GETTER------------------------------------
	public double getPtRate() {
		if (this.personCnt > 0)
			return (double) this.ptUserCnt / (double) this.personCnt;
		System.err.println("there is no persons gecheckt!!");
		return -1.0;
	}

	/**
	 * @return the personCnt.
	 */
	public int getPersonCnt() {
		return this.personCnt;
	}

	/**
	 * @return the ptUserCnt.
	 */
	public int getPtUserCnt() {
		return this.ptUserCnt;
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
	 * @param ptUserCnt
	 *            - The ptUserCnt to set.
	 */
	public void setPtUserCnt(int ptUserCnt) {
		this.ptUserCnt = ptUserCnt;
	}

	/**
	 * writes public transit user amount and fraction into .txt-file.
	 * 
	 * @param Iter
	 *            - number of iteration
	 * @throws IOException
	 */
	public void write(int Iter) throws IOException {
		this.out.write(Iter + "\t" + this.personCnt + "\t" + getPtRate() + "\t"
				+ getPtUserCnt() + "\t" + this.carUserCnt + "\n");
		// System.out.println("There are " + ptRate * 100
		// + "% persons who use Public Transportation! " + ptUserCnt + "/"
		// + getPersonCnt());
		this.out.flush();
	}

	public void writeEnd() throws IOException {
		this.out.close();
	}

	/**
	 * resets number of all persons and persons, who use public transit
	 */
	public void resetCnt() {
		setPersonCnt(0);
		setPtUserCnt(0);
		this.carUserCnt = 0;
	}
}
