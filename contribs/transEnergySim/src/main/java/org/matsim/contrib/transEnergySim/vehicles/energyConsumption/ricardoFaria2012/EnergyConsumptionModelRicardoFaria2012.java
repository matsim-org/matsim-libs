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

package org.matsim.contrib.transEnergySim.vehicles.energyConsumption.ricardoFaria2012;

import java.util.Iterator;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.AbstractInterpolatedEnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumption;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;

/**
 * TODO: explain, that the model is based on the following paper:
 * http://www2.isr.uc.pt/~carlospatrao/VE/A_sustainability_assessment_of_EVs.pdf
 * 
 * A sustainability assessment of electric vehicles as a personal mobility
 * system R Faria, P Moura, J Delgado, AT de Almeida - Energy Conversion and …,
 * 2012 - Elsevier
 * 
 * 
 * @author Zain Ul Abedin
 * 
 */
public class EnergyConsumptionModelRicardoFaria2012 extends AbstractInterpolatedEnergyConsumptionModel {

	public EnergyConsumptionModelRicardoFaria2012() {
		initModell();
	}

	private void initModell() {
		queue.add(new EnergyConsumption(5.555555556, 3.19E+02));
		queue.add(new EnergyConsumption(8.333333333, 3.10E+02));
		queue.add(new EnergyConsumption(11.11111111, 3.29E+02));
		queue.add(new EnergyConsumption(13.88888889, 3.56E+02));
		queue.add(new EnergyConsumption(16.66666667, 4.14E+02));
		queue.add(new EnergyConsumption(19.44444444, 4.50E+02));
		queue.add(new EnergyConsumption(22.22222222, 5.13E+02));
		queue.add(new EnergyConsumption(25, 5.85E+02));
		queue.add(new EnergyConsumption(27.77777778, 6.62E+02));
		queue.add(new EnergyConsumption(30.55555556, 7.52E+02));
		queue.add(new EnergyConsumption(33.33333333, 8.46E+02));
	}

}
