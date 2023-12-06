package org.matsim.contrib.drt.sharingfactor;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.MatsimServices;

/**
 * @author nkuehnel / MOIA
 */
public class SharingFactorModule extends AbstractDvrpModeModule {

    private final DrtConfigGroup drtConfigGroup;

    public SharingFactorModule(DrtConfigGroup drtConfigGroup) {
        super(drtConfigGroup.getMode());
        this.drtConfigGroup = drtConfigGroup;
    }

    @Override
    public void install() {
        bindModal(SharingFactorTracker.class).toProvider(modalProvider(getter ->
                new SharingFactorTracker(new SharingFactorTracker.GroupPredicate() {
                    @Override
                    public boolean isGroupRepresentative(Id<Person> personId) {
                        return SharingFactorTracker.GroupPredicate.super.isGroupRepresentative(personId);
                    }
                }))).asEagerSingleton();
        addEventHandlerBinding().to(modalKey(SharingFactorTracker.class));
        bindModal(SharingFactorControlerListener.class).toProvider(modalProvider(getter ->
                new SharingFactorControlerListener(getConfig(), drtConfigGroup,
                        getter.getModal(SharingFactorTracker.class),
                        getter.get(MatsimServices.class))
        ));
        addControlerListenerBinding().to(modalKey(SharingFactorControlerListener.class));
    }
}
