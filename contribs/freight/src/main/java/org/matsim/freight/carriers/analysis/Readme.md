**The package name (org.matsim.contrib.freight) was chosen on purpose.**
It should allow to move the stuff to the freight contrib later without breaking any code.

This package contains some analysis stuff for freight outputs.
The content was created by Jakob Hanisch during the MATSim advanced class 2020/21.

**It is untested and needs some kind of review --> be very careful when using it!**
(KMT, Sep 21)

**Update April 2023 -> Event-based analysis (KMT)**
We do now have an (unfortunately!!!) untested new approach:
We now can base on freight events, that were introduce during the last year.
This avoids the _guessing_ of some information, like vehicleType or carrierId.

This analysis is of course not perfect and can/should be extended (and then moved over to the freight contrib.)
Since I programmed it for the SimGV class, I believe it is a good option moving it to a more central place.

I also deprecated the "old" guessing approach.