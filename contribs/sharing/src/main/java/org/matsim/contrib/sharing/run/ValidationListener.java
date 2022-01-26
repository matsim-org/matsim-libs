package org.matsim.contrib.sharing.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.sharing.io.SharingServiceSpecification;
import org.matsim.contrib.sharing.io.validation.SharingServiceValidator;
import org.matsim.contrib.sharing.service.SharingService;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

class ValidationListener implements StartupListener {
	private final Logger logger = Logger.getLogger(ValidationListener.class);

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
