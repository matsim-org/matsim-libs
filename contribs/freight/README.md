
# Freight

This contrib contains the following packages:

## Carriers
(This is formally knows as 'freight contrib')

Package that plugs vehicle routing problem algorithms (programmed in external package jsprit) into MATSim.

A good starting point for jsprit is [ https://github.com/graphhopper/jsprit](https://github.com/graphhopper/jsprit).

For runnable code see, e.g., the packages org.matsim.contrib.freight.carriers.usecases.* above .

## Logistics
(This code comes from [https://github.com/matsim-vsp/logistics/](https://github.com/matsim-vsp/logistics/) )

This code deals with creating logistics chains for freight transport.

Here the decision agent is the logistics service provider (LSP) who decides on the logistics chain to be used for a given freight transport request.
Therefore, it can use carriers (see above) and hubs.

This package bases on work in the dfg-freight project.

   