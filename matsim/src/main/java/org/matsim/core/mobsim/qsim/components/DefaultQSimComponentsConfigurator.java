package org.matsim.core.mobsim.qsim.components;

public class DefaultQSimComponentsConfigurator implements QSimComponentsConfigurator {
	@Override
	public void configure( QSimComponentsConfig annotationsRegistry ) {
		annotationsRegistry.clear();
		QSimComponentsConfigGroup.DEFAULT_COMPONENTS.forEach(annotationsRegistry::addNamedAnnotation );
	}
}
