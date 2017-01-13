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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

	WorldCheckServiceImpl() {
		matchStrengthList.add("EXACT");
		matchStrengthList.add("STRONG");
		matchStrengthList.add("MEDIUM");
	}

	@Override
	public String getWCResultByCustomer(CustomerRequest customer) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd");
		Date dob = format.parse(customer.getDob());
		Calendar c = Calendar.getInstance();
		c.setTime(dob);
		int year = c.get(Calendar.YEAR);
		int yearLowerRange = year - 3;
		int yearUpperrange = year + 3;
		String natinality = customer.getNationality();
		String saveResult = saveCaseforCustomer(customer);
		try {
			JSONObject responseCase = (JSONObject) parser.parse(saveResult);
			String wcCaseid = (String) responseCase.get("caseSystemId");
			String json = "";
			String url = String.format(WCConstant.SCREENCASE, wcCaseid);
			ResponseEntity<String> result = worldCheckCall(url, HttpMethod.POST, json);// screening
			System.out.println(result.getBody());
			System.out.println(result.getStatusCodeValue());

			Thread.sleep(3000);

			String urlCaseResult = String.format(WCConstant.CASE_RESULT, wcCaseid);
			System.out.println(url);
			ResponseEntity<String> wcresult = worldCheckCall(urlCaseResult, HttpMethod.GET, "parameters");
			System.out.println(wcresult.getBody());
			JSONArray jsonArray = (JSONArray) parser.parse(wcresult.getBody());
			@SuppressWarnings("unchecked")
			Stream<JSONObject> stream = jsonArray.parallelStream();
			List<WCHits> hits = new ArrayList<>();
			//stream.filter(hit -> matchStrengthList.contains(hit.get("matchStrength").toString()))
					//.forEach(p -> wcDiscountLogic(p, hits, yearLowerRange, yearUpperrange, natinality));
			stream.forEach(p -> wcDiscountLogic(p, hits, yearLowerRange, yearUpperrange, natinality));
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
			String arrayToJson = objectMapper.writeValueAsString(hits);
			return arrayToJson;

		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	private String saveCaseforCustomer(CustomerRequest customerRequest) {
		WcCustomer customer = new WcCustomer();
		customer.setCaseId(customerRequest.getCustomerId());
		customer.setName(customerRequest.getName());
		customer.setEntityType("INDIVIDUAL");
		customer.setGroupId("ba7f147f-cfbb-49c7-9e42-7ee9e5df62a8");
		customer.setProviderTypes(new String[] { "WATCHLIST" });
		customer.setCustomFields(new Field[] {});
		DobField dob = new DobField();
		dob.setTypeId("SFCT_2");
		dob.setDateTimeValue(customerRequest.getDob());
		NationalityField nationality = new NationalityField();
		nationality.setTypeId("SFCT_5");
		nationality.setValue("IND");
		ObjectMapper mapper = new ObjectMapper();
		customer.setSecondaryFields(new Field[] { dob, nationality });
		String json = "";
		try {
			json = mapper.writeValueAsString(customer);
			System.out.println(json);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		ResponseEntity<String> result = worldCheckCall(WCConstant.CASE, HttpMethod.POST, json);
		System.out.println();
		return result.getBody();

	}

	/*
	 * @Override public String getWCResult() { String saveresult = saveCase();
	 * try { JSONObject responseCase = (JSONObject) parser.parse(saveresult);
	 * String wcCaseid = (String) responseCase.get("caseSystemId"); String json
	 * = ""; String url = String.format(WCConstant.SCREENCASE, wcCaseid);
	 * ResponseEntity<String> result = worldCheckCall(url, HttpMethod.POST,
	 * json);// screening System.out.println(result.getBody());
	 * System.out.println(result.getStatusCodeValue());
	 * 
	 * Thread.sleep(4000);
	 * 
	 * String urlCaseResult = String.format(WCConstant.CASE_RESULT, wcCaseid);
	 * System.out.println(url); ResponseEntity<String> wcresult =
	 * worldCheckCall(urlCaseResult, HttpMethod.GET, "parameters");
	 * System.out.println(wcresult.getBody()); JSONArray jsonArray = (JSONArray)
	 * parser.parse(wcresult.getBody());
	 * 
	 * @SuppressWarnings("unchecked") Stream<JSONObject> stream =
	 * jsonArray.parallelStream(); List<WCHits> hits = new ArrayList<>(); //
	 * stream.filter(hit //
	 * ->matchStrengthList.contains(hit.get("matchStrength").toString())).
	 * forEach(p // -> wcDiscountLogic(p,hits)); stream.forEach(p ->
	 * wcDiscountLogic(p, hits)); ObjectMapper objectMapper = new
	 * ObjectMapper(); objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
	 * String arrayToJson = objectMapper.writeValueAsString(hits); return
	 * arrayToJson;
	 * 
	 * } catch (org.json.simple.parser.ParseException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } catch
	 * (InterruptedException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } catch (JsonProcessingException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } return saveresult; }
	 */

	private void wcDiscountLogic(final JSONObject record, List<WCHits> hits, int lowerRange, int upperRange,
			String natinality) {

		if(!matchStrengthList.contains(record.get("matchStrength").toString()))
				return ;
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
				Object nationality = obj.get("matchedDateTimeValue");
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
			riskStatus = "reffred";
			reason = "match found";
		} else if ((hit.getNationality().equalsIgnoreCase("null") || hit.getNationality().equalsIgnoreCase(natinality))
				&& (yearValue == 0 || (yearValue > lowerRange && yearValue < upperRange))) {
			riskStatus = "reffred";
			reason = "match found";
		}
		hit.setRiskStatus(riskStatus);
		hit.setReason(reason);
		hits.add(hit);
		return;
	}

	private synchronized int getYearEachHit(String date) {
		Date d;
		int yearvalue = 0;
		try {
			d = yearFormat.parse(date);
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			yearvalue = c.get(Calendar.YEAR);
			// year = String.valueOf(yearvalue);
			// System.out.println(date +" ===>" + year);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return yearvalue;
	}

	@Override
	public String getGroups() {

		ResponseEntity<String> result = worldCheckCall(WCConstant.GROUP_URL, HttpMethod.GET, "parameters");
		System.out.println(result.getBody());
		return result.getBody();
	}

	@Override
	public String getCountryCodes() {

		ResponseEntity<String> result = worldCheckCall(WCConstant.COUNTRY_URL, HttpMethod.GET, "parameters");
		System.out.println(result.getBody());
		return result.getBody();
	}

	@Override
	public String getScreeningResult() {
		String caseSystemId = "4d964e9f-b125-49b5-ab2c-72e575f9727c";
		String url = String.format(WCConstant.CASE_RESULT, caseSystemId);
		System.out.println(url);
		ResponseEntity<String> result = worldCheckCall(url, HttpMethod.GET, "parameters");
		System.out.println(result.getBody());
		return result.getBody();
	}

	@Override
	public String saveCase() {
		WcCustomer customer = new WcCustomer();
		customer.setCaseId("12345");
		customer.setName("Jitendra Mishra");
		customer.setEntityType("INDIVIDUAL");
		customer.setGroupId("ba7f147f-cfbb-49c7-9e42-7ee9e5df62a8");
		customer.setProviderTypes(new String[] { "WATCHLIST" });
		customer.setCustomFields(new Field[] {});
		DobField dob = new DobField();
		dob.setTypeId("SFCT_2");
		DateTimeValue dateTimeValue = new DateTimeValue();
		dateTimeValue.setTimelinePrecision("ON");
		dateTimeValue.setPointInTimePrecision("DAY");

		NationalityField nationality = new NationalityField();
		nationality.setTypeId("SFCT_5");
		nationality.setValue("IND");
		String json = "";
		ObjectMapper mapper = new ObjectMapper();
		try {
			/*
			 * SimpleDateFormat sdf = new SimpleDateFormat();
			 * sdf.applyPattern("dd MMM yyyy HH:mm:ss z"); sdf.setTimeZone(new
			 * SimpleTimeZone(0, "GMT"));
			 */
			String str = "1979-06-05";
			/*
			 * long epoch = (sdf.parse(str).getTime())/1000;
			 * System.out.println(epoch);
			 */
			dateTimeValue.setUtcDateTime(str);
			// dateTimeValue.setUtcDateTime("316310400000");
			dob.setDateTimeValue(str);
			customer.setSecondaryFields(new Field[] { dob, nationality });
			json = mapper.writeValueAsString(customer);
			System.out.println(json);
			System.out.println(json.length());
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponseEntity<String> result = worldCheckCall(WCConstant.CASE, HttpMethod.POST, json);
		System.out.println(result.getBody());
		return result.getBody();
	}

	private ResponseEntity<String> worldCheckCall(String methodUrl, HttpMethod method, String body) {

		String getUri = WCConstant.HTTP_SCHEMA + config.getWorldcheckUrl() + WCConstant.PATH_SEPARATOR
				+ WCConstant.BASE_PATH + WCConstant.PATH_SEPARATOR + methodUrl;
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
		if (methodUrl.equalsIgnoreCase(WCConstant.CASE)) {
			dataToSign = dataToSign + "\n" + "content-type: application/json" + "\n" + "content-length: "
					+ body.length() + "\n" + body;
			authorisation = authorisation + " content-type content-length";
			headers.set("Content-Length", String.valueOf(body.length()));
			headers.set("content-type", "application/json");
		}
		String hmac = generateAuthHeader(dataToSign);

		authorisation = authorisation + "\",signature=\"" + hmac + "\"";
		System.out.println(authorisation);
		headers.set("Date", date);
		headers.set("Authorization", authorisation);
		HttpEntity<String> entity = new HttpEntity<String>(body, headers);

		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.exchange(getUri, method, entity, String.class);
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
			System.out.println(hash);
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
		System.out.println(s);
		return s;
	}

}
