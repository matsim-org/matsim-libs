package org.matsim.core.mobsim.qsim.interfaces;

import org.matsim.facilities.Facility;

import static org.matsim.core.mobsim.qsim.interfaces.TripInfo.*;

public class TripInfoRequest{
	private final Facility fromFacility;
	private final Facility toFacility;
	private final double time;
	private final TimeInterpretation timeInterpretation;

	private TripInfoRequest( Facility fromFacility, Facility toFacility, double time, TimeInterpretation timeInterpretation ){
		this.fromFacility = fromFacility;
		this.toFacility = toFacility;
		this.time = time;
		this.timeInterpretation = timeInterpretation;
	}

	public Facility getFromFacility(){
		return fromFacility;
	}

	public Facility getToFacility(){
		return toFacility;
	}

	public double getTime(){
		return time;
	}

	public TimeInterpretation getTimeInterpretation(){
		return timeInterpretation;
	}

	public static class Builder{
		// this is deliberately a builder and not a constructor so that we can add arguments later without having to add constructors with longer and longer
		// argument lists.  kai, mar'19

		private Facility fromFacility;
		private Facility toFacility;
		private double time;
		private TimeInterpretation timeInterpretation = TimeInterpretation.departure ;

		public Builder setFromFacility( Facility fromFacility ){
			this.fromFacility = fromFacility;
			return this;
		}

		public Builder setToFacility( Facility toFacility ){
			this.toFacility = toFacility;
			return this;
		}

		public Builder setTime( double time ){
			this.time = time;
			return this;
		}

		public Builder setTimeInterpretation( TimeInterpretation timeInterpretation ){
			this.timeInterpretation = timeInterpretation;
			return this;
		}

		public TripInfoRequest createRequest(){
			return new TripInfoRequest( fromFacility, toFacility, time, timeInterpretation );
		}
	}
}
