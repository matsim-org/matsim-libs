package playground.kai.test;

import junit.framework.TestCase;

class BaseClass {
	private int age ;

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
}

class ExtendedClass extends BaseClass {
	public void incAge() {
		setAge( getAge()+1 ) ;
	}
}

public class OurNewTest {

	public static void main( String[] args ) {

		BaseClass bc = new ExtendedClass() ;

		ExtendedClass ec = (ExtendedClass) bc ;
		
	}


}
