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
package playground.ivt.maxess.nestedlogitaccessibility.scripts.simpleleisure;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class SimpleNestedLogitUtilityConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "simpleNestedUtility";

	private double muCar = 1;
	private double muPt = 1;
	private double muBike = 6.4;
	private double muWalk = 1.74;

	private double ascCar = 0;
	private double betaTtCar = -0.276;

	private double ascPt = 0.0644;
	private double betaTtPt = -0.508;
	private double betaTtPtGa = 0.158;
	private double betaTtPtHt = 0.0653;
	private double betaTtPtLocal = 0.169;

	private double ascBike = 1.85;
	private double betaTtBike = -0.235;
	private double betaLicenseBike = -0.614;

	private double ascWalk = 6.86;
	private double betaTtWalk = -0.917;


	public SimpleNestedLogitUtilityConfigGroup( ) {
		super( GROUP_NAME );
	}

	@StringGetter("ascBike")
	public double getAscBike() {
		return ascBike;
	}

	@StringSetter("ascBike")
	public void setAscBike( final double ascBike ) {
		this.ascBike = ascBike;
	}

	@StringGetter("ascCar")
	public double getAscCar() {
		return ascCar;
	}

	@StringSetter("ascCar")
	public void setAscCar( final double ascCar ) {
		this.ascCar = ascCar;
	}

	@StringGetter("ascPt")
	public double getAscPt() {
		return ascPt;
	}

	@StringSetter("ascPt")
	public void setAscPt( final double ascPt ) {
		this.ascPt = ascPt;
	}

	@StringGetter("ascWalk")
	public double getAscWalk() {
		return ascWalk;
	}

	@StringSetter("ascWalk")
	public void setAscWalk( final double ascWalk ) {
		this.ascWalk = ascWalk;
	}

	@StringGetter("betaLicenseBike")
	public double getBetaLicenseBike() {
		return betaLicenseBike;
	}

	@StringSetter("betaLicenseBike")
	public void setBetaLicenseBike( final double betaLicenseBike ) {
		this.betaLicenseBike = betaLicenseBike;
	}

	@StringGetter("betaTtBike")
	public double getBetaTtBike() {
		return betaTtBike;
	}

	@StringSetter("betaTtBike")
	public void setBetaTtBike( final double betaTtBike ) {
		this.betaTtBike = betaTtBike;
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

	@StringGetter("betaTtPtGa")
	public double getBetaTtPtGa() {
		return betaTtPtGa;
	}

	@StringSetter("betaTtPtGa")
	public void setBetaTtPtGa( final double betaTtPtGa ) {
		this.betaTtPtGa = betaTtPtGa;
	}

	@StringGetter("betaTtPtHt")
	public double getBetaTtPtHt() {
		return betaTtPtHt;
	}

	@StringSetter("betaTtPtHt")
	public void setBetaTtPtHt( final double betaTtPtHt ) {
		this.betaTtPtHt = betaTtPtHt;
	}

	@StringGetter("betaTtPtLocal")
	public double getBetaTtPtLocal() {
		return betaTtPtLocal;
	}

	@StringSetter("betaTtPtLocal")
	public void setBetaTtPtLocal( final double betaTtPtLocal ) {
		this.betaTtPtLocal = betaTtPtLocal;
	}

	@StringGetter("betaTtWalk")
	public double getBetaTtWalk() {
		return betaTtWalk;
	}

	@StringSetter("betaTtWalk")
	public void setBetaTtWalk( final double betaTtWalk ) {
		this.betaTtWalk = betaTtWalk;
	}

	@StringGetter("muBike")
	public double getMuBike() {
		return muBike;
	}

	@StringSetter("muBike")
	public void setMuBike( final double muBike ) {
		this.muBike = muBike;
	}

	@StringGetter("muCar")
	public double getMuCar() {
		return muCar;
	}

	@StringSetter("muCar")
	public void setMuCar( final double muCar ) {
		this.muCar = muCar;
	}

	@StringGetter("muPt")
	public double getMuPt() {
		return muPt;
	}

	@StringSetter("muPt")
	public void setMuPt( final double muPt ) {
		this.muPt = muPt;
	}

	@StringGetter("muWalk")
	public double getMuWalk() {
		return muWalk;
	}

	@StringSetter("muWalk")
	public void setMuWalk( final double muWalk ) {
		this.muWalk = muWalk;
	}
}
