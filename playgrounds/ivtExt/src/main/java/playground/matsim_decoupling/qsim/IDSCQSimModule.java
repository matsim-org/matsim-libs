package playground.matsim_decoupling.qsim;

import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.av.config.AVConfig;
import ch.ethz.matsim.av.framework.AVQSimModule;
import ch.ethz.matsim.av.schedule.AVOptimizer;
import playground.clruch.traveltimetracker.AVTravelTimeRecorder;
import playground.matsim_decoupling.TrackingHelper;

public class IDSCQSimModule extends AbstractModule {
	final private QSim qsim;

	public IDSCQSimModule(QSim qsim) {
		this.qsim = qsim;
	}

	@Provides
	@Singleton
	public LegCreator provideLegCreator(AVOptimizer avOptimizer) {
		return TrackingHelper.createLegCreatorWithIDSCTracking(avOptimizer, qsim.getSimTimer());
	}

	@Override
	protected void configure() {
		bind(AVTravelTimeRecorder.class);
	}
}
