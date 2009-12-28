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
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * outputs the amount of public transit user or its fraction into .txt-files
 * 
 * @author ychen
 * 
 */
public class PtCheck2 extends AbstractPersonAlgorithm {
	// ----------------------MEMBER VARIABLES----------------------------
	/**
	 * Counter of all read persons
	 */
	private int personCnt;

	private int licensedCnt;

	private int licensedCarUserCnt;

	/**
	 * @param licensedPtUserCnt
	 *            - Counter of all persons, who use public transport
	 */
	private int licensedPtUserCnt;

	private int licensedOtherModalCnt;

	/**
	 * @param out
	 *            - internal outputStream
	 */
	private BufferedWriter out;

	// -----------------------CONSTRUCTOR--------------------------------
	/**
	 * @param fileName
	 *            - the filename of the .txt-file, in which the public transit
	 *            user amount and fraction will be saved.
	 * @throws IOException
	 */
	public PtCheck2(String fileName) throws IOException {
		this.out = IOUtils.getBufferedWriter(fileName);
		System.out.println("-->begins to write txt-file about pt-rate");
		this.out
				.write("Iter\tPersons\tlicensed\tlicensedPtUser\tptRate in licensed\tlicensedCarUser\tcarRate in licensed\totherModals\totherModalsRate in licensed\n");
		this.out.flush();
		this.personCnt = 0;
		this.licensedCnt = 0;
		this.licensedPtUserCnt = 0;
		this.licensedCarUserCnt = 0;
		this.licensedOtherModalCnt = 0;
	}

	@Override
	public void run(Person pp) {
		PersonImpl person = (PersonImpl) pp;
		this.personCnt++;
		if (person.getLicense().equals("yes")) {
			this.licensedCnt++;
			// Plan.Type planType = person.getSelectedPlan().getType();
			Plan selectedPlan = person.getSelectedPlan();
			if (
			// (planType != null) && (Plan.Type.UNDEFINED != planType)
			!PlanModeJudger.useUndefined(selectedPlan)) {
				if (
				// planType.equals(Plan.Type.PT)
				PlanModeJudger.usePt(selectedPlan)) {
					this.licensedPtUserCnt++;
				} else if (
				// planType.equals(Plan.Type.CAR)
				PlanModeJudger.useCar(selectedPlan)) {
					this.licensedCarUserCnt++;
				} else {
					this.licensedOtherModalCnt++;
				}
			}
		}
	}

	// ---------------------------GETTER------------------------------------
	public double getLicensedPtRate() {
		if (this.licensedCnt > 0)
			return (double) this.licensedPtUserCnt / (double) this.licensedCnt;
		System.err.println("there is no persons licensed gecheckt!!");
		return -1.0;
	}

	public double getLicensedCarRate() {
		if (this.licensedCnt > 0)
			return (double) this.licensedCarUserCnt / (double) this.licensedCnt;
		System.err.println("there is no persons licensed gecheckt!!");
		return -1.0;
	}

	public double getLicensedOtherModalRate() {
		if (this.licensedCnt > 0)
			return (double) this.licensedOtherModalCnt
					/ (double) this.licensedCnt;
		System.err.println("there is no persons licensed gecheckt!!");
		return -1.0;
	}

	/**
	 * @return the licensedCarUserCnt
	 */
	public int getLicensedCarUserCnt() {
		return this.licensedCarUserCnt;
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
	public int getLicensedPtUserCnt() {
		return this.licensedPtUserCnt;
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
	public void setLicensedPtUserCnt(int ptUserCnt) {
		this.licensedPtUserCnt = ptUserCnt;
	}

	/**
	 * writes public transit user amount and fraction into .txt-file.
	 * 
	 * @param Iter
	 *            - number of iteration
	 * @throws IOException
	 */
	public void write(int Iter) throws IOException {
		this.out.write(Iter + "\t" + this.personCnt + "\t" + this.licensedCnt
				+ "\t" + this.licensedPtUserCnt + "\t" + getLicensedPtRate()
				+ "\t" + this.licensedCarUserCnt + "\t" + getLicensedCarRate()
				+ "\t" + this.licensedOtherModalCnt + "\t"
				+ getLicensedOtherModalRate() + "\n");
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
		setLicensedPtUserCnt(0);
		this.licensedCarUserCnt = 0;
		this.licensedCnt = 0;
		this.licensedOtherModalCnt = 0;
	}

	/**
	 * @return the licensedCnt
	 */
	public int getLicensedCnt() {
		return this.licensedCnt;
	}
}
