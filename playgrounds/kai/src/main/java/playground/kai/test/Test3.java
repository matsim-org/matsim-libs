package playground.kai.test;


public class Test3 {
	
	Object oo ;

	void run() {
		Aa aa = new Aa( oo ) ;
		System.out.println( aa.pointer ) ;
		oo = new Object() ;
		System.out.println( aa.pointer ) ;
	}
	
	public static void main( String[] args ) {
		new Test3().run() ;
		
	}
	
	static class Aa {
		Object pointer ;
		Aa ( Object oo ) {
			pointer = oo ;
		}
	}

}
