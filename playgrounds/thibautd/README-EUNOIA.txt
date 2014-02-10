This contains the EUNOIA-related MATSim release, corresponding to MATSim SVN revision 27897.
This "release" can be used or using this package, or by checking out the code at the revision
specified above and building it. You will need to checkout the whole MATSim repository (including
"contribs" and "playgrounds"), as the eunoia package is located in the thibautd playground.
Instructions on checking out the code can be found at http://matsim.org/docs/devguide/eclipse.

Two important parts are:
- the eu.eunoiaproject.bikesharing package, that one can find in the thibautd-0.6.0-SNAPSHOT.jar file
- the standard MATSim, that is included as a dependency in the libs/ directory

the apidocs/ directory contains the javadoc of the thibautd playground project. The only important
part is the eu.eunoiaproject.bikesharing package and its subpackages.

This release can be used as an Eclipse project very much the same way as the standard MATSim release:
follow th instructions in http://matsim.org/docs/tutorials/8lessons/installation/software

It can also be used from the command line in the same way as a nightly build: http://matsim.org/downloads/nightly.
Using it this way gives access to both standard MATSim and the eunoia bike sharing module, though
command line completion works only for the classes in the eunoia package. To get command line completion
for MATSim as well, you can use java -cp thibautd-0.6.0-SNAPSHOT.jar:matsim-0.6.0-SNAPSHOT.jar <classname> <args>.

For examples of usage of the bike sharing module, have a look at the code under eu.eunoiaproject.bikesharing.examples.
