package org.matsim.core.mobsim.qsim.interfaces;

import org.matsim.facilities.Facility;

public class TripInfoRequest{
	private final Facility fromFacility;
	private final Facility toFacility;
	private final double time;
	private final TripInfo.TimeInterpretation timeInterpretation;
	private final long requestId;

	private TripInfoRequest( Facility fromFacility, Facility toFacility, double time, TripInfo.TimeInterpretation timeInterpretation, long requestId ){
		this.fromFacility = fromFacility;
		this.toFacility = toFacility;
		this.time = time;
		this.timeInterpretation = timeInterpretation;
		this.requestId = requestId;
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

	public long getRequestId(){
		return requestId;
	}

	public static class Builder{
		private Facility fromFacility;
		private Facility toFacility;
		private double time;
		private TripInfo.TimeInterpretation timeInterpretation;
		private long requestId;

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

		public Builder setRequestId( long requestId ){
			this.requestId = requestId;
			return this;
		}

		public TripInfoRequest createRequest(){
			return new TripInfoRequest( fromFacility, toFacility, time, timeInterpretation, requestId );
		}
	}
}
