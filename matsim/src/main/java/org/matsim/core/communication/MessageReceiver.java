package org.matsim.core.communication;

@FunctionalInterface
public interface MessageReceiver {

    boolean expectsMoreMessages();

}
