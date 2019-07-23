/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Graph.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.guice;

import com.google.common.collect.Lists;
import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.grapher.AbstractInjectorGrapher;
import com.google.inject.grapher.Alias;
import com.google.inject.grapher.NodeId;
import com.google.inject.spi.ProviderBinding;
import com.google.inject.util.Types;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerI;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vis.snapshotwriters.SnapshotWriter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GraphReduced {

    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        String configFileToGraph = args[0];
        String moduleToGraph = args[1];
        String outputDirectory = args[2];

        Config configToGraph = ConfigUtils.loadConfig(configFileToGraph);
        configToGraph.controler().setOutputDirectory(outputDirectory);
        configToGraph.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        Class<?> moduleClass = Class.forName(moduleToGraph);
        Object module = moduleClass.newInstance();

        com.google.inject.Injector matsimInjector = Injector.createInjector(configToGraph, (AbstractModule) module);
        matsimInjector.getInstance(ControlerI.class);
        try (PrintWriter out = new PrintWriter(new File(outputDirectory +"/guice.dot"))) {
            JGraphTGrapher renderer = new JGraphTGrapher(new AbstractInjectorGrapher.GrapherParameters()
					.setAliasCreator(bindings -> {
                                // Copied from ProviderAliasCreator
                                List<Alias> allAliases = Lists.newArrayList();
                                for (Binding<?> binding : bindings) {
                                    if (binding instanceof ProviderBinding) {
                                        allAliases.add(new Alias(NodeId.newTypeId(binding.getKey()),
                                                NodeId.newTypeId(((ProviderBinding<?>) binding).getProvidedKey())));
                                    }
                                }
                                allAliases.addAll(getMapBinderAliases(String.class, TravelTime.class, bindings));
                                allAliases.addAll(getMapBinderAliases(String.class, TravelDisutilityFactory.class, bindings));
                                allAliases.addAll(getMapBinderAliases(String.class, RoutingModule.class, bindings));
                                allAliases.addAll(getMapBinderAliases(StrategyConfigGroup.StrategySettings.class, PlanStrategy.class, bindings));
                                allAliases.addAll(getMultibinderAliases(ControlerListener.class, bindings));
                                allAliases.addAll(getMultibinderAliases(SnapshotWriter.class, bindings));
                                allAliases.addAll(getMultibinderAliases(MobsimListener.class, bindings));
                                allAliases.addAll(getMultibinderAliases(EventHandler.class, bindings));
                                return allAliases;
                            }
                    ), out);
            renderer.graph(matsimInjector.getInstance(com.google.inject.Injector.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static List<Alias> getMultibinderAliases(Type aClass, Iterable<Binding<?>> bindings) {
		List<Alias> aliases = Lists.newArrayList();
		NodeId toId = NodeId.newTypeId(Key.get(Types.setOf(aClass)));
		ParameterizedType comGoogleInjectProvider = Types.newParameterizedType(Provider.class, aClass);
		ParameterizedType javaxInjectProvider = Types.newParameterizedType(javax.inject.Provider.class, aClass);
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.setOf(aClass))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.newParameterizedType(Collection.class, comGoogleInjectProvider))), toId));
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.newParameterizedType(Collection.class, comGoogleInjectProvider))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.newParameterizedType(Collection.class, javaxInjectProvider))), toId));
		for (Binding<?> binding : bindings) {
			if (binding.getKey().getTypeLiteral().getType().equals(aClass) && binding.getKey().getAnnotationType() != null && binding.getKey().getAnnotationType().getName().equals("com.google.inject.multibindings.Element")) {
				aliases.add(new Alias(NodeId.newTypeId(binding.getKey()), toId));
			}
		}
		return aliases;
	}

    static <K> List<Alias> getMapBinderAliases(Class<K> keyType, Type aClass, Iterable<Binding<?>> bindings) {
		List<Alias> aliases = Lists.newArrayList();
		NodeId toId = NodeId.newTypeId(Key.get(Types.mapOf(keyType, aClass)));
		ParameterizedType comGoogleInjectProvider = Types.newParameterizedType(Provider.class, aClass);
		ParameterizedType javaxInjectProvider = Types.newParameterizedType(javax.inject.Provider.class, aClass);
		ParameterizedType stringToComGoogleInjectProviderMapEntry = Types.newParameterizedTypeWithOwner(Map.class, Map.Entry.class, keyType, comGoogleInjectProvider);
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.setOf(stringToComGoogleInjectProviderMapEntry))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.mapOf(keyType, comGoogleInjectProvider))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.mapOf(keyType, javaxInjectProvider))), toId));
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.mapOf(keyType, comGoogleInjectProvider))), toId));
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.mapOf(keyType, aClass))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.setOf(stringToComGoogleInjectProviderMapEntry))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.newParameterizedType(Collection.class, Types.newParameterizedType(javax.inject.Provider.class, stringToComGoogleInjectProviderMapEntry)))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.newParameterizedType(Collection.class, Types.newParameterizedType(Provider.class, stringToComGoogleInjectProviderMapEntry)))), toId));
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.newParameterizedType(Collection.class, Types.newParameterizedType(Provider.class, stringToComGoogleInjectProviderMapEntry)))), toId));
		for (Binding<?> binding : bindings) {
			if (binding.getKey().getTypeLiteral().getType().equals(aClass) && binding.getKey().getAnnotationType() != null && binding.getKey().getAnnotationType().getName().equals("com.google.inject.multibindings.Element")) {
				aliases.add(new Alias(NodeId.newTypeId(binding.getKey()), toId));
				aliases.add(new Alias(NodeId.newInstanceId(binding.getKey()), toId));
			}
			if (binding.getKey().getTypeLiteral().getType().equals(stringToComGoogleInjectProviderMapEntry) && binding.getKey().getAnnotationType() != null && binding.getKey().getAnnotationType().getName().equals("com.google.inject.multibindings.Element")) {
				aliases.add(new Alias(NodeId.newTypeId(binding.getKey()), toId));
				aliases.add(new Alias(NodeId.newInstanceId(binding.getKey()), toId));
			}
		}
		return aliases;
	}
}
