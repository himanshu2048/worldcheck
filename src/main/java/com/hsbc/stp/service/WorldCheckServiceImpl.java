package com.hsbc.stp.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.hsbc.stp.domain.CustomerRequest;
import com.hsbc.stp.domain.DateTimeValue;
import com.hsbc.stp.domain.DobField;
import com.hsbc.stp.domain.Field;
import com.hsbc.stp.domain.NationalityField;
import com.hsbc.stp.domain.WCHits;
import com.hsbc.stp.domain.WCScreening;
import com.hsbc.stp.domain.WcCustomer;
import com.hsbc.stp.utility.STPConfig;
import com.hsbc.stp.utility.WCConstant;

@Transactional(propagation = Propagation.REQUIRED)
@Service("worldCheckService")
public class WorldCheckServiceImpl implements WorldCheckService {

	@Autowired
	private STPConfig config;

	private JSONParser parser = new JSONParser();

	private List<String> matchStrengthList = new ArrayList<>();

	private SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");

	private String wcStatus = "Clean";

	WorldCheckServiceImpl() {
		matchStrengthList.add("EXACT");
		matchStrengthList.add("STRONG");
		matchStrengthList.add("MEDIUM");
	}

	@Override
	public String getWCResultByCustomer(CustomerRequest customer) throws ParseException {

		WCScreening wcScreening = new WCScreening();
		wcScreening.setArn(customer.getArn());
		wcScreening.setCustomerId(customer.getCustomerId());
		wcScreening.setScreeningDate(new Date());
		SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd");
		Date dob = format.parse(customer.getDob());
		Calendar c = Calendar.getInstance();
		c.setTime(dob);
		int year = c.get(Calendar.YEAR);
		int yearLowerRange = year - 3;
		int yearUpperrange = year + 3;
		String natinality = customer.getNationality();
		// String saveResult = saveCaseforCustomer(customer);
		WcCustomer wcCustomer = new WcCustomer();
		wcCustomer.setCaseId(customer.getCustomerId());
		wcCustomer.setName(customer.getName());
		wcCustomer.setEntityType("INDIVIDUAL");
		wcCustomer.setGroupId("ba7f147f-cfbb-49c7-9e42-7ee9e5df62a8");
		wcCustomer.setProviderTypes(new String[] { "WATCHLIST" });
		wcCustomer.setCustomFields(new Field[] {});
		DobField dobField = new DobField();
		dobField.setTypeId("SFCT_2");
		dobField.setDateTimeValue(customer.getDob());
		NationalityField nationality = new NationalityField();
		nationality.setTypeId("SFCT_5");
		nationality.setValue("IND");
		ObjectMapper mapper = new ObjectMapper();
		wcCustomer.setSecondaryFields(new Field[] { dobField, nationality });
		String json = "";
		try {
			json = mapper.writeValueAsString(wcCustomer);
			System.out.println(json);

			String caseIdentiResult = worldCheckCall(WCConstant.CASE_REFERENCES, HttpMethod.GET, "",
					"?caseId=" + customer.getCustomerId());
			String saveResult = "";
			if (caseIdentiResult != null && caseIdentiResult.equalsIgnoreCase(WCConstant.NOT_FOUND))
				saveResult = worldCheckCall(WCConstant.CASE, HttpMethod.POST, json, "");
			else {
				JSONObject responseCase = (JSONObject) parser.parse(caseIdentiResult);
				String wcCaseid = (String) responseCase.get("caseSystemId");
				String caseid = (String) responseCase.get("caseId");
				saveResult = worldCheckCall(WCConstant.UPDATE_CASE + wcCaseid, HttpMethod.PUT, json, "");
			}

			JSONObject responseCase = (JSONObject) parser.parse(saveResult);
			String wcCaseid = (String) responseCase.get("caseSystemId");
			System.out.println("wcCaseid======>" + wcCaseid);
			String modificationDate = (String) responseCase.get("modificationDate");
			System.out.println(modificationDate);
			String url = String.format(WCConstant.SCREENCASE, wcCaseid);
			String result = worldCheckCall(url, HttpMethod.POST, "", "");// screening
			System.out.println(result);

			String wcresult=getScreeningResult(wcCaseid);
			JSONArray jsonArray = (JSONArray) parser.parse(wcresult);
			@SuppressWarnings("unchecked")
			Stream<JSONObject> stream = jsonArray.parallelStream();
			List<WCHits> hits = new ArrayList<>();

			stream.forEach(p -> wcDiscountLogic(p, hits, yearLowerRange, yearUpperrange, natinality, modificationDate));
			wcScreening.setWcStatus(wcStatus);
			wcScreening.setWchits(hits);
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
			String arrayToJson = objectMapper.writeValueAsString(wcScreening);
			return arrayToJson;

		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			if(e.getMessage().equalsIgnoreCase("Time Out"))
			{
				return e.getMessage();
			}
		}
		return "";
	}

	private String getScreeningResult(String wcCaseid) throws Exception {
		try {
			Thread.sleep(3000);
			String auditevent = audtiEvent(wcCaseid);
			JSONObject jsonObject = (JSONObject) parser.parse(auditevent);
			JSONArray jsonArray=(JSONArray) jsonObject.get("results");
			if(!jsonArray.isEmpty())
			{
				JSONObject resultEvent=(JSONObject) jsonArray.get(0);
				JSONObject resultDetails=(JSONObject) resultEvent.get("details");
				String status=(String)resultDetails.get("statusCode");
				System.out.println(status);
				if(status.equalsIgnoreCase("COMPLETED"))
				{
					String urlCaseResult = String.format(WCConstant.CASE_RESULT, wcCaseid);
					String wcresult = worldCheckCall(urlCaseResult, HttpMethod.GET, "", "");
					System.out.println(wcresult);
					return wcresult;
				}
			}
			else
			{
				Thread.sleep(30000);
				String auditeventNext = audtiEvent(wcCaseid);
				JSONObject jsonObjectNext = (JSONObject) parser.parse(auditeventNext);
				JSONArray jsonArrayNext=(JSONArray) jsonObjectNext.get("results");
				if(!jsonArrayNext.isEmpty())
				{
					JSONObject resultEventNext=(JSONObject) jsonArrayNext.get(0);
					JSONObject resultDetailsNext=(JSONObject) resultEventNext.get("details");
					String statusNext=(String)resultDetailsNext.get("statusCode");
					System.out.println(statusNext);
					if(statusNext.equalsIgnoreCase("COMPLETED"))
					{
						String urlCaseResult = String.format(WCConstant.CASE_RESULT, wcCaseid);
						String wcresult = worldCheckCall(urlCaseResult, HttpMethod.GET, "", "");
						System.out.println(wcresult);
						return wcresult;
					}
					else
					{
						System.out.println("Time Out");
						throw new Exception("Time Out");
					}
				}
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}

	private void wcDiscountLogic(final JSONObject record, List<WCHits> hits, int lowerRange, int upperRange,
			String natinality, String modificationDate) {

		if (!matchStrengthList.contains(record.get("matchStrength").toString()) && (record.get("modificationDate"))
				.toString().equalsIgnoreCase((record.get("creationDate")).toString())) {
			return;
		} else {
			String riskStatus = "Clean";
			String reason = "";
			WCHits hit = new WCHits();
			hit.setName((String) record.get("matchedTerm"));
			hit.setMatchStrength((String) record.get("matchStrength"));
			int yearValue = 0;
			JSONArray secondaryFieldResults = (JSONArray) record.get("secondaryFieldResults");
			for (int i = 0; i < secondaryFieldResults.size(); i++) {
				JSONObject obj = (JSONObject) secondaryFieldResults.get(i);
				if (obj.get("typeId").toString().equalsIgnoreCase("SFCT_5")) {
					Object nationality = obj.get("matchedValue");
					String nationalityString = (nationality != null) ? (String) nationality : "null";
					hit.setNationality(nationalityString);
				} else {
					Object dob = obj.get("matchedDateTimeValue");
					yearValue = (dob != null) ? getYearEachHit((String) dob) : 0;
					String risk = (dob != null) ? (String) dob : "";
					hit.setDateOfBirth(risk);
				}

			}
			if (hit.getMatchStrength().equalsIgnoreCase("EXACT")) {
				riskStatus = "Referred";
				reason = "match found";
				wcStatus = "Referred";
			} else if ((hit.getNationality().equalsIgnoreCase("null")
					|| hit.getNationality().equalsIgnoreCase(natinality))
					&& (yearValue == 0 || (yearValue > lowerRange && yearValue < upperRange))) {
				riskStatus = "Referred";
				reason = "match found";
				wcStatus = "Referred";
			}
			hit.setRiskStatus(riskStatus);
			hit.setReason(reason);
			hits.add(hit);
			return;
		}
	}

	private synchronized int getYearEachHit(String date) {
		Date d;
		int yearvalue = 0;
		try {
			d = yearFormat.parse(date);
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			yearvalue = c.get(Calendar.YEAR);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return yearvalue;
	}

	@Override
	public String getGroups() {
		return worldCheckCall(WCConstant.GROUP_URL, HttpMethod.GET, "", "");
	}

	@Override
	public String getCountryCodes() {
		return worldCheckCall(WCConstant.COUNTRY_URL, HttpMethod.GET, "", "");
	}

	private String worldCheckCall(String methodUrl, HttpMethod method, String body, String parameter) {

		String getUri = WCConstant.HTTP_SCHEMA + config.getWorldcheckUrl() + WCConstant.PATH_SEPARATOR
				+ WCConstant.BASE_PATH + WCConstant.PATH_SEPARATOR + methodUrl + parameter;
		// System.out.println(getUri);
		HttpHeaders headers = new HttpHeaders();
		headers.set("Host", config.getWorldcheckUrl());
		headers.set("Port", "443");
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.setCacheControl("no-cache");
		// headers.setExpires(600000);
		headers.set("Connection", "keep-alive");
		headers.set("Accept-Encoding", "gzip, deflate, sdch, br");
		headers.set("Accept-Language", "en-GB,en-US;q=0.8,en;q=0.6");

		String date = toGMTformat();
		String authorisation = "Signature keyId=\"" + "e82624e3-1574-4dfb-bb85-0506086cc25a"
				+ "\",algorithm=\"hmac-sha256\",headers=\"(request-target) host date";
		String dataToSign = "(request-target): " + method.name().toLowerCase() + " " + WCConstant.PATH_SEPARATOR
				+ WCConstant.BASE_PATH + WCConstant.PATH_SEPARATOR + methodUrl + "\n" + "host: "
				+ "rms-world-check-one-api-pilot.thomsonreuters.com" + "\n" + "date: " + date;
		if (!body.isEmpty()) {
			dataToSign = dataToSign + "\n" + "content-type: application/json" + "\n" + "content-length: "
					+ body.length() + "\n" + body;
			authorisation = authorisation + " content-type content-length";
			headers.set("Content-Length", String.valueOf(body.length()));
			headers.set("content-type", "application/json");
		}
		String hmac = generateAuthHeader(dataToSign);

		authorisation = authorisation + "\",signature=\"" + hmac + "\"";
		// System.out.println(authorisation);
		headers.set("Date", date);
		headers.set("Authorization", authorisation);
		HttpEntity<String> entity = new HttpEntity<String>(body, headers);
		ResponseEntity<String> result = null;
		RestTemplate restTemplate = new RestTemplate();
		try {
			result = restTemplate.exchange(getUri, method, entity, String.class);
			System.out.println(method + "===========>" + result.getStatusCode() + " : " + result.getStatusCodeValue());
		} catch (HttpClientErrorException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				return WCConstant.NOT_FOUND;
			}
			throw ex;
		} catch (RestClientException e) {
			throw e;
		}
		return result.getBody();
	}

	private static String generateAuthHeader(String dataToSign) {

		String hash = "";
		try {
			String secret = "5lzi6VL8chvVOwJDVRWU9VKe3YrAi44uiJRg0IFG2tuN5D/7B6eZ/NagYiH3XuZ8fV6mLlHarVth4WhdhWuKkA==";
			String message = dataToSign;

			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
			sha256_HMAC.init(secret_key);

			hash = Base64.encodeBase64String(sha256_HMAC.doFinal(message.getBytes()));
			// System.out.println(hash);
		} catch (Exception e) {
			System.out.println("Error");
		}
		return hash;
	}

	private static String toGMTformat() {
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
		sdf.applyPattern("dd MMM yyyy HH:mm:ss z");
		Date testDate = new Date();
		/*
		 * Calendar cal = Calendar.getInstance(); cal.setTime(testDate);
		 * cal.add(Calendar.MINUTE, 1); return sdf.format(cal.getTime());
		 */
		String s = sdf.format(testDate);
		// System.out.println(s);
		return s;
	}

	@Override
	public String caseIdentifiers(String customerId) {
		String url = WCConstant.CASE_IDENTIFIERS;
		return worldCheckCall(url, HttpMethod.HEAD, "", "?caseId=" + customerId);
	}

	@Override
	public String caseReferences(String customerId) {
		String url = WCConstant.CASE_REFERENCES;
		return worldCheckCall(url, HttpMethod.GET, "", "?caseId=" + customerId);
	}

	@Override
	public String audtiEvent(String caseSystemId) {
		String body = "{\"query\" : \"actionType==SCREENED_CASE;" + "actionedByUserId=="
				+ "d15f4c7a-018b-4aa8-b65d-3d0e0b33fa78;"
				+ "eventDate>2010-01-01T00:00:00Z;eventDate<2020-01-01T00:00:00Z\" }";

		String url = WCConstant.AUDIT_EVENT + caseSystemId + "/auditEvents";
		String auditEventResult= worldCheckCall(url, HttpMethod.POST, body, "");
		System.out.println("auditEventResult  ==>"+auditEventResult);
		return auditEventResult;
	}
}
