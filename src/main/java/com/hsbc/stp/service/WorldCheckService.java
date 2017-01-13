package com.hsbc.stp.service;

import java.text.ParseException;

import com.hsbc.stp.domain.CustomerRequest;

public interface WorldCheckService {

	String getGroups();
	
	String getCountryCodes();
	
	String getScreeningResult();
	
	String saveCase();
	
	//String getWCResult();
	
	String getWCResultByCustomer(CustomerRequest customer) throws ParseException;
}
