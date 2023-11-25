package org.matsim.contrib.protobuf;

/**
 * Holds the current version of the protobuf wireformat for each type. Parsers should never attempt
 * to read newer versions. Supporting old versions is up to the individual implementations, but
 * increasing this value should indicate special handling is necessary.
 */
public final class PBVersion {

  /** Event format version. */
  public static final int EVENTS = 1;
}
