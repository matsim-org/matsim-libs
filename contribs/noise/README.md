
# Noise

This package provides some tools to compute... 
 
 *  noise (emission) levels on network links (based on traffic volume, share of heavy goods vehicles, speed level) 
 *  noise (immission) levels at receiver points (based on the surrounding links' emissions and distances) 
 *  population units (= population densities) at receiver points (mapping agents to the nearest receiver point)
 *  noise damages (= noise exposure costs) at receiver points (based on population units and noise levels) 


The computation of noise levels is based on the German RLS 90 approach (Richtlinien fuer den Laermschutz and Strassen, Forschungsgesellschaft fuer Strassen- und Verkehrswesen), but applies some simplifications and minor modifications.
 Thus, noise levels should be considered as estimates rather than exact values. The conversion of noise exposures into monetary costs follows a slightly modified version of the German EWS approach (Empfehlungen fuer Wirtschaftlichkeitsuntersuchungen an Strassen, Forschungsgesellschaft fuer Strassen- und Verkehrswesen). 
 A more detailed description of the computation methodology and the model limitations is provided in the related publications.  Related publications (list not regularly updated): 
  *  I. Kaddoura, L. Kroeger, and K. Nagel. User-specific and dynamic internalization of road traffic noise exposures. Networks and Spatial Economics, 2016. DOI: 10.1007/s11067-016-9321-2. Preprint available [ here](https://svn.vsp.tu-berlin.de/repos/public-svn/publications/vspwp/2015/15-12/). 
  *  I. Kaddoura and K. Nagel. Activity-based computation of marginal noise exposure costs: Implications for traffic management. Transportation Research Record 2597, 2016. DOI: 10.3141/2597-15. Preprint available [ here](https://svn.vsp.tu-berlin.de/repos/public-svn/publications/vspwp/2015/15-13/). 

 For more documentation, please look into the javadoc documentation [here](http://ci.matsim.org:8080/job/MATSim_contrib_M2/org.matsim.contrib$noise/javadoc/org/matsim/contrib/noise/package-summary.html). 



  