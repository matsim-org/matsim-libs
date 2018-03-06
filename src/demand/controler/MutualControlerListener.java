package demand.controler;

import java.util.ArrayList;
import javax.inject.Inject;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;

import demand.decoratedLSP.LSPDecorators;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjects;
import demand.mutualReplanning.MutualReplanningModule;
import demand.scoring.MutualScoringModule;
import lsp.LSP;
import lsp.LSPPlan;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.controler.FreightControlerListener;
import lsp.functions.Info;
import lsp.mobsim.CarrierResourceTracker;
import lsp.resources.CarrierResource;
import lsp.scoring.LSPScoringModule;
import lsp.shipment.LSPShipment;
import lsp.tracking.SimulationTracker;

public class MutualControlerListener implements FreightControlerListener, BeforeMobsimListener, AfterMobsimListener,
		ScoringListener, ReplanningListener, IterationEndsListener, StartupListener {

	private CarrierResourceTracker carrierResourceTracker;
	private Carriers carriers;
	private LSPDecorators lsps;
	private DemandObjects demandObjects;
	private MutualScoringModule mutualScoringModule;
	private MutualReplanningModule replanningModule;

	private ArrayList<EventHandler> registeredHandlers;

	@Inject
	EventsManager eventsManager;
	@Inject
	Network network;

	@Inject
	protected MutualControlerListener(LSPDecorators lsps, DemandObjects demandObjects,
			MutualScoringModule demandScoringModule, MutualReplanningModule replanningModule) {
		this.lsps = lsps;
		this.demandObjects = demandObjects;
		this.mutualScoringModule = demandScoringModule;
		this.replanningModule = replanningModule;
		this.carriers = getCarriers();
	}

	public CarrierResourceTracker getCarrierResourceTracker() {
		return carrierResourceTracker;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		SupplyClearer supplyClearer = new SupplyClearer(lsps);
		supplyClearer.notifyIterationEnds(event);

		for (EventHandler handler : registeredHandlers) {
			eventsManager.removeHandler(handler);
		}

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LSPShipment shipment : lsp.getShipments()) {
				shipment.getEventHandlers().clear();
			}

			for (LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
				for (EventHandler handler : solution.getEventHandlers()) {
					handler.reset(event.getIteration());
				}
				for (LogisticsSolutionElement element : solution.getSolutionElements()) {
					for (EventHandler handler : element.getEventHandlers()) {
						handler.reset(event.getIteration());
					}
					for (EventHandler handler : element.getResource().getEventHandlers()) {
						handler.reset(event.getIteration());
					}
				}
			}
		}

	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		replanningModule.replan(event);
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		boolean score = true;
		for (LSP lsp : lsps.getLSPs().values()) {
			if (lsp.getScorer() == null) {
				score = false;
			}
			if (score == true) {
				mutualScoringModule.scoreLSPs();
			}
		}

		for (DemandObject demandObject : demandObjects.getDemandObjects().values()) {
			if (demandObject.getScorer() == null) {
				score = false;
			}
			if (score == true) {
				mutualScoringModule.scoreDemandObjects();
			}
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		eventsManager.removeHandler(carrierResourceTracker);

		ArrayList<SimulationTracker> alreadyUpdatedTrackers = new ArrayList<SimulationTracker>();
		for (LSP lsp : lsps.getLSPs().values()) {
			for (LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
				for (LogisticsSolutionElement element : solution.getSolutionElements()) {
					for (SimulationTracker tracker : element.getResource().getSimulationTrackers()) {
						if (!alreadyUpdatedTrackers.contains(tracker)) {
							tracker.notifyAfterMobsim(event);
							alreadyUpdatedTrackers.add(tracker);
							tracker.reset();
						}
					}
					for (SimulationTracker tracker : element.getSimulationTrackers()) {
						tracker.notifyAfterMobsim(event);
						tracker.reset();
					}
				}
				for (SimulationTracker tracker : solution.getSimulationTrackers()) {
					tracker.notifyAfterMobsim(event);
					tracker.reset();
				}
			}
		}

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
				for (LogisticsSolutionElement element : solution.getSolutionElements()) {
					for (Info info : element.getInfos()) {
						info.update();
					}
				}
				for (Info info : solution.getInfos()) {
					info.update();
				}
			}
		}

	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		carrierResourceTracker = new CarrierResourceTracker(carriers, network, this);
		eventsManager.addHandler(carrierResourceTracker);

		SupplyRescheduler rescheduler = new SupplyRescheduler(lsps);
		rescheduler.notifyBeforeMobsim(event);

		registeredHandlers = new ArrayList<EventHandler>();

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LSPShipment shipment : lsp.getShipments()) {
				for (EventHandler handler : shipment.getEventHandlers()) {
					eventsManager.addHandler(handler);
					registeredHandlers.add(handler);
				}
			}
			LSPPlan selectedPlan = lsp.getSelectedPlan();
			for (LogisticsSolution solution : selectedPlan.getSolutions()) {
				for (EventHandler handler : solution.getEventHandlers()) {
					eventsManager.addHandler(handler);
					registeredHandlers.add(handler);
				}
				for (LogisticsSolutionElement element : solution.getSolutionElements()) {
					for (EventHandler handler : element.getEventHandlers()) {
						eventsManager.addHandler(handler);
						registeredHandlers.add(handler);
					}
					ArrayList<EventHandler> resourceHandlers = (ArrayList<EventHandler>) element.getResource()
							.getEventHandlers();
					for (EventHandler handler : resourceHandlers) {
						if (!registeredHandlers.contains(handler)) {
							eventsManager.addHandler(handler);
							registeredHandlers.add(handler);
						}
					}
				}
			}
		}
	}

	public void processEvent(Event event) {
		eventsManager.processEvent(event);
	}

	private Carriers getCarriers() {
		Carriers carriers = new Carriers();
		for (LSP lsp : lsps.getLSPs().values()) {
			LSPPlan selectedPlan = lsp.getSelectedPlan();
			for (LogisticsSolution solution : selectedPlan.getSolutions()) {
				for (LogisticsSolutionElement element : solution.getSolutionElements()) {
					if (element.getResource() instanceof CarrierResource) {

						CarrierResource carrierResource = (CarrierResource) element.getResource();
						Carrier carrier = carrierResource.getCarrier();
						if (!carriers.getCarriers().containsKey(carrier.getId())) {
							carriers.addCarrier(carrier);
						}
					}
				}
			}
		}
		return carriers;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		InitialDemandAssigner initialAssigner = new InitialDemandAssigner(demandObjects, lsps);
		initialAssigner.notifyStartup(event);
	}

}
