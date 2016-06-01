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

import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.AbstractInterpolatedEnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumption;

/**
 * TODO: explain, that the model is based on the following paper:
 * http://www2.isr.uc.pt/~carlospatrao/VE/A_sustainability_assessment_of_EVs.pdf
 * 
 * A sustainability assessment of electric vehicles as a personal mobility
 * system R Faria, P Moura, J Delgado, AT de Almeida - Energy Conversion and …,
 * 2012 - Elsevier
 * This model is used in several studies for VSP (Mielec, Berlin)
 * Attention: values are very optimistic, typically resulting in a range of ~180-200km for a 20kWh battery as in the paper almost no aux energy is consumed 
 * @author Zain Ul Abedin
 * 
 */
public class EnergyConsumptionModelRicardoFaria2012 extends AbstractInterpolatedEnergyConsumptionModel {

	public EnergyConsumptionModelRicardoFaria2012() {
		initModell(1.0); 
	}
	
	// Given the very optimistic Value of the original Ricardo/Faria profile, values are multiplied by a factor here.
	public EnergyConsumptionModelRicardoFaria2012(double scalefactor) {
		initModell(scalefactor);
	}

	private void initModell(double scalefactor) {
		queue.add(new EnergyConsumption(5.555555556, scalefactor *3.19E+02));
		queue.add(new EnergyConsumption(8.333333333, scalefactor *3.10E+02));
		queue.add(new EnergyConsumption(11.11111111, scalefactor *3.29E+02));
		queue.add(new EnergyConsumption(13.88888889, scalefactor *3.56E+02));
		queue.add(new EnergyConsumption(16.66666667, scalefactor *4.14E+02));
		queue.add(new EnergyConsumption(19.44444444, scalefactor *4.50E+02));
		queue.add(new EnergyConsumption(22.22222222, scalefactor *5.13E+02));
		queue.add(new EnergyConsumption(25, scalefactor *5.85E+02));
		queue.add(new EnergyConsumption(27.77777778, scalefactor *6.62E+02));
		queue.add(new EnergyConsumption(30.55555556, scalefactor *7.52E+02));
		queue.add(new EnergyConsumption(33.33333333, scalefactor *8.46E+02));
	}

}
