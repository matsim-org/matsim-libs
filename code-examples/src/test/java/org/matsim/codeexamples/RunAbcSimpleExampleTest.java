package org.matsim.codeexamples;

import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.*;

public class RunAbcSimpleExampleTest{
	private static final Logger log = Logger.getLogger(RunAbcSimpleExampleTest.class) ;

	@Test
	public void main(){
		try{
			RunAbcSimpleExample.main( null );
		} catch ( Exception ee ) {
			log.fatal(ee) ;
			fail() ;
		}

	}
}
