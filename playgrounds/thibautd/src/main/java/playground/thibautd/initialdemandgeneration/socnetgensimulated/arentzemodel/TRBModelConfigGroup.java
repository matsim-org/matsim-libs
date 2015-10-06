/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.arentzemodel;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class TRBModelConfigGroup extends ReflectiveConfigGroup {
	private static final String GROUP_NAME = "utility";

	private double b_logDist = -1.222;
	private double b_sameGender = 0.725;
	private double b_ageDiff0 = 0.918;
	private double b_ageDiff2 = -0.227;
	private double b_ageDiff3 = -1.314;
	private double b_ageDiff4 = -1.934;

	public TRBModelConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter("b_logDist")
	public double getB_logDist() {
		return b_logDist;
	}

	@StringSetter("b_logDist")
	public void setB_logDist(double b_logDist) {
		this.b_logDist = b_logDist;
	}

	@StringGetter("b_sameGender")
	public double getB_sameGender() {
		return b_sameGender;
	}

	@StringSetter("b_sameGender")
	public void setB_sameGender(double b_sameGender) {
		this.b_sameGender = b_sameGender;
	}

	@StringGetter("b_ageDiff0")
	public double getB_ageDiff0() {
		return b_ageDiff0;
	}

	@StringSetter("b_ageDiff0")
	public void setB_ageDiff0(double b_ageDiff0) {
		this.b_ageDiff0 = b_ageDiff0;
	}

	@StringGetter("b_ageDiff2")
	public double getB_ageDiff2() {
		return b_ageDiff2;
	}

	@StringSetter("b_ageDiff2")
	public void setB_ageDiff2(double b_ageDiff2) {
		this.b_ageDiff2 = b_ageDiff2;
	}

	@StringGetter("b_ageDiff3")
	public double getB_ageDiff3() {
		return b_ageDiff3;
	}

	@StringSetter("b_ageDiff3")
	public void setB_ageDiff3(double b_ageDiff3) {
		this.b_ageDiff3 = b_ageDiff3;
	}

	@StringGetter("b_ageDiff4")
	public double getB_ageDiff4() {
		return b_ageDiff4;
	}

	@StringSetter("b_ageDiff4")
	public void setB_ageDiff4(double b_ageDiff4) {
		this.b_ageDiff4 = b_ageDiff4;
	}
}
