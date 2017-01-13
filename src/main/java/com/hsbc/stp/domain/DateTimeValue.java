package com.hsbc.stp.domain;

public class DateTimeValue {

	private String timelinePrecision;
	
	private String pointInTimePrecision;
	
	private String utcDateTime;
	
	private String timeZone;

	public String getTimelinePrecision() {
		return timelinePrecision;
	}

	public void setTimelinePrecision(String timelinePrecision) {
		this.timelinePrecision = timelinePrecision;
	}

	public String getPointInTimePrecision() {
		return pointInTimePrecision;
	}

	public void setPointInTimePrecision(String pointInTimePrecision) {
		this.pointInTimePrecision = pointInTimePrecision;
	}

	public String getUtcDateTime() {
		return utcDateTime;
	}

	public void setUtcDateTime(String utcDateTime) {
		this.utcDateTime = utcDateTime;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}
}
