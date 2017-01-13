package com.hsbc.stp.domain;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class TestWcCustomer {
	private static SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");

	/*public static void main(String[] args) throws ParseException, FileNotFoundException, IOException, org.json.simple.parser.ParseException {
		
		JSONParser parser = new JSONParser();
		JSONArray jsonArray = (JSONArray) parser.parse(new FileReader("c:\\jitendra\\test.json"));
		@SuppressWarnings("unchecked")
		Stream<JSONObject> stream = jsonArray.parallelStream();
		List<WCHits> hits = new ArrayList<>();
		// stream.filter(hit
		// ->matchStrengthList.contains(hit.get("matchStrength").toString())).forEach(p
		// -> wcDiscountLogic(p,hits));
		stream.forEach(p -> wcDiscountLogic(p, hits));
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		String arrayToJson = objectMapper.writeValueAsString(hits);
		System.out.println(arrayToJson);

	}
	*/
	private static List<WCHits> wcDiscountLogic(final JSONObject record, List<WCHits> hits) {

		WCHits hit = new WCHits();
		String nationalityString="";
		hit.setName((String) record.get("matchedTerm"));
		hit.setMatchStrength((String) record.get("matchStrength"));

		JSONArray secondaryFieldResults = (JSONArray) record.get("secondaryFieldResults");
		for (int i = 0; i < secondaryFieldResults.size(); i++) {
			JSONObject obj = (JSONObject) secondaryFieldResults.get(i);
			if (obj.get("typeId").toString().equalsIgnoreCase("SFCT_5")) {
				Object nationality = obj.get("matchedDateTimeValue");
				nationalityString = (nationality != null) ? (String) nationality : "null";
				hit.setNationality(nationalityString);
			} else {				
				Object dob = obj.get("matchedDateTimeValue");			
				String s = (dob != null) ? getYearEachHit((String) dob) : "null";			
				String risk = (dob != null) ? (String) dob : "null";							
				//Object value = dobfield.get("dateTimeValue");
				//String reason = (dob != null) ? (String) value : "null";
				//System.out.println(Thread.currentThread() + "==========>" + risk + "=====>" + s);
				hit.setDateOfBirth(s);
				hit.setRiskStatus(risk);
				Object id=record.get("referenceId");
				String idString= (id != null) ? (String) id : "null";
				hit.setReason(idString);
			}
            
			
		}

		
		hits.add(hit);
		return hits;
	}

	private synchronized static String getYearEachHit(String date) {
		Date d;
		String year = "";
		try {
			d = yearFormat.parse(date);
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			int yearvalue = c.get(Calendar.YEAR);
			year = String.valueOf(yearvalue);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return year;
	}

}
