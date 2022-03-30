package lsp.shipment;

//import demand.UtilityFunction;
import lsp.LogisticsSolutionElement;
import lsp.LSPResource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;

public class ShipmentUtils{
	private ShipmentUtils(){} // do not instantiate

	public static class LoggedShipmentHandleBuilder {
		private double startTime;
		private double endTime;
		private LogisticsSolutionElement element;
		private Id<LSPResource> resourceId;
		private Id<Link> linkId;

		private LoggedShipmentHandleBuilder(){
		}

		public static LoggedShipmentHandleBuilder newInstance(){
			return new LoggedShipmentHandleBuilder();
		}

		public void setStartTime(double startTime){
			this.startTime = startTime;
		}

		public void setEndTime(double endTime){
			this.endTime = endTime;
		}

		public void setLogisticsSolutionElement(LogisticsSolutionElement element){
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId){
			this.resourceId = resourceId;
		}

		public void setLinkId(Id<Link> linkId){
			this.linkId = linkId;
		}

		public ShipmentPlanElement build(){
			return new LoggedShipmentHandle(this);
		}

		// --- Getters --- //

		public double getStartTime() {
			return startTime;
		}

		public double getEndTime() {
			return endTime;
		}

		public LogisticsSolutionElement getElement() {
			return element;
		}

		public Id<LSPResource> getResourceId() {
			return resourceId;
		}

		public Id<Link> getLinkId() {
			return linkId;
		}
	}

	public static class LoggedShipmentLoadBuilder {
		private double startTime;
		private double endTime;
		private LogisticsSolutionElement element;
		private Id<LSPResource> resourceId;
		private Id<Carrier> carrierId;
		private Id<Link> linkId;

		private LoggedShipmentLoadBuilder(){
		}

		public static LoggedShipmentLoadBuilder newInstance(){
			return new LoggedShipmentLoadBuilder();
		}

		public void setStartTime(double startTime){
			this.startTime = startTime;
		}

		public void setEndTime(double endTime){
			this.endTime = endTime;
		}

		public void setLogisticsSolutionElement(LogisticsSolutionElement element){
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId){
			this.resourceId = resourceId;
		}

		public void setLinkId(Id<Link> linkId){
			this.linkId = linkId;
		}

		public void setCarrierId(Id<Carrier> carrierId){
			this.carrierId = carrierId;
		}

		public LoggedShipmentLoad build(){
			return new LoggedShipmentLoad(this);
		}

		// --- Getters --- //

		public double getStartTime() {
			return startTime;
		}

		public double getEndTime() {
			return endTime;
		}

		public LogisticsSolutionElement getElement() {
			return element;
		}

		public Id<LSPResource> getResourceId() {
			return resourceId;
		}

		public Id<Carrier> getCarrierId() {
			return carrierId;
		}

		public Id<Link> getLinkId() {
			return linkId;
		}
	}

	public static class LoggedShipmentTransportBuilder {
		private double startTime;
		private LogisticsSolutionElement element;
		private Id<LSPResource> resourceId;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private Id<Carrier> carrierId;

		private LoggedShipmentTransportBuilder(){
		}

		public static LoggedShipmentTransportBuilder newInstance(){
			return new LoggedShipmentTransportBuilder();
		}

		public void setStartTime(double startTime){
			this.startTime = startTime;
		}

		public void setLogisticsSolutionElement(LogisticsSolutionElement element){
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId){
			this.resourceId = resourceId;
		}

		public void setFromLinkId(Id<Link> fromLinkId){
			this.fromLinkId = fromLinkId;
		}

		public void setToLinkId(Id<Link> toLinkId){
			this.toLinkId = toLinkId;
		}

		public void setCarrierId(Id<Carrier> carrierId){
			this.carrierId = carrierId;
		}

		public LoggedShipmentTransport build(){
			return new LoggedShipmentTransport(this);
		}

		// --- Getters --- //
		public double getStartTime() {
			return startTime;
		}

		public LogisticsSolutionElement getElement() {
			return element;
		}

		public Id<LSPResource> getResourceId() {
			return resourceId;
		}

		public Id<Link> getFromLinkId() {
			return fromLinkId;
		}

		public Id<Link> getToLinkId() {
			return toLinkId;
		}

		public Id<Carrier> getCarrierId() {
			return carrierId;
		}
	}

	public static class ScheduledShipmentUnloadBuilder{
		double startTime;
		double endTime;
		LogisticsSolutionElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> linkId;
		CarrierService carrierService;

		private ScheduledShipmentUnloadBuilder(){
		}

		public static ScheduledShipmentUnloadBuilder newInstance(){
			return new ScheduledShipmentUnloadBuilder();
		}

		public void setStartTime(double startTime ){
			this.startTime = startTime;
		}

		public void setEndTime(double endTime ){
			this.endTime = endTime;
		}

		public void setLogisticsSolutionElement(LogisticsSolutionElement element ){
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId ){
			this.resourceId = resourceId;
		}

		public void setCarrierId(Id<Carrier> carrierId ){
			this.carrierId = carrierId;
		}

		public void setLinkId(Id<Link> linkId ){
			this.linkId = linkId;
		}

		public void setCarrierService(CarrierService carrierService ){
			this.carrierService = carrierService;
		}

		public ScheduledShipmentUnload build(){
			return new ScheduledShipmentUnload(this);
		}
	}

	public static class ScheduledShipmentTransportBuilder{
		double startTime;
		double endTime;
		LogisticsSolutionElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> fromLinkId;
		Id<Link> toLinkId;
		CarrierService carrierService;

		private ScheduledShipmentTransportBuilder(){
		}

		public static ScheduledShipmentTransportBuilder newInstance(){
			return new ScheduledShipmentTransportBuilder();
		}

		public void setStartTime(double startTime ){
			this.startTime = startTime;
		}

		public void setEndTime(double endTime ){
			this.endTime = endTime;
		}

		public void setLogisticsSolutionElement(LogisticsSolutionElement element ){
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId ){
			this.resourceId = resourceId;
		}

		public void setCarrierId(Id<Carrier> carrierId ){
			this.carrierId = carrierId;
		}

		public void setFromLinkId(Id<Link> fromLinkId ){
			this.fromLinkId = fromLinkId;
		}

		public void setToLinkId(Id<Link> toLinkId ){
			this.toLinkId = toLinkId;
		}

		public void setCarrierService(CarrierService carrierService ){
			this.carrierService = carrierService;
		}

		public ScheduledShipmentTransport build(){
			return new ScheduledShipmentTransport(this);
		}
	}

	public static class ScheduledShipmentLoadBuilder{
		double startTime;
		double endTime;
		LogisticsSolutionElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> linkId;
		CarrierService carrierService;

		private ScheduledShipmentLoadBuilder(){
		}

		public static ScheduledShipmentLoadBuilder newInstance(){
			return new ScheduledShipmentLoadBuilder();
		}

		public void setStartTime(double startTime ){
			this.startTime = startTime;
		}

		public void setEndTime(double endTime ){
			this.endTime = endTime;
		}

		public void setLogisticsSolutionElement(LogisticsSolutionElement element ){
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId ){
			this.resourceId = resourceId;
		}

		public void setLinkId(Id<Link> linkId ){
			this.linkId = linkId;
		}

		public void setCarrierId(Id<Carrier> carrierId ){
			this.carrierId = carrierId;
		}

		public void setCarrierService(CarrierService carrierService ){
			this.carrierService = carrierService;
		}

		public ScheduledShipmentLoad build(){
			return new ScheduledShipmentLoad(this);
		}
	}

	public static class ScheduledShipmentHandleBuilder{
		double startTime;
		double endTime;
		LogisticsSolutionElement element;
		Id<LSPResource> resourceId;

		private ScheduledShipmentHandleBuilder(){
		}

		public static ScheduledShipmentHandleBuilder newInstance(){
			return new ScheduledShipmentHandleBuilder();
		}

		public void setStartTime(double startTime ){
			this.startTime = startTime;
		}

		public void setEndTime(double endTime ){
			this.endTime = endTime;
		}

		public void setLogisticsSolutionElement(LogisticsSolutionElement element ){
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId ){
			this.resourceId = resourceId;
		}

		public void setLinkId(Id<Link> linkId ){
		}

		public ScheduledShipmentHandle build(){
			return new ScheduledShipmentHandle(this);
		}
	}

	public static class LoggedShipmentUnloadBuilder{
		double startTime;
		double endTime;
		LogisticsSolutionElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> linkId;

		private LoggedShipmentUnloadBuilder(){
		}

		public static LoggedShipmentUnloadBuilder newInstance(){
			return new LoggedShipmentUnloadBuilder();
		}

		public void setStartTime(double startTime ){
			this.startTime = startTime;
		}

		public void setEndTime(double endTime ){
			this.endTime = endTime;
		}

		public void setLogisticsSolutionElement(LogisticsSolutionElement element ){
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId ){
			this.resourceId = resourceId;
		}

		public void setLinkId(Id<Link> linkId ){
			this.linkId = linkId;
		}

		public void setCarrierId(Id<Carrier> carrierId ){
			this.carrierId = carrierId;
		}

		public LoggedShipmentUnload build(){
			return new LoggedShipmentUnload(this);
		}
	}
}
