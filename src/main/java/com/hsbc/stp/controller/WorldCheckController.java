package com.hsbc.stp.controller;

import java.text.ParseException;

import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.hsbc.stp.domain.CustomerRequest;
import com.hsbc.stp.service.WorldCheckService;

@RestController
@RequestMapping("/wc")
public class WorldCheckController {

	@Autowired(required = true)
	private WorldCheckService worldCheckService;

	@RequestMapping(path = "/groups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
	@ResponseBody
	public String getGroupsDetails() {
		return worldCheckService.getGroups();
	}

	@RequestMapping(path = "/countrycodes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
	@ResponseBody
	public String getCountryCodes() {
		return worldCheckService.getCountryCodes();
	}

	@RequestMapping(path = "/result", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
	@ResponseBody
	public String getScreeningResult() {
		return worldCheckService.getScreeningResult();
	}

	@RequestMapping(path = "/savecase", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
	@ResponseBody
	public String saveCase() {
		return worldCheckService.saveCase();
	}

	/*@RequestMapping(path = "/wcresult", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
	@ResponseBody
	public String getWCResult() {
		return worldCheckService.getWCResult();
	}*/

	@RequestMapping(path = "/postCall", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
	public @ResponseBody String getWCResultByCustomer(@RequestBody CustomerRequest customer) {
		String result="";
		try {
		    result= worldCheckService.getWCResultByCustomer(customer);
		} catch (ParseException e) {
			result=e.getMessage();
		}
		return result;
	}

}
