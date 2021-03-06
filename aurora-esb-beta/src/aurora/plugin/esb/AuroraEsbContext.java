package aurora.plugin.esb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.impl.DefaultCamelContext;

import uncertain.composite.CompositeMap;
import uncertain.core.IContainer;
import uncertain.event.Configuration;
import uncertain.logging.ILogger;
import uncertain.ocm.IObjectRegistry;
import uncertain.proc.IProcedureManager;
import uncertain.proc.Procedure;
import aurora.plugin.esb.adapter.AdapterManager;
import aurora.plugin.esb.adapter.RouteCheckBuilder;
import aurora.plugin.esb.adapter.RouteStatusChecker;
import aurora.plugin.esb.config.DataStore;
import aurora.plugin.esb.model.DirectConfig;
import aurora.plugin.esb.util.FileStore;
import aurora.service.IServiceFactory;
import aurora.service.ServiceThreadLocal;
import aurora.service.http.HttpServiceInstance;

public class AuroraEsbContext {
	private static final String RUNNING_PROPERTIES = "running_properties";
	private AuroraEsbServer server;
	private List<DirectConfig> task_configs = new ArrayList<DirectConfig>();
	private DefaultCamelContext context;

	private String log_proc;

	private boolean isNeedCommandConsole = true;

	private DataStore ds;

	private AdapterManager adapterManager = new AdapterManager();

	private List<CompositeMap> producerMaps = new ArrayList<CompositeMap>();

	private List<CompositeMap> consumerMaps = new ArrayList<CompositeMap>();

	private List<CompositeMap> autoStartProducerMaps = new ArrayList<CompositeMap>();

	private List<CompositeMap> autoStartConsumerMaps = new ArrayList<CompositeMap>();

	private Map<String, CompositeMap> activeProducerMaps = new HashMap<String, CompositeMap>();
	private Map<String, CompositeMap> activeConsumerMaps = new HashMap<String, CompositeMap>();

	private CompositeMap properties = new CompositeMap(RUNNING_PROPERTIES);
	private FileStore fs;

	private RouteCheckBuilder ecb = new RouteCheckBuilder();

	public AuroraEsbContext() {
	}

	public void loadProperties() {
		fs = new FileStore(getWorkPath());
		setProperties(fs.load(RUNNING_PROPERTIES));
	}

	public void saveProperties() {
		fs.save(getProperties(), RUNNING_PROPERTIES);
	}

	private String workPath = null;
	private ILogger mLogger;
	private IObjectRegistry registry;

	public IObjectRegistry getRegistry() {
		return registry;
	}

	public AuroraEsbServer getServer() {
		return server;
	}

	public void setServer(AuroraEsbServer server) {
		this.server = server;
	}

	public List<DirectConfig> getDirectConfigs() {
		return task_configs;
	}

	public void addTaskConfig(DirectConfig config) {
		task_configs.add(config);

	}

	public void setCamelContext(DefaultCamelContext context) {
		this.context = context;
	}

	public DefaultCamelContext getCamelContext() {
		return context;
	}

	public String getWorkPath() {
		return workPath;
	}

	public void setWorkPath(String workPath) {
		if (workPath.endsWith("/"))
			this.workPath = workPath;
		else
			this.workPath = workPath + "/";
	}

	public DataStore getDataStore() {
		return ds;
	}

	public void setDataStore(DataStore ds) {
		this.ds = ds;
	}

	public AdapterManager getAdapterManager() {
		return adapterManager;
	}

	public void setAdapterManager(AdapterManager adapterManager) {
		this.adapterManager = adapterManager;
	}

	public void setLoger(ILogger mLogger) {
		this.mLogger = mLogger;
	}

	public ILogger getmLogger() {
		return mLogger;
	}

	public CompositeMap executeProc(String proc_name, CompositeMap para)
			throws Exception {
		IProcedureManager procedureManager = (IProcedureManager) registry
				.getInstanceOfType(IProcedureManager.class);
		IServiceFactory serviceFactory = (IServiceFactory) registry
				.getInstanceOfType(IServiceFactory.class);
		String autoLoginProc = proc_name;
		Procedure proc = procedureManager.loadProcedure(autoLoginProc);
		CompositeMap auroraContext = new CompositeMap("esb_conext");
		CompositeMap createChild = auroraContext.createChild("parameter");
		// String locale = request.getLocale().toString();
		createChild.addChild(para);
		// createChild.put("locale", locale);
		HttpServiceInstance svc = createHttpService(autoLoginProc,
				procedureManager, auroraContext);

		ServiceThreadLocal.setCurrentThreadContext(auroraContext);
		ServiceInvoker.invokeProcedureWithTransaction(autoLoginProc, proc,
				serviceFactory, svc, auroraContext);
		// System.out.println("=================");
		// System.out.println(auroraContext.toXML());
		return auroraContext;

		// ServiceThreadLocal.setCurrentThreadContext(auroraContext);
		// HttpRequestTransfer.copyRequest(svc);
		// HttpSessionCopy.copySession(auroraContext,
		// request.getSession(false));
	}

	public HttpServiceInstance createHttpService(String service_name,

	IProcedureManager procedureManager, CompositeMap context) {
		HttpServiceInstance svc = new HttpServiceInstance(service_name,
				procedureManager);
		// svc.setRequest(request);
		// svc.setResponse(response);
		svc.setContextMap(context);
		svc.setName(service_name);
		// HttpRequestTransfer.copyRequest(svc);
		// HttpSessionCopy.copySession(svc.getContextMap(),
		// request.getSession(false));
		IContainer container = (IContainer) registry
				.getInstanceOfType(IContainer.class);
		Configuration config = (Configuration) container.getEventDispatcher();
		if (config != null)
			svc.setRootConfig(config);
		return svc;
	}

	public void setRegistry(IObjectRegistry registry) {
		this.registry = registry;
	}

	public List<CompositeMap> getProducerMaps() {
		return producerMaps;
	}

	public void addProducerMap(CompositeMap producerMap) {
		if (producerMap.getBoolean("autoStart".toLowerCase(), false)) {
			this.getAutoStartProducerMaps().add(producerMap);
		}
		this.producerMaps.add(producerMap);
	}

	public List<CompositeMap> getConsumerMaps() {
		return consumerMaps;
	}

	public void addConsumerMap(CompositeMap consumerMap) {
		if (consumerMap.getBoolean("autoStart".toLowerCase(), false)) {
			this.getAutoStartConsumerMaps().add(consumerMap);
		}
		this.consumerMaps.add(consumerMap);
	}

	public boolean isNeedCommandConsole() {
		return isNeedCommandConsole;
	}

	public void setNeedCommandConsole(boolean isNeedCommandConsole) {
		this.isNeedCommandConsole = isNeedCommandConsole;
	}

	public List<CompositeMap> getAutoStartProducerMaps() {
		return autoStartProducerMaps;
	}

	public List<CompositeMap> getAutoStartConsumerMaps() {
		return autoStartConsumerMaps;
	}

	public void bindProducer(CompositeMap compositeMap) throws Exception {

		// ProducerConsumer pc = new ProducerConsumer();
		// pc.setProducerMap(compositeMap);
		// this.getProducerConsumer().add(pc);
		// this.context.addRoutes(new ProducerBuilder(this, producer));
		String name = compositeMap.getString("name", "");
		if ("".equals(name.trim())) {
			return;
		}
		activeProducerMaps.put(name, compositeMap);
		this.context.addRoutes(this.adapterManager.createProducerRouteBuilder(
				this, compositeMap));
	}

	public boolean isActiveProducer(String producer) {
		CompositeMap compositeMap = activeProducerMaps.get(producer);
		return compositeMap != null;
	}

	public void shutdown() {
		saveProperties();
		if (context != null)
			try {
				context.shutdown();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	public CompositeMap getProperties() {
		return properties;
	}

	public void setProperties(CompositeMap properties) {
		this.properties = properties;
	}

	public String getLog_proc() {
		return log_proc;
	}

	public void setLog_proc(String log_proc) {
		this.log_proc = log_proc;
	}

	public void bindConsumer(CompositeMap compositeMap) throws Exception {

		// ProducerConsumer pc = new ProducerConsumer();
		// pc.setProducerMap(compositeMap);
		// this.getProducerConsumer().add(pc);
		// this.context.addRoutes(new ProducerBuilder(this, producer));
		String name = compositeMap.getString("name", "");
		if ("".equals(name.trim())) {
			return;
		}
		activeConsumerMaps.put(name, compositeMap);
		this.context.addRoutes(this.adapterManager.createConsumerRouteBuilder(
				this, compositeMap));

	}

	public void restartRoute(String routeId) {
		try {
			this.context.stopRoute(routeId);
			this.context.startRoute(routeId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addRouteChecker(String routeId, RouteStatusChecker checker) {
		this.ecb.addRouteChecker(routeId, checker);
	}

	public void removeRouteChecker(String routeId, RouteStatusChecker checker) {
		this.ecb.removeRouteChecker(routeId, checker);
	}

}
