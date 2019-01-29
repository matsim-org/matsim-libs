package org.matsim.withinday.utils;

/**
 * For example when activity to be replanned is in the past.  So that BDI code can catch this separately from other exceptions
 * and possibly react to it.
 * 
 * @author kainagel
 */
public class ReplanningException extends RuntimeException {
	public ReplanningException() {
		super() ;
	}
	public ReplanningException( String msg ) {
		super( msg ) ;
	}
}