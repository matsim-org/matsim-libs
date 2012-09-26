package org.matsim.contrib.freight.carrier;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.Module;
import org.matsim.core.config.groups.PlansConfigGroup;

public class CarrierConfig {
	
	public static class VehiclesConfigGroup extends Module {

		private static final long serialVersionUID = 1L;

		public static final String GROUP_NAME = "vehicles";

		private static final String INPUT_FILE = "vehicleFile";
		
		private String inputFile = null;
		

		public VehiclesConfigGroup() {
			super(GROUP_NAME);
		}

		@Override
		public String getValue(final String key) {
			if (INPUT_FILE.equals(key)) {
				return getInputFile();
			}
			else {
				throw new IllegalArgumentException(key);
			}
		}

		@Override
		public void addParam(final String key, final String value) {
			if (INPUT_FILE.equals(key)) {
				setInputFile(value.replace('\\', '/'));
			} else {
				throw new IllegalArgumentException(key);
			}
		}

		@Override
		public Map<String, String> getComments() {
			Map<String, String> comments = super.getComments();
			return comments;
		}

		@Override
		public final TreeMap<String, String> getParams() {
			TreeMap<String, String> map = new TreeMap<String, String>();
			addParameterToMap(map, INPUT_FILE);
			return map;
		}

		/* direct access */

		public String getInputFile() {
			return this.inputFile;
		}

		public void setInputFile(final String inputFile) {
			this.inputFile = inputFile;
		}

	}
	
	private final TreeMap<String, Module> modules = new TreeMap<String, Module>();
	
	private PlansConfigGroup plans = null;
	
	private VehiclesConfigGroup vehicles = null;
	
	public void addCoreModules(){
		plans = new PlansConfigGroup();
		this.modules.put(PlansConfigGroup.GROUP_NAME, this.plans);
		
		vehicles = new VehiclesConfigGroup();
		this.modules.put(VehiclesConfigGroup.GROUP_NAME, this.vehicles);
	}

	public PlansConfigGroup plans() {
		return plans;
	}

	public VehiclesConfigGroup vehicles() {
		return vehicles;
	}
		
	
}
