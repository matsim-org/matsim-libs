package org.matsim.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.util.Modules;

import java.util.Map;

/**
 * Created by michaelzilske on 23/09/15.
 */
public class OverrideMultibinding {

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                install(Modules.override(new AbstractModule() {
                    @Override
                    protected void configure() {
                        MapBinder.newMapBinder(binder(), String.class, String.class).addBinding("wurst").toInstance("blubb");
                    }
                }).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        MapBinder.newMapBinder(binder(), String.class, String.class).addBinding("wurst").toInstance("pröt");
                    }
                }));
                bind(Pups.class);
            }
        });
        injector.getInstance(Pups.class).sag();
    }

    private static class Pups {

        @Inject
        Map<String, String> strings;

        void sag() {
            System.out.println(strings.get("wurst"));
        }

    }

}
