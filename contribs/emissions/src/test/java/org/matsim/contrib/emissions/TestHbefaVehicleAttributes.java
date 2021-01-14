/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestHbefaVehicleAttributesEmission.java                                 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.contrib.emissions;

import org.junit.Assert;
import org.junit.Test;


/*
 * test for playground.vsp.emissions.types.HbefaVehicleAttributes
 * 1 test equals method for two complete vehicle attributes
 * 2 test equals method for one complete and one incomplete vehicle attributes
 * 3 test the initialization
 */

public class TestHbefaVehicleAttributes {
	private final String technology = "technology";
	private final String sizeClass = "size class";
	private final String concept = "concept";
	private String message;
	private String assertErrorMessage;
	private HbefaVehicleAttributes normal;
	private HbefaVehicleAttributes differentValues;

	@Test
	public final void testEqualsForCompleteAttributes(){

		//two equal objects - no default values
		normal = new HbefaVehicleAttributes();
		setToNormal(normal);

		HbefaVehicleAttributes compare = new HbefaVehicleAttributes();
		compare.setHbefaTechnology(technology);
		compare.setHbefaSizeClass(sizeClass);
		compare.setHbefaEmConcept(concept);

		String assertErrorMessage = "These hbefa vehicle attribute objects should have been the same: ";
		assertErrorMessage += normal.toString() + " and " + compare.toString();
		Assert.assertEquals(assertErrorMessage, normal, compare);
		Assert.assertEquals(assertErrorMessage, compare, normal);

	}

	@Test
	public final void testEqualsForCompleteAttributes_emConcept(){

		//two unequal but complete objects
		normal = new HbefaVehicleAttributes();
		setToNormal(normal);

		differentValues = new HbefaVehicleAttributes();

		//different em concepts
		differentValues.setHbefaEmConcept("concept2");
		differentValues.setHbefaSizeClass(sizeClass);
		differentValues.setHbefaTechnology(technology);
		assertErrorMessage = "These hbefa vehicle attribute objects should not have been the same: ";
		assertErrorMessage += normal.toString() + " and " + differentValues.toString();
		Assert.assertNotEquals(assertErrorMessage, normal, differentValues);
		Assert.assertNotEquals(assertErrorMessage, differentValues, normal);
	}

	@Test
	public final void testEqualsForCompleteAttributes_sizeClass(){

		//two unequal but complete objects
		normal = new HbefaVehicleAttributes();
		setToNormal(normal);
		differentValues = new HbefaVehicleAttributes();

		//different size classes
		differentValues.setHbefaEmConcept(concept);
		differentValues.setHbefaSizeClass("small size");
		differentValues.setHbefaTechnology(technology);
		assertErrorMessage = "These hbefa vehicle attribute objects should not have been the same: ";
		assertErrorMessage += normal.toString() + " and " + differentValues.toString();
		Assert.assertNotEquals(assertErrorMessage, normal, differentValues);
		Assert.assertNotEquals(assertErrorMessage, differentValues, normal);

	}

	@Test
	public final void testEqualsForCompleteAttributes_technologies(){

		//two unequal but complete objects
		normal = new HbefaVehicleAttributes();
		setToNormal(normal);
		differentValues = new HbefaVehicleAttributes();

		//different technologies
		differentValues.setHbefaEmConcept(concept);
		differentValues.setHbefaSizeClass(sizeClass);
		differentValues.setHbefaTechnology("other technology");
		assertErrorMessage = "These hbefa vehicle attribute objects should not have been the same: ";
		assertErrorMessage += normal.toString() + " and " + differentValues.toString();
		Assert.assertNotEquals(assertErrorMessage, normal, differentValues);
		Assert.assertNotEquals(assertErrorMessage, differentValues, normal);

	}

	// the following tests each compare incomplete to complete attribute data
	// if some of the vehicle attributes are not set manually they are set to 'average' by default
	// thus, the equals method should not throw nullpointer exceptions but return false or respectively true

	@Test
	public final void testEqualsForIncompleteAttributes_emConcept(){
		//generate a complete key and set its parameters
		normal = new HbefaVehicleAttributes();
		setToNormal(normal);

		//em concept missing
		HbefaVehicleAttributes noEmConcept = new HbefaVehicleAttributes();
		noEmConcept.setHbefaSizeClass(sizeClass);
		noEmConcept.setHbefaTechnology(technology);
		message = "no em concept was set, therefore " + normal.getHbefaEmConcept() + " should not equal " + noEmConcept.getHbefaEmConcept();
		Assert.assertNotEquals(message, noEmConcept, normal);
		Assert.assertNotEquals(message, normal, noEmConcept);

		normal.setHbefaEmConcept("average");
		message = "no em concept was set, therefore " + noEmConcept.getHbefaEmConcept() + "should be set to 'average'";
		Assert.assertEquals(message, noEmConcept, normal);
		Assert.assertEquals(message, normal, noEmConcept);
	}


	@Test
	public final void testEqualsForIncompleteAttributes_technology() {

		// generate a complete key and set its parameters
		normal = new HbefaVehicleAttributes();
		setToNormal(normal);

		// technology missing
		HbefaVehicleAttributes noTechnology = new HbefaVehicleAttributes();
		noTechnology.setHbefaEmConcept(concept);
		noTechnology.setHbefaSizeClass(sizeClass);
		message = "no technology was set, therefore " + normal.getHbefaTechnology() + " should not equal " + noTechnology.getHbefaTechnology();
		Assert.assertNotEquals(message, noTechnology, normal);
		Assert.assertNotEquals(message, normal, noTechnology);

		//set the hbefa technology of the normal vehicle attributes to 'average'
		//then noTechnology is equal to normal
		normal.setHbefaTechnology("average");
		message = "no em concept was set, therefore " + noTechnology.getHbefaEmConcept() + "should be set to 'average'";
		Assert.assertEquals(message, noTechnology, normal);
		Assert.assertEquals(message, normal, noTechnology);

	}

	@Test
	public final void testEqualsForIncompleteAttributes_sizeClass(){

		//generate a complete key and set its parameters
		normal = new HbefaVehicleAttributes();
		setToNormal(normal);

		//size class missing
		setToNormal(normal);
		HbefaVehicleAttributes noSize = new HbefaVehicleAttributes();
		noSize.setHbefaEmConcept(concept);
		noSize.setHbefaTechnology(technology);
		message = "no size class was set, therefore " + normal.getHbefaSizeClass() + " should not equal " + noSize.getHbefaSizeClass();
		Assert.assertNotEquals(message, noSize, normal);
		Assert.assertNotEquals(message, normal, noSize);

		normal.setHbefaSizeClass("average");
		message = "no size class was set, therefore " + noSize.getHbefaEmConcept() + "should be set to 'average'";
		Assert.assertEquals(message, noSize, normal);
		Assert.assertEquals(message, normal, noSize);

	}

	private void setToNormal(HbefaVehicleAttributes normal) {
		normal.setHbefaEmConcept(concept);
		normal.setHbefaSizeClass(sizeClass);
		normal.setHbefaTechnology(technology);
	}

}
	

	

