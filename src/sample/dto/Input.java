package sample.dto;

import java.io.Serializable;

import com.google.gson.Gson;

import gov.pnnl.goss.gridappsd.dto.DifferenceMessage;


public class Input implements Serializable{

	private static final long serialVersionUID = 1L;
	
	public String simulation_id;
	public DifferenceMessage message;
		
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
}
