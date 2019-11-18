package com.tarento.analytics.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {
	/**
     * Allowed Origins for CORS Bean
     */
    public static final String GET = "GET";
    public static final String POST = "POST"; 
    public static final String PUT = "PUT"; 
    public static final String DELETE = "DELETE";
    public static final String OPTIONS = "OPTIONS"; 
    

	public static int UNAUTHORIZED_ID = 401;
	public static int SUCCESS_ID = 200;
	public static int FAILURE_ID = 320;
	public static String UNAUTHORIZED = "Invalid credentials. Please try again.";
	public static String PROCESS_FAIL = "Process failed, Please try again.";
	public static String SUCCESS= "success";
	
	
	//chart format
	
	public static final String D3 = "d3";
	public static final String CHARTJS = "chartjs";
	
	//chart type
	public static final String BAR = "bar";
	public static final String PIE ="pie";
	public static final String STACKEDBAR ="stackedbar";
	public static final String LINE ="line";
	public static final String HORIZONTAL_BAR="horizontalBar";
	public static final String DOUGHNUT="doughnut";
	public static final String Heat = "heat";
	public static final String RADAR ="radar";
	
	public static final Long FEEDBACK_MESSAGE_TIMEOUT = 2000l;
	
	public static final String STORE_ID = "storeId";
	
	public static final String PLACEMENTS_DASHBOARD = "DASHBOARD"; 
	public static final String PLACEMENTS_HOME = "HOME"; 
	
	public static final List<Long> RATING_LIST = new ArrayList<>(Arrays.asList(1l,2l,3l,4l,5l));
	public static final List<String> RATING_LIST_STRING = new ArrayList<>(Arrays.asList("1","2","3","4","5"));
	public static final List<String> RATING_LIST_STRING_STAR = new ArrayList<>(Arrays.asList("1 Star","2 Star","3 Star","4 Star","5 Star"));
	
	public interface Modules { 
		final static String HOME_REVENUE = "HOME_REVENUE";
		final static String HOME_SERVICES = "HOME_SERVICES";
		final static String COMMON = "COMMON"; 
		final static String PT = "PT"; 
		final static String TL = "TL"; 
	}

	public interface KafkaTopics { 
		final static String NEW_CONTENT_MESSAGE = "SaveContent";
		final static String SIMULATOR_TRANSACTION = "SaveTransaction"; 
	}
	
	public interface ConfigurationFiles { 
		final static String CHART_API_CONFIG = "ChartApiConfig.json"; 
	}
	
	public interface JsonPaths { 
		final static String CHART_TYPE = "chartType"; 
		final static String QUERIES = "queries"; 
		final static String AGGREGATION_QUERY= "aggrQuery";
		final static String INDEX_NAME = "indexName";
		final static String REQUEST_QUERY_MAP = "requestQueryMap"; 
		final static String DATE_REF_FIELD = "dateRefField"; 
		final static String AGGS = "aggs";
		final static String AGGREGATIONS = "aggregations" ;
		final static String MODULE = "module"; 
	}
	
	public interface Filters { 
		final static String MODULE = "module"; 
		final static String FILTER_ALL = "*"; 
	}

	public interface Catagory {
		final static String SEVICE = "service";
		final static String REVENUE = "revenue";
	}


}
