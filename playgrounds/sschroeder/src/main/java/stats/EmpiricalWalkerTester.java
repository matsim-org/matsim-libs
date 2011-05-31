package stats;

import org.apache.commons.math.stat.Frequency;

public class EmpiricalWalkerTester {
	public static void main(String[] args) {
		EmpiricalWalker empiricalWalker = new EmpiricalWalker();
		empiricalWalker.getFrequency().addValue("Foo");
		empiricalWalker.getFrequency().addValue("Foo");
		empiricalWalker.getFrequency().addValue("Foo");
		empiricalWalker.getFrequency().addValue("FooFoo");
		for(int i=0;i<10;i++){
			System.out.println(empiricalWalker.nextValue());
		}
		
		Frequency freq = new Frequency();
	
	}
}
