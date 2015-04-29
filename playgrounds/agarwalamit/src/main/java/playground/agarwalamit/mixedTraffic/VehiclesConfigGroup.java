/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;


/**
 * @author amit
 */
public class VehiclesConfigGroup extends ConfigGroup {

	public VehiclesConfigGroup() {
		super(GROUP_NAME);
		this.modes2VehicleTypes.put(TransportMode.car, VehicleUtils.getDefaultVehicleType());
	}

	public static final String GROUP_NAME = "vehicles";
	private static final String INPUT_FILE= "inputVehiclesFile";
	private static final String TRAVEL_MODES = "travelModes";

	private HashMap<String, VehicleType> modes2VehicleTypes = new LinkedHashMap<String, VehicleType>();
	private String inputFile = null;
	private Collection<String> travelModes = Arrays.asList(TransportMode.car);;

	@Override
	public String getValue(final String key) {
		if (VehiclesConfigGroup.INPUT_FILE.equals(key)) {
			return getInputFile();
		} else if (VehiclesConfigGroup.TRAVEL_MODES.equals(key)){
			boolean isFirst = true;
			StringBuilder str = new StringBuilder();
			for(String mode:this.travelModes){
				if(!isFirst){
					str.append(',');
				}
				str.append(mode);
				isFirst=false;
			}
			return str.toString();
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;

		if (VehiclesConfigGroup.INPUT_FILE.equals(key)) {
			setInputFile(value);
		} else if(TRAVEL_MODES.equals(key)){
			setMainModes(Arrays.asList(value.split(",")));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(INPUT_FILE, getInputFile());
		map.put(TRAVEL_MODES, CollectionUtils.setToString(new HashSet<String>(getMainModes())));
		return map;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(INPUT_FILE, "Optional Input file containing the vehicles and corresponding vehicle attributes "
				+ "used for simulation.");
		comments.put(TRAVEL_MODES, "Comma-separated list of travel modes that are handled. Default is set to 'car'.");
		return comments;
	}

	public String getInputFile() {
		return this.inputFile;
	}

	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}

	public Collection<String>  getMainModes(){
		return this.modes2VehicleTypes.keySet();
	}

	public void setMainModes(final Collection<String> modes){
		this.travelModes = modes;
	}

	public HashMap<String, VehicleType> getMode2VehicleType(){
		if(this.inputFile!=null){
			Vehicles vehicles = VehicleUtils.createVehiclesContainer();
			VehicleReaderV1 reader = new VehicleReaderV1(vehicles);
			reader.readFile(this.inputFile);
			vehicles.getVehicleTypes();
			for(Id<VehicleType> id:vehicles.getVehicleTypes().keySet()){
				this.modes2VehicleTypes.put(id.toString(), vehicles.getVehicleTypes().get(id));
			}
		} else {
			for(String mode:this.travelModes) {
				this.modes2VehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
			}
		}
		return this.modes2VehicleTypes; 
	}
}
