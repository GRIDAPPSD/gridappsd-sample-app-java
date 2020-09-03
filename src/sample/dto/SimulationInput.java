/**
 * 
 */
package sample.dto;

import java.io.Serializable;

import com.google.gson.Gson;


public class SimulationInput implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public String command = "update";
	public Input input;

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}

}

