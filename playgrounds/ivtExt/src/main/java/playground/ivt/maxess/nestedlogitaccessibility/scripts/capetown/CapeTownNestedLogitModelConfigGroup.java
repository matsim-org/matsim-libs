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
package playground.ivt.maxess.nestedlogitaccessibility.scripts.capetown;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class CapeTownNestedLogitModelConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "capeTownNestedLogitUtility";

	private double muCar = 1;
	private double muPt = 1;
	private double muWalk = 1;
	private double muRide = 1;
	private double muTaxi = 1;

	private double ascCar = 0;
	private double betaTtCar = 0;
	private double betaNCarsPerPerson = 0;

	private double ascPt = 0;
	private double betaTtPt = 0;

	private double ascWalk = 0;
	private double betaTtWalk = -0;

	private double ascRide = 0;
	private double betaTtRide = -0;

	private double ascTaxi = 0;
	private double betaTtTaxi = -0;

	public CapeTownNestedLogitModelConfigGroup() {
		super( GROUP_NAME );
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

	@StringGetter("betaTtWalk")
	public double getBetaTtWalk() {
		return betaTtWalk;
	}

	@StringSetter("betaTtWalk")
	public void setBetaTtWalk( final double betaTtWalk ) {
		this.betaTtWalk = betaTtWalk;
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

	@StringGetter("betaNCarsPerPerson")
	public double getBetaNCarsPerPerson() {
		return betaNCarsPerPerson;
	}

	@StringSetter("betaNCarsPerPerson")
	public void setBetaNCarsPerPerson( final double betaNCarsPerPerson ) {
		this.betaNCarsPerPerson = betaNCarsPerPerson;
	}

	@StringGetter("muRide")
	public double getMuRide() {
		return muRide;
	}

	@StringSetter("muRide")
	public void setMuRide(double muRide) {
		this.muRide = muRide;
	}

	@StringGetter("muTaxi")
	public double getMuTaxi() {
		return muTaxi;
	}

	@StringSetter("muTaxi")
	public void setMuTaxi(double muTaxi) {
		this.muTaxi = muTaxi;
	}

	@StringGetter("ascRide")
	public double getAscRide() {
		return ascRide;
	}

	@StringSetter("ascRide")
	public void setAscRide(double ascRide) {
		this.ascRide = ascRide;
	}

	@StringGetter("betaTtRide")
	public double getBetaTtRide() {
		return betaTtRide;
	}

	@StringSetter("betaTtRide")
	public void setBetaTtRide(double betaTtRide) {
		this.betaTtRide = betaTtRide;
	}

	@StringGetter("ascTaxi")
	public double getAscTaxi() {
		return ascTaxi;
	}

	@StringSetter("ascTaxi")
	public void setAscTaxi(double ascTaxi) {
		this.ascTaxi = ascTaxi;
	}

	@StringGetter("betaTtTaxi")
	public double getBetaTtTaxi() {
		return betaTtTaxi;
	}

	@StringSetter("betaTtTaxi")
	public void setBetaTtTaxi(double betaTtTaxi) {
		this.betaTtTaxi = betaTtTaxi;
	}
}
