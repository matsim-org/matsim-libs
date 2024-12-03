/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.freight.receiver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * A container for all {@link Receiver}s.
 *
 * @author wlbean, jwjoubert
 */
public final class Receivers implements Attributable {

    private final Logger log = LogManager.getLogger(Receivers.class);
    private final Attributes attributes = new AttributesImpl();
    private String desc = "";

    /**
     * Create empty receiver collection.
     */
    private final Map<Id<Receiver>, Receiver> receiverMap = new TreeMap<>();
    private final Map<Id<ProductType>, ProductType> productTypeMap = new TreeMap<>();

    /**
     * The constructor is hidden. To instantiate, use {@link ReceiverUtils#createReceivers()}.
     */
    Receivers() {
    }

    /**
     * Returns the receivers in the container. The design choice (May'21)
     * was that this should be unmodifiable. If you want to edit a
     * {@link Receiver}, you cannot access it via this map, but should rather
     * use {@link Receivers#getReceiver(Id)}.
     */
    public Map<Id<Receiver>, Receiver> getReceivers() {
        return Collections.unmodifiableMap(receiverMap);
    }

    /**
     * Find a specific receiver or null if not found.
     */
    public Receiver getReceiver(Id<Receiver> receiverId) {
        if (!this.receiverMap.containsKey(receiverId)) {
            log.warn("Receiver '" + receiverId.toString() + "' does not exist. Returning null.");
            return null;
        }
        return this.receiverMap.get(receiverId);
    }

    /**
     * Add a new receiver to the container.
     */
    public void addReceiver(Receiver receiver) {
        if (!receiverMap.containsKey(receiver.getId())) {
            receiverMap.put(receiver.getId(), receiver);
        } else log.warn("receiver \"" + receiver.getId() + "\" already exists. Not adding it again.");
    }


    /**
     * Creates and adds a product type. Each product type is a unique stock
     * keeping unit (SKU). When the product type is created, it is registered
     * here inside the receivers container to ensure all unique SKUs are used
     * consistently.
     */
    ProductType createAndAddProductType(final Id<ProductType> typeId, final Id<Link> originLinkId) {
        if(this.productTypeMap.containsKey(typeId)){
            log.warn("Product type '" + typeId.toString() + "' already exists. Returning the existing value.");
            return this.productTypeMap.get(typeId);
        }

        ProductType productType = new ProductType(typeId, originLinkId);
        this.productTypeMap.put(typeId, productType);
        return productType;
    }


    /**
     * Gets a registered {@link ProductType} that represents a unique SKU. If
     * the product type has not been registered, the code will crash with a
     * {@link RuntimeException}.
     */
    ProductType getProductType(Id<ProductType> typeId) {
        if (this.productTypeMap.containsKey(typeId)) {
            return this.productTypeMap.get(typeId);
        } else {
            log.error("You must first create a unique product type before using it.");
            log.error("First instantiate using ReceiverUtils.createProductType(...)");
            throw new RuntimeException("The product type '" + typeId.toString() + "' does not exist.");
        }
    }

    public Collection<ProductType> getAllProductTypes() {
        return this.productTypeMap.values();
    }

    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    public void setDescription(String desc) {
        this.desc = desc;
    }

    public String getDescription() {
        return this.desc;
    }
}
