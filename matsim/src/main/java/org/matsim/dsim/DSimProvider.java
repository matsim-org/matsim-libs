package org.matsim.dsim;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.core.mobsim.framework.Mobsim;

/**
 * Simple provider for the DSim class.
 */
public class DSimProvider implements Provider<Mobsim> {

    @Inject
    private DSim dsim;

    @Override
    public Mobsim get() {
        return dsim;
    }

}
