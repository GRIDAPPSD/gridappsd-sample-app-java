package sample.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.jms.JMSException;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.northconcepts.exception.SystemException;

import gov.pnnl.goss.gridappsd.dto.Difference;
import gov.pnnl.goss.gridappsd.dto.DifferenceMessage;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;
import sample.dto.Input;
import sample.dto.SimulationInput;

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
	
	
	public JsonArray getCapacitorMrid(String modelMrid) throws SystemException, JMSException{
		
		String request = "{\"requestType\": \"QUERY_OBJECT_IDS\","+
					"\"modelId\": \""+modelMrid+"\","+
					"\"resultFormat\": \"JSON\","+
					"\"objectType\": \"LinearShuntCompensator\"}";
		
		String topic = "goss.gridappsd.process.request.data.powergridmodel";
		 
		String response = client.getResponse(request, topic, RESPONSE_FORMAT.JSON).toString();
		
		JsonParser parser = new JsonParser();
		JsonObject json = (JsonObject) parser.parse(response);
		return json.getAsJsonObject("data").getAsJsonArray("objectIds");
	
	}
	
	public JsonArray getPVObjetcDict(String modelMrid) throws SystemException, JMSException{
		
		String request = "{\"requestType\": \"QUERY_OBJECT_DICT\","+
					"\"modelId\": \""+modelMrid+"\","+
					"\"resultFormat\": \"JSON\","+
					"\"objectType\": \"PowerElectronicsCollection\"}";
		
		String topic = "goss.gridappsd.process.request.data.powergridmodel";
		 
		String response = client.getResponse(request, topic, RESPONSE_FORMAT.JSON).toString();
		
		JsonParser parser = new JsonParser();
		JsonObject json = (JsonObject) parser.parse(response);
		return json.getAsJsonObject("data").getAsJsonArray();
	
	}
	
	public JsonArray getPVObjetcMeasurements(String modelMrid) throws SystemException, JMSException{
		
		String request = "{\"requestType\": \"QUERY_OBJECT_MESUREMENTS\","+
					"\"modelId\": \""+modelMrid+"\","+
					"\"resultFormat\": \"JSON\","+
					"\"objectType\": \"PowerElectronicsCollection\"}";
		
		String topic = "goss.gridappsd.process.request.data.powergridmodel";
		 
		String response = client.getResponse(request, topic, RESPONSE_FORMAT.JSON).toString();
		
		JsonParser parser = new JsonParser();
		JsonObject json = (JsonObject) parser.parse(response);
		return json.getAsJsonObject("data").getAsJsonArray();
	
	}
	
	
	public static void main(String[] args){
		
		try {
			
			String simulationId = args[0];
			String simulationOutputTopic = GridAppsDConstants.topic_simulationOutput+"."+simulationId;
			
			RequestSimulation request = RequestSimulation.parse(args[1]);
			String modelMrid = request.getPower_system_config().getLine_name();
			
			SampleApp sampleApp = new SampleApp();
			
			JsonArray pv_object = sampleApp.getPVObjetcDict(modelMrid);
			System.out.println(pv_object);
			JsonArray pv_measurements = sampleApp.getPVObjetcMeasurements(modelMrid);
			System.out.println(pv_measurements);
			
			
			//sampleApp.client.subscribe(simulationOutputTopic, new ResponseEvent(capacitors, sampleApp.client, simulationId));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}

class ResponseEvent implements GossResponseEvent{
	
	JsonArray capacitors = null;
	int messageCount = 0;
	int messagePeriod = 5;
	boolean lastToggleOn = false;
	DifferenceMessage differenceMessage = null;
	Client client;
	String simulationId;
	
	
	ResponseEvent(JsonArray capacitors, Client client, String simulationId){
		
		super();
		this.capacitors = capacitors;
		//this.client = client;
		this.simulationId = simulationId;
		this.differenceMessage = new DifferenceMessage();
		
		this.differenceMessage.forward_differences = createDifferenceList(capacitors);
		this.differenceMessage.reverse_differences = createDifferenceList(capacitors);
		
		ClientServiceFactory factory = new ClientServiceFactory();
		Credentials credentials = new UsernamePasswordCredentials("system","manager");

		try {
			this.client = factory.create(PROTOCOL.STOMP,credentials);
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		
	}

	@Override
	public void onMessage(Serializable response) {
		
		messageCount += 1;
		
		if(messageCount % messagePeriod ==0) {
			
			DataResponse message = (DataResponse)response;
			
			
			JsonParser parser = new JsonParser();
			JsonObject json = (JsonObject) parser.parse(message.getData().toString());
			
			JsonPrimitive timestamp = json.getAsJsonObject("message").getAsJsonPrimitive(("timestamp"));
			
			
			
			this.differenceMessage.difference_mrid = UUID.randomUUID().toString();
			this.differenceMessage.timestamp = timestamp.getAsLong();
			
			if(lastToggleOn) {
				for(Object obj : differenceMessage.reverse_differences) {
					Difference difference = ((Difference)obj);
					difference.value = 0;
				}
				for(Object obj : differenceMessage.forward_differences) {
					Difference difference = (Difference)obj;
					difference.value = 1;
				}
				lastToggleOn = false;
			}
			else {
				for(Object obj : differenceMessage.reverse_differences) {
					Difference difference = (Difference)obj;
					difference.value = 1;
				}
				for(Object obj : differenceMessage.forward_differences) {
					Difference difference = (Difference)obj;
					difference.value = 0;
				}
				lastToggleOn = true;
			}
			
			
			Input input = new Input();
			input.simulation_id = simulationId;
			input.message = differenceMessage;
			
			SimulationInput simulationInput = new SimulationInput();
			simulationInput.input = input;
			System.out.println(simulationInput);
			this.client.publish(GridAppsDConstants.topic_simulationInput+"."+simulationId, simulationInput);
		}
		
	}
	
	private List<Object> createDifferenceList(JsonArray capacitors){
		
		List<Object> differenceList = new ArrayList<Object>();
		
		for(JsonElement obj : capacitors) {
			Difference difference = new Difference();
			difference.object = obj.getAsString();
			difference.attribute = "ShuntCompensator.sections";
			differenceList.add(difference);
		}
		
		return differenceList;
	}
	
}
