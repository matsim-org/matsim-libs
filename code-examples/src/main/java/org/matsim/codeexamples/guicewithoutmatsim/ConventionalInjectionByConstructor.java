package org.matsim.codeexamples.guicewithoutmatsim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class ConventionalInjectionByConstructor{

	private static final Logger log = LogManager.getLogger( BasicInjection.class ) ;

	public static void main(String[] args){
		Helper helper = new MyHelper1();
		Simulation simulation = new MySimulation1( helper );
		simulation.run();
	}

	interface Simulation {
		void run() ;
	}

	interface Helper {
		Object getAccessToSomething() ;
	}

	static class MySimulation1 implements Simulation{
		private final Helper helper;
		MySimulation1( Helper helper ) {
			this.helper = helper;
		}
		@Override public void run() {
			log.info( "called MySimulation1 run method") ;
			helper.getAccessToSomething() ;
		}
	}
	static class MySimulation2 implements Simulation{
		private final Helper helper;
		MySimulation2( Helper helper ) {
			this.helper = helper;
		}
		@Override public void run() {
			log.info( "called MySimulation2 run method") ;
			helper.getAccessToSomething() ;
		}
	}

	static class MyHelper1 implements Helper{
		@Override public Object getAccessToSomething(){
			log.info( "called MyHelper1 getAccess... method") ;
			return null ;
		}
	}
	static class MyHelper2 implements Helper{
		@Override public Object getAccessToSomething(){
			log.info( "called MyHelper2 getAccess... method") ;
			return null ;
		}
	}
}
