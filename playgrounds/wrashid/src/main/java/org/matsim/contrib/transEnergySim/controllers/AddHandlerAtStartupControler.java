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

package org.matsim.contrib.transEnergySim.controllers;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.road.InductiveStreetCharger;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.api.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.galus.EnergyConsumptionModelGalus;
import org.matsim.contrib.transEnergySim.vehicles.impl.IC_BEV;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;

import com.sun.tools.xjc.reader.internalizer.DOMForest.Handler;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelPSL;
/**
 * @author wrashid
 *
 */
public class AddHandlerAtStartupControler extends Controler {

	protected LinkedList<EventHandler> handler = new LinkedList<EventHandler>();

	public AddHandlerAtStartupControler(Config config) {
		super(config);
		addControlerListener(new EventHandlerAdder());
	}

	public AddHandlerAtStartupControler(String[] args) {
		super(args);
		addControlerListener(new EventHandlerAdder());
	}

	public void addHandler(EventHandler handler) {
		this.handler.add(handler);
	}

	private class EventHandlerAdder implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {
			for (EventHandler h : handler) {
				getEvents().addHandler(h);
			}
		}

	}

}
