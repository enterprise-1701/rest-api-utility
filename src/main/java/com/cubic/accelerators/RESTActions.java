package com.cubic.accelerators;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.SourceType;
import org.jsonschema2pojo.rules.RuleFactory;
import org.w3c.dom.Document;

import com.cubic.genericutils.GenericConstants;
import com.cubic.genericutils.JsonUtil;
import com.cubic.genericutils.XmlUtil;
import com.cubic.logutils.Log4jUtil;
import com.cubic.reportengine.report.CustomReports;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.sun.codemodel.JCodeModel;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * This class contains the implementation for generic methods to work with rest Json and XML webservices. 
 */
public class RESTActions {
	private final Logger LOG = Logger.getLogger(this.getClass().getName());
	private CustomReports customReports = null;
	private String testCaseName = null;
	
	private String isSSLCertificationVerificationValue  = GenericConstants.GENERIC_FW_CONFIG_PROPERTIES.get("isSSLCertificationVerifcationEnabled");
	
	
	/**
	 * Constructor (creates the RESTActions instance)
	 * 
	 * @param customReports reference variable is declared with in the class  
	 * @param testCaseName reference variable is declared with in the class(testCaseName should be in the format &lt;&lt;TESTCASE_ID&gt;&gt; : &lt;&lt;TESTCASE DESCRIPTION&gt;&gt;)
	 */
	public RESTActions(CustomReports customReports, String testCaseName) {
		this.customReports = customReports;
		this.testCaseName = testCaseName;
	}

	/**
	 * This method should be used with the test frameworks which doesn't extend the TestEngineRest
	 * Call to this method should be the last line in the test method(i.e. in @Test) and should be in finally block
	 * 
	 * @param restActions pass the restActions instance to perform destruction of restActions instance
	 */
	public static void flush(RESTActions restActions) {
		if (restActions != null) {
			restActions = null;
		}
	}

	/**
	 * For adding the success step to the detailed report.
	 * - This method will NOT add the link to verify the jsonResponse, when the script pass.
	 * 
	 * @param stepName writes the step name to the custom report
	 * @param description write the description to the custom report
	 */
	public void successReport(String stepName, String description) {
		if (customReports != null) {
			customReports.successReport(stepName, description, testCaseName);
		}
	}

	/**
	 * <pre>
	 * For adding the failure step to the detailed report.
	 *   - This method will NOT add the link to verify the jsonResponse, when the script fails.
	 * </pre>
	 * @param stepName writes the step name to the custom report
	 * @param description write the description to the custom report
	 */
	public void failureReport(String stepName, String description) {
		if (customReports != null) {
			customReports.failureReport(stepName, description, testCaseName);
		}
	}
	
	/**
	 * For adding the failure step to the detailed report.<br>
	 * <pre>  
	 *   - This method will add the link(i.e. response as .txt) to verify the response, when the script fails.
	 *   - use failureReportForJsonWebService() or failureReportForXmlWebService(), since this method is deprecated.   
	 * </pre>
	 * @param stepName writes the step name to the custom report
	 * @param description write the description to the custom report
	 * @param response reponse need to be supplied, this will be added as link to customReport for reference when script fails.
	 * @throws IOException
	 */
	@Deprecated
	public void failureReportWebService(String stepName, String description, String response) throws IOException {
		LOG.warn("Use failureReportForJsonWebService() or failureReportForXmlWebService(), since this method is deprecated."); 
		if (customReports != null) {
			customReports.failureReportWebService(stepName, description, response,
					testCaseName);
		}
	}
	
	/**
	 * <pre>
	 * For adding the JSON failure step to the detailed report.
	 *  - This method will add the link to verify the jsonResponse(i.e. response as .json), when the script fails.
	 * </pre>
	 * @param stepName writes the step name to the custom report
	 * @param description write the description to the custom report
	 * @param response jsonReponse need to be supplied, this will be added as link to customReport for reference when script fails.
	 */
	public void failureReportForJsonWebService(String stepName, String description, String response) throws IOException {
		if (customReports != null) {
			customReports.failureReportForJsonWebService(stepName, description, response,
					testCaseName);
		}
	}

	/**
	 * <pre>
	 * For adding the XML failure step to the detailed report.
	 * - This method will add the link(i.e. response as .xml) to verify the response(i.e. response as .xml), when the script fails.
	 * </pre>
	 * @param stepName writes the step name to the custom report
	 * @param description write the description to the custom report
	 * @param response xmlReponse need to be supplied, this will be added as link to customReport for reference when script fails.
	 */
	public void failureReportForXmlWebService(String stepName, String description, String response) throws IOException {
		if (customReports != null) {
			customReports.failureReportForXmlWebService(stepName, description, response,
					testCaseName);
		}
	}
	
	/**
	 * <pre>
	 * For adding the success step to the detailed report.
	 * - This method will add the link(i.e. .txt) to verify the response, when the script Pass.
	 *  - use successReportForJsonWebService() or successReportForXmlWebService(), since this method is deprecated.
	 * </pre> 
	 * @param stepName writes the step name to the custom report
	 * @param description write the description to the custom report
	 * @param response reponse need to be supplied, this will be added as link to customReport for reference when script fails.
	 * @throws IOException
	 */
	@Deprecated
	public void successReportWebService(String stepName, String description, String response) throws IOException {
		LOG.warn("Use successReportForJsonWebService() or successReportForXmlWebService(), since this method is deprecated.");
		if (customReports != null) {
			customReports.successReportForWebService(stepName, description, response,
					testCaseName);
		}
	}	

	/**
	 * <pre>
	 * For adding the JSON success step to the detailed report.
	 * - This method will add the link(i.e. .json) to verify the response(i.e. .json), when the script Pass.
	 * </pre>
	 * @param stepName writes the step name to the custom report
	 * @param description write the description to the custom report
	 * @param response jsonReponse need to be supplied, this will be added as link to customReport for reference when script fails.
	 * @throws IOException
	 */
	public void successReportForJsonWebService(String stepName, String description, String response) throws IOException {
		if (customReports != null) {
			customReports.successReportForJsonWebService(stepName, description, response,
					testCaseName);
		}
	}	

	/**
	 * <pre>
	 * For adding the XML success step to the detailed report.
	 * - This method will add the link(i.e. .xml) to verify the response(i.e. .xml), when the script Pass.
	 * </pre>
	 * @param stepName writes the step name to the custom report
	 * @param description write the description to the custom report
	 * @param response xmlReponse need to be supplied, this will be added as link to customReport for reference when script fails.
	 * @throws IOException
	 */
	public void successReportForXmlWebService(String stepName, String description, String response) throws IOException {
		if (customReports != null) {
			customReports.successReportForXmlWebService(stepName, description, response,
					testCaseName);
		}
	}
	
	/**
	 * Generic to process the "GET" request
	 * 
	 * @param url End point url
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return ClientResponse : com.sun.jersey.api.client.ClientResponse
	 */
	public ClientResponse getClientResponse(String url, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) {
		ClientResponse clientResponse = null;
		try {
			LOG.info("Class name : " + getCallerClassName() + "Method name : " + getCallerMethodName());
			Client client = null;
			
		//	String isSSLCertificationVerifcationEnabled = "ON";
			if(isSSLCertificationVerificationValue==null){
				isSSLCertificationVerificationValue="ON";
			}
			
			if(isSSLCertificationVerificationValue.equalsIgnoreCase("off")){
				//Specific to SSL
				client = hostIgnoringClient();
			}else{
				//With out SSL
				client = Client.create(new DefaultClientConfig());
			}
			 WebResource resource = client.resource(url);
			//WebResource resource = Client.create(new DefaultClientConfig()).resource(url);

			// If url query parameters are present
			if ((urlQueryParameters != null) && (urlQueryParameters.keySet().toArray().length > 0)) {
				MultivaluedMap<String, String> queryParams = convertHashTableToMultivaluedMap(urlQueryParameters);
				resource = resource.queryParams(queryParams);
			}

			WebResource.Builder builder = resource.accept(contentType);

			// If request headers are present
			if ((requestHeaders != null) && (requestHeaders.keySet().toArray().length > 0)) {
				for (String key : requestHeaders.keySet()) {
					builder.header(key, requestHeaders.get(key));
				}
			}

			clientResponse = builder.get(ClientResponse.class);

			LOG.info("Response : "+clientResponse); 
		} catch (Throwable e) {
			LOG.fatal(Log4jUtil.getStackTrace(e));
			failureReport("Retrieving Client Response ",e.toString());
			throw new RuntimeException(e);
		}
		return clientResponse;
	}

	/**
	 * Generic to process the "GET" request
	 * 
	 * @param url End point url
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @param options java.util.Hashtable<br>                     
	 *                      is added for handling additional method level arguments.
	 * @return ClientResponse : com.sun.jersey.api.client.ClientResponse
	 */
	public ClientResponse getClientResponse(String url, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType, Hashtable<String, String> options) {
		ClientResponse clientResponse = null;
		try {
			LOG.info("Class name : " + getCallerClassName() + "Method name : " + getCallerMethodName());

			WebResource resource = Client.create(new DefaultClientConfig()).resource(url);

			// If url query parameters are present
			if ((urlQueryParameters != null) && (urlQueryParameters.keySet().toArray().length > 0)) {
				MultivaluedMap<String, String> queryParams = convertHashTableToMultivaluedMap(urlQueryParameters);
				resource = resource.queryParams(queryParams);
			}

			WebResource.Builder builder = resource.accept(contentType);

			// If request headers are present
			if ((requestHeaders != null) && (requestHeaders.keySet().toArray().length > 0)) {
				for (String key : requestHeaders.keySet()) {
					builder.header(key, requestHeaders.get(key));
				}
			}

			clientResponse = builder.get(ClientResponse.class);

			LOG.info("Response : "+clientResponse); 
		} catch (Throwable e) {
			
			//IF exception handling is disabled then exception will not be thrown and failure report is not added.
			//Below logic is added for handling the negative scenarios(i.e. test scripts).
			if(!isExceptionDisabled(options)){
				LOG.fatal(Log4jUtil.getStackTrace(e));
				failureReport("Retrieving Client Response ",e.toString());
				throw new RuntimeException(e);
			}
			
		}
		return clientResponse;
	}


	/**Verify ExceptionHandling is disabled or NOT 
	 * 
	 * @param options
	 * @return
	 */
	private boolean isExceptionDisabled(Hashtable<String, String> options){
		
		if(options!=null){
			String disableExceptions = options.get(RESTConstants.DISABLE_EXCEPTIONS);
			boolean isDisableExceptions =  (disableExceptions==null || disableExceptions.trim().length()<=0) ? (false) :
														(disableExceptions.trim().equalsIgnoreCase("true") ? true : false);
			return isDisableExceptions;
		}else{
			return false;
		}
		
	}
	

	/**<pre>
	 * Generic to process the "GET" request
	 *  - This method returns both response and response headers.
	 *  - response and response headers are present in java.util.Hashtable
	 *  - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return responseHeaderAndResponseBodyTable : java.util.Hashtable
	 */
	private Hashtable<String, String> getResponseHeadersAndBody(String url, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) {

		ClientResponse clientResponse = null;
		Hashtable<String, String> responseHeaderAndResponseBodyTable = null;

		try {
			clientResponse = getClientResponse(url, requestHeaders, urlQueryParameters, contentType);

			MultivaluedMap<String, String> multivaluedMap = clientResponse.getHeaders();

			// If multivaluedMap is not null and length greater than zero
			if ((multivaluedMap != null) && (multivaluedMap.keySet().toArray().length > 0)) {
				responseHeaderAndResponseBodyTable = new Hashtable<String, String>();
				for (String key : multivaluedMap.keySet()) {
					responseHeaderAndResponseBodyTable.put(key, multivaluedMap.get(key).get(0));
				}

				String strResponse = null;
				try{
					responseHeaderAndResponseBodyTable.put(RESTConstants.RESPONSE_CODE, ""+clientResponse.getStatus());
					strResponse = clientResponse.getEntity(String.class);
				}catch (Exception e) {
					strResponse = "";
				}
				
				responseHeaderAndResponseBodyTable.put(RESTConstants.RESPONSE_BODY, strResponse);
			}

		} catch (Throwable e) {
			LOG.info(e);
			throw new RuntimeException(e);
		}

		return responseHeaderAndResponseBodyTable;
	}
	
	/**
	 * Returns Text response as String for Text 'GET' method
	 * 
	 * @param url : End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable
	 */
	public String getTextResponseAsString(String url, Hashtable<String, String> headerParameters,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String responseString = getResponseAsString(url, headerParameters, urlQueryParameters, RESTConstants.APPLICATION_TEXT);
		return responseString;
	}
	
	
	/**
	 * Returns Text/xml response as String for Text 'GET' method
	 * 
	 * @param url : End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable
	 */
	public String getTextXMLResponseAsString(String url, Hashtable<String, String> headerParameters,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String responseString = getResponseAsString(url, headerParameters, urlQueryParameters, RESTConstants.TEXT_XML);
		return responseString;
	}
	
	// Only for internal use with in the class
	/**<pre>
	 * Generic to process the "GET" request and return the response(i.e. Json/XML) in string 
	 * - Only for internal use with in the class
	 * </pre>
	 * @param url End point url
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return responseString java.lang.String
	 */
	private String getResponseAsString(String url, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) {
		ClientResponse clientResponse = null;
		String responseString = null;
		try {
			clientResponse = getClientResponse(url, requestHeaders, urlQueryParameters, contentType);
			try{
				responseString = clientResponse.getEntity(String.class);
			}catch (Exception e) {
				responseString = "";
			}
			
		} catch (Throwable e) {
			LOG.fatal(Log4jUtil.getStackTrace(e));
			failureReport("Retrieving Client Response ",e.toString());
			throw new RuntimeException(e);
		}
		return responseString;
	}

	// TODO need to add the method getJSONResponseAsJsonObject
	/**
	 * Returns JSON response as String for Json 'GET' method
	 * 
	 * @param url : End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable
	 */
	public String getJSONResponseAsString(String url, Hashtable<String, String> headerParameters,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String responseString = getResponseAsString(url, headerParameters, urlQueryParameters, RESTConstants.APPLICATION_JSON);
		return responseString;
	}

	// TODO need to add the method getXMLResponseAsDocumentObject
	/**
	 * Returns XML response as String, for XML 'GET' method
	 * 
	 * @param url End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 */
	public String getXMLResponseAsString(String url, Hashtable<String, String> headerParameters,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String responseString = getResponseAsString(url, headerParameters, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return responseString;
	}

	/**
	 * Returns XML response as Document, for XML 'GET' method
	 * 
	 * @param url End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return org.w3c.dom.Document Response in Document object
	 */
	public Document getXMLResponseAsDocument(String url, Hashtable<String, String> headerParameters,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String responseString = getResponseAsString(url, headerParameters, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return XmlUtil.getXMLDocumentObject(responseString);
	}
	
	/**
	 * <pre>
	 * Returns both JSON response and response header, for Json 'GET' method
	 * - Json response and response headers are present in java.util.Hashtable
	 * - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 */
	public Hashtable<String, String> getJSONResponseHeadersAndBody(String url,
			Hashtable<String, String> headerParameters, Hashtable<String, String> urlQueryParameters) throws Throwable {
		Hashtable<String, String> responseHeaderAndResponseBodyTable = getResponseHeadersAndBody(url, headerParameters,
				urlQueryParameters, RESTConstants.APPLICATION_JSON);
		return responseHeaderAndResponseBodyTable;
	}

	/**
	 * <pre>
	 * Returns both XML response and response header, for XML 'GET' method
	 * - XML response and response headers are present in java.util.Hashtable
	 * - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url : End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 */
	public Hashtable<String, String> getXMLResponseHeadersAndBody(String url,
			Hashtable<String, String> headerParameters, 
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		Hashtable<String, String> responseHeaderAndResponseBodyTable = getResponseHeadersAndBody(url, headerParameters,
				urlQueryParameters, RESTConstants.APPLICATION_XML);
		return responseHeaderAndResponseBodyTable;
	}
	
	/**
	 * Returns the response code.
	 * 
	 * @param response com.sun.jersey.api.client.ClientResponse
	 * @return int  returns Response code as int
	 * @throws Throwable java.lang.Throwable
	 */
	public int getResponseCode(ClientResponse response) throws Throwable {
		int responseCode = 0;
		try {
			LOG.info("Class name : " + getCallerClassName() + "Method name : " + getCallerMethodName());
			responseCode = response.getStatus();
		} catch (Exception e) {
			failureReport("Responce Code  ", "Unable to retrieve response code due to  :: " + e);
			LOG.info(e);
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (responseCode != 200) {
				failureReport("Verifying the Response Code ", "Response  Code :: " + responseCode+", <br>"+response);
			} else {
				successReport("Verifying the Response Code ", "Response Code :: " + responseCode+", <br>"+response);
			}
		}
		return responseCode;
	}

	/**
	 * Generic to process the "POST" request.
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param requestHeaders Request header information, <br>
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return ClientResponse com.sun.jersey.api.client.ClientResponse
	 * @throws Throwable
	 */
	public ClientResponse postClientResponse(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) throws Throwable {
		ClientResponse clientResponse = null;
		Client client =null;
		try {
			
			if(isSSLCertificationVerificationValue==null){
				isSSLCertificationVerificationValue="ON";
			}
			if(isSSLCertificationVerificationValue.equalsIgnoreCase("off")){
				//Specific to SSL
				client = hostIgnoringClient();
			}else{
				//With out SSL
				client = Client.create(new DefaultClientConfig());
			}
			 WebResource resource = client.resource(url);
			
		    //WebResource resource = Client.create(new DefaultClientConfig()).resource(url);

			// If url query parameters are present
			if ((urlQueryParameters != null) && (urlQueryParameters.keySet().toArray().length > 0)) {
				MultivaluedMap<String, String> queryParams = convertHashTableToMultivaluedMap(urlQueryParameters);
				resource = resource.queryParams(queryParams);
			}

			WebResource.Builder builder = resource.accept(contentType);

			// If request headers are present
			if ((requestHeaders != null) && (requestHeaders.keySet().toArray().length > 0)) {
				for (String key : requestHeaders.keySet()) {
					builder.header(key, requestHeaders.get(key));
				}
			}

			if (input == null) {
				clientResponse = builder.post(ClientResponse.class);
			} else {
				clientResponse = builder.post(ClientResponse.class, input);
			}

			LOG.info("Response : "+clientResponse);
		} catch (Exception e) {
			LOG.fatal(Log4jUtil.getStackTrace(e));
			e.printStackTrace();
			failureReport("Retrieving Client Response ",e.toString());			
			throw new RuntimeException(e);
		}
		return clientResponse;
	}

	/**
	 * Generic to process the "POST" request.
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param requestHeaders Request header information, <br>
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @param options java.util.Hashtable<br>                     
	 *                      is added for handling additional method level arguments.                      
	 * @return ClientResponse com.sun.jersey.api.client.ClientResponse
	 * @throws Throwable
	 */
	public ClientResponse postClientResponse(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType, Hashtable<String, String> options) throws Throwable {
		ClientResponse clientResponse = null;
		Client client =null;
		try {
			
			if(isSSLCertificationVerificationValue==null){
				isSSLCertificationVerificationValue="ON";
			}
			if(isSSLCertificationVerificationValue.equalsIgnoreCase("off")){
				//Specific to SSL
				client = hostIgnoringClient();
			}else{
				//With out SSL
				client = Client.create(new DefaultClientConfig());
			}
			 WebResource resource = client.resource(url);
			
		    //WebResource resource = Client.create(new DefaultClientConfig()).resource(url);

			// If url query parameters are present
			if ((urlQueryParameters != null) && (urlQueryParameters.keySet().toArray().length > 0)) {
				MultivaluedMap<String, String> queryParams = convertHashTableToMultivaluedMap(urlQueryParameters);
				resource = resource.queryParams(queryParams);
			}

			WebResource.Builder builder = resource.accept(contentType);

			// If request headers are present
			if ((requestHeaders != null) && (requestHeaders.keySet().toArray().length > 0)) {
				for (String key : requestHeaders.keySet()) {
					builder.header(key, requestHeaders.get(key));
				}
			}

			if (input == null) {
				clientResponse = builder.post(ClientResponse.class);
			} else {
				clientResponse = builder.post(ClientResponse.class, input);
			}

			LOG.info("Response : "+clientResponse);
		} catch (Exception e) {
			
			//IF exception handling is disabled then exception will not be thrown and failure report is not added.
			//Below logic is added for handling the negative scenarios(i.e. test scripts).
			if(!isExceptionDisabled(options)){
				LOG.fatal(Log4jUtil.getStackTrace(e));
				e.printStackTrace();
				failureReport("Retrieving Client Response ",e.toString());
				throw new RuntimeException(e);
			}			
		}
		return clientResponse;
	}
	
	/**<pre>
	 * Generic to process the "POST" request
	 *  - This method returns both response and response headers.
	 *  - response and response headers are present in java.util.Hashtable
	 *  - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param input input restWebservices request input data
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters,<br> 
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return responseHeaderAndResponseBodyTable : java.util.Hashtable
	 */
	private Hashtable<String, String> postResponseHeadersAndBody(String url, String input,
			Hashtable<String, String> requestHeaders, Hashtable<String, String> urlQueryParameters,
			String contentType) {

		ClientResponse clientResponse = null;
		Hashtable<String, String> responseHeaderAndResponseBodyTable = null;

		try {
			clientResponse = postClientResponse(url, input, requestHeaders, urlQueryParameters, contentType);

			
			MultivaluedMap<String, String> multivaluedMap = clientResponse.getHeaders();

			// If multivaluedMap is not null and length greater than zero
			if ((multivaluedMap != null) && (multivaluedMap.keySet().toArray().length > 0)) {
				responseHeaderAndResponseBodyTable = new Hashtable<String, String>();
				for (String key : multivaluedMap.keySet()) {
					responseHeaderAndResponseBodyTable.put(key, multivaluedMap.get(key).get(0));
				}

				String strResponse = null;
				try{
					responseHeaderAndResponseBodyTable.put(RESTConstants.RESPONSE_CODE, ""+clientResponse.getStatus());
					strResponse = clientResponse.getEntity(String.class);
				}catch (Exception e) {
					strResponse = "";
				}				
				responseHeaderAndResponseBodyTable.put(RESTConstants.RESPONSE_BODY, strResponse);
			}

		} catch (Throwable e) {
			LOG.info(e);
			throw new RuntimeException(e);
		}

		return responseHeaderAndResponseBodyTable;
	}	
	
	/**
	 * Returns Text response as String, for Text 'POST' method
	 * @param url - url End point url
	 * @param input - Text payload
	 * @param requestHeaders - Request header information, if 'headerParameters' are null then 'headerParameters' are ignored.
	 * @param urlQueryParameters - url parameters, if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.
	 * @return Response in String format
	 * @throws Throwable
	 */
	public String postTextResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = postResponseAsString(url, input, requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_TEXT);
		return clientResponse;
	}
	
	// Only for internal use with in the class
	/**
	 * <pre>
	 * Generic to process the "POST" request and return the response(i.e. Json/XML) in string 
	 *  -Only for internal use with in the class
	 * </pre>
	 * @param url End point url
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters,<br> 
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return responseString java.lang.String
	 * @throws Throwable java.lang.Throwable
	 */
	private String postResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) throws Throwable {
		String responseString = null;

		ClientResponse clientResponse = postClientResponse(url, input, requestHeaders, urlQueryParameters, contentType);
		try{
			responseString = clientResponse.getEntity(String.class);
		}catch (Exception e) {
			responseString = "";
		}
		return responseString;
	}

	// TODO need to add the method postJSONResponseAsJsonObject
	/**
	 * Returns JSON response as String, for JSON 'POST' method
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters,<br> 
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String : Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public String postJSONResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = postResponseAsString(url, input, requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_JSON);
		return clientResponse;
	}	

	/**
	 * Returns XML response as String, for XML 'POST' method
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public String postXMLResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = postResponseAsString(url, input, requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return clientResponse;
	}	
 
	/**
	 * Returns XML response as Document, for XML 'POST' method
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param requestHeaders requestHeaders while sending the webservice request 
	 * @param urlQueryParameters urlQueryParameters while sending the webservice request
	 * @return org.w3c.dom.Document Document as response object
	 * @throws Throwable java.lang.Throwable
	 */
	public Document postXMLResponseAsDocument(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = postResponseAsString(url, input, requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return XmlUtil.getXMLDocumentObject(clientResponse);
	}	

	/**<pre>
	 * Returns both JSON response and response header, for Json 'POST' method
	 * - Json response and response headers are present in java.util.Hashtable
	 * - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public Hashtable<String, String> postJSONResponseHeadersAndBody(String url, String input,
			Hashtable<String, String> requestHeaders, Hashtable<String, String> urlQueryParameters) throws Throwable {
		Hashtable<String, String> responseHeaderAndResponseBodyTable = postResponseHeadersAndBody(url, input,
				requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_JSON);
		return responseHeaderAndResponseBodyTable;
	}

	/**<pre>
	 * Returns both XML response and response header, for xml 'POST' method
	 * - XML response and response headers are present in java.util.Hashtable
	 * - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url<br>
	 * @param input restWebservices request input data 
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public Hashtable<String, String> postXMLResponseHeadersAndBody(String url, String input,
			Hashtable<String, String> requestHeaders, Hashtable<String, String> urlQueryParameters) throws Throwable {
		Hashtable<String, String> responseHeaderAndResponseBodyTable = postResponseHeadersAndBody(url, input,
				requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return responseHeaderAndResponseBodyTable;
	}

	/**
	 * Generic to process the "PUT" request.
	 * 
	 * @param url End point url
	 * @param input restWebservices request input data 
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return ClientResponse : com.sun.jersey.api.client.ClientResponse
	 * @throws Throwable java.lang.Throwable
	 */
	public ClientResponse putClientResponse(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) throws Throwable {
		ClientResponse clientResponse = null;
		Client client =null;
		try {
			
			if(isSSLCertificationVerificationValue==null){
				isSSLCertificationVerificationValue="ON";
			}
			
			if(isSSLCertificationVerificationValue.equalsIgnoreCase("off")){
				//Specific to SSL
				client = hostIgnoringClient();
			}else{
				//With out SSL
				client = Client.create(new DefaultClientConfig());
			}
			WebResource resource = client.resource(url);
			
			 
			//WebResource resource = Client.create(new DefaultClientConfig()).resource(url);

			// If url query parameters are present
			if ((urlQueryParameters != null) && (urlQueryParameters.keySet().toArray().length > 0)) {
				MultivaluedMap<String, String> queryParams = convertHashTableToMultivaluedMap(urlQueryParameters);
				resource = resource.queryParams(queryParams);
			}

			WebResource.Builder builder = resource.accept(contentType);

			// If request headers are present
			if ((requestHeaders != null) && (requestHeaders.keySet().toArray().length > 0)) {
				for (String key : requestHeaders.keySet()) {
					builder.header(key, requestHeaders.get(key));
				}
			}

			if (input == null) {
				clientResponse = builder.put(ClientResponse.class);
			} else {
				clientResponse = builder.put(ClientResponse.class, input);
			}

			LOG.info("Response : "+clientResponse);
		} catch (Exception e) {
			LOG.fatal(Log4jUtil.getStackTrace(e));
			failureReport("Retrieving Client Response ",e.toString());
			throw new RuntimeException(e);
		}

		return clientResponse;
	}

	/**
	 * Generic to process the "PUT" request.
	 * 
	 * @param url End point url
	 * @param input restWebservices request input data 
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @param options java.util.Hashtable<br>                     
	 *                      is added for handling additional method level arguments.
	 * @return ClientResponse : com.sun.jersey.api.client.ClientResponse
	 * @throws Throwable java.lang.Throwable
	 */
	public ClientResponse putClientResponse(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType, Hashtable<String, String> options) throws Throwable {
		ClientResponse clientResponse = null;
		Client client =null;
		try {
			
			if(isSSLCertificationVerificationValue==null){
				isSSLCertificationVerificationValue="ON";
			}
			
			if(isSSLCertificationVerificationValue.equalsIgnoreCase("off")){
				//Specific to SSL
				client = hostIgnoringClient();
			}else{
				//With out SSL
				client = Client.create(new DefaultClientConfig());
			}
			WebResource resource = client.resource(url);
			
			 
			//WebResource resource = Client.create(new DefaultClientConfig()).resource(url);

			// If url query parameters are present
			if ((urlQueryParameters != null) && (urlQueryParameters.keySet().toArray().length > 0)) {
				MultivaluedMap<String, String> queryParams = convertHashTableToMultivaluedMap(urlQueryParameters);
				resource = resource.queryParams(queryParams);
			}

			WebResource.Builder builder = resource.accept(contentType);

			// If request headers are present
			if ((requestHeaders != null) && (requestHeaders.keySet().toArray().length > 0)) {
				for (String key : requestHeaders.keySet()) {
					builder.header(key, requestHeaders.get(key));
				}
			}

			if (input == null) {
				clientResponse = builder.put(ClientResponse.class);
			} else {
				clientResponse = builder.put(ClientResponse.class, input);
			}

			LOG.info("Response : "+clientResponse);
		} catch (Exception e) {
			
			//IF exception handling is disabled then exception will not be thrown and failure report is not added.
			//Below logic is added for handling the negative scenarios(i.e. test scripts).
			if(!isExceptionDisabled(options)){
				LOG.fatal(Log4jUtil.getStackTrace(e));
				failureReport("Retrieving Client Response ",e.toString());
				throw new RuntimeException(e);
			}
			
		}

		return clientResponse;
	}	
	/**<pre>
	 * Generic to process the "PUT" request
	 *  - This method returns both response and response headers.
	 *  - response and response headers are present in java.util.Hashtable
	 *  - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters,<br> 
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String <br>
	 *                      Ex: contentType = "application/json"
	 * @return responseHeaderAndResponseBodyTable java.util.Hashtable
	 */
	private Hashtable<String, String> putResponseHeadersAndBody(String url, String input,
			Hashtable<String, String> requestHeaders, Hashtable<String, String> urlQueryParameters,
			String contentType) {

		ClientResponse clientResponse = null;
		Hashtable<String, String> responseHeaderAndResponseBodyTable = null;

		try {
			clientResponse = putClientResponse(url, input, requestHeaders, urlQueryParameters, contentType);

			MultivaluedMap<String, String> multivaluedMap = clientResponse.getHeaders();

			// If multivaluedMap is not null and length greater than zero
			if ((multivaluedMap != null) && (multivaluedMap.keySet().toArray().length > 0)) {
				responseHeaderAndResponseBodyTable = new Hashtable<String, String>();
				for (String key : multivaluedMap.keySet()) {
					responseHeaderAndResponseBodyTable.put(key, multivaluedMap.get(key).get(0));
				}

				String strResponse = null;
				try{
					responseHeaderAndResponseBodyTable.put(RESTConstants.RESPONSE_CODE, ""+clientResponse.getStatus());
					strResponse = clientResponse.getEntity(String.class);
				}catch (Exception e) {
					strResponse = "";
				}	
				responseHeaderAndResponseBodyTable.put(RESTConstants.RESPONSE_BODY, strResponse);
			}

		} catch (Throwable e) {
			LOG.info(e);
			throw new RuntimeException(e);
		}

		return responseHeaderAndResponseBodyTable;
	}
	
	/**
	 * Returns Text response as String, for Text 'PUT' method
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public String putTextResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = putResponseAsString(url, input, requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_TEXT);
		return clientResponse;
	}
	
	// Only for internal use with in the class
	/**<pre>
	 * Generic to process the "PUT" request as return the response(i.e. Json/XML) in string 
	 *  -Only for internal use with in the class
	 * </pre> 
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return responseString : java.lang.String
	 * @throws Throwable java.lang.Throwable
	 */
	private String putResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) throws Throwable {
		String responseString = null;

		ClientResponse clientResponse = putClientResponse(url, input, requestHeaders, urlQueryParameters, contentType);
		try{
			responseString = clientResponse.getEntity(String.class);
		}catch (Exception e) {
			responseString = "";
		}
		return responseString;
	}
	
	// TODO need to add the method putJSONResponseAsJsonObject
	/**
	 * Returns JSON response as String, for JSON 'PUT' method
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public String putJSONResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = putResponseAsString(url, input, requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_JSON);
		return clientResponse;
	}

	/**
	 * Returns XML response as String object for XML 'PUT' method
	 *
	 * @param url End point url
	 * @param input input restWebservices request input data  
	 * @param headerParameters Request header information,<br>
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String object
	 * @throws Throwable java.lang.Throwable
	 */
	public String putXMLResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = putResponseAsString(url, input, requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return clientResponse;
	}	

	/**
	 * Returns XML response as Document object for XML 'PUT' method
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param requestHeaders Request header information, <br>
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.
	 * @return org.w3c.dom.Document Response in Document object
	 * @throws Throwable java.lang.Throwable
	 */
	public Document putXMLResponseAsDocument(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = putResponseAsString(url, input, requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return XmlUtil.getXMLDocumentObject(clientResponse);
	}	
	/**<pre>
	 * Returns both JSON response and response header, for Json 'PUT' method
	 * - Json response and response headers are present in java.util.Hashtable
	 * - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public Hashtable<String, String> putJSONResponseHeadersAndBody(String url, String input,
			Hashtable<String, String> requestHeaders, Hashtable<String, String> urlQueryParameters) throws Throwable {
		Hashtable<String, String> responseHeaderAndResponseBodyTable = putResponseHeadersAndBody(url, input,
				requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_JSON);
		return responseHeaderAndResponseBodyTable;
	}

	/**<pre>
	 * Returns both XML response and response header, for Json 'PUT' method
	 * - XML response and response headers are present in java.util.Hashtable
	 * - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param input input restWebservices request input data
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public Hashtable<String, String> putXMLResponseHeadersAndBody(String url, String input,
			Hashtable<String, String> requestHeaders, Hashtable<String, String> urlQueryParameters) throws Throwable {
		Hashtable<String, String> responseHeaderAndResponseBodyTable = putResponseHeadersAndBody(url, input,
				requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return responseHeaderAndResponseBodyTable;
	}
	
	public ClientResponse deleteClientResponse(String url, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) throws Throwable {

		return deleteClientResponse(url, null, requestHeaders, urlQueryParameters, contentType);
	}
	
	/**
	 * Generic to process the "DELETE" request with request body payload.
	 * 
	 * @param url End point url
	 * @param input input restWebservices request body data 
	 * @param requestHeaders Request header information, <br>
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      ExcontentType = "application/json"
	 * @return ClientResponse com.sun.jersey.api.client.ClientResponse
	 * @throws Throwable java.lang.Throwable
	 */
	public ClientResponse deleteClientResponse(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) throws Throwable {
		ClientResponse clientResponse = null;
		Client client = null;

		try {
			LOG.info("Class name : " + getCallerClassName() + "Method name : " + getCallerMethodName());
			if(isSSLCertificationVerificationValue==null){
				isSSLCertificationVerificationValue="ON";
			}
			
			if(isSSLCertificationVerificationValue.equalsIgnoreCase("off")){
				//Specific to SSL
				client = hostIgnoringClient();
			}else{
				//With out SSL
				client = Client.create(new DefaultClientConfig());
			}
			 WebResource resource = client.resource(url);
			
			
			//WebResource resource = Client.create(new DefaultClientConfig()).resource(url);

			// If url query parameters are present
			if ((urlQueryParameters != null) && (urlQueryParameters.keySet().toArray().length > 0)) {
				MultivaluedMap<String, String> queryParams = convertHashTableToMultivaluedMap(urlQueryParameters);
				resource = resource.queryParams(queryParams);
			}

			WebResource.Builder builder = resource.accept(contentType);

			// If request headers are present
			if ((requestHeaders != null) && (requestHeaders.keySet().toArray().length > 0)) {
				for (String key : requestHeaders.keySet()) {
					builder.header(key, requestHeaders.get(key));
				}
			}

			if (input == null) {
				clientResponse = builder.delete(ClientResponse.class);
			} else {
				clientResponse = builder.delete(ClientResponse.class, input);
			}

			LOG.info("Response : "+clientResponse);
		} catch (Exception e) {
			LOG.fatal(Log4jUtil.getStackTrace(e));
			failureReport("Retrieving Client Response ",e.toString());
			throw new RuntimeException(e);
		}
		return clientResponse;
	}

	/**
	 * Generic to process the "DELETE" request
	 * 
	 * @param url End point url
	 * @param requestHeaders Request header information, <br>
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      ExcontentType = "application/json"
	 * @param options java.util.Hashtable<br>                     
	 *                      is added for handling additional method level arguments.                      
	 * @return ClientResponse com.sun.jersey.api.client.ClientResponse
	 * @throws Throwable java.lang.Throwable
	 */
	public ClientResponse deleteClientResponse(String url, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType, Hashtable<String, String> options) throws Throwable {
		ClientResponse clientResponse = null;
		Client client = null;

		try {
			LOG.info("Class name : " + getCallerClassName() + "Method name : " + getCallerMethodName());
			if(isSSLCertificationVerificationValue==null){
				isSSLCertificationVerificationValue="ON";
			}
			
			if(isSSLCertificationVerificationValue.equalsIgnoreCase("off")){
				//Specific to SSL
				client = hostIgnoringClient();
			}else{
				//With out SSL
				client = Client.create(new DefaultClientConfig());
			}
			 WebResource resource = client.resource(url);
			
			
			//WebResource resource = Client.create(new DefaultClientConfig()).resource(url);

			// If url query parameters are present
			if ((urlQueryParameters != null) && (urlQueryParameters.keySet().toArray().length > 0)) {
				MultivaluedMap<String, String> queryParams = convertHashTableToMultivaluedMap(urlQueryParameters);
				resource = resource.queryParams(queryParams);
			}

			WebResource.Builder builder = resource.accept(contentType);

			// If request headers are present
			if ((requestHeaders != null) && (requestHeaders.keySet().toArray().length > 0)) {
				for (String key : requestHeaders.keySet()) {
					builder.header(key, requestHeaders.get(key));
				}
			}

			clientResponse = builder.delete(ClientResponse.class);

			LOG.info("Response : "+clientResponse);
		} catch (Exception e) {
			
			//IF exception handling is disabled then exception will not be thrown and failure report is not added.
			//Below logic is added for handling the negative scenarios(i.e. test scripts).
			if(!isExceptionDisabled(options)){
				LOG.fatal(Log4jUtil.getStackTrace(e));
				e.printStackTrace();
				failureReport("Retrieving Client Response ",e.toString());
				throw new RuntimeException(e);
			}			
		}
		return clientResponse;
	}	
	/**<pre>
	 * Generic to process the "GET" request
	 *  - This method returns both response and response headers.
	 *  - response and response headers are present in java.util.Hashtable
	 *  - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param requestHeaders Request header information, 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.<br>  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return responseHeaderAndResponseBodyTable java.util.Hashtable
	 */
	private Hashtable<String, String> deleteResponseHeadersAndBody(String url, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) {

		ClientResponse clientResponse = null;
		Hashtable<String, String> responseHeaderAndResponseBodyTable = null;

		try {
			clientResponse = deleteClientResponse(url, requestHeaders, urlQueryParameters, contentType);

			MultivaluedMap<String, String> multivaluedMap = clientResponse.getHeaders();

			// If multivaluedMap is not null and length greater than zero
			if ((multivaluedMap != null) && (multivaluedMap.keySet().toArray().length > 0)) {
				responseHeaderAndResponseBodyTable = new Hashtable<String, String>();
				for (String key : multivaluedMap.keySet()) {
					responseHeaderAndResponseBodyTable.put(key, multivaluedMap.get(key).get(0));
				}

				String strResponse = null;
				try{
					responseHeaderAndResponseBodyTable.put(RESTConstants.RESPONSE_CODE, ""+clientResponse.getStatus());
					strResponse = clientResponse.getEntity(String.class);
				}catch (Exception e) {
					strResponse = "";
				}
				responseHeaderAndResponseBodyTable.put(RESTConstants.RESPONSE_BODY, strResponse);
			}

		} catch (Throwable e) {
			LOG.error(Log4jUtil.getStackTrace(e));
			throw new RuntimeException(e);
		}

		return responseHeaderAndResponseBodyTable;
	}
	
	/**
	 * Returns Text response as String, for Text 'DELETE' method
	 * 
	 * @param url End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public String deleteTextResponseAsString(String url, Hashtable<String, String> headerParameters,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = deleteResponseAsString(url, headerParameters, urlQueryParameters, RESTConstants.APPLICATION_TEXT);
		return clientResponse;
	}
	
	// Only for internal use with in the class
	/**<pre>
	 * Generic to process the "GET" request as return the response(i.e. Json/XML) in string 
	 *  -Only for internal use with in the class
	 * </pre>
	 * @param url End point url
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return responseString java.lang.String
	 */
	private String deleteResponseAsString(String url, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) {
		ClientResponse clientResponse = null;
		String response = null;
		try {
			clientResponse = deleteClientResponse(url, requestHeaders, urlQueryParameters, contentType);
			try{
				response = clientResponse.getEntity(String.class);
			}catch (Exception e) {
				response = "";
			}
		} catch (Throwable e) {
			LOG.error(Log4jUtil.getStackTrace(e));
			failureReport("Retrieving Client Response ",e.toString());
			throw new RuntimeException(e);
		}
		return response;
	}
	
	// TODO need to add the method deleteJSONResponseAsJsonObject
	/**
	 * Returns JSON response as String, for JSON 'DELETE' method
	 * 
	 * @param url End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public String deleteJSONResponseAsString(String url, Hashtable<String, String> headerParameters,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = deleteResponseAsString(url, headerParameters, urlQueryParameters, RESTConstants.APPLICATION_JSON);
		return clientResponse;
	}

	/**<pre>
	 * Returns XML response as String, for XML 'DELETE' method
	 *  - Returns response as String object 
	 * </pre> 
	 * @param url End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public String deleteXMLResponseAsString(String url, Hashtable<String, String> headerParameters,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = deleteResponseAsString(url, headerParameters, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return clientResponse;
	}
 
	/**<pre>
	 * Returns XML response as String, for XML 'DELETE' method
	 *  - Returns response as Document object
	 * </pre> 
	 * @param url End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.
	 * @return org.w3c.dom.Document Response as Document object
	 * @throws Throwable java.lang.Throwable
	 */
	public Document deleteXMLResponseAsDocument(String url, Hashtable<String, String> headerParameters,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = deleteResponseAsString(url, headerParameters, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return XmlUtil.getXMLDocumentObject(clientResponse);
	}
	
	/**<pre>
	 * Returns both JSON response and response header, for Json 'DELETE' method
	 * - Json response and response headers are present in java.util.Hashtable
	 * - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public Hashtable<String, String> deleteJSONResponseHeadersAndBody(String url,
			Hashtable<String, String> headerParameters, Hashtable<String, String> urlQueryParameters) throws Throwable {
		Hashtable<String, String> responseHeaderAndResponseBodyTable = deleteResponseHeadersAndBody(url,
				headerParameters, urlQueryParameters, RESTConstants.APPLICATION_JSON);
		return responseHeaderAndResponseBodyTable;
	}

	/**<pre>
	 * Returns both XML response and response header, for XML 'DELETE' method
	 * - XML response and response headers are present in java.util.Hashtable
	 * - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public Hashtable<String, String> deleteXMLResponseHeadersAndBody(String url,
			Hashtable<String, String> headerParameters, Hashtable<String, String> urlQueryParameters) throws Throwable {
		Hashtable<String, String> responseHeaderAndResponseBodyTable = deleteResponseHeadersAndBody(url,
				headerParameters, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return responseHeaderAndResponseBodyTable;
	}	
	
	/**<pre>
	 * Converts the Hashtable to MultivaluedMap. MultivaluedMap is used to
	 * construct the url with query parameters.
	 * </pre>
	 * @param urlQueryParameters java.util.Hashtable&lt;String, String&gt;
	 * @return queryParams as javax.ws.rs.core.MultivaluedMap&lt;String,String&gt;
	 * @throws Exception
	 */
	private MultivaluedMap<String, String> convertHashTableToMultivaluedMap(
			Hashtable<String, String> urlQueryParameters) throws Exception {
		MultivaluedMap<String, String> queryParams = null;
		if (urlQueryParameters == null || urlQueryParameters.size() == 0) {
			throw new Exception("url parameters are missing.");
		}

		queryParams = new MultivaluedMapImpl();
		for (String reqKey : urlQueryParameters.keySet()) {
			queryParams.add(reqKey, urlQueryParameters.get(reqKey));
		}

		return queryParams;
	}

	/**<pre>
	 * Converts the inputXml(i.e. String xml) into org.w3c.dom.Document and returns the org.w3c.dom.Document object.
	 *  - use XmlUtil.getXMLDocumentObject(String inputXml), since this method is deprecated.
	 * </pre> 
	 * @param inputXml provide inputXml asjava.lang.String
	 * @return org.w3c.dom.Document converts the inputXml to Document object and returns the Document object.
	 * @throws Exception java.lang.Exception
	 */
	@Deprecated
	public Document getXMLDocumentObject(String inputXml)
			throws Exception {
		LOG.warn("Use XmlUtil.getXMLDocumentObject(String inputXml), since this method is deprecated");
		return XmlUtil.getXMLDocumentObject(inputXml);	
	}
		 
	/**
	 * Returns value from jsonData based on jsonPathExpression
	 *   - use JsonUtil.getJsonElement(String jsonData, String jsonPathExpression), since this method is deprecated.
	 * 
	 * @param jsonData input jsonData as java.lang.String
	 * @param jsonPathExpression jsonPathExpression as java.lang.String 
	 * @return actualVal returns string value from jsonData based on jsonPathExpression
	 * @throws Throwable
	 */
	@Deprecated
	public String getJsonElement(String jsonData, String jsonPathExpression) throws Throwable {
		LOG.warn("Use JsonUtil.getJsonElement(String jsonData, String jsonPathExpression), since this method is deprecated");
		return JsonUtil.getJsonElement(jsonData, jsonPathExpression);
	}

	/**
	 * Returns value from xmlData based on xmlPathExpression
	 * 	- use XmlUtil.getXmlElement(String xmlData, String xmlPathExpression), since this method is deprecated.
	 * 
	 * @param xmlData input xmlData as java.lang.String
	 * @param xmlPathExpression xmlPathExpression as java.lang.String
	 * @return actualVal returns string value from jsonData based on jsonPathExpression
	 * @throws Throwable
	 */
	@Deprecated
	public String getXmlElement(String xmlData, String xmlPathExpression) throws Throwable {
		LOG.warn("Use XmlUtil.getXmlElement(String xmlData, String xmlPathExpression), since this method is deprecated"); 
		return XmlUtil.getXmlElement(xmlData, xmlPathExpression); 
	}
	
	/**<pre>
	 * Verify the jsonData(i.e. json string) against the jsonPathExpressaion and expectedValue.
	 *   - This method verify against only single jsonItem/JsonElement         
	 * </pre>           
	 * @param jsonData input jsonData as java.lang.String
	 * @param jsonPathExpression jsonPathExpression as java.lang.String 
	 * @param expectedValue jsonValue to compare in jsonData based on jsonPathExpression
	 * @param fieldNameForCustomReport value supplied with be used in customReport 
	 * @return boolean true if condition matches else returns false
	 * @throws Throwable java.lang.Exception
	 */
	public boolean assertJsonElement(String jsonData, String jsonPathExpression, String expectedValue,
			String fieldNameForCustomReport) throws Throwable {
		String actualVal = null;
		boolean flag = true;
		try {
			LOG.info("Class name : " + getCallerClassName() + "Method name : " + getCallerMethodName());

			actualVal = JsonUtil.getJsonElement(jsonData, jsonPathExpression);
            if(actualVal == null){
            	failureReportForJsonWebService("Verifying property value :: " + fieldNameForCustomReport,
						"Retrieved Property value is not as expected  ::" + actualVal, jsonData);
            	flag = false;
            }else
				
			if (actualVal.equals(expectedValue)) {
				successReportForJsonWebService("Verify property value '" + fieldNameForCustomReport+"'", 
						"Successfully verified '" + fieldNameForCustomReport + "' property, value  is '" + actualVal+"'", jsonData);
				flag = true;
			} else {
				failureReportForJsonWebService("Verify property value '" + fieldNameForCustomReport+"'",
						"Property value is not as expected, expected value is '"+expectedValue+"' where as actual value is '" + actualVal+"'", jsonData);
				flag = false;
			}
		} catch (Exception e) {
			failureReport("Validate the responce Object ", "Unable to Validate the Responce Property due to :: " + e);
			LOG.error(Log4jUtil.getStackTrace(e));
			flag = false;
			//e.printStackTrace();
			throw new RuntimeException(e);
		}
		return flag;
		
	}

	/**<pre>
	 * Verify the xmlData(i.e. xml string) against the xmlPathExpressaion and expectedValue.
	 *   - This method verify against only single xmlItem/XmlElement
	 * </pre>           
	 * @param xmlData input xmlData as java.lang.String
	 * @param xmlPathExpression xmlPathExpression as java.lang.String
	 * @param expectedValue xmlValue to compare in xmlData based on xmlPathExpression
	 * @param fieldNameForCustomReport value supplied with be used in customReport
	 * @return boolean
	 * @throws Throwable
	 */
	public boolean assertXmlElement(String xmlData, String xmlPathExpression, String expectedValue,
			String fieldNameForCustomReport) throws Throwable {
		String actualVal = null;
		boolean flag = true;		
		try {
			LOG.info("Class name : " + getCallerClassName() + "Method name : " + getCallerMethodName());

			actualVal = XmlUtil.getXmlElement(xmlData, xmlPathExpression);

            if(actualVal == null){
            	failureReportForXmlWebService("Verifying property value :: " + fieldNameForCustomReport,
						"Retrieved Property value is not as expected  ::" + actualVal, xmlData);
            	flag = false;
            }else			
			if (actualVal.equals(expectedValue)) {
				successReportForXmlWebService("Verifying property value :: " + fieldNameForCustomReport,
						"Successfully Verified " + fieldNameForCustomReport + " Property Value  is :: " + actualVal, xmlData);
				
				//TODO need to remove below commented code after executing the scripts.
				/*successReport("Verifying property value :: " + fieldNameForCustomReport,
						"Successfully Verified " + fieldNameForCustomReport + " Property Value  is :: " + actualVal);*/
				flag = true;
			} else {
				failureReportForXmlWebService("Verifying property value :: " + fieldNameForCustomReport,
						"Retrieved Property value is not as expected  ::" + actualVal, xmlData);
				flag = false;
			}
		} catch (Exception e) {
			failureReport("Validate the responce Object ", "Unable to Validate the Responce Property due to :: " + e);
			LOG.error(Log4jUtil.getStackTrace(e));
			//e.printStackTrace();
			throw new RuntimeException(e);
		}
		return flag;
	}	
	
	/**<pre>
	 * Verify the jsonData(i.e. json string) against the jsonPathExpressaion and expectedData.
	 *   - This method verify against multiple jsonItem/JsonElement, with in the single parent JsonItem/JsonElement         
	 * </pre>
	 * @param jsonData input jsonData as java.lang.String
	 * @param jsonPathExpression jsonPathExpression as java.lang.String 
	 * @param expectedData java.util.Hashtable
	 *        expectedData(i.e. Hashtable) should contain both the jsonItem name and its value.             
	 * @throws Throwable java.lang.Throwable
	 */
	public boolean assertJsonElements(String jsonData, String jsonPathExpression, Hashtable<String, String> expectedData) throws Throwable {
		LOG.info("Class name : " + getCallerClassName() + "Method name : " + getCallerMethodName());
		
		if(jsonData==null || jsonData.trim().length()==0){
			LOG.error("Json data is null or blank");
			return false;
		}
		
		if(jsonPathExpression==null || jsonPathExpression.trim().length()==0){
			LOG.error("jsonPathExpression is null or blank");
			return false;
		}		
		
		if(expectedData==null || expectedData.size()==0){
			LOG.error("expectedData is null or blank");
			return false;
		}
		
		
		List<Boolean> flagList = new ArrayList<Boolean>() ;
		jsonPathExpression = jsonPathExpression.trim();
		
		for(String key : expectedData.keySet()){
			try{
				boolean flag = assertJsonElement(jsonData, jsonPathExpression+"."+key, expectedData.get(key), key);
				flagList.add(flag);
			}catch (Exception e) {
				LOG.error(Log4jUtil.getStackTrace(e));
			}
		}
		
		if(flagList.contains(false)){
			return false;
		}else{
			return true;
		}
	}

	/**<pre>
	 * Verify the xmlData(i.e. xml string) against the xmlPathExpressaion and expectedData.
	 *   - This method verify against multiple xmlItem/XmlElement, with in the single parent XmlItem/XmlElement
	 * </pre>           
	 * @param xmlData input xmlData as java.lang.String
	 * @param xmlPathExpression xmlPathExpression as java.lang.String
	 * @param expectedData java.util.Hashtable<br>
	 *        expectedData(i.e. Hashtable) should contain both the xmlElement name and its value.
	 * @throws Throwable java.lang.Throwable
	 */
	public void assertXmlElements(String xmlData, String xmlPathExpression, Hashtable<String, String> expectedData) throws Throwable {
		LOG.info("Class name : " + getCallerClassName() + "Method name : " + getCallerMethodName());
		
		if(xmlData==null || xmlData.trim().length()==0){
			LOG.error("Xml data is null or blank");
			return;
		}
		
		if(xmlPathExpression==null || xmlPathExpression.trim().length()==0){
			LOG.error("xmlPathExpression is null or blank");
			return;
		}		
		
		if(expectedData==null || expectedData.size()==0){
			LOG.error("expectedData is null or blank");
			return;
		}
		
		xmlPathExpression = xmlPathExpression.trim();
		
		for(String key : expectedData.keySet()){
			try{
				assertXmlElement(xmlData, xmlPathExpression+"/"+key, expectedData.get(key), key);
			}catch (Exception e) {
				LOG.error(e.getStackTrace());
			}
		}
	}
	
	/**
	 * getCallerClassName
	 * 
	 * @return String
	 */
	private static String getCallerClassName() {
		StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
		return stElements[3].getClassName();
	}

	/**
	 * getCallerMethodName
	 * 
	 * @return String
	 */
	private static String getCallerMethodName() {
		StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
		return stElements[3].getMethodName();
	}
	
	/**
	 * Compares two string values
	 * @param actText text1
	 * @param expText text2
	 * @return boolean value indicating success of the operation
	 */
	public boolean assertTextStringMatching(String actText, String expText,boolean expected){
		boolean flag = false;
		try {
			LOG.info("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			LOG.info("Class name : " + getCallerClassName() + "Method name : " + getCallerMethodName());
			String ActualText = actText.trim();
			LOG.info("act - " + ActualText);
			LOG.info("exp - " + expText);
			if (ActualText.equalsIgnoreCase(expText.trim())) {
				LOG.info("in if loop");
				flag = true;
				LOG.info("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(Log4jUtil.getStackTrace(e));
		} finally {
			if(expected){
			if (!flag) {
				failureReport("Actual Text :: "+ actText +" Should equal to :: " + expText , "Actual Text :: "+ actText +" is not equal to :: " + expText);
			} else {
				successReport("Actual Text :: "+ actText +" Should equal to :: " + expText , "Actual Text :: "+ actText +" is equal to :: " + expText);
				}
			}
		}
		return flag;
	}
 
	/**
	 * Generic to process the "PATCH" request.
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param requestHeaders Request header information, <br>
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return ClientResponse com.sun.jersey.api.client.ClientResponse
	 * @throws Throwable
	 */
	public ClientResponse patchClientResponse(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) throws Throwable {
		ClientResponse clientResponse = null;
		try {
			
			DefaultClientConfig config = new DefaultClientConfig();
			
			//This workaround has some limitation - you can't put an entity to the request. And additionally, 
			//it \*probably\* wont work on some containers and maybe in future versions of JDK (containers sometimes have their own implementation of HttpURLConnection).
		    config.getProperties().put(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true);
		    
			WebResource resource = Client.create(config).resource(url);

			// If url query parameters are present
			if ((urlQueryParameters != null) && (urlQueryParameters.keySet().toArray().length > 0)) {
				MultivaluedMap<String, String> queryParams = convertHashTableToMultivaluedMap(urlQueryParameters);
				resource = resource.queryParams(queryParams);
			}

			WebResource.Builder builder = resource.accept(contentType);

			// If request headers are present
			if ((requestHeaders != null) && (requestHeaders.keySet().toArray().length > 0)) {
				for (String key : requestHeaders.keySet()) {
					builder.header(key, requestHeaders.get(key));
				}
			}

			if (input == null) {
				clientResponse = builder.method("PATCH", ClientResponse.class);
			} else {
				clientResponse = builder.method("PATCH", ClientResponse.class, input);
			}

			LOG.info("Response : "+clientResponse);
		} catch (Exception e) {
			LOG.fatal(Log4jUtil.getStackTrace(e));
			e.printStackTrace();
			failureReport("Retrieving Client Response ",e.toString());			
			throw new RuntimeException(e);
		}
		return clientResponse;
	}	

	/**
	 * Generic to process the "PATCH" request.
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param requestHeaders Request header information, <br>
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @param options java.util.Hashtable<br>                     
	 *                      is added for handling additional method level arguments.
	 * @return ClientResponse com.sun.jersey.api.client.ClientResponse
	 * @throws Throwable
	 */
	public ClientResponse patchClientResponse(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType, Hashtable<String, String> options) throws Throwable {
		ClientResponse clientResponse = null;
		try {
			
			DefaultClientConfig config = new DefaultClientConfig();
			
			//This workaround has some limitation - you can't put an entity to the request. And additionally, 
			//it \*probably\* wont work on some containers and maybe in future versions of JDK (containers sometimes have their own implementation of HttpURLConnection).
		    config.getProperties().put(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true);
		    
			WebResource resource = Client.create(config).resource(url);

			// If url query parameters are present
			if ((urlQueryParameters != null) && (urlQueryParameters.keySet().toArray().length > 0)) {
				MultivaluedMap<String, String> queryParams = convertHashTableToMultivaluedMap(urlQueryParameters);
				resource = resource.queryParams(queryParams);
			}

			WebResource.Builder builder = resource.accept(contentType);

			// If request headers are present
			if ((requestHeaders != null) && (requestHeaders.keySet().toArray().length > 0)) {
				for (String key : requestHeaders.keySet()) {
					builder.header(key, requestHeaders.get(key));
				}
			}

			if (input == null) {
				clientResponse = builder.method("PATCH", ClientResponse.class);
			} else {
				clientResponse = builder.method("PATCH", ClientResponse.class, input);
			}

			LOG.info("Response : "+clientResponse);
		} catch (Exception e) {
			
			//IF exception handling is disabled then exception will not be thrown and failure report is not added.
			//Below logic is added for handling the negative scenarios(i.e. test scripts).
			if(!isExceptionDisabled(options)){
				LOG.fatal(Log4jUtil.getStackTrace(e));
				e.printStackTrace();
				failureReport("Retrieving Client Response ",e.toString());
				throw new RuntimeException(e);
			}			
		}
		return clientResponse;
	}	

	/**<pre>
	 * Generic method to process the "PATCH" request
	 *  - This method returns both response and response headers.
	 *  - response and response headers are present in java.util.Hashtable
	 *  - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param input input restWebservices request input data
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored.  
	 * @param urlQueryParameters url parameters,<br> 
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return responseHeaderAndResponseBodyTable : java.util.Hashtable
	 */
	private Hashtable<String, String> patchResponseHeadersAndBody(String url, String input,
			Hashtable<String, String> requestHeaders, Hashtable<String, String> urlQueryParameters,
			String contentType) {

		ClientResponse clientResponse = null;
		Hashtable<String, String> responseHeaderAndResponseBodyTable = null;

		try {
			clientResponse = patchClientResponse(url, input, requestHeaders, urlQueryParameters, contentType);

			
			MultivaluedMap<String, String> multivaluedMap = clientResponse.getHeaders();

			// If multivaluedMap is not null and length greater than zero
			if ((multivaluedMap != null) && (multivaluedMap.keySet().toArray().length > 0)) {
				responseHeaderAndResponseBodyTable = new Hashtable<String, String>();
				for (String key : multivaluedMap.keySet()) {
					responseHeaderAndResponseBodyTable.put(key, multivaluedMap.get(key).get(0));
				}

				String strResponse = null;
				try{
					responseHeaderAndResponseBodyTable.put(RESTConstants.RESPONSE_CODE, ""+clientResponse.getStatus());
					strResponse = clientResponse.getEntity(String.class);
				}catch (Exception e) {
					strResponse = "";
				}				
				responseHeaderAndResponseBodyTable.put(RESTConstants.RESPONSE_BODY, strResponse);
			}

		} catch (Throwable e) {
			LOG.info(e);
			throw new RuntimeException(e);
		}

		return responseHeaderAndResponseBodyTable;
	}	
	
	/**<pre>
	 * Returns both JSON response and response header, for Json 'PATCH' method
	 * - Json response and response headers are present in java.util.Hashtable
	 * - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url
	 * @param input input restWebservices request input data 
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public Hashtable<String, String> patchJSONResponseHeadersAndBody(String url, String input,
			Hashtable<String, String> requestHeaders, Hashtable<String, String> urlQueryParameters) throws Throwable {
		Hashtable<String, String> responseHeaderAndResponseBodyTable = patchResponseHeadersAndBody(url, input,
				requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_JSON);
		return responseHeaderAndResponseBodyTable;
	}

	/**<pre>
	 * Returns both XML response and response header, for xml 'PATCH' method
	 * - XML response and response headers are present in java.util.Hashtable
	 * - To fetch the response, you can use hashTable.get(RestConstants.RESPONSE_BODY)
	 * </pre>
	 * @param url End point url<br>
	 * @param input restWebservices request input data 
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters, <br>
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public Hashtable<String, String> patchXMLResponseHeadersAndBody(String url, String input,
			Hashtable<String, String> requestHeaders, Hashtable<String, String> urlQueryParameters) throws Throwable {
		Hashtable<String, String> responseHeaderAndResponseBodyTable = patchResponseHeadersAndBody(url, input,
				requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return responseHeaderAndResponseBodyTable;
	}	
	
	/**
	 * Returns Text response as String(if there is a response), for Text 'PATCH' method
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters,<br> 
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String : Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public String patchTextResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = patchResponseAsString(url, input, requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_TEXT);
		return clientResponse;
	}
	
	// Only for internal use with in the class
	/**
	 * <pre>
	 * Generic method to process the "PATCH" request and return the response(i.e. Json/XML) in string 
	 *  -Only for internal use with in the class
	 * </pre>
	 * @param url End point url
	 * @param requestHeaders Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters,<br> 
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @param contentType java.lang.String<br>
	 *                      Ex: contentType = "application/json"
	 * @return responseString java.lang.String
	 * @throws Throwable java.lang.Throwable
	 */
	private String patchResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters, String contentType) throws Throwable {
		String responseString = null;

		ClientResponse clientResponse = patchClientResponse(url, input, requestHeaders, urlQueryParameters, contentType);
		try{
			responseString = clientResponse.getEntity(String.class);
		}catch (Exception e) {
			responseString = "";
		}
		return responseString;
	}
	
	/**
	 * Returns JSON response as String(if there is a response), for JSON 'PATCH' method
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters,<br> 
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String : Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public String patchJSONResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = patchResponseAsString(url, input, requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_JSON);
		return clientResponse;
	}

	/**
	 * Returns XML response as String(if there is a response), for XML 'PATCH' method
	 * 
	 * @param url End point url
	 * @param input input restWebservices request input data
	 * @param headerParameters Request header information,<br> 
	 * 							 if 'headerParameters' are null then 'headerParameters' are ignored. 
	 * @param urlQueryParameters url parameters,<br> 
	 * 							   if 'urlQueryParameters' are null then 'urlQueryParameters' are ignored.  
	 * @return java.lang.String : Response in String format
	 * @throws Throwable java.lang.Throwable
	 */
	public String patchXMLResponseAsString(String url, String input, Hashtable<String, String> requestHeaders,
			Hashtable<String, String> urlQueryParameters) throws Throwable {
		String clientResponse = patchResponseAsString(url, input, requestHeaders, urlQueryParameters, RESTConstants.APPLICATION_XML);
		return clientResponse;
	}
	
	/**
	 * Asserts the condition
	 * @param condition of boolean
	 * @param message to be included in the execution report
	 * @return boolean value indicating success of the operation
	 */
	public boolean assertTrue(boolean condition, String message)  {
		try {
			if (condition)
			return true;
			else
				return false;
		} catch (Exception e) {
			LOG.info("++++++++++++++++++++++++++++Catch Block Start+++++++++++++++++++++++++++++++++++++++++++");
			LOG.info("Class name" + getCallerClassName() + "Method name : " + getCallerMethodName());
			LOG.info("++++++++++++++++++++++++++++Catch Block End+++++++++++++++++++++++++++++++++++++++++++");
			LOG.error(Log4jUtil.getStackTrace(e));
			return false;
		} finally{
			if (!condition) {
				failureReport("Expected :: " + message, message + " is :: " + condition);
			} else {
				successReport("Expected :: " + message, message + " is :: " + condition);
			}
	}
	}
	
	/*
	 * Ignore the Certification while executing rest calls
	 */

	/**
	 * Takes a json schema document and returns Pojo Java classes
	 * 
	 * @param jsonBeanName - The Pojo class name<br> 
	 * @param jsonSchemaFQName - json schema fully qualified name, e.g. "C:\temp\rest\jsonschemas\paymentRequest.json"<br> 
	 * @param packageName - Package name of the pojo java class, e.g. "com.cubic.rest.beans"<br> 
	 * @param destDir - Destination directory of the package, e.g. "src\\main\\java"<br> 

	 */
	public void jsonBeanGenerator(String jsonBeanName, String jsonSchemaFQName, String packageName, String destDir){
		JCodeModel codeModel = new JCodeModel();
		try {
			URL source = new URL("file:///"+jsonSchemaFQName);
			GenerationConfig config = new DefaultGenerationConfig() {
				@Override
				public boolean isGenerateBuilders() { // set config option by overriding method
					return true;
				}
				public SourceType getSourceType(){
					return SourceType.JSONSCHEMA;
				}
			};

			SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), new SchemaGenerator());
			mapper.generate(codeModel, jsonBeanName, packageName, source);

			codeModel.build(new File(destDir));

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}  
	}
	
	public Client hostIgnoringClient() {
	    try{
	   
	    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				// TODO Auto-generated method stub
				return new X509Certificate[0];
			}
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				// TODO Auto-generated method stub
			}
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				// TODO Auto-generated method stub
				
			}
		}};
	    
	        SSLContext sslcontext = SSLContext.getInstance( "TLS" );
	        sslcontext.init( null, trustAllCerts, new java.security.SecureRandom());
	        DefaultClientConfig config = new DefaultClientConfig();
	        Map<String, Object> properties = config.getProperties();
	        HTTPSProperties httpsProperties = new HTTPSProperties(
	                new HostnameVerifier()
	                {
	                    @Override
	                    public boolean verify( String s, SSLSession sslSession )
	                    {
	                        return true;
	                    }
	                }, sslcontext
	        );
	        properties.put( HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, httpsProperties );
	        config.getClasses().add( JacksonJsonProvider.class );
	        return Client.create(config);
	    }
	    catch ( KeyManagementException | NoSuchAlgorithmException e )
	    {
	        throw new RuntimeException( e );
	    }
	}

	
}