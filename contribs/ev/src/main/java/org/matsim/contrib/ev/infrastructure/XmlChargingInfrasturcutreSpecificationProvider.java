package org.matsim.contrib.ev.infrastructure;

import java.util.Map;

import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.utils.objectattributes.AttributeConverter;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class XmlChargingInfrasturcutreSpecificationProvider implements Provider<ChargingInfrastructureSpecification> {
    private final Config config;
    private final Map<Class<?>, AttributeConverter<?>> converters;

    @Inject
    public XmlChargingInfrasturcutreSpecificationProvider(Config config,
            Map<Class<?>, AttributeConverter<?>> converters) {
        this.config = config;
        this.converters = converters;
    }

    @Override
    public ChargingInfrastructureSpecification get() {
        // do not replace this by @NotNull inside the config group as people tend to
        // dynamically bind their custom infrastructure
        EvConfigGroup evConfig = EvConfigGroup.get(config);

        Preconditions.checkNotNull(evConfig.getChargersFile(),
                "Need to specify a chargers file in the ev config group.");

        ChargingInfrastructureSpecification chargingInfrastructureSpecification = new ChargingInfrastructureSpecificationDefaultImpl();

        ChargerReader reader = new ChargerReader(chargingInfrastructureSpecification);
        reader.putAttributeConverters(converters);
        reader.parse(
                ConfigGroup.getInputFileURL(config.getContext(), evConfig.getChargersFile()));

        return chargingInfrastructureSpecification;
    }
}
