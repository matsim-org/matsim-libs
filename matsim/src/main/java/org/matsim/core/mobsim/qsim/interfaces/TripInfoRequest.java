package org.matsim.core.mobsim.qsim.interfaces;

import org.matsim.facilities.Facility;

public class TripInfoRequest{
	private final Facility fromFacility;
	private final Facility toFacility;
	private final double time;
	private final TripInfo.TimeInterpretation timeInterpretation;

	private TripInfoRequest( Facility fromFacility, Facility toFacility, double time, TripInfo.TimeInterpretation timeInterpretation ){
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

	public TripInfo.TimeInterpretation getTimeInterpretation(){
		return timeInterpretation;
	}

	public static class Builder{
		// this is deliberately a builder and not a constructor so that we can add arguments later without having to add constructors with longer and longer
		// argument lists.  kai, mar'19

		private Facility fromFacility;
		private Facility toFacility;
		private double time;
		private TripInfo.TimeInterpretation timeInterpretation;

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

		public Builder setTimeInterpretation( TripInfo.TimeInterpretation timeInterpretation ){
			this.timeInterpretation = timeInterpretation;
			return this;
		}

		public TripInfoRequest createRequest(){
			return new TripInfoRequest( fromFacility, toFacility, time, timeInterpretation );
		}
	}
}
