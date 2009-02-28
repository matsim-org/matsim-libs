/* *********************************************************************** *
 * project: org.matsim.*
 * FeatureGeneratorBuilder.java
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

package org.matsim.utils.gis.matsim2esri.network;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class FeatureGeneratorBuilder {


	private final Network network;
	private CoordinateReferenceSystem crs;

	private Constructor<? extends FeatureGenerator> featureGeneratorPrototypeContructor;
	private static final Class[] FEATURE_GENERATOR_PROTOTYPECONSTRUCTOR =  { WidthCalculator.class, CoordinateReferenceSystem.class}; 

	private Constructor<? extends WidthCalculator> widthCalculatorPrototypeContructor;

	private double widthCoefficient = 1;
	private static final Class[] WIDTH_CALCULATOR_PROTOTYPECONSTRUCTOR =  { Network.class, Double.class};


	public FeatureGeneratorBuilder(final Network network) {
		this.network = network;
		this.crs = MGC.getCRS(Gbl.getConfig().global().getCoordinateSystem());
		try {
			this.featureGeneratorPrototypeContructor = PolygonFeatureGenerator.class.getConstructor(FEATURE_GENERATOR_PROTOTYPECONSTRUCTOR);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		try {
			this.widthCalculatorPrototypeContructor = LanesBasedWidthCalculator.class.getConstructor(WIDTH_CALCULATOR_PROTOTYPECONSTRUCTOR);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	public FeatureGenerator createFeatureGenerator() {

		WidthCalculator widthCalc = createWidthCalculator();
		FeatureGenerator ret;
		Exception ex;
		try {
			ret = this.featureGeneratorPrototypeContructor.newInstance(new Object[]{widthCalc, this.crs});
			return ret;
		} catch (IllegalArgumentException e) {
			ex = e;
		} catch (InstantiationException e) {
			ex = e;
		} catch (IllegalAccessException e) {
			ex = e;
		} catch (InvocationTargetException e) {
			ex = e;
		}
		throw new RuntimeException(
				"Could not instantiate feature generator from prototype!",
				ex);
	}

	private WidthCalculator createWidthCalculator() {
		WidthCalculator ret;
		Exception ex;
		try {
			ret = this.widthCalculatorPrototypeContructor.newInstance(new Object[] {this.network, this.widthCoefficient});
			return ret;
		} catch (IllegalArgumentException e) {
			ex = e;
		} catch (InstantiationException e) {
			ex = e;
		} catch (IllegalAccessException e) {
			ex = e;
		} catch (InvocationTargetException e) {
			ex = e;
		}
		throw new RuntimeException(
				"Could not instantiate width calculator from prototype!",
				ex);

	}


	public void setWidthCoefficient(final double coef) {
		this.widthCoefficient  = coef;
	}

	public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs) {
		this.crs = crs;
	}

	public void setWidthCalculatorPrototype(final Class<? extends WidthCalculator> prototype) {

		try {
			Constructor<? extends WidthCalculator> c = prototype.getConstructor(WIDTH_CALCULATOR_PROTOTYPECONSTRUCTOR);
			if (null != c) {
				this.widthCalculatorPrototypeContructor = c;
			}
			else {
				throw new IllegalArgumentException("Wrong prototype constructor!");
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	public void setFeatureGeneratorPrototype(final Class<? extends FeatureGenerator> prototype) {

		try {
			Constructor<? extends FeatureGenerator> c = prototype.getConstructor(FEATURE_GENERATOR_PROTOTYPECONSTRUCTOR);
			if (null != c) {
				this.featureGeneratorPrototypeContructor = c;
			}
			else {
				throw new IllegalArgumentException("Wrong prototype constructor!");
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

}
