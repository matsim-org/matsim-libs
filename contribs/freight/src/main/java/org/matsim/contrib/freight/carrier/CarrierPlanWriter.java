package org.matsim.contrib.freight.carrier;

import com.google.inject.Inject;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.util.Map;

/**
 * @author mrieser / Simunto
 */
public class CarrierPlanWriter {

	private final Carriers carriers;
	private Map<Class<?>, AttributeConverter<?>> attributeConverters = null;

	public CarrierPlanWriter(Carriers carriers) {
		this.carriers = carriers;
	}

	@Inject
	public void setAttributeConverters(Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		this.attributeConverters = attributeConverters;
	}

	public void write(String filename) {
		this.writeV2(filename);
	}

	/**
	 * @deprecated The underlining {@Link{CarrierPlanXmlWriterV1} is deprecated since April21
	 */
	@Deprecated
	public void writeV1(String filename) {
		new CarrierPlanXmlWriterV1(this.carriers.getCarriers().values()).write(filename);
	}


	/**
	 * Writes out the Carriers file in version 2.
//	 * Please use the method {@link #write(String)} instead to always ensure writing out to the newest format.
	 *
	 * @param filename Name of the file that should be written.
	 */
	public void writeV2(String filename) {
		CarrierPlanXmlWriterV2 writer = new CarrierPlanXmlWriterV2(this.carriers);
		if (this.attributeConverters != null) {
			writer.putAttributeConverters(this.attributeConverters);
		}
		writer.write(filename);
	}
}
