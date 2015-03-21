/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.test.threads;

/**
 * @author nagel
 *
 */
final class Main {
	
	private final Object paused = new Object() ;
	private final Object dummy = new Object() ;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Main main = new Main() ;
		main.run() ;
	}
	
	void run() {
		System.out.println( "here10") ;
		
		Thread thread = new Thread() {
			@Override
			public void run() {
				System.out.println("thread entered run") ;
				try {
					System.out.println("just before wait") ;
					for ( int ii=0 ; ii<10 ; ii++ ) {
						System.out.print(".");
						synchronized(dummy) {
							dummy.wait(1000) ;
						}
					}
					System.out.println("\njust after wait") ;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				synchronized(paused) {
					paused.notifyAll();
				}
			}
		} ;
		
		thread.start(); 
		
		System.out.println("main is just before pausing");

		try {
			synchronized(paused) {
				paused.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1) ;
		} 
		
		System.out.println("got woken up again");
		
		
		
	}

}
