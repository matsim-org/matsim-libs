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

package playground.emissions.types;

import org.junit.Assert;
import org.junit.Test;

import playground.vsp.emissions.types.HbefaVehicleAttributes;

/*
 * test for playground.vsp.emissions.types.HbefaVehicleAttributes
 * 1 test equals method for two correct objects
 * 2 test equals method for one correct and one partially initiated object
 * 3 test the initialization
 */

public class TestHbefaVehicleAttributes {
	String technology = "technology", sizeClass = "size class", concept = "concept";
	
	@Test
	public final void testEqualsForCompleteKeys(){
		
		//two equal objects - no default values
		
		HbefaVehicleAttributes normal = new HbefaVehicleAttributes();
		setToNormal(normal);
		
		HbefaVehicleAttributes compare = new HbefaVehicleAttributes();
		compare.setHbefaTechnology(technology);
		compare.setHbefaSizeClass(sizeClass);
		compare.setHbefaEmConcept(concept);
		
		String assertErrorMessage = "These hbefa vehicle attribute objects should have been the same: ";
		assertErrorMessage += normal.toString() + " and " + compare.toString();
		Assert.assertTrue(assertErrorMessage, normal.equals(compare));
		Assert.assertTrue(assertErrorMessage, compare.equals(normal));
		
		//two unequal but correct objects
		HbefaVehicleAttributes differentValues = new HbefaVehicleAttributes();
		
		//different em concepts
		differentValues.setHbefaEmConcept("concept2");
		differentValues.setHbefaSizeClass(sizeClass);
		differentValues.setHbefaTechnology(technology);
		assertErrorMessage = "These hbefa vehicle attribute objects should not have been the same: ";
		assertErrorMessage += normal.toString() + " and " + differentValues.toString();
		Assert.assertFalse(assertErrorMessage, normal.equals(differentValues));
		Assert.assertFalse(assertErrorMessage, differentValues.equals(normal));

		//different size classes
		differentValues.setHbefaEmConcept(concept);
		differentValues.setHbefaSizeClass("small size");
		differentValues.setHbefaTechnology(technology);
		assertErrorMessage = "These hbefa vehicle attribute objects should not have been the same: ";
		assertErrorMessage += normal.toString() + " and " + differentValues.toString();
		Assert.assertFalse(assertErrorMessage, normal.equals(differentValues));
		Assert.assertFalse(assertErrorMessage, differentValues.equals(normal));
		
		//different technologies
		differentValues.setHbefaEmConcept(concept);
		differentValues.setHbefaSizeClass(sizeClass);
		differentValues.setHbefaTechnology("other technology");
		assertErrorMessage = "These hbefa vehicle attribute objects should not have been the same: ";
		assertErrorMessage += normal.toString() + " and " + differentValues.toString();
		Assert.assertFalse(assertErrorMessage, normal.equals(differentValues));
		Assert.assertFalse(assertErrorMessage, differentValues.equals(normal));
		
	}
	
	@Test
	public final void testEqualsForIncompleteKeys(){
		//correct object - no default values

		String message; 
		
		HbefaVehicleAttributes normal = new HbefaVehicleAttributes();
		
		//one correct object, one partial objects
		
		//em concept missing
		setToNormal(normal);
		HbefaVehicleAttributes noEmConcept = new HbefaVehicleAttributes();
		noEmConcept.setHbefaSizeClass(sizeClass);
		noEmConcept.setHbefaTechnology(technology);
		message = "no em concept was set, therefore " + normal.getHbefaEmConcept() + " should not equal " + noEmConcept.getHbefaEmConcept();
		Assert.assertFalse(message, noEmConcept.equals(normal));
		Assert.assertFalse(message, normal.equals(noEmConcept));
		
		normal.setHbefaEmConcept("average");
		message = "no em concept was set, therefore " + noEmConcept.getHbefaEmConcept() + "should be set to 'average'";
		Assert.assertTrue(message, noEmConcept.equals(normal));
		Assert.assertTrue(message, normal.equals(noEmConcept));
		
		//technology missing
		setToNormal(normal);
		HbefaVehicleAttributes noTechnology = new HbefaVehicleAttributes();
		noTechnology.setHbefaEmConcept(concept);
		noTechnology.setHbefaSizeClass(sizeClass);
		message = "no technology was set, therefore " + normal.getHbefaTechnology() + " should not equal " + noEmConcept.getHbefaTechnology();
		Assert.assertFalse(message, noTechnology.equals(normal));
		Assert.assertFalse(message, normal.equals(noTechnology));
		
		normal.setHbefaTechnology("average");
		message = "no em concept was set, therefore " + noEmConcept.getHbefaEmConcept() + "should be set to 'average'";
		Assert.assertTrue(message, noTechnology.equals(normal));
		Assert.assertTrue(message, normal.equals(noTechnology));
		
		//size class missing
		setToNormal(normal);
		HbefaVehicleAttributes noSize = new HbefaVehicleAttributes();
		noSize.setHbefaEmConcept(concept);
		noSize.setHbefaTechnology(technology);
		message = "no size class was set, therefore " + normal.getHbefaSizeClass() + " should not equal " + noSize.getHbefaSizeClass();
		Assert.assertFalse(message, noSize.equals(normal));
		Assert.assertFalse(message, normal.equals(noSize));
		
		normal.setHbefaSizeClass("average");
		message = "no size class was set, therefore " + noSize.getHbefaEmConcept() + "should be set to 'average'";
		Assert.assertTrue(message, noSize.equals(normal));
		Assert.assertTrue(message, normal.equals(noSize));
		
		//ein korrektes, ein nicht-objekt - null?
		setToNormal(normal);
		message = "if a HbefaVehicleAttribute is compared to a different object, 'false' shouls be returned";
		Assert.assertFalse(message, normal.equals(null));
		Assert.assertFalse(message, normal.equals("a string"));
		Assert.assertFalse(message, normal.equals(1));
	}

	private void setToNormal(HbefaVehicleAttributes normal) {
		normal.setHbefaEmConcept(concept);
		normal.setHbefaSizeClass(sizeClass);
		normal.setHbefaTechnology(technology);
	}
	
	public final void testInit(){
		//leeres Objekt soll mit "average" initialisiert werden
		HbefaVehicleAttributes emptyAttributes = new HbefaVehicleAttributes();
		HbefaVehicleAttributes average = new  HbefaVehicleAttributes();
		average.setHbefaEmConcept("average");
		average.setHbefaSizeClass("average");
		average.setHbefaTechnology("average");
		String message = "The empty and the average initialization of an HbefaVehicleAttributes should be the same: ";
		message += emptyAttributes.toString() + " and " + average.toString();
		Assert.assertTrue(message, emptyAttributes.equals(average));
		Assert.assertTrue(message, average.equals(emptyAttributes));
	}
	
}
	

	

