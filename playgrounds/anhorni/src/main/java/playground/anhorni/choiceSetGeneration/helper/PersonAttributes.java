/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.choiceSetGeneration.helper;

public class PersonAttributes {
	
	private double WP;
	private int age;
	private int gender;
	private int incomeHH;
	private int numberOfPersonsHH;
	private int civilStatus;
	private int education;
	private int start_is_home;
	
	
	
	public PersonAttributes(double wp, int age, int gender, int incomeHH,
			int numberOfPersonsHH, int civilStatus, int education) {
		super();
		WP = wp;
		this.age = age;
		this.gender = gender;
		this.incomeHH = incomeHH;
		this.numberOfPersonsHH = numberOfPersonsHH;
		this.civilStatus = civilStatus;
		this.education = education;
	}
	
	
	public double getWP() {
		return WP;
	}
	public void setWP(double wp) {
		WP = wp;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public int getGender() {
		return gender;
	}
	public void setGender(int gender) {
		this.gender = gender;
	}
	public int getIncomeHH() {
		return incomeHH;
	}
	public void setIncomeHH(int incomeHH) {
		this.incomeHH = incomeHH;
	}
	public int getNumberOfPersonsHH() {
		return numberOfPersonsHH;
	}
	public void setNumberOfPersonsHH(int numberOfPersonsHH) {
		this.numberOfPersonsHH = numberOfPersonsHH;
	}
	public int getCivilStatus() {
		return civilStatus;
	}
	public void setCivilStatus(int civilStatus) {
		this.civilStatus = civilStatus;
	}
	public int getEducation() {
		return education;
	}
	public void setEducation(int education) {
		this.education = education;
	}
	public int getStart_is_home() {
		return start_is_home;
	}
	public void setStart_is_home(int start_is_home) {
		this.start_is_home = start_is_home;
	}
}
