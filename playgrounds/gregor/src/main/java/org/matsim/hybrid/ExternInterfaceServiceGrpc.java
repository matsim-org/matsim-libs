package org.matsim.hybrid;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;

@javax.annotation.Generated("by gRPC proto compiler")
public class ExternInterfaceServiceGrpc {

  private ExternInterfaceServiceGrpc() {}

  public static final String SERVICE_NAME = "org.matsim.hybrid.ExternInterfaceService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace,
      org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> METHOD_REQ_MATSIM2EXTERN_HAS_SPACE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "org.matsim.hybrid.ExternInterfaceService", "reqMATSim2ExternHasSpace"),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent,
      org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> METHOD_REQ_MATSIM2EXTERN_PUT_AGENT =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "org.matsim.hybrid.ExternInterfaceService", "reqMATSim2ExternPutAgent"),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternDoSimStep,
      org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> METHOD_REQ_EXTERN_DO_SIM_STEP =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "org.matsim.hybrid.ExternInterfaceService", "reqExternDoSimStep"),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternDoSimStep.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim,
      org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> METHOD_REQ_EXTERN_ON_PREPARE_SIM =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "org.matsim.hybrid.ExternInterfaceService", "reqExternOnPrepareSim"),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternAfterSim,
      org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> METHOD_REQ_EXTERN_AFTER_SIM =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "org.matsim.hybrid.ExternInterfaceService", "reqExternAfterSim"),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternAfterSim.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed.getDefaultInstance()));

  public static ExternInterfaceServiceStub newStub(io.grpc.Channel channel) {
    return new ExternInterfaceServiceStub(channel);
  }

  public static ExternInterfaceServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ExternInterfaceServiceBlockingStub(channel);
  }

  public static ExternInterfaceServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ExternInterfaceServiceFutureStub(channel);
  }

  public static interface ExternInterfaceService {

    public void reqMATSim2ExternHasSpace(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> responseObserver);

    public void reqMATSim2ExternPutAgent(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> responseObserver);

    public void reqExternDoSimStep(org.matsim.hybrid.MATSimInterface.ExternDoSimStep request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> responseObserver);

    public void reqExternOnPrepareSim(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> responseObserver);

    public void reqExternAfterSim(org.matsim.hybrid.MATSimInterface.ExternAfterSim request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> responseObserver);
  }

  public static interface ExternInterfaceServiceBlockingClient {

    public org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed reqMATSim2ExternHasSpace(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request);

    public org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed reqMATSim2ExternPutAgent(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request);

    public org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived reqExternDoSimStep(org.matsim.hybrid.MATSimInterface.ExternDoSimStep request);

    public org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed reqExternOnPrepareSim(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request);

    public org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed reqExternAfterSim(org.matsim.hybrid.MATSimInterface.ExternAfterSim request);
  }

  public static interface ExternInterfaceServiceFutureClient {

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> reqMATSim2ExternHasSpace(
        org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> reqMATSim2ExternPutAgent(
        org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> reqExternDoSimStep(
        org.matsim.hybrid.MATSimInterface.ExternDoSimStep request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> reqExternOnPrepareSim(
        org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> reqExternAfterSim(
        org.matsim.hybrid.MATSimInterface.ExternAfterSim request);
  }

  public static class ExternInterfaceServiceStub extends io.grpc.stub.AbstractStub<ExternInterfaceServiceStub>
      implements ExternInterfaceService {
    private ExternInterfaceServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ExternInterfaceServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExternInterfaceServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ExternInterfaceServiceStub(channel, callOptions);
    }

    @java.lang.Override
    public void reqMATSim2ExternHasSpace(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REQ_MATSIM2EXTERN_HAS_SPACE, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void reqMATSim2ExternPutAgent(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REQ_MATSIM2EXTERN_PUT_AGENT, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void reqExternDoSimStep(org.matsim.hybrid.MATSimInterface.ExternDoSimStep request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_DO_SIM_STEP, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void reqExternOnPrepareSim(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_ON_PREPARE_SIM, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void reqExternAfterSim(org.matsim.hybrid.MATSimInterface.ExternAfterSim request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_AFTER_SIM, getCallOptions()), request, responseObserver);
    }
  }

  public static class ExternInterfaceServiceBlockingStub extends io.grpc.stub.AbstractStub<ExternInterfaceServiceBlockingStub>
      implements ExternInterfaceServiceBlockingClient {
    private ExternInterfaceServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ExternInterfaceServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExternInterfaceServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ExternInterfaceServiceBlockingStub(channel, callOptions);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed reqMATSim2ExternHasSpace(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request) {
      return blockingUnaryCall(
          getChannel().newCall(METHOD_REQ_MATSIM2EXTERN_HAS_SPACE, getCallOptions()), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed reqMATSim2ExternPutAgent(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request) {
      return blockingUnaryCall(
          getChannel().newCall(METHOD_REQ_MATSIM2EXTERN_PUT_AGENT, getCallOptions()), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived reqExternDoSimStep(org.matsim.hybrid.MATSimInterface.ExternDoSimStep request) {
      return blockingUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_DO_SIM_STEP, getCallOptions()), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed reqExternOnPrepareSim(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request) {
      return blockingUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_ON_PREPARE_SIM, getCallOptions()), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed reqExternAfterSim(org.matsim.hybrid.MATSimInterface.ExternAfterSim request) {
      return blockingUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_AFTER_SIM, getCallOptions()), request);
    }
  }

  public static class ExternInterfaceServiceFutureStub extends io.grpc.stub.AbstractStub<ExternInterfaceServiceFutureStub>
      implements ExternInterfaceServiceFutureClient {
    private ExternInterfaceServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ExternInterfaceServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExternInterfaceServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ExternInterfaceServiceFutureStub(channel, callOptions);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> reqMATSim2ExternHasSpace(
        org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REQ_MATSIM2EXTERN_HAS_SPACE, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> reqMATSim2ExternPutAgent(
        org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REQ_MATSIM2EXTERN_PUT_AGENT, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> reqExternDoSimStep(
        org.matsim.hybrid.MATSimInterface.ExternDoSimStep request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_DO_SIM_STEP, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> reqExternOnPrepareSim(
        org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_ON_PREPARE_SIM, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> reqExternAfterSim(
        org.matsim.hybrid.MATSimInterface.ExternAfterSim request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_AFTER_SIM, getCallOptions()), request);
    }
  }

  public static io.grpc.ServerServiceDefinition bindService(
      final ExternInterfaceService serviceImpl) {
    return io.grpc.ServerServiceDefinition.builder(SERVICE_NAME)
      .addMethod(
        METHOD_REQ_MATSIM2EXTERN_HAS_SPACE,
        asyncUnaryCall(
          new io.grpc.stub.ServerCalls.UnaryMethod<
              org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace,
              org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed>() {
            @java.lang.Override
            public void invoke(
                org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request,
                io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> responseObserver) {
              serviceImpl.reqMATSim2ExternHasSpace(request, responseObserver);
            }
          }))
      .addMethod(
        METHOD_REQ_MATSIM2EXTERN_PUT_AGENT,
        asyncUnaryCall(
          new io.grpc.stub.ServerCalls.UnaryMethod<
              org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent,
              org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed>() {
            @java.lang.Override
            public void invoke(
                org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request,
                io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> responseObserver) {
              serviceImpl.reqMATSim2ExternPutAgent(request, responseObserver);
            }
          }))
      .addMethod(
        METHOD_REQ_EXTERN_DO_SIM_STEP,
        asyncUnaryCall(
          new io.grpc.stub.ServerCalls.UnaryMethod<
              org.matsim.hybrid.MATSimInterface.ExternDoSimStep,
              org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived>() {
            @java.lang.Override
            public void invoke(
                org.matsim.hybrid.MATSimInterface.ExternDoSimStep request,
                io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> responseObserver) {
              serviceImpl.reqExternDoSimStep(request, responseObserver);
            }
          }))
      .addMethod(
        METHOD_REQ_EXTERN_ON_PREPARE_SIM,
        asyncUnaryCall(
          new io.grpc.stub.ServerCalls.UnaryMethod<
              org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim,
              org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed>() {
            @java.lang.Override
            public void invoke(
                org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request,
                io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> responseObserver) {
              serviceImpl.reqExternOnPrepareSim(request, responseObserver);
            }
          }))
      .addMethod(
        METHOD_REQ_EXTERN_AFTER_SIM,
        asyncUnaryCall(
          new io.grpc.stub.ServerCalls.UnaryMethod<
              org.matsim.hybrid.MATSimInterface.ExternAfterSim,
              org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed>() {
            @java.lang.Override
            public void invoke(
                org.matsim.hybrid.MATSimInterface.ExternAfterSim request,
                io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> responseObserver) {
              serviceImpl.reqExternAfterSim(request, responseObserver);
            }
          })).build();
  }
}
