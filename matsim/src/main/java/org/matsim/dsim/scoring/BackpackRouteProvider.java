package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Message;

/**
 * BackpackRouteProviders create new {@link BackpackRoute}s for each leg an agent starts. It is expected that one {@link BackpackRouteProvider} per mode is bound.
 * We need this slightly complex construct instead of directly binding {@link BackpackRoute} to a guice provider, because we need to re-construct
 * {@link BackpackRoute}s when {@link Backpack}s are transferred between network partitions. Add a {@link BackpackRouteProvider} like the following
 * example:
 *
 * <pre>
 * {@code
 * class SomeModule extends AbstractModule {
 *
 *     @Override
 *     protected void install() {
 *         var binder = MapBinder.newMapBinder(binder(), String.class, BackpackRouteProvider.class);
 *         binder.addBinding("my-special-mode").to(MySpecialBackpackRouteProvider.class);
 *     }
 * }
 * }
 *
 * </pre>
 */
public interface BackpackRouteProvider {

	BackpackRoute get();

	BackpackRoute get(Message fromMessage);
}
