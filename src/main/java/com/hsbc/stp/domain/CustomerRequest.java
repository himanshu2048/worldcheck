package com.hsbc.stp.domain;

import java.io.Serializable;

public class CustomerRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3839765913234591698L;

	private String Name;
	
	private String dob;
	
	private String nationality;
	
	private String customerId;
	
	/*public CustomerRequest(String Name,String dob, String nationality, String customerId )
	{
		this.Name=Name;
		this.dob=dob;
		this.nationality=nationality;
		this.customerId=customerId;
	}*/

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	public String getDob() {
		return dob;
	}

	public void setDob(String dob) {
		this.dob = dob;
	}

	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
}
