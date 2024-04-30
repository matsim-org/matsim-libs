package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.ConfigGroup;

import java.net.URL;

/**
 * @author nkuehnel / MOIA
 */
public class OperationFacilitiesParams extends ReflectiveConfigGroupWithConfigurableParameterSets {

	public static final String SET_NAME = "operationFacilities";

	@Parameter
	@Comment("path to operation facility xml")
	public String operationFacilityInputFile;

	public OperationFacilitiesParams() {
		super(SET_NAME);
	}

	public URL getOperationFacilityInputUrl(URL context) {
		return operationFacilityInputFile == null ?
				null :
				ConfigGroup.getInputFileURL(context, operationFacilityInputFile);
	}
}
