/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.av.robotaxi.fares.taxi;/*
 * created by jbischoff, 11.12.2018
 */

import javax.inject.Inject;

import org.matsim.core.controler.AbstractModule;

public class TaxiFareModule extends AbstractModule {
	@Inject
	private TaxiFaresConfigGroup taxiFaresConfigGroup;

	@Override
	public void install() {
		for (TaxiFareConfigGroup taxiFareCfg : taxiFaresConfigGroup.getTaxiFareConfigGroups()) {
			addEventHandlerBinding().toInstance(new TaxiFareHandler(taxiFareCfg));
		}
	}
}
