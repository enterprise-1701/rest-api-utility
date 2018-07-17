package com.cubic.accelerators;

import java.time.Instant;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Objects;

import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.cubic.database.DataBaseUtil;
import com.cubic.genericutils.GenericConstants;
import com.cubic.genericutils.TimeUtil;
import com.cubic.logutils.Log4jUtil;
import com.cubic.reportengine.bean.CustomReportBean;
import com.cubic.reportengine.bean.DetailedReportBean;
import com.cubic.reportengine.report.CustomReports;
import com.cubic.testrail.TestRailUtil;

/**
 * BaseRestTest have all the generic methods to execute to drive the test cases.
 * 
 * @since 1.0
 */
public class RESTEngine{

	private Hashtable<String, RESTActions> restActionsList =  null;
	private Hashtable<String , String> propTable = GenericConstants.GENERIC_FW_CONFIG_PROPERTIES;
	private String testRailProjectID;
	private String testRailSuiteID;
	private String testRailRunID;
	private boolean testRailFlag;
	
	/**
	 * This method will be executed before the suite.
	 * CustomReport folder structure is created in this phase.
	 *
	 * @param context
	 * @throws Exception
	 */
	@BeforeSuite
	@Parameters({"projectID","suiteID","runID","test_Rail_Integration_Enable_Flag","runName"})
	public void beforeSuite(ITestContext context,
			@Optional String projectID,
			@Optional String suiteID,
			@Optional String runID,
			@Optional String test_Rail_Integration_Enable_Flag,
			@Optional String runName) throws Exception {
		Log4jUtil.configureLog4j(GenericConstants.LOG4J_FILEPATH);
		
		
		try{
		testRailProjectID=TestRailUtil.getTestRailProjectID(projectID);
		testRailSuiteID=TestRailUtil.getTestRailSuiteID(suiteID);
		testRailFlag=TestRailUtil.getTestRailEnableFlag(test_Rail_Integration_Enable_Flag,testRailProjectID,testRailSuiteID);
		System.out.println("::::Before Suite::::testRailFlag "+testRailFlag);
		
		// Create custom report folder structure.
				
		createFolderStructureForCustomReport(context,testRailFlag);
		if(testRailFlag){
			
			if((runID==null) || (runID.equalsIgnoreCase("0") || runID.equalsIgnoreCase("%runID%") || runID.equalsIgnoreCase("${runID}") )){
				    // Need to generate the Test Run JSON (with Test Cases) to filter out the test cases that need to be added to the TestRail Run
				    TestRailUtil.generateTestRunJSONFromTestNG(context, testRailProjectID, testRailSuiteID);
				    
			        // Generate Test Run in TestRail
					TestRailUtil.generateTestRunsForTestCases(testRailProjectID, testRailSuiteID, customReports.getCustomReportBean().getSuiteStartDateAndTime(), runName);
			}else if(runID!=null && !(runID.equals("0"))){
				testRailRunID = runID;
				TestRailUtil.setExistingTestRunID(testRailRunID);
			}
			
			}
			
		}catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	/**
	 * This method will be executed after the suite.
	 * Generating summary report and freeing up the custom report instances are done in this phase.
	 * 
	 * @param context
	 * @throws Exception
	 */
	@AfterSuite
	@Parameters({"projectID","suiteID"})
	public void afterSuite(ITestContext context,
			@Optional String projectID,
			@Optional String suiteID) throws Exception {
		
		// Update test execution results into the Test Run under Test Rail project
		System.out.println("::::After Suite::::testRailFlag"+testRailFlag);
		// Generates the Summary report.
				generateSummaryReport(context,testRailFlag);
		
		if(testRailFlag){
			try{
				if((propTable.get("Test_Rail_Results_Update_End_of_Suite")==null)||(propTable.get("Test_Rail_Results_Update_End_of_Suite").equalsIgnoreCase("true"))){
					TestRailUtil.updateTestResultsinTestRail();
				}
				
			}catch (Exception e) {
		        e.printStackTrace();
		    }
		}

		cleanUpCustomReports();
	}

	/**
	 * This method will be executed before the class 
	 * 
	 * @param context
	 * @throws Exception
	 */
	@BeforeClass
	public void beforeClass(ITestContext context) throws Exception {
		customReports = (CustomReports) context.getAttribute("customReports");
		
		restActionsList = new Hashtable<String, RESTActions>();
	}
	
	/**
	 * Prerequisite setup at test method level(@Test method level). Call to this
	 * method should be the first line in the test method(i.e. in @Test)
	 * 
	 * Ex: 	  String testCaseName = "<<TESTCASE ID>> : <<TESTCASE DESCRIPTION>>"
	 *        RESTActions restActions = setupAutomationTest(context, testCaseName);
	 * 
	 * Note: testCaseName(ex: "TC 01 : Sample Test") should be same when you are calling the method 'setupWebTest' and 'teardownWebTest'
	 * 
	 * @param context
	 * @param testCaseName
	 * @return
	 * @throws Exception
	 */
	public RESTActions setupAutomationTest(ITestContext context, String testCaseName) throws Exception {
		RESTActions restActions = null;
		try{
		    DataBaseUtil.resetConnectionCount();      // Reset any database connection counters before every test starts
		    
			// For generating the detailed report for test case(i.e. test method)
			setupReport(context, testCaseName);

			restActions = getRestActions(testCaseName);
			
			restActionsList.put(testCaseName, restActions);
			
		}catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Unable to execute the method 'setupRestTest'");
		}
		
		return restActions;

	}	
	
	/**
	 * Closing every associated instances related to the test(i.e. @Test method). 
	 * Call to this method should be the last line in the test method(i.e. in @Test), should be written in finally block.
	 * 
	 * Ex:   
	 * String testCaseName = "<<TESTCASE ID>> : <<TESTCASE DESCRIPTION>>"
	 * teardownAutomationTest(context, testCaseName);
	 * 
	 * Note: testCaseName(ex: "TC 01 : Sample Test") should be same when you are calling the method 'setupWebTest' and 'teardownWebTest'
	 * 
	 * @param context
	 * @param testCaseName
	 * @throws Exception
	 */
	public void teardownAutomationTest(ITestContext context, String testCaseName) throws Exception {
		try{
			//String testCaseName = getClassNameWithMethodName(method, description);

			// Captures the test case execution details like time taken for
			// executing the test case, test case status pass/fail, etc.
			// This details will be used for generating summary report.
			System.out.println("::::testRailFlag value in teardownAutomationTest::::::"+testRailFlag);
			System.out.println("::::testRailFlag value in Test Util::::::"+TestRailUtil.testRailFlag);
			teardownReport(context, testCaseName,TestRailUtil.testRailFlag);
			
			restActionsList.remove(testCaseName);
			// If test is fail then assert false, this is for testNG
			assert !Objects.equals(this.customReports.getCustomReportBean().getDetailedReportMap().get(testCaseName).getOverallStatus().toLowerCase(), "fail");
		}catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Unable to execute the method 'teardownWebTest'");
		}
	}	
	
	private RESTActions getRestActions(String testCaseName) throws Exception {
		RESTActions actionEngineRest = null; 
		try{
			actionEngineRest = new RESTActions(customReports, testCaseName);
		}catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Unable to execute the method 'getRestActions'");
		}
		return actionEngineRest;
	}		

	public CustomReports customReports = null;
	
	/**
	 * Creates the custom report folder structure. 
	 * This method should be called in at suite level(i.e.before suite), 
	 * since custom folder structure before starting testing.
	 * 
	 * @param context
	 * @return boolean
	 * @throws Exception 
	 */
	protected void createFolderStructureForCustomReport(ITestContext context,boolean testRailFlag) throws Exception {
		try {
			customReports = new CustomReports();
			System.out.println("::::Create Folder Structure::::testRailFlag"+testRailFlag);
			customReports.createFolderStructureForCustomReport(testRailFlag);
			context.setAttribute("customReports", customReports);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Unable to create the folder structure for custom report");
		}
	}	
	
	/**
	 * Generates Summary Report.
	 * This method need to be called at the end of the suite.
	 * 
	 * @param context
	 * @throws Exception
	 */
	protected void generateSummaryReport(ITestContext context,boolean testRailFlag) throws Exception{
		customReports = (CustomReports) context.getAttribute("customReports");
		System.out.println("::::Generate Summary Report::::testRailFlag"+testRailFlag);
		customReports.generateSummaryReport(testRailFlag);
	}
	
	/** Initialize the detailed report for the test case(at test method level @Test) 
	 * 
	 * @param context
	 * @param testCaseName
	 * @return
	 */
	private boolean setupReport(ITestContext context, String testCaseName){
		boolean flag = false;
		try{
			customReports = (CustomReports) context.getAttribute("customReports");
			
			CustomReportBean customReportBean = customReports.getCustomReportBean();
			LinkedHashMap<String, DetailedReportBean> detailedReportMap = customReportBean.getDetailedReportMap();
			
			//Check test case is already present.
			if(detailedReportMap.get(testCaseName) == null){
				
				//Create the detailed report, holds information related to test case.
				 DetailedReportBean detailedReportBean = new DetailedReportBean();
				 detailedReportBean.setTestCaseName(testCaseName); 
				 detailedReportBean.setTestCaseStartTime(TimeUtil.getCurrentInstant());
				 
				 //Add the detailed report map having test case information to detailed report map.
				 detailedReportMap.put(testCaseName, detailedReportBean);
				 
				 customReports.intializeDetailedReport(testCaseName);
				 
				 customReportBean.setDetailedReportMap(detailedReportMap);
				 context.setAttribute("customReports", customReports);
				 System.out.println("Test Case ID ::::"+testCaseName.split(":")[0]);
			}			
			
			flag = true;
		}catch (Exception e) {
			e.printStackTrace();
			flag = false;
		}
		return flag;
	}
	
	/**
	 *  Collects the information like test case status pass/fail, total time taken to execute the test case after executing the test case
	 *  (i.e. at test method level @Test), this information will be used for generating the summary report. 
	 *  
	 * @param context
	 * @param testCaseName
	 * @return
	 */
	private boolean teardownReport(ITestContext context, String testCaseName,boolean testRailFlag){
		boolean flag = false;
		String testCaseID=null;
		String finalResult=null;
		String comment=null;
		System.out.println("::::testRailFlag value in teardownReport::::::"+testRailFlag);
		try{
			CustomReports customReports =(CustomReports) context.getAttribute("customReports");
			CustomReportBean customReportBean = customReports.getCustomReportBean();

			LinkedHashMap<String, DetailedReportBean> detailedReportMap = customReportBean.getDetailedReportMap();
			
			DetailedReportBean detailedReportBean = detailedReportMap.get(testCaseName);
			if(detailedReportBean != null){
				
				Instant endTime = TimeUtil.getCurrentInstant();
				Instant startTime = detailedReportBean.getTestCaseStartTime();
				String testCaseTotalTime = TimeUtil.getTimeDifference(startTime, endTime);
				detailedReportBean.setTestCaseEndTime(endTime);
				detailedReportBean.setTotalTimeForTestCase(testCaseTotalTime);
				
				long overallExecutionTimeInMillis = (long) customReportBean.getOverallExecutionTimeInMillis();
				int totalTestScriptsPassed = (int) customReportBean.getTotalTestScriptsPassed();
				int totalTestScriptsFailed = (int) customReportBean.getTotalTestScriptsFailed();
			
				if(GenericConstants.TEST_CASE_PASS.equalsIgnoreCase(detailedReportBean.getOverallStatus())){
					totalTestScriptsPassed = totalTestScriptsPassed + 1;
				}else{
					totalTestScriptsFailed = totalTestScriptsFailed + 1;
				}
				overallExecutionTimeInMillis = overallExecutionTimeInMillis + TimeUtil.getTimeDifferenceInMillis(startTime, endTime);

				customReportBean.setOverallExecutionTimeInMillis(overallExecutionTimeInMillis);
				customReportBean.setTotalTestScriptsPassed(totalTestScriptsPassed);
				customReportBean.setTotalTestScriptsFailed(totalTestScriptsFailed);
				context.setAttribute("customReports", customReports);
				testCaseID=detailedReportBean.getTestCaseID();
				finalResult=detailedReportBean.getOverallStatus();
				comment=detailedReportBean.getFailStepDescription();
			}			
			
			flag = true;
		}catch (Exception e) {
			e.printStackTrace();
			flag = false;
		}finally{
			System.out.println("Test Case ID ::::"+testCaseID);
			System.out.println("Test Status :::::"+finalResult);
			
			System.out.println("::::Tear Down Report::::testRailFlag "+testRailFlag);
			boolean testResultsUpdateFlag=false;
			if((propTable.get("Test_Rail_Results_Update_End_of_Suite")==null)||(propTable.get("Test_Rail_Results_Update_End_of_Suite").equalsIgnoreCase("true"))){
				testResultsUpdateFlag=false;
			}else if(propTable.get("Test_Rail_Results_Update_End_of_Suite").equalsIgnoreCase("false")){
				testResultsUpdateFlag=true;
			}
			
			if(testRailFlag){
				try{
					if(testResultsUpdateFlag){
						if(comment==null){
							comment="";
						}
						TestRailUtil.updateTestResultinTestRail(testCaseID,finalResult,comment);
					}
					
				}catch (Exception e) {
			        e.printStackTrace();
			    }
			}
		}
		
		return flag;
	}

	/** Frees up the customReport instance.
	 *  This method should be called in after suite(i.e. at the end of the suite.) 
	 * 
	 */
	protected void cleanUpCustomReports() {
		customReports = null;
	}	
}
