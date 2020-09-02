package sample.app;

import java.util.List;

import javax.jms.JMSException;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.northconcepts.exception.SystemException;

import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;

public class SampleApp {
	
	Client client;
	
	public SampleApp(){ 
		
	ClientServiceFactory factory = new ClientServiceFactory();
	Credentials credentials = new UsernamePasswordCredentials("system","manager");

	try {
		client = factory.create(PROTOCOL.STOMP,credentials);
	} catch (Exception e) {
		e.printStackTrace();
	}

	}
	
	
	public void get_capacitor_mrids(String modelMrid) throws SystemException, JMSException{
		
		String request = "{\"requestType\": \"QUERY_OBJECT_TYPES\","+
					"\"modelId\": \"_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3\","+
					"\"resultFormat\": \"JSON\"}";
		
		String topic = "goss.gridappsd.process.request.data.powergridmodel";
		 
		String response = client.getResponse(request, topic, RESPONSE_FORMAT.JSON).toString();
		
		System.out.println(response);
		
		
	
	}
	
	
	public static void main(String[] args){
		
		String simulationId = args[0];
		String simulationOutputTopic = GridAppsDConstants.topic_simulationOutput+simulationId;
		
		RequestSimulation request = RequestSimulation.parse(args[1]);
		String modelMrid = request.getPower_system_config().getLine_name();
		
		SampleApp sampleApp = new SampleApp();
		
		try {
			sampleApp.get_capacitor_mrids(modelMrid);
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
	}

}
