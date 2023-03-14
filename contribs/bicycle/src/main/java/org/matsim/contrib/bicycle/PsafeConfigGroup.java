package org.matsim.contrib.bicycle;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.api.core.v01.TransportMode;
import java.util.Map;

public final class PsafeConfigGroup extends ReflectiveConfigGroup{

	public static final String GROUP_NAME = "Psafe";
	
	private static final String INPUT_PERCEIVED_SAFETY_CAR = "marginalUtilityOfPerceivedSafety_car_m";
	private static final String INPUT_PERCEIVED_SAFETY_EBIKE = "marginalUtilityOfPerceivedSafety_ebike_m";
	private static final String INPUT_PERCEIVED_SAFETY_ESCOOT = "marginalUtilityOfPerceivedSafety_escoot_m";
	private static final String INPUT_PERCEIVED_SAFETY_WALK = "marginalUtilityOfPerceivedSafety_walk_m";
	private static final String INPUT_DMAX_CAR = "Dmax_car_m";
	private static final String INPUT_DMAX_EBIKE = "Dmax_ebike_m";
	private static final String INPUT_DMAX_ESCOOT = "Dmax_escoot_m";
	private static final String INPUT_DMAX_WALK = "Dmax_walk_m";
    private static final String CAR_MODE = "carMode";
    private static final String EBIKE_MODE = "ebikeMode";
    private static final String ESCOOT_MODE = "escootMode";
    private static final String WALK_MODE = "walkMode";
	private static final String INPUT_PSAFE_THRESHOLD = "inputPsafeThreshold_m";
	
	private double marginalUtilityOfPerceivedSafety_car;
    private double marginalUtilityOfPerceivedSafety_ebike;
    private double marginalUtilityOfPerceivedSafety_escoot;
    private double marginalUtilityOfPerceivedSafety_walk;
    private double Dmax_car;
    private double Dmax_ebike;
    private double Dmax_escoot;
    private double Dmax_walk;
    private int inputPsafeThreshold;
    private String carMode = "car";
    private String ebikeMode = "ebike";
    private String escootMode = "escoot";
    private String walkMode = "walk";
   
	public PsafeConfigGroup(){
		super(GROUP_NAME);
		}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(INPUT_PERCEIVED_SAFETY_CAR, "marginalUtilityOfPerceivedSafety_car");
		map.put(INPUT_PERCEIVED_SAFETY_EBIKE, "marginalUtilityOfPerceivedSafety_ebike");
		map.put(INPUT_PERCEIVED_SAFETY_ESCOOT, "marginalUtilityOfPerceivedSafety_escoot");
		map.put(INPUT_PERCEIVED_SAFETY_WALK, "marginalUtilityOfPerceivedSafety_walk");
		map.put(INPUT_DMAX_CAR, "Dmax_car");
		map.put(INPUT_DMAX_EBIKE, "Dmax_ebike");
		map.put(INPUT_DMAX_ESCOOT, "Dmax_escoot");
		map.put(INPUT_PSAFE_THRESHOLD, "inputPsafeThreshold");
		return map;
	}
	
	
	@StringSetter(INPUT_PERCEIVED_SAFETY_CAR)
	public void setMarginalUtilityOfPerceivedSafety_car_m(final double value) {
		this.marginalUtilityOfPerceivedSafety_car = value; // PROSTHESE MONTE CARLO SIMULATION
	}
	@StringGetter( INPUT_PERCEIVED_SAFETY_CAR)
	public double getMarginalUtilityOfPerceivedSafety_car_m() {
		return this.marginalUtilityOfPerceivedSafety_car;
	}
	
	@StringSetter(INPUT_PERCEIVED_SAFETY_EBIKE)
	public void setMarginalUtilityOfPerceivedSafety_ebike_m(final double value) {
		this.marginalUtilityOfPerceivedSafety_ebike = value;
	}
	@StringGetter( INPUT_PERCEIVED_SAFETY_EBIKE)
	public double getMarginalUtilityOfPerceivedSafety_ebike_m() {
		return this.marginalUtilityOfPerceivedSafety_ebike;
	}
	
	@StringSetter(INPUT_PERCEIVED_SAFETY_ESCOOT)
	public void setMarginalUtilityOfPerceivedSafety_escoot_m(final double value) {
		this.marginalUtilityOfPerceivedSafety_escoot= value;
	}
	@StringGetter( INPUT_PERCEIVED_SAFETY_ESCOOT)
	public double getMarginalUtilityOfPerceivedSafety_escoot_m() {
		return this.marginalUtilityOfPerceivedSafety_escoot;
	}
	
	@StringSetter(INPUT_PERCEIVED_SAFETY_WALK)
	public void setMarginalUtilityOfPerceivedSafety_walk_m(final double value) {
		this.marginalUtilityOfPerceivedSafety_walk = value;
	}
	@StringGetter( INPUT_PERCEIVED_SAFETY_WALK)
	public double getMarginalUtilityOfPerceivedSafety_walk_m() {
		return this.marginalUtilityOfPerceivedSafety_walk;
	}
	
	@StringSetter(INPUT_DMAX_CAR)
	public void setDmax_car_m(final double value) {
		this.Dmax_car = value;
	}
	@StringGetter(INPUT_DMAX_CAR)
	public double getDmax_car_m() {
		return this.Dmax_car;	
	}

	@StringSetter(INPUT_DMAX_EBIKE)
	public void setDmax_ebike_m(final double value) {
		this.Dmax_ebike = value;
	}
	@StringGetter(INPUT_DMAX_EBIKE)
	public double getDmax_ebike_m() {
		return this.Dmax_ebike;	
	}

	@StringSetter(INPUT_DMAX_ESCOOT)
	public void setDmax_escoot_m(final double value) {
		this.Dmax_escoot = value;
	}
	@StringGetter(INPUT_DMAX_ESCOOT)
	public double getDmax_escoot_m() {
		return this.Dmax_escoot;	
	}

	@StringSetter(INPUT_DMAX_WALK)
	public void setDmax_walk_m(final double value) {
		this.Dmax_walk = value;
	}
	@StringGetter(INPUT_DMAX_WALK)
	public double getDmax_walk_m() {
		return this.Dmax_walk;	
	}

	@StringGetter( CAR_MODE )
	public String getCarMode() {
		return this.carMode;
	}
	@StringSetter( CAR_MODE )
	public void setCarMode(String carMode) {
		this.carMode = carMode;
	}	

	@StringGetter( EBIKE_MODE )
	public String getEbikeMode() {
		return this.ebikeMode;
	}
	@StringSetter( EBIKE_MODE )
	public void setEbikeMode(String ebikeMode) {
		this.ebikeMode = ebikeMode;
	}
	
	@StringGetter( ESCOOT_MODE )
	public String getEscootMode() {
		return this.escootMode;
	}
	@StringSetter( ESCOOT_MODE )
	public void setEscootMode(String escootMode) {
		this.escootMode = escootMode;
	}

	@StringGetter( WALK_MODE )
	public String getWalkMode() {
		return this.walkMode;
	}
	@StringSetter( WALK_MODE )
	public void setWalkMode(String walkMode) {
		this.walkMode = walkMode;
	}
	
	
	
	@StringSetter(INPUT_PSAFE_THRESHOLD)
	public void setInputPsafeThreshold_m(final int value) {
		this.inputPsafeThreshold = value;
	}
	@StringGetter(INPUT_PSAFE_THRESHOLD)
	public int getInputPsafeThreshold_m() {
		return this.inputPsafeThreshold;	
	}
	
	
}
