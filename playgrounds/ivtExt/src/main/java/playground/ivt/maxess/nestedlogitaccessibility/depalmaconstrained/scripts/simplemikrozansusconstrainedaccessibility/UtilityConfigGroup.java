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
package playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained.scripts.simplemikrozansusconstrainedaccessibility;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class UtilityConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "simpleUtility";

	public enum FunctionalForm {linear,log}

	private double betaTtCar = -1;
	private double betaTtPt = -1;
	private FunctionalForm functionalForm = FunctionalForm.log;
	private boolean alwaysUseCar = false;

	public UtilityConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter("betaTtCar")
	public double getBetaTtCar() {
		return betaTtCar;
	}

	@StringSetter("betaTtCar")
	public void setBetaTtCar( final double betaTtCar ) {
		this.betaTtCar = betaTtCar;
	}

	@StringGetter("betaTtPt")
	public double getBetaTtPt() {
		return betaTtPt;
	}

	@StringSetter("betaTtPt")
	public void setBetaTtPt( final double betaTtPt ) {
		this.betaTtPt = betaTtPt;
	}

	@StringGetter("functionalForm")
	public FunctionalForm getFunctionalForm() {
		return functionalForm;
	}

	@StringSetter("functionalForm")
	public void setFunctionalForm( final FunctionalForm functionalForm ) {
		this.functionalForm = functionalForm;
	}

	@StringGetter("alwaysUseCar")
	public boolean getAlwaysUseCar() {
		return alwaysUseCar;
	}

	@StringSetter("alwaysUseCar")
	public void setAlwaysUseCar(boolean alwaysUseCar) {
		this.alwaysUseCar = alwaysUseCar;
	}
}
