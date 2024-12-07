package org.matsim.contrib.ev.strategic.access;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingUtils;

/**
 * Utility service which is used to cache which person has which charging
 * subscriptions and which charger required which subscriptions.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class SubscriptionRegistry {
    static public final String SUBSCRIPTIONS_ATTRIBUTE = "sevc:subscriptions";

    private final IdMap<Person, Set<String>> personCache = new IdMap<>(Person.class);

    public Set<String> getPersonSubscriptions(Person person) {
        Set<String> subscriptions = personCache.get(person.getId());

        if (subscriptions == null) {
            String rawSubscriptions = (String) person.getAttributes().getAttribute(SUBSCRIPTIONS_ATTRIBUTE);

            if (rawSubscriptions != null) {
                subscriptions = new HashSet<>();

                for (String subscription : rawSubscriptions.split(",")) {
                    subscriptions.add(subscription.trim());
                }

                if (subscriptions.size() == 1) {
                    subscriptions = Collections.singleton(subscriptions.iterator().next());
                }
            } else {
                subscriptions = Collections.emptySet();
            }

            personCache.put(person.getId(), subscriptions);
        }

        return subscriptions;
    }

    private final IdMap<Charger, Set<String>> chargerCache = new IdMap<>(Charger.class);

    public Set<String> getChargerSubscriptions(ChargerSpecification charger) {
        Set<String> subscriptions = chargerCache.get(charger.getId());

        if (subscriptions == null) {
            String rawSubscriptions = (String) charger.getAttributes().getAttribute(SUBSCRIPTIONS_ATTRIBUTE);

            if (rawSubscriptions != null) {
                subscriptions = new HashSet<>();

                for (String subscription : rawSubscriptions.split(",")) {
                    subscriptions.add(subscription.trim());
                }

                if (subscriptions.size() == 1) {
                    subscriptions = Collections.singleton(subscriptions.iterator().next());
                }
            } else {
                subscriptions = Collections.emptySet();
            }

            chargerCache.put(charger.getId(), subscriptions);
        }

        return subscriptions;
    }

    /**
     * Adds a subscription for a person
     */
    static public void addSubscription(Person person, String subscription) {
        Set<String> subscriptions = StrategicChargingUtils.readList(person, SUBSCRIPTIONS_ATTRIBUTE);
        subscriptions.add(subscription);
        StrategicChargingUtils.writeList(person, SUBSCRIPTIONS_ATTRIBUTE, subscriptions);
    }

    /**
     * Returns the subscriptions of a person
     */
    static public Set<String> getSubscriptions(Person person) {
        return StrategicChargingUtils.readList(person, SUBSCRIPTIONS_ATTRIBUTE);
    }

    /**
     * Adds a required subscription for a charger
     */
    static public void addSubscription(ChargerSpecification person, String subscription) {
        Set<String> subscriptions = StrategicChargingUtils.readList(person, SUBSCRIPTIONS_ATTRIBUTE);
        subscriptions.add(subscription);
        StrategicChargingUtils.writeList(person, SUBSCRIPTIONS_ATTRIBUTE, subscriptions);
    }

    /**
     * Returns the required subscriptions for a charger
     */
    static public Set<String> getSubscriptions(ChargerSpecification charger) {
        return StrategicChargingUtils.readList(charger, SUBSCRIPTIONS_ATTRIBUTE);
    }
}
