package com.hsbc.stp.domain;

public class WcCustomer {

	private String entityType;
	
	private String groupId;
	
	private String name;
	
	private Field[] secondaryFields;
	
	private Field[] customFields;
	
	private String[] providerTypes;
	
	private String caseId;

	public String getCaseId() {
		return caseId;
	}

	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Field[] getSecondaryFields() {
		return secondaryFields;
	}

	public void setSecondaryFields(Field[] secondaryFields) {
		this.secondaryFields = secondaryFields;
	}

	public Field[] getCustomFields() {
		return customFields;
	}

	public void setCustomFields(Field[] customFields) {
		this.customFields = customFields;
	}

	public String[] getProviderTypes() {
		return providerTypes;
	}

	public void setProviderTypes(String[] providerTypes) {
		this.providerTypes = providerTypes;
	}
	
}
