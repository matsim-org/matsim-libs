package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.ConfigGroup;

import java.net.URL;
import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class OperationFacilitiesParams extends ReflectiveConfigGroupWithConfigurableParameterSets {

	public static final String SET_NAME = "operationFacilities";

	private static final String OPERATION_FACILITY_INPUT_FILE = "operationFacilityInputFile";

	private String operationFacilityInputFile;


	public OperationFacilitiesParams() {
		super(SET_NAME);
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(OPERATION_FACILITY_INPUT_FILE, "path to operation facility xml");
		return map;
	}

	@StringSetter( OPERATION_FACILITY_INPUT_FILE )
	public void setOperationFacilityInputFile(final String operationFacilityInputFile) {
		this.operationFacilityInputFile = operationFacilityInputFile;
	}


	@StringGetter( OPERATION_FACILITY_INPUT_FILE )
	public String getOperationFacilityInputFile() {
		return this.operationFacilityInputFile;
	}

	public URL getOperationFacilityInputUrl(URL context) {
		return operationFacilityInputFile == null ?
				null :
				ConfigGroup.getInputFileURL(context, operationFacilityInputFile);
	}
}
