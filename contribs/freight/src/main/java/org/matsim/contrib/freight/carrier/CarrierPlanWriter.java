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

	public void writeV1(String filename) {
		new CarrierPlanXmlWriterV1(this.carriers.getCarriers().values()).write(filename);
	}

	public void writeV2(String filename) {
		CarrierPlanXmlWriterV2 writer = new CarrierPlanXmlWriterV2(this.carriers);
		if (this.attributeConverters != null) {
			writer.putAttributeConverters(this.attributeConverters);
		}
		writer.write(filename);
	}
}
