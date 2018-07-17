package com.cubic.accelerators;

import java.time.Instant;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Objects;

import org.apache.log4j.Logger;
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
	private final Logger LOG = Logger.getLogger(this.getClass().getName());

	/**
	 * This method will be executed before the suite.
	 * CustomReport folder structure is created in this phase.
	 *
	 * @param context
	 *
	 */
	@BeforeSuite(alwaysRun=true)
	@Parameters({"projectID","suiteID","runID","test_Rail_Integration_Enable_Flag","runName"})
	public void beforeSuite(ITestContext context,
			@Optional String projectID,
			@Optional String suiteID,
			@Optional String runID,
			@Optional String test_Rail_Integration_Enable_Flag,
			@Optional String runName) {
		Log4jUtil.configureLog4j(GenericConstants.LOG4J_FILEPATH);

		try{
		testRailProjectID=TestRailUtil.getTestRailProjectID(projectID);
		testRailSuiteID=TestRailUtil.getTestRailSuiteID(suiteID);
		testRailFlag=TestRailUtil.getTestRailEnableFlag(test_Rail_Integration_Enable_Flag,testRailProjectID,testRailSuiteID);
		LOG.info("::::Before Suite::::testRailFlag " + testRailFlag);

		// Create custom report folder structure.

		createFolderStructureForCustomReport(context, testRailFlag);
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
			LOG.error(Log4jUtil.getStackTrace(e));
			throw new RuntimeException(e);
	    }
	}

	/**
	 * This method will be executed after the suite.
	 * Generating summary report and freeing up the custom report instances are done in this phase.
	 *
	 * @param context
	 * @throws Exception
	 */
	@AfterSuite(alwaysRun=true)
	@Parameters({"projectID","suiteID"})
	public void afterSuite(ITestContext context,
			@Optional String projectID,
			@Optional String suiteID) throws Exception {

		// Update test execution results into the Test Run under Test Rail project
		LOG.info("::::After Suite::::testRailFlag " + testRailFlag);
		// Generates the Summary report.
				generateSummaryReport(context,testRailFlag);

		if(testRailFlag){
			try{
				if((propTable.get("Test_Rail_Results_Update_End_of_Suite")==null)||(propTable.get("Test_Rail_Results_Update_End_of_Suite").equalsIgnoreCase("true"))){
					TestRailUtil.updateTestResultsinTestRail();
				}

			}catch (Exception e) {
				LOG.error(Log4jUtil.getStackTrace(e));
				throw new RuntimeException(e);
			}
		}

		cleanUpCustomReports();
	}

	/**
	 * This method will be executed before the class
	 *
	 * @param context
	 *
	 */
	@BeforeClass(alwaysRun=true)
	public void beforeClass(ITestContext context) {
		customReports = (CustomReports) context.getAttribute(RESTConstants.CUSTOM_REPORTS);
		restActionsList = new Hashtable<>();
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
	public RESTActions setupAutomationTest(ITestContext context, String testCaseName) {
		RESTActions restActions;
		try{
		    DataBaseUtil.resetConnectionCount();      // Reset any database connection counters before every test starts
			// For generating the detailed report for test case(i.e. test method)
			setupReport(context, testCaseName);
			restActions = getRestActions(testCaseName);
			restActionsList.put(testCaseName, restActions);

		}catch (Exception e) {
			LOG.error(Log4jUtil.getStackTrace(e));
			throw new RuntimeException(e);
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
	 */
	public void teardownAutomationTest(ITestContext context, String testCaseName) {
		try{
			//String testCaseName = getClassNameWithMethodName(method, description);

			// Captures the test case execution details like time taken for
			// executing the test case, test case status pass/fail, etc.
			// This details will be used for generating summary report.
			LOG.info("::::testRailFlag value in teardownAutomationTest:::::: " + testRailFlag);
			LOG.info("::::testRailFlag value in Test Util:::::: " + TestRailUtil.testRailFlag);
			teardownReport(context, testCaseName, TestRailUtil.testRailFlag);

			restActionsList.remove(testCaseName);

			// If test is fail then assert false, this is for testNG
			assert !Objects.equals(this.customReports.getCustomReportBean().getDetailedReportMap().get(testCaseName).getOverallStatus().toLowerCase(), "fail");

		}catch (Exception e) {
			LOG.error(Log4jUtil.getStackTrace(e));
			throw new RuntimeException(e);
		}
	}

	private RESTActions getRestActions(String testCaseName)  {
		RESTActions actionEngineRest;
		try{
			actionEngineRest = new RESTActions(customReports, testCaseName);
		}catch (Exception e) {
			LOG.error(Log4jUtil.getStackTrace(e));
			throw new RuntimeException(e);
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
	protected void createFolderStructureForCustomReport(ITestContext context, boolean testRailFlag)  {
		try {
			customReports = new CustomReports();
			LOG.info("::::Create Folder Structure::::testRailFlag " + testRailFlag);
			customReports.createFolderStructureForCustomReport(testRailFlag);
			context.setAttribute(RESTConstants.CUSTOM_REPORTS, customReports);
		}catch (Exception e) {
			LOG.error(Log4jUtil.getStackTrace(e));
			throw new RuntimeException(e);
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
		customReports = (CustomReports) context.getAttribute(RESTConstants.CUSTOM_REPORTS);
		LOG.info("::::Generate Summary Report::::testRailFlag " + testRailFlag);
		customReports.generateSummaryReport(testRailFlag);
	}

	/** Initialize the detailed report for the test case(at test method level @Test)
	 *
	 * @param context
	 * @param testCaseName
	 * @return
	 */
	private boolean setupReport(ITestContext context, String testCaseName){
		boolean flag;
		try{
			customReports = (CustomReports) context.getAttribute(RESTConstants.CUSTOM_REPORTS);

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
				 context.setAttribute(RESTConstants.CUSTOM_REPORTS, customReports);
				 LOG.info("Test Case ID :::: "+testCaseName.split(":")[0]);
			}

			flag = true;
		}catch (Exception e) {
			LOG.error(Log4jUtil.getStackTrace(e));
			flag = false;
			throw new RuntimeException(e);
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
	private boolean teardownReport(ITestContext context, String testCaseName, boolean testRailFlag){
		boolean flag;
		String testCaseID=null;
		String finalResult=null;
		String comment=null;
		LOG.info("::::testRailFlag value in teardownReport:::::: "+testRailFlag);
		try{
			CustomReports customReports =(CustomReports) context.getAttribute(RESTConstants.CUSTOM_REPORTS);
			CustomReportBean customReportBean = customReports.getCustomReportBean();

			LinkedHashMap<String, DetailedReportBean> detailedReportMap = customReportBean.getDetailedReportMap();

			DetailedReportBean detailedReportBean = detailedReportMap.get(testCaseName);
			if(detailedReportBean != null){

				Instant endTime = TimeUtil.getCurrentInstant();
				Instant startTime = detailedReportBean.getTestCaseStartTime();
				String testCaseTotalTime = TimeUtil.getTimeDifference(startTime, endTime);
				detailedReportBean.setTestCaseEndTime(endTime);
				detailedReportBean.setTotalTimeForTestCase(testCaseTotalTime);

				long overallExecutionTimeInMillis = customReportBean.getOverallExecutionTimeInMillis();
				int totalTestScriptsPassed = customReportBean.getTotalTestScriptsPassed();
				int totalTestScriptsFailed = customReportBean.getTotalTestScriptsFailed();

				if(GenericConstants.TEST_CASE_PASS.equalsIgnoreCase(detailedReportBean.getOverallStatus())){
					totalTestScriptsPassed = totalTestScriptsPassed + 1;
				}else{
					totalTestScriptsFailed = totalTestScriptsFailed + 1;
				}
				overallExecutionTimeInMillis = overallExecutionTimeInMillis + TimeUtil.getTimeDifferenceInMillis(startTime, endTime);

				customReportBean.setOverallExecutionTimeInMillis(overallExecutionTimeInMillis);
				customReportBean.setTotalTestScriptsPassed(totalTestScriptsPassed);
				customReportBean.setTotalTestScriptsFailed(totalTestScriptsFailed);
				context.setAttribute(RESTConstants.CUSTOM_REPORTS, customReports);
				testCaseID=detailedReportBean.getTestCaseID();
				finalResult=detailedReportBean.getOverallStatus();
				comment=detailedReportBean.getFailStepDescription();
			}

			flag = true;
		}catch (Exception e) {
			LOG.error(Log4jUtil.getStackTrace(e));
			flag = false;
			throw new RuntimeException(e);
		}finally{
			LOG.info("Test Case ID :::: " + testCaseID);
			LOG.info("Test Status ::::: " + finalResult);
			LOG.info("::::Tear Down Report::::testRailFlag " + testRailFlag);
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
					LOG.error(Log4jUtil.getStackTrace(e));
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
