package org.matsim.contrib.shared_mobility.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.shared_mobility.io.SharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.validation.SharingServiceValidator;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

class ValidationListener implements StartupListener {
	private final Logger logger = LogManager.getLogger(ValidationListener.class);

	private final Id<SharingService> serviceId;
	private final SharingServiceValidator validator;
	private final SharingServiceSpecification specification;

	ValidationListener(Id<SharingService> serviceId, SharingServiceValidator validator,
			SharingServiceSpecification specification) {
		this.serviceId = serviceId;
		this.validator = validator;
		this.specification = specification;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		logger.info("Validating sharing service " + serviceId.toString() + "...");
		validator.validate(specification);
	}
}
