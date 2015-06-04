# SetUp (on Mac)

One needs the correct version of the `protoc` compiler for `build.sh` to work.  Currently, this seems to be the version 
v3.0.0-alpha-2.  I had success with the following sequence of steps:

* `git clone https://github.com/google/protobuf.git`

* `git tag v3.0.0-alpha-2`

* compile protobuf according to instructions on https://github.com/google/protobuf .

# without grpc

grpc is the piece that supports remote procedure calls.  That might not be necessary to get started.  In that case, the
corresponding line in build.sh can be commented out.

# Build sequence (on Mac)

I (kai, jun'15) have not tried this, am only guessing.

* edit the *.proto files in `src.main.resources/proto/...` according to needs

* run `sh build.sh` in `protobuild` directory.  This will generate the necessary java classes.  Currently, they seem
to end up in `playground.gregor.proto`.

* One can now program against those java classes.  For example, `VisServer.initScenario()` is copying the network part of the
matsim scenario into the network part of the proto scenario.  That scenario is public, thus it can be used from the outside; 
it seems to be used in `BlockingVisServiceImpl`.

`update_from_local_build.sh` is just there for Gregor to maintain the pieces.  Those pieces are commited into the MATSim 
repository, so they are there when you try to use this.


