package com.hsbc.stp.domain;

import java.util.Date;
import java.util.List;

public class WCScreening {

	private String arn;
	
	private int customerId;
	
	private Date screeningDate;
	
	private String wcStatus;
	
	private List<WCHits> wchits;

	public List<WCHits> getWchits() {
		return wchits;
	}

	public void setWchits(List<WCHits> wchits) {
		this.wchits = wchits;
	}

	public String getArn() {
		return arn;
	}

	public void setArn(String arn) {
		this.arn = arn;
	}

	public int getCustomerId() {
		return customerId;
	}

	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}

	public Date getScreeningDate() {
		return screeningDate;
	}

	public void setScreeningDate(Date screeningDate) {
		this.screeningDate = screeningDate;
	}

	public String getWcStatus() {
		return wcStatus;
	}

	public void setWcStatus(String wcStatus) {
		this.wcStatus = wcStatus;
	}
	
	/*private Date createdDate;
	
	private String createdBy;*/
}
