package gov.healthit.chpl.web.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

import gov.healthit.chpl.auth.permission.GrantedPermission;
import gov.healthit.chpl.auth.user.JWTAuthenticatedUser;
import gov.healthit.chpl.caching.UnitTestRules;
import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.domain.CQMResultCertification;
import gov.healthit.chpl.domain.CQMResultDetails;
import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.MeaningfulUseUser;
import gov.healthit.chpl.web.controller.results.MeaningfulUseUserResults;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { gov.healthit.chpl.CHPLTestConfig.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class })
@DatabaseSetup("classpath:data/testData.xml") 
public class CertifiedProductControllerTest {

	private static final String CSV_SEPARATOR = ",";
	private static final Logger logger = LogManager.getLogger(CertifiedProductControllerTest.class);
	@Rule
    @Autowired
    public UnitTestRules cacheInvalidationRule;
	
	@Autowired
	CertifiedProductController certifiedProductController;
	
	private static JWTAuthenticatedUser adminUser;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		adminUser = new JWTAuthenticatedUser();
		adminUser.setFirstName("Administrator");
		adminUser.setId(-2L);
		adminUser.setLastName("Administrator");
		adminUser.setSubjectName("admin");
		adminUser.getPermissions().add(new GrantedPermission("ROLE_ADMIN"));
	}
	
	/** 
	 * Given that a user with ROLE_ONC_STAFF or ROLE_ADMIN has uploaded a CSV with meaningfulUseUser counts (passed in as MultipartFile file)
	 * When the UI calls the API at /uploadMeaningfulUse
	 * When the CSV contains incorrectly named headers
	 * When the CSV contains some incorrect CHPLProductNumbers
	 * When the CSV contains 2014 & 2015 edition CHPL Product Numbers
	 * When the CSV contains a row with leading and trailing spaces for CHPL Product Number
	 * When the CSV contains a duplicate CHPL Product Number
	 * Then the API parses the contents of the csv
	 * Then the API updates the meaningfulUseUser count for each CHPL Product Number/Certified Product
	 * Then the API returns MeaningfulUseUserResults to the UI as a JSON response
	 * Then the MeaningfulUseUserResults contains an array with unsuccessful updates
	 * Then the API trims leading and trailing spaces and successfully updates meaningfulUseUsers for the CHPLProductNumber
	 * Then the duplicate CHPL Product Number results in an error added to the results errors array
	 * Then the certifiedProductId is updated for non-error results
	 * @throws IOException 
	 * @throws JSONException 
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_uploadMeaningfulUseUsers_returnsMeaningfulUseUserResults() throws EntityRetrievalException, EntityCreationException, IOException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		logger.info("Running test_uploadMeaningfulUseUsers_returnsMeaningfulUseUserResults");
		
		// Create CSV input for API
		MeaningfulUseUser meaningfulUseUser1 = new MeaningfulUseUser("CHP-024050", 10L); // MeaningfulUseUser 0
		MeaningfulUseUser meaningfulUseUser2 = new MeaningfulUseUser("CHP-024051", 20L); // MeaningfulUseUser 1
		MeaningfulUseUser meaningfulUseUser3 = new MeaningfulUseUser(" CHP-024052 ", 30L); // MeaningfulUseUser 2
		MeaningfulUseUser meaningfulUseUser4 = new MeaningfulUseUser(" 15.01.01.1009.EIC08.36.1.1.160402 ", 40L); // MeaningfulUseUser 3
		MeaningfulUseUser meaningfulUseUser5 = new MeaningfulUseUser("wrongChplProductNumber", 50L); // Errors 0
		MeaningfulUseUser meaningfulUseUser6 = new MeaningfulUseUser(" CHPL-024053 ", 60L); // Errors 1
		MeaningfulUseUser meaningfulUseUser7 = new MeaningfulUseUser("15.02.03.9876.AB01.01.0.1.123456", 70L); // Errors 2
		MeaningfulUseUser meaningfulUseUser8 = new MeaningfulUseUser("15.01.01.1009.EIC08.36.1.1.160402", 70L); // Errors 3 (because duplicate of MeaningfulUseUser 3
		logger.info("Created 8 of MeaningfulUseUser to be updated in the database");
		
		List<MeaningfulUseUser> meaningfulUseUserList = new ArrayList<MeaningfulUseUser>();
		meaningfulUseUserList.add(meaningfulUseUser1);
		meaningfulUseUserList.add(meaningfulUseUser2);
		meaningfulUseUserList.add(meaningfulUseUser3);
		meaningfulUseUserList.add(meaningfulUseUser4);
		meaningfulUseUserList.add(meaningfulUseUser5);
		meaningfulUseUserList.add(meaningfulUseUser6);
		meaningfulUseUserList.add(meaningfulUseUser7);
		meaningfulUseUserList.add(meaningfulUseUser8);
		logger.info("Populated List of MeaningfulUseUser from 8 MeaningfulUseUser");
		
		File file = new File("testMeaningfulUseUsers.csv");
		
		try
        {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getName()), "UTF-8"));
            StringBuffer headerLine = new StringBuffer();
            headerLine.append("cipl_product_number");
            headerLine.append(CSV_SEPARATOR);
            headerLine.append("n_meaningful_use");
            bw.write(headerLine.toString());
            bw.newLine();
            for (MeaningfulUseUser muUser : meaningfulUseUserList)
            {
                StringBuffer oneLine = new StringBuffer();
                oneLine.append(muUser.getProductNumber() != null ? muUser.getProductNumber() : "");
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(muUser.getNumberOfUsers() != null ? muUser.getNumberOfUsers() : "");
                bw.write(oneLine.toString());
                bw.newLine();
            }
            bw.flush();
            bw.close();
        }
        catch (UnsupportedEncodingException e) {}
        catch (FileNotFoundException e){}
        catch (IOException e){}
		logger.info("Wrote meaningfulUseUserList to testMeaningfulUseUsers.csv");
	
		FileInputStream input = new FileInputStream(file);
		logger.info("Create FileInputStream from csv file to populate MultipartFile");
		MultipartFile multipartFile = new MockMultipartFile("file", file.getName(), "text/csv", IOUtils.toByteArray(input));
		logger.info("Create & populate MultipartFile from csv file");
		
		MeaningfulUseUserResults apiResult = new MeaningfulUseUserResults();
		try {
			apiResult = certifiedProductController.uploadMeaningfulUseUsers(multipartFile);
		} catch (MaxUploadSizeExceededException e) {
			e.printStackTrace();
		} catch (ValidationException e) {
			e.printStackTrace();
		}
		
		input.close();
		file.delete();
		
		assertTrue("MeaningfulUseUserResults should return 4 but returned " + apiResult.getMeaningfulUseUsers().size(), apiResult.getMeaningfulUseUsers().size() == 4);
		assertTrue("Errors should return 4 but returned " + apiResult.getErrors().size(), apiResult.getErrors().size() == 4);
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(0).getProductNumber() + " but should return CHP-024050", 
				apiResult.getMeaningfulUseUsers().get(0).getProductNumber().equals("CHP-024050"));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(0).getNumberOfUsers() + " but should return " + 10L, 
				apiResult.getMeaningfulUseUsers().get(0).getNumberOfUsers().equals(10L));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(0).getCertifiedProductId() + " but should return certifiedProductId 1", 
				apiResult.getMeaningfulUseUsers().get(0).getCertifiedProductId() == 1);
		assertTrue("MeaningfulUseUserResults should return correct product number", apiResult.getMeaningfulUseUsers().get(1).getProductNumber().equals("CHP-024051"));
		assertTrue("MeaningfulUseUserResults should return correct numMeaningfulUseUsers", apiResult.getMeaningfulUseUsers().get(1).getNumberOfUsers().equals(20L));
		assertTrue("MeaningfulUseUserResults should return correct product number", apiResult.getMeaningfulUseUsers().get(2).getProductNumber().equals("CHP-024052"));
		assertTrue("MeaningfulUseUserResults should return correct numMeaningfulUseUsers", apiResult.getMeaningfulUseUsers().get(2).getNumberOfUsers().equals(30L));
		assertTrue("MeaningfulUseUserResults should return correct product number", apiResult.getMeaningfulUseUsers().get(3).getProductNumber().equals("15.01.01.1009.EIC08.36.1.1.160402"));
		assertTrue("MeaningfulUseUserResults should return correct numMeaningfulUseUsers", apiResult.getMeaningfulUseUsers().get(3).getNumberOfUsers().equals(40L));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(3).getProductNumber() + " but should return 15.01.01.1009.EIC08.36.1.1.160402", 
				apiResult.getMeaningfulUseUsers().get(3).getProductNumber().equals("15.01.01.1009.EIC08.36.1.1.160402"));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(3).getNumberOfUsers() + " but should return " + 40L, 
				apiResult.getMeaningfulUseUsers().get(3).getNumberOfUsers().equals(40L));
		assertTrue("MeaningfulUseUserResults errors array should return incorrect CHPL Product Number for row with {wrongChplProductNumber, 50L} but returned " 
		+ apiResult.getErrors().get(0).getError(), apiResult.getErrors().get(0).getError() != null);
		assertTrue("MeaningfulUseUserResults errors array should return incorrect CHPL Product Number for row with {CHPL-024053, 60L} but returned " 
				+ apiResult.getErrors().get(1).getError(), apiResult.getErrors().get(1).getError() != null);
		assertTrue("MeaningfulUseUserResults errors array for row {CHPL-024053, 60L} should have Product Number CHPL-024053 but has " 
				+ apiResult.getErrors().get(1).getProductNumber(), apiResult.getErrors().get(1).getProductNumber().equals("CHPL-024053"));
		assertTrue("MeaningfulUseUserResults errors array for row {CHPL-024053, 60L} should have num_meaningful_use 60L but has " 
				+ apiResult.getErrors().get(1).getNumberOfUsers(), apiResult.getErrors().get(1).getNumberOfUsers() == 60L);
		assertTrue("MeaningfulUseUserResults errors array should return incorrect CHPL Product Number for row with {15.02.03.9876.AB01.01.0.1.123456, 70L} but returned " 
				+ apiResult.getErrors().get(2).getError(), apiResult.getErrors().get(2).getError() != null);
		assertTrue("MeaningfulUseUserResults errors array for row {15.02.03.9876.AB01.01.0.1.123456, 70L} should have Product Number 15.02.03.9876.AB01.01.0.1.123456 but has " 
				+ apiResult.getErrors().get(2).getProductNumber(), apiResult.getErrors().get(2).getProductNumber().equals("15.02.03.9876.AB01.01.0.1.123456"));
		assertTrue("MeaningfulUseUserResults errors array for row {15.02.03.9876.AB01.01.0.1.123456, 70L} should have num_meaningful_use 70L but has " 
				+ apiResult.getErrors().get(2).getNumberOfUsers(), apiResult.getErrors().get(2).getNumberOfUsers() == 70L);
		assertTrue("MeaningfulUseUserResults errors array should return incorrect CHPL Product Number for row with {15.01.01.1009.EIC08.36.1.1.160402, 70L} but returned " 
				+ apiResult.getErrors().get(3).getError(), apiResult.getErrors().get(3).getError() != null);
		assertTrue("MeaningfulUseUserResults errors array for row {12.01.01.1234.AB01.01.0.1.123456, 70L} should have Product Number 15.01.01.1009.EIC08.36.1.1.160402 but has " 
				+ apiResult.getErrors().get(3).getProductNumber(), apiResult.getErrors().get(3).getProductNumber().equals("15.01.01.1009.EIC08.36.1.1.160402"));
		assertTrue("MeaningfulUseUserResults errors array for row {15.01.01.1009.EIC08.36.1.1.160402, 70L} should have num_meaningful_use 70L but has " 
				+ apiResult.getErrors().get(3).getNumberOfUsers(), apiResult.getErrors().get(3).getNumberOfUsers() == 70L);
	}
	
	/** 
	 * Given that a user with ROLE_ONC_STAFF or ROLE_ADMIN has uploaded a CSV with meaningfulUseUser counts (passed in as MultipartFile file)
	 * When the UI calls the API at /uploadMeaningfulUse
	 * When the CSV contains no header
	 * When the CSV contains a 2014 CHPL Product Number with a non-legacy format
	 * Then the API continues with updating the CHPL Product Numbers with their respective num_meaningful_users
	 * @throws IOException 
	 * @throws JSONException 
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_uploadMeaningfulUseUsers_noHeader_runsWithoutError() throws EntityRetrievalException, EntityCreationException, IOException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		logger.info("Running test_uploadMeaningfulUseUsers_returnsMeaningfulUseUserResults");
		
		// Create CSV input for API
		MeaningfulUseUser meaningfulUseUser1 = new MeaningfulUseUser("CHP-024050", 10L);
		MeaningfulUseUser meaningfulUseUser2 = new MeaningfulUseUser("CHP-024051", 20L);
		MeaningfulUseUser meaningfulUseUser3 = new MeaningfulUseUser("CHP-024052", 30L);
		MeaningfulUseUser meaningfulUseUser4 = new MeaningfulUseUser("15.01.01.1009.EIC08.36.1.1.160402", 40L);
		MeaningfulUseUser meaningfulUseUser5 = new MeaningfulUseUser("14.01.01.1009.EIC08.36.1.1.160402", 50L);
		logger.info("Created 5 of MeaningfulUseUser to be updated in the database");
		
		List<MeaningfulUseUser> meaningfulUseUserList = new ArrayList<MeaningfulUseUser>();
		meaningfulUseUserList.add(meaningfulUseUser1);
		meaningfulUseUserList.add(meaningfulUseUser2);
		meaningfulUseUserList.add(meaningfulUseUser3);
		meaningfulUseUserList.add(meaningfulUseUser4);
		meaningfulUseUserList.add(meaningfulUseUser5);
		logger.info("Populated List of MeaningfulUseUser from 5 MeaningfulUseUser");
		
		File file = new File("testMeaningfulUseUsers.csv");
		
		try
        {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getName()), "UTF-8"));
            for (MeaningfulUseUser muUser : meaningfulUseUserList)
            {
                StringBuffer oneLine = new StringBuffer();
                oneLine.append(muUser.getProductNumber() != null ? muUser.getProductNumber() : "");
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(muUser.getNumberOfUsers() != null ? muUser.getNumberOfUsers() : "");
                bw.write(oneLine.toString());
                bw.newLine();
            }
            bw.flush();
            bw.close();
        }
        catch (UnsupportedEncodingException e) {}
        catch (FileNotFoundException e){}
        catch (IOException e){}
		logger.info("Wrote meaningfulUseUserList to testMeaningfulUseUsers.csv");
	
		FileInputStream input = new FileInputStream(file);
		logger.info("Create FileInputStream from csv file to populate MultipartFile");
		MultipartFile multipartFile = new MockMultipartFile("file", file.getName(), "text/csv", IOUtils.toByteArray(input));
		logger.info("Create & populate MultipartFile from csv file");
		
		MeaningfulUseUserResults apiResult = new MeaningfulUseUserResults();
		try {
			apiResult = certifiedProductController.uploadMeaningfulUseUsers(multipartFile);
		} catch (MaxUploadSizeExceededException e) {
			e.printStackTrace();
		} catch (ValidationException e) {
			e.printStackTrace();
		}
		
		input.close();
		file.delete();
		
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().size() + " but should return 5 results", apiResult.getMeaningfulUseUsers().size() == 5);
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(0).getProductNumber() + " but should return CHP-024050", 
				apiResult.getMeaningfulUseUsers().get(0).getProductNumber().equals("CHP-024050"));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(0).getNumberOfUsers() + " but should return " + 10L, 
				apiResult.getMeaningfulUseUsers().get(0).getNumberOfUsers().equals(10L));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(0).getCertifiedProductId() + " but should return " + 1L, 
				apiResult.getMeaningfulUseUsers().get(0).getCertifiedProductId() == 1L);
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(1).getProductNumber() + " but should return CHP-024051", 
				apiResult.getMeaningfulUseUsers().get(1).getProductNumber().equals("CHP-024051"));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(1).getNumberOfUsers() + " but should return " + 20L, 
				apiResult.getMeaningfulUseUsers().get(1).getNumberOfUsers().equals(20L));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(1).getCertifiedProductId() + " but should return " + 2L, 
				apiResult.getMeaningfulUseUsers().get(1).getCertifiedProductId() == 2L);
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(2).getProductNumber() + " but should return CHP-024052", 
				apiResult.getMeaningfulUseUsers().get(2).getProductNumber().equals("CHP-024052"));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(2).getNumberOfUsers() + " but should return " + 30L, 
				apiResult.getMeaningfulUseUsers().get(2).getNumberOfUsers().equals(30L));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(2).getCertifiedProductId() + " but should return " + 3L, 
				apiResult.getMeaningfulUseUsers().get(2).getCertifiedProductId() == 3L);
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(3).getProductNumber() + " but should return 15.01.01.1009.EIC08.36.1.1.160402", 
				apiResult.getMeaningfulUseUsers().get(3).getProductNumber().equals("15.01.01.1009.EIC08.36.1.1.160402"));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(3).getNumberOfUsers() + " but should return " + 40L, 
				apiResult.getMeaningfulUseUsers().get(3).getNumberOfUsers().equals(40L));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(3).getCertifiedProductId() + " but should return " + 5L, 
				apiResult.getMeaningfulUseUsers().get(3).getCertifiedProductId() == 5L);
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(3).getProductNumber() + " but should return 14.01.01.1009.EIC08.36.1.1.160402", 
				apiResult.getMeaningfulUseUsers().get(4).getProductNumber().equals("14.01.01.1009.EIC08.36.1.1.160402"));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(4).getNumberOfUsers() + " but should return " + 50L, 
				apiResult.getMeaningfulUseUsers().get(4).getNumberOfUsers().equals(50L));
		assertTrue("MeaningfulUseUserResults returned " + apiResult.getMeaningfulUseUsers().get(4).getCertifiedProductId() + " but should return " + 1L, 
				apiResult.getMeaningfulUseUsers().get(4).getCertifiedProductId() == 1L);
	}
	
	/** 
	 * Given that a user with ROLE_ADMIN edits/updates an existing Certified Product
	 * When the UI calls the API at /certified_products/update
	 * When the user tries to update a Certified Product with ics = true and retired test tool = true and set ics = false
	 * Then the API returns an error
	 * @throws IOException 
	 * @throws JSONException 
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_updateCertifiedProduct_icsAndRetiredEqualTrue_icsFalseReturnsError() throws EntityRetrievalException, EntityCreationException, IOException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		
		CertifiedProductSearchDetails updateRequest = new CertifiedProductSearchDetails();
		updateRequest.setCertificationDate(1440090840000L);
		updateRequest.setId(1L); // Certified_product_id = 1 has icsCode = true and is associated with TestTool with id=2 that has retired = true
		updateRequest.setIcs(false);
		updateRequest.setChplProductNumber("CHP-024050");
		Map<String, Object> certStatus = new HashMap<String, Object>();
		certStatus.put("name", "Active");
		updateRequest.setCertificationStatus(certStatus);
		Map<String, Object> certificationEdition = new HashMap<String, Object>();
		String certEdition = "2015";
		certificationEdition.put("name", certEdition);
		updateRequest.setCertificationEdition(certificationEdition);
		List<CertificationResult> certificationResults = new ArrayList<CertificationResult>();
		CertificationResult cr = new CertificationResult();
		cr.setAdditionalSoftware(null);
		cr.setApiDocumentation(null);
		cr.setG1Success(false);
		cr.setG2Success(false);
		cr.setGap(null);
		cr.setNumber("170.314 (b)(6)");
		cr.setPrivacySecurityFramework(null);
		cr.setSed(null);
		cr.setSuccess(false);
		cr.setTestDataUsed(null);
		cr.setTestFunctionality(null);
		cr.setTestProcedures(null);
		cr.setTestStandards(null);
		cr.setTestTasks(null);
		cr.setTestToolsUsed(null);
		cr.setTitle("Inpatient setting only - transmission of electronic laboratory tests and values/results to ambulatory providers");
		cr.setUcdProcesses(null);
		certificationResults.add(cr);
		updateRequest.setCertificationResults(certificationResults);
		List<CQMResultDetails> cqms = new ArrayList<CQMResultDetails>();
		CQMResultDetails cqm = new CQMResultDetails();
		Set<String> versions = new HashSet<String>();
		versions.add("v0");
		versions.add("v1");
		versions.add("v2");
		versions.add("v3");
		versions.add("v4");
		versions.add("v5");
		cqm.setAllVersions(versions);
		cqm.setCmsId("CMS60");
		List<CQMResultCertification> cqmResultCertifications = new ArrayList<CQMResultCertification>();
		cqm.setCriteria(cqmResultCertifications);
		cqm.setDescription("Acute myocardial infarction (AMI) patients with ST-segment elevation on the ECG closest to arrival time receiving "
				+ "fibrinolytic therapy during the hospital visit"); 
		cqm.setDomain(null);
		cqm.setId(0L);
		cqm.setNqfNumber("0164");
		cqm.setNumber(null);
		cqm.setSuccess(true);
		Set<String> successVersions = new HashSet<String>();
		successVersions.add("v2");
		successVersions.add("v3");
		cqm.setSuccessVersions(successVersions);
		cqm.setTitle("Fibrinolytic Therapy Received Within 30 Minutes of Hospital Arrival");
		cqm.setTypeId(2L);
		cqms.add(cqm);
		updateRequest.setCqmResults(cqms);
		try {
			certifiedProductController.updateCertifiedProduct(updateRequest);
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		} catch (ValidationException e) {
			assertNotNull(e);
			assertTrue(e.getErrorMessages().contains("Cannot set Ics to false for a Certified "
					+ "Product with Ics=true and attested criteria that have a retired Test Tool. "
					+ "The following are attested criteria have a retired Test Tool: "
					+ "[170.314 (a)(1), 170.314 (a)(1)]"));
		}
		
	}
	
	/** 
	 * Given that a user with ROLE_ADMIN selects a Certified Product to view on the CHPL
	 * When the UI calls the API at /certified_products/{certifiedProductId}/details
	 * Then the API returns the Certified Product Details
	 * @throws IOException 
	 * @throws JSONException 
	 */
	@Transactional
	@Test
	public void test_getCertifiedProductById() throws EntityRetrievalException, EntityCreationException, IOException {
		Long cpId = 1L;
		CertifiedProductSearchDetails cpDetails = certifiedProductController.getCertifiedProductById(1L);
		assertNotNull(cpDetails);
	}
	
}
