package aurora.plugin.adapter.std.ws.consumer;

import org.apache.camel.builder.RouteBuilder;

import uncertain.composite.CompositeMap;
import aurora.plugin.esb.AuroraEsbContext;
import aurora.plugin.esb.console.ConsoleLog;
import aurora.plugin.esb.model.BusinessModel;
import aurora.plugin.esb.model.BusinessModelConsumer;
import aurora.plugin.esb.util.BusinessModelUtil;

public class STDConsumerBuilder extends RouteBuilder {

	private ConsoleLog clog = new ConsoleLog();
	private AuroraEsbContext esbContext;
	private CompositeMap consumerMap;

	private String name;

	public STDConsumerBuilder(AuroraEsbContext esbContext, CompositeMap consumer) {
		this.esbContext = esbContext;
		this.consumerMap = consumer;
//		this.name = consumerMap.getString("name", "");
	}

	public STDConsumerBuilder(String name) {
		this.name = name;
	}

	@Override
	public void configure() throws Exception {

		// BusinessModel businessModel = new BusinessModel("test");
//
//		BusinessModelConsumer businessModelConsumer = new BusinessModelConsumer();
//
//		businessModelConsumer.set("001", "test", "001_consumer");
		String id = consumerMap.getString("id","");
		BusinessModelConsumer businessModelConsumer = BusinessModelUtil.getBusinessModelConsumer(id);
		from("direct:" + businessModelConsumer.getId()).bean(
				new ConsumerHolder(businessModelConsumer), "consumer");
		//
		// from("seda:file").bean(new
		// DataSaveBean(),"save2File").to("seda:consumer");
		// from("seda:db").bean(new
		// DataSaveBean(),"save2DB").to("seda:consumer");
		//
		// // BusinessModel.name
		//
		// from("seda:consumer").bean("");
	}
}
