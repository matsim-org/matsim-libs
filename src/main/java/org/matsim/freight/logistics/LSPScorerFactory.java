package org.matsim.freight.logistics;

import org.matsim.core.api.internal.MatsimFactory;

public interface LSPScorerFactory extends MatsimFactory {
  LSPScorer createScoringFunction();
}
