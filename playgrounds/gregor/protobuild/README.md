# Setup of protobuf (tested on Mac)

One needs the correct version of the `protoc` compiler for `build.sh` to work.  Currently, this seems to be the version 
v3.0.0-alpha-2.  I (kai, jun-15) had success with the following sequence of steps:

* `git clone https://github.com/google/protobuf.git`

* `git tag v3.0.0-alpha-2`

* compile protobuf according to instructions on [github](https://github.com/google/protobuf) .

# Setup of grpc

grpc is the piece that supports remote procedure calls.  That might not be necessary to get started.  In that case, the
corresponding line in build.sh can be commented out.  However, it seems that the programming is really easier when this is
included.  So it might be easier to invest the additional effort in the setup.

I (kai, jun-15) had success with the following:

* go to [search.maven.org](http://search.maven.org) and search for `grpc-java`.

* Download the corresponding `*.exe` for your platform (it is also `*.exe` for Mac) and put it in the directory where 
`build.sh` resides.

* Replace `protoc-gen-grpc-java` in `build.sh` by the name of the file you just downloaded.

The grpc documentation implies that this could also be included via maven.  This, however, needs maven3, and matsim 
currently does not have that, so it would have to be done somehow "on the side".  The above path seems easier for the
time being ... in particular since one does not need these files to *run* the code, just to compile it.  



# Build sequence (on Mac)

* edit the *.proto files in `src.main.resources/proto/...` according to needs

* run `sh build.sh` in `protobuild` directory.  This will generate the necessary java classes.  The class name is the
same as the name of the `*.proto` file.  The package is given inside the `*.proto` file.

* One can now program against those java classes.  For example, `VisServer.initScenario()` is copying the network part of the
matsim scenario into the network part of the proto scenario.  That scenario is public, thus it can be used from the outside; 
it seems to be used in `BlockingVisServiceImpl`.

`update_from_local_build.sh` is just there for Gregor to maintain the pieces.  Those pieces are commited into the MATSim 
repository, so they are there when you try to use this.


