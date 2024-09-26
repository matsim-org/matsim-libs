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

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;

/**
 * Design thoughts:<ul>
 * <li> I would argue that basing this on reflection is software design overkill.  In my understanding, reflection is useful to decouple
 * pieces of software, i.e. be able to insert a class (a constructor) into compiled code (e.g. a jar file) if I do not have access to the source
 * code of the jar file.  This is, however, not the situation here.  kai, mar'14
 * <li> Practically, the current design is in my way.  I need additional information inside the width calculator, because I do not want to
 * calculate width based on a network property, but on something else (speed differences).  So now I will have to abuse "capacity"
 * for the quantity of interest. kai, mar'14
 * </ul
 */
public class FeatureGeneratorBuilderImpl implements FeatureGeneratorBuilder {


	private final Network network;
	private CoordinateReferenceSystem crs;

	private Constructor<? extends FeatureGenerator> featureGeneratorPrototypeContructor;
	private static final Class[] FEATURE_GENERATOR_PROTOTYPECONSTRUCTOR =  { WidthCalculator.class, CoordinateReferenceSystem.class};

	private Constructor<? extends WidthCalculator> widthCalculatorPrototypeContructor;

	private double widthCoefficient = 1;
	private static final Class[] WIDTH_CALCULATOR_PROTOTYPECONSTRUCTOR =  { Network.class, Double.class};


	public FeatureGeneratorBuilderImpl(final Network network, final String coordinateSystem) {
		this.network = network;
		this.crs = MGC.getCRS(coordinateSystem);
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

	@Override
	public FeatureGenerator createFeatureGenerator() {

		WidthCalculator widthCalc = createWidthCalculator();
		FeatureGenerator ret;
		Exception ex;
		try {
			ret = this.featureGeneratorPrototypeContructor.newInstance(widthCalc, this.crs);
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
				"Could not instantiate feature generator from prototype! " + this.featureGeneratorPrototypeContructor.getDeclaringClass().getCanonicalName(),
				ex);
	}

	private WidthCalculator createWidthCalculator() {
		WidthCalculator ret;
		Exception ex;
		try {
			ret = this.widthCalculatorPrototypeContructor.newInstance(this.network, this.widthCoefficient);
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
