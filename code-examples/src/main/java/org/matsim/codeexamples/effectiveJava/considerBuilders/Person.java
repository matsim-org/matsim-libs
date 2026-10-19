package org.matsim.codeexamples.effectiveJava.considerBuilders;

class Person{
	double age ;
	double height ;
	double weight ;
	String lastName ;
	String firstName ;

	Person( String lastName, String firstName, double weight, double height, double age ) {

		this.lastName = lastName;
		this.firstName = firstName;
		this.weight = weight;
		this.height = height;
		this.age = age;
	}

}
