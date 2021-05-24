package org.matsim.utils.objectattributes;

import java.util.function.Function;

@Deprecated // for refactoring only
public class FailingObjectAttributes extends ObjectAttributes{
	private final Function<String, String> msgFunction;

	// This is only meant as a transitory refactoring device. For this reason,
	// all "specialized" versions are created in static methods in this class,
	// to keep it all contained here
	private FailingObjectAttributes(Function<String, String> msgFunction) {
		this.msgFunction = msgFunction;
	}

	public static FailingObjectAttributes createPersonAttributes() {
		return new FailingObjectAttributes(
				str -> "population.getPersonAttributes()." + str + " will be deprecated; use PopulationUtils" +
				".get/put/...PersonAttribute... instead.  kai, may'19"
		);
	}

	public static FailingObjectAttributes createFacilitiesAttributes() {
		return new FailingObjectAttributes(
				str -> "facilities.getPersonAttributes()." + str + " will be deprecated; use FacilitiesUtils" +
						".get/put/...FacilityAttribute... instead.  td, aug'19"
		);
	}

	public static FailingObjectAttributes createHouseholdsAttributes() {
		return new FailingObjectAttributes(
				str -> "households.getHouseholdAttributes()." + str + " will be deprecated; use HouseholdUtils" +
						".get/put/...HouseholdAttribute... instead.  td, aug'19"
		);
	}

	public static FailingObjectAttributes createTransitStopsAttributes() {
		return new FailingObjectAttributes(
				str -> "schedule.getTransitStopsAttributes()." + str + " will be deprecated; use TransitScheduleUtils" +
						".get/put/...Attribute... instead.  td, aug'19"
		);
	}

	public static FailingObjectAttributes createTransitLinesAttributes() {
		return new FailingObjectAttributes(
				str -> "schedule.getTransitLinesAttributes()." + str + " will be deprecated; use TransitScheduleUtils" +
						".get/put/...Attribute... instead.  td, aug'19"
		);
	}

	@Override public Object getAttribute( String personId, String key) {
		throw new RuntimeException( msgFunction.apply("getAttribute"));
	}
	@Override public Object putAttribute( String personId, String key, Object value) {
		throw new RuntimeException( msgFunction.apply("putAttribute"));
	}
	@Override public Object removeAttribute( String personId, String key ) {
		throw new RuntimeException( msgFunction.apply("removeAttribute"));
	}
	@Override public void removeAllAttributes( String personId ) {
		throw new RuntimeException( msgFunction.apply("removeAllAttributes"));
	}
	@Override public void clear() {
		throw new RuntimeException( msgFunction.apply( "clear"));
	}
	@Override public String toString() {
		return super.toString() ;
	}

	// for retrofitting.  Called from PopulationUtils only.  Remove eventually.  kai, may'19
	Object getAttributeDirectly( String personId, String key ){
		return super.getAttribute( personId, key ) ;
	}
	Object removeAttributeDirectly( String personId, String key ){
		return super.removeAttribute( personId, key ) ;
	}
	void removeAllAttributesDirectly( String personId ) {
		super.removeAllAttributes( personId );
	}
}