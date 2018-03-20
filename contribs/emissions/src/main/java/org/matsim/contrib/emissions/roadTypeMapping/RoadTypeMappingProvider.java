package org.matsim.contrib.emissions.roadTypeMapping;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.contrib.emissions.roadTypeMapping.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.roadTypeMapping.VisumHbefaRoadTypeMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;

import java.net.URL;

public class RoadTypeMappingProvider implements Provider<HbefaRoadTypeMapping> {
    @Inject
    private Config config;

    @Override
    public HbefaRoadTypeMapping get() {
        URL context = config.getContext();
        EmissionsConfigGroup ecg1 = (EmissionsConfigGroup) config.getModules().get(EmissionsConfigGroup.GROUP_NAME);

        String roadTypeMappingFile = ecg1.getEmissionRoadTypeMappingFileURL(context).getFile();
        return VisumHbefaRoadTypeMapping.createVisumRoadTypeMapping(roadTypeMappingFile);
    }
}
