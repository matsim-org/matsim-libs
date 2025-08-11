package org.matsim.contribs.discrete_mode_choice.modules;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityWriter;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.utils.ExtractPlanUtilities;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import java.io.IOException;

public class UtilitiesWriterHandler implements ShutdownListener, IterationStartsListener, IterationEndsListener {
	private final OutputDirectoryHierarchy outputDirectoryHierarchy;
	private final ControllerConfigGroup controllerConfigGroup;
	private final Population population;
	private final DiscreteModeChoiceConfigGroup discreteModeChoiceConfigGroup;

	@Inject
	public UtilitiesWriterHandler(ControllerConfigGroup controllerConfigGroup, DiscreteModeChoiceConfigGroup discreteModeChoiceConfigGroup, Population population, OutputDirectoryHierarchy outputDirectoryHierarchy) {
		this.controllerConfigGroup = controllerConfigGroup;
		this.discreteModeChoiceConfigGroup = discreteModeChoiceConfigGroup;
		this.population = population;
		this.outputDirectoryHierarchy = outputDirectoryHierarchy;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String filePath = this.outputDirectoryHierarchy.getOutputFilename("dmc_utilities.csv");
		try {
			ExtractPlanUtilities.writePlanUtilities(population, filePath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (this.discreteModeChoiceConfigGroup.getMultinomialLogitSelectorConfig().getWriteDetailedUtilities()) {
			String detailedUtilitiesFilePath = this.outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "detailed_utilities.csv");
			UtilityWriter.init(detailedUtilitiesFilePath);
		}

		if(event.getIteration() == controllerConfigGroup.getFirstIteration() ||  this.discreteModeChoiceConfigGroup.getWriteUtilitiesInterval() % event.getIteration() != 0) {
			return;
		}
		String filePath = this.outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "dmc_utilities.csv");
		try {
			ExtractPlanUtilities.writePlanUtilities(population, filePath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
		if (this.discreteModeChoiceConfigGroup.getMultinomialLogitSelectorConfig().getWriteDetailedUtilities()) {
			UtilityWriter.close();
		}
	}
}
