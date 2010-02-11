package tutorial.old.example0;

import org.matsim.run.Controler;

public class MyControler0 {
	
	public static void main ( String[] args ) {
	
		String configFile = "examples/tutorial/myConfig.xml" ;
		
		Controler controler = new Controler( configFile ) ;
		
		controler.setOverwriteFiles(true) ;
		controler.run() ;
		
	}

}
