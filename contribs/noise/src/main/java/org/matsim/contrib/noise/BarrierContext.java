package org.matsim.contrib.noise;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.util.Collection;

public class BarrierContext {

    Logger logger = Logger.getLogger(BarrierContext.class);

    private final STRtree noiseBarriers;

    @Inject
    public BarrierContext(Config config) {
        NoiseConfigGroup noiseParams = ConfigUtils.addOrGetModule(config, NoiseConfigGroup.class);

        this.noiseBarriers = new STRtree();
        if (noiseParams.isConsiderNoiseBarriers()) {
        final Collection<FeatureNoiseBarrierImpl> barriers
                = FeatureNoiseBarriersReader.read(noiseParams.getNoiseBarriersFilePath(),
                noiseParams.getNoiseBarriersSourceCRS(), config.global().getCoordinateSystem());

            for (NoiseBarrier barrier : barriers) {
                try {
                    this.noiseBarriers.insert(barrier.getGeometry().getGeometry().getEnvelopeInternal(), barrier);
                } catch (IllegalArgumentException e) {
                    logger.warn("Could not add noise barrier " + barrier.getId() + " to quad tree. Ignoring it.");
                }
            }
        }
    }


    public BarrierContext(Collection<FeatureNoiseBarrierImpl> barriers) {
        this.noiseBarriers = new STRtree();
        for (NoiseBarrier barrier : barriers) {
            try {
                this.noiseBarriers.insert(barrier.getGeometry().getGeometry().getEnvelopeInternal(), barrier);
            } catch (IllegalArgumentException e) {
                logger.warn("Could not add noise barrier " + barrier.getId() + " to quad tree. Ignoring it.");
            }
        }
    }

    public Collection<NoiseBarrier> query(Envelope envelopeInternal) {
        return noiseBarriers.query(envelopeInternal);
    }
}
