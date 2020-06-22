
# Minibus

Package that takes demand and infrastructure (roads, ...) as input and runs an adaptive "minibus" model to serve that demand. The resulting minibus lines can for example be used as follows:
 *  As a starting point to construct a schedule for formal public transit. 
 *  As paratransit supply for scenarios where no information about paratransit is available. 

 Some publications (list not regularly updated): 
 *  Ph.D. dissertation of Andreas Neumann, pdf-version available at [ UB, TU Berlin ](http://nbn-resolving.de/urn/resolver.pl?urn:nbn:de:kobv:83-opus4-53866) 
 *  Towards a simulation of minibuses in South Africa, preprint available from [ VSP, TU Berlin ](https://svn.vsp.tu-berlin.de/repos/public-svn/publications/vspwp/2014/14-03/) 

Look into [ org.matsim.contrib.minibus.PMain](http://ci.matsim.org:8080/job/MATSim_contrib_M2/org.matsim.contrib$minibus/javadoc/org/matsim/contrib/minibus/PMain.html) to get started. 

Quickstart from command line (without eclipse): 
1.  Create an empty directory and cd into it. 
2.  Download minibus-0.X.0-SNAPSHOT-rXXXXX.zip from [ matsim.org](http://matsim.org/files/builds/) into that directory and unzip its content.
3.  Get the illustrative scenario from [ VSP, TU Berlin](http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/examples/countries/atlantis/minibus/), and place it in the same directory.
4.  Run the scenario from the command line by typing   
    
        java -Xmx2000m -jar minibus-0.X.0-SNAPSHOT.jar config.xml  



  