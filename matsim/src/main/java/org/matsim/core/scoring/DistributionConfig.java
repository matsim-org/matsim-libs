package org.matsim.core.scoring;

import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;

/**
 * Describes what distribution is applied.
 */
record DistributionConfig(TasteVariationsConfigParameterSet.VariationType distribution, double scale) {
}
