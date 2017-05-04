package gov.healthit.chpl.web.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

import gov.healthit.chpl.auth.permission.GrantedPermission;
import gov.healthit.chpl.auth.user.JWTAuthenticatedUser;
import gov.healthit.chpl.caching.UnitTestRules;
import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.dao.PendingCertifiedProductDAO;
import gov.healthit.chpl.domain.CQMResultCertification;
import gov.healthit.chpl.domain.CQMResultDetails;
import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertificationResultTestTool;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.ListingUpdateRequest;
import gov.healthit.chpl.domain.PendingCertifiedProductDetails;
import gov.healthit.chpl.dto.PendingCertificationResultDTO;
import gov.healthit.chpl.dto.PendingCertificationResultTestToolDTO;
import gov.healthit.chpl.dto.PendingCertifiedProductDTO;
import gov.healthit.chpl.dto.PendingCqmCriterionDTO;
import gov.healthit.chpl.validation.certifiedProduct.CertifiedProductValidator;
import gov.healthit.chpl.validation.certifiedProduct.CertifiedProductValidatorFactory;
import gov.healthit.chpl.web.controller.exception.ObjectMissingValidationException;
import gov.healthit.chpl.web.controller.exception.ValidationException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { gov.healthit.chpl.CHPLTestConfig.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class })
@DatabaseSetup("classpath:data/testData.xml") 
public class CertifiedProductControllerTest {
	@Rule
    @Autowired
    public UnitTestRules cacheInvalidationRule;
	
	@Autowired
	CertifiedProductController certifiedProductController;
	
	@Autowired
	PendingCertifiedProductDAO pcpDAO;
	
	@Autowired
	CertifiedProductValidatorFactory validatorFactory;
	
	private static JWTAuthenticatedUser adminUser;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		adminUser = new JWTAuthenticatedUser();
		adminUser.setFirstName("Administrator");
		adminUser.setId(-2L);
		adminUser.setLastName("Administrator");
		adminUser.setSubjectName("admin");
		adminUser.getPermissions().add(new GrantedPermission("ROLE_ADMIN"));
		adminUser.getPermissions().add(new GrantedPermission("ROLE_ACB_ADMIN"));
	}
	
	/** 
	 * This tests 4 scenarios for CP Update(CertifiedProductSearchDetails) to determine that a warning is returned for mismatched Certification Status + CHPL Product Number ICS. 
	 * An error should be returned when Certification Status + CHPL Product Number ICS are matching Boolean values 
	 * because a Certified Product cannot carry a retired Test Tool when the CP ICS = false.
	 * 
	 * 1. 2015 Certification Edition + false Certification Status + true ICS = returns error (no mismatch)
	 * Given that a user with sufficient privileges edits/updates an existing Certified Product 
	 * (Note: the logged in user must have ROLE_ADMIN or ROLE_ACB_ADMIN and have administrative authority on the ACB that certified the product. 
	 * If a different ACB is passed in as part of the request, an ownership change will take place and the logged in user must have ROLE_ADMIN)
	 * When the UI calls the API at /certified_products/update
	 * When the user tries to update a 2015 Certified Product with the following:
	 * existing ics = true
	 * retired test tool = true
	 * Inherited Certification Status false(unchecked) and the user sets the CHPL Product Number's ICS to 0
	 * Then the API returns an error because there is no mismatch between Certification Status and CHPL Product Number ICS
	 * 
	 * 2. 2015 Certification Edition + true Certification Status + false ICS = returns warning (mismatch)
	 * When the user tries to update a 2015 Certified Product with the following:
	 * existing ics = true
	 * retired test tool = true
	 * Inherited Certification Status true(checked) and the user sets the CHPL Product Number's ICS to 0
	 * Then the API returns a warning because Inherited Certification Status and CHPL Product Number ICS are mismatched
	 * 
	 * 3. 2014 Certification Edition + false Certification Status + true ICS = returns error (no mismatch)
	 * * When the user tries to update a 2014 Certified Product with the following:
	 * existing ics = false
	 * retired test tool = true
	 * Inherited Certification Status false(unchecked) and the user sets the CHPL Product Number's ICS to 0
	 * Then the API returns an error because there is no mismatch between Certification Status and CHPL Product Number ICS
	 * 
	 * 4. 2014 Certification Edition + true Certification Status + false ICS = returns warning (mismatch)
	 * * When the user tries to update a 2014 Certified Product with the following:
	 * existing ics = true
	 * retired test tool = true
	 * Inherited Certification Status true(checked) and the user sets the CHPL Product Number's ICS to 0
	 * Then the API returns a warning because Inherited Certification Status and CHPL Product Number ICS are mismatched
	 * @throws IOException 
	 * @throws JSONException 
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_updateCertifiedProductSearchDetails_icsAndRetiredTTs_warningvsError() throws EntityRetrievalException, EntityCreationException, IOException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		CertifiedProductSearchDetails updateRequest = new CertifiedProductSearchDetails();
		updateRequest.setCertificationDate(1440090840000L);
		updateRequest.setId(1L); // Certified_product_id = 1 has icsCode = true and is associated with TestTool with id=2 & id = 3 that have retired = true
		Map<String, Object> certStatus = new HashMap<String, Object>();
		certStatus.put("name", "Active");
		updateRequest.setCertificationStatus(certStatus);
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
		List<CertificationResultTestTool> crttList = new ArrayList<CertificationResultTestTool>();
		CertificationResultTestTool crtt = new CertificationResultTestTool();
		crtt.setId(2L);
		crtt.setRetired(true);
		crtt.setTestToolId(2L);
		crtt.setTestToolName("Transport Test Tool");
		crttList.add(crtt);
		cr.setTestToolsUsed(crttList);
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
		Map<String, Object> certificationEdition = new HashMap<String, Object>();
		String certEdition = "2015";
		certificationEdition.put("name", certEdition);
		updateRequest.setCertificationEdition(certificationEdition);
		updateRequest.setIcs(false); // Inherited Status = product.getIcs();
		updateRequest.setChplProductNumber("15.07.07.2642.EIC04.36.0.1.160402");
		try {
			ListingUpdateRequest listingUpdateRequest = new ListingUpdateRequest();
			listingUpdateRequest.setListing(updateRequest);
			certifiedProductController.updateCertifiedProduct(listingUpdateRequest);
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		} catch (ValidationException e) {
			assertNotNull(e);
			// ICS is false, 15.07.07.2642.EIC04.36.0.1.160402 shows false ICS. No mismatch = error message
			assertTrue(e.getErrorMessages().contains("Test Tool 'Transport Test Tool' can not be used for criteria '170.314 (b)(6)', "
					+ "as it is a retired tool, and this Certified Product does not carry ICS."));
		}
		
		updateRequest.setIcs(true);
		try {
			ListingUpdateRequest listingUpdateRequest = new ListingUpdateRequest();
			listingUpdateRequest.setListing(updateRequest);
			certifiedProductController.updateCertifiedProduct(listingUpdateRequest);
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		} catch (ValidationException e) {
			assertNotNull(e);
			// ICS is true, 15.07.07.2642.EIC04.36.0.1.160402 shows false ICS. Mismatch = warning message
			assertTrue(e.getWarningMessages().contains("Test Tool 'Transport Test Tool' can not be used for criteria '170.314 (b)(6)', "
					+ "as it is a retired tool, and this Certified Product does not carry ICS."));
		}
		Map<String, Object> classificationType = new HashMap<String, Object>();
		classificationType.put("name", "Modular EHR");
		updateRequest.setClassificationType(classificationType);
		Map<String, Object> practiceType = new HashMap<String, Object>();
		practiceType.put("name", "AMBULATORY");
		updateRequest.setPracticeType(practiceType);
		Map<String, Object> certificationEdition2014 = new HashMap<String, Object>();
		String certEdition2014 = "2014";
		certificationEdition2014.put("name", certEdition2014);
		updateRequest.setCertificationEdition(certificationEdition2014);
		updateRequest.setIcs(false);
		try {
			ListingUpdateRequest listingUpdateRequest = new ListingUpdateRequest();
			listingUpdateRequest.setListing(updateRequest);
			certifiedProductController.updateCertifiedProduct(listingUpdateRequest);
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		} catch (ValidationException e) {
			assertNotNull(e);
			// 2014 certEdition; ICS is false, 15.07.07.2642.EIC04.36.0.1.160402 shows false ICS. No mismatch = error message
			assertTrue(e.getErrorMessages().contains("Test Tool 'Transport Test Tool' can not be used for criteria '170.314 (b)(6)', "
					+ "as it is a retired tool, and this Certified Product does not carry ICS."));
		}
		
		updateRequest.setIcs(true);
		try {
			ListingUpdateRequest listingUpdateRequest = new ListingUpdateRequest();
			listingUpdateRequest.setListing(updateRequest);
			certifiedProductController.updateCertifiedProduct(listingUpdateRequest);
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		} catch (ValidationException e) {
			assertNotNull(e);
			// 2014 certEdition; ICS is true, 15.07.07.2642.EIC04.36.0.1.160402 shows false ICS. Mismatch = warning message
			assertTrue(e.getWarningMessages().contains("Test Tool 'Transport Test Tool' can not be used for criteria '170.314 (b)(6)', "
					+ "as it is a retired tool, and this Certified Product does not carry ICS."));
		}
		
	}
	
	/** 
	 * This tests 4 scenarios for CP Update(PendingCertifiedProductDTO) to determine that a warning is returned for mismatched Certification Status + CHPL Product Number ICS. 
	 * An error should be returned when Certification Status + CHPL Product Number ICS are matching Boolean values 
	 * because a Certified Product cannot carry a retired Test Tool when the CP ICS = false.
	 * 
	 * 1. 2015 Certification Edition + false Certification Status + true ICS = returns error (no mismatch)
	 * Given that a user with sufficient privileges edits/updates an existing Certified Product 
	 * (Note: the logged in user must have ROLE_ADMIN or ROLE_ACB_ADMIN and have administrative authority on the ACB that certified the product. 
	 * If a different ACB is passed in as part of the request, an ownership change will take place and the logged in user must have ROLE_ADMIN)
	 * When the UI calls the API at /certified_products/update
	 * When the user tries to update a 2015 Certified Product with the following:
	 * existing ics = true
	 * retired test tool = true
	 * Inherited Certification Status false(unchecked) and the user sets the CHPL Product Number's ICS to 0
	 * Then the API returns an error because there is no mismatch between Certification Status and CHPL Product Number ICS
	 * 
	 * 2. 2015 Certification Edition + true Certification Status + false ICS = returns warning (mismatch)
	 * When the user tries to update a 2015 Certified Product with the following:
	 * existing ics = true
	 * retired test tool = true
	 * Inherited Certification Status true(checked) and the user sets the CHPL Product Number's ICS to 0
	 * Then the API returns a warning because Inherited Certification Status and CHPL Product Number ICS are mismatched
	 * 
	 * 3. 2014 Certification Edition + false Certification Status + true ICS = returns error (no mismatch)
	 * * When the user tries to update a 2014 Certified Product with the following:
	 * existing ics = false
	 * retired test tool = true
	 * Inherited Certification Status false(unchecked) and the user sets the CHPL Product Number's ICS to 0
	 * Then the API returns an error because there is no mismatch between Certification Status and CHPL Product Number ICS
	 * 
	 * 4. 2014 Certification Edition + true Certification Status + false ICS = returns warning (mismatch)
	 * * When the user tries to update a 2014 Certified Product with the following:
	 * existing ics = true
	 * retired test tool = true
	 * Inherited Certification Status true(checked) and the user sets the CHPL Product Number's ICS to 0
	 * Then the API returns a warning because Inherited Certification Status and CHPL Product Number ICS are mismatched
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_updatePendingCertifiedProductDTO_icsAndRetiredTTs_warningvsError() throws EntityRetrievalException, EntityCreationException, IOException, ParseException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		PendingCertifiedProductDTO pcpDTO = new PendingCertifiedProductDTO();
		String certDateString = "11-09-2016";
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date inputDate = dateFormat.parse(certDateString);
		pcpDTO.setCertificationDate(inputDate);
		pcpDTO.setId(1L); // Certified_product_id = 1 has icsCode = true and is associated with TestTool with id=2 & id = 3 that have retired = true
		List<CertificationResultTestTool> crttList = new ArrayList<CertificationResultTestTool>();
		CertificationResultTestTool crtt = new CertificationResultTestTool();
		crtt.setId(1L);
		crtt.setRetired(true);
		crtt.setTestToolId(2L);
		crtt.setTestToolName("Transport Test Tool");
		crttList.add(crtt);
		List<PendingCertificationResultDTO> pcrDTOs = new ArrayList<PendingCertificationResultDTO>();
		PendingCertificationResultDTO pcpCertResultDTO1 = new PendingCertificationResultDTO();
		pcpCertResultDTO1.setPendingCertifiedProductId(1L);
		pcpCertResultDTO1.setId(1L);
		pcpCertResultDTO1.setAdditionalSoftware(null);
		pcpCertResultDTO1.setApiDocumentation(null);
		pcpCertResultDTO1.setG1Success(false);
		pcpCertResultDTO1.setG2Success(false);
		pcpCertResultDTO1.setGap(null);
		pcpCertResultDTO1.setNumber("170.314 (b)(6)");
		pcpCertResultDTO1.setPrivacySecurityFramework(null);
		pcpCertResultDTO1.setSed(null);
		pcpCertResultDTO1.setG1Success(false);
		pcpCertResultDTO1.setG2Success(false);
		pcpCertResultDTO1.setTestData(null);
		pcpCertResultDTO1.setTestFunctionality(null);
		pcpCertResultDTO1.setTestProcedures(null);
		pcpCertResultDTO1.setTestStandards(null);
		pcpCertResultDTO1.setTestTasks(null);
		pcpCertResultDTO1.setMeetsCriteria(true);
		List<PendingCertificationResultTestToolDTO> pcprttdtoList = new ArrayList<PendingCertificationResultTestToolDTO>();
		PendingCertificationResultTestToolDTO pcprttdto1 = new PendingCertificationResultTestToolDTO();
		pcprttdto1.setId(2L);
		pcprttdto1.setName("Transport Test Tool");
		pcprttdto1.setPendingCertificationResultId(1L);
		pcprttdto1.setTestToolId(2L);
		pcprttdto1.setVersion(null);
		PendingCertificationResultTestToolDTO pcprttdto2 = new PendingCertificationResultTestToolDTO();
		pcprttdto2.setId(3L);
		pcprttdto2.setName("Transport Test Tool");
		pcprttdto2.setPendingCertificationResultId(1L);
		pcprttdto2.setTestToolId(3L);
		pcprttdto2.setVersion(null);
		pcprttdtoList.add(pcprttdto1);
		pcprttdtoList.add(pcprttdto2);
		pcpCertResultDTO1.setTestTools(pcprttdtoList);
		pcrDTOs.add(pcpCertResultDTO1);
		PendingCertificationResultDTO pcpCertResultDTO2 = new PendingCertificationResultDTO();
		pcpCertResultDTO2.setId(2L);
		pcpCertResultDTO2.setAdditionalSoftware(null);
		pcpCertResultDTO2.setApiDocumentation(null);
		pcpCertResultDTO2.setG1Success(false);
		pcpCertResultDTO2.setG2Success(false);
		pcpCertResultDTO2.setGap(null);
		pcpCertResultDTO2.setNumber("170.314 (b)(6)");
		pcpCertResultDTO2.setPrivacySecurityFramework(null);
		pcpCertResultDTO2.setSed(null);
		pcpCertResultDTO2.setG1Success(false);
		pcpCertResultDTO2.setG2Success(false);
		pcpCertResultDTO2.setTestData(null);
		pcpCertResultDTO2.setTestFunctionality(null);
		pcpCertResultDTO2.setTestProcedures(null);
		pcpCertResultDTO2.setTestStandards(null);
		pcpCertResultDTO2.setTestTasks(null);
		pcpCertResultDTO2.setMeetsCriteria(true);
		pcpCertResultDTO2.setTestTools(pcprttdtoList);
		pcrDTOs.add(pcpCertResultDTO2);
		pcpDTO.setCertificationCriterion(pcrDTOs);
		List<PendingCqmCriterionDTO> cqmCriterionDTOList = new ArrayList<PendingCqmCriterionDTO>();
		PendingCqmCriterionDTO cqm = new PendingCqmCriterionDTO();
		cqm.setVersion("v0");;
		cqm.setCmsId("CMS60");
		cqm.setCqmCriterionId(0L);
		cqm.setCqmNumber(null); 
		cqm.setDomain(null);
		cqm.setId(0L);
		cqm.setNqfNumber("0164");
		cqm.setTitle("Fibrinolytic Therapy Received Within 30 Minutes of Hospital Arrival");
		cqm.setTypeId(2L);
		cqmCriterionDTOList.add(cqm);
		pcpDTO.setCqmCriterion(cqmCriterionDTOList);
		String certEdition = "2015";
		pcpDTO.setCertificationEdition(certEdition);
		pcpDTO.setCertificationEditionId(3L); // 1 = 2011; 2 = 2014; 3 = 2015
		pcpDTO.setIcs(false); // Inherited Status = product.getIcs();
		pcpDTO.setUniqueId("15.07.07.2642.EIC04.36.0.1.160402");
		CertifiedProductValidator validator = validatorFactory.getValidator(pcpDTO);
		if(validator != null) {
			validator.validate(pcpDTO);
		}
		// test 1
		// ICS is false, 15.07.07.2642.EIC04.36.0.1.160402 shows false ICS. No mismatch = error message
		assertTrue(pcpDTO.getErrorMessages().contains("Test Tool 'Transport Test Tool' can not be used for criteria '170.314 (b)(6)', "
					+ "as it is a retired tool, and this Certified Product does not carry ICS."));
		
		// test 2
		pcpDTO.setIcs(true); // Inherited Status = product.getIcs();
		validator = validatorFactory.getValidator(pcpDTO);
		if(validator != null) {
			validator.validate(pcpDTO);
		}
		// ICS is false, 15.07.07.2642.EIC04.36.0.1.160402 shows false ICS. No mismatch = error message
		assertTrue(pcpDTO.getErrorMessages().contains("Test Tool 'Transport Test Tool' can not be used for criteria '170.314 (b)(6)', "
					+ "as it is a retired tool, and this Certified Product does not carry ICS."));
		
		// test 3
		certEdition = "2014";
		pcpDTO.setCertificationEdition(certEdition);
		pcpDTO.setIcs(false); // Inherited Status = product.getIcs();
		pcpDTO.setPracticeType("AMBULATORY");
		pcpDTO.setProductClassificationName("Modular EHR");
		validator = validatorFactory.getValidator(pcpDTO);
		if(validator != null) {
			validator.validate(pcpDTO);
		}
		// ICS is false, 15.07.07.2642.EIC04.36.0.1.160402 shows false ICS. No mismatch = error message
		assertTrue(pcpDTO.getErrorMessages().contains("Test Tool 'Transport Test Tool' can not be used for criteria '170.314 (b)(6)', "
					+ "as it is a retired tool, and this Certified Product does not carry ICS."));
		
		// test 4
		pcpDTO.setIcs(true);
		validator = validatorFactory.getValidator(pcpDTO);
		if(validator != null) {
			validator.validate(pcpDTO);
		}
		// ICS is false, 15.07.07.2642.EIC04.36.0.1.160402 shows false ICS. No mismatch = error message
		assertTrue(pcpDTO.getErrorMessages().contains("Test Tool 'Transport Test Tool' can not be used for criteria '170.314 (b)(6)', "
					+ "as it is a retired tool, and this Certified Product does not carry ICS."));
	}
	
	/** 
	 * GIVEN A user edits a certified product
	 * WHEN they view their pending products
	 * THEN they should see errors if any privacy and security framework values do not match one of "Approach 1", "Approach 2", "Approach 1;Approach 2"
	 * (Note: Validation should be generous with case and whitespace)
	 * @throws IOException 
	 * @throws ValidationException 
	 * @throws JSONException 
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_updateCertifiedProductSearchDetails_privacyAndSecurityFramework_badValueShowsError() throws EntityRetrievalException, EntityCreationException, IOException, ValidationException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		CertifiedProductSearchDetails updateRequest = new CertifiedProductSearchDetails();
		updateRequest.setCertificationDate(1440090840000L);
		updateRequest.setId(1L);
		Map<String, Object> certStatus = new HashMap<String, Object>();
		certStatus.put("name", "Active");
		updateRequest.setCertificationStatus(certStatus);
		List<CertificationResult> certificationResults = new ArrayList<CertificationResult>();
		CertificationResult cr = new CertificationResult();
		cr.setAdditionalSoftware(null);
		cr.setApiDocumentation(null);
		cr.setG1Success(false);
		cr.setG2Success(false);
		cr.setGap(null);
		cr.setNumber("170.314 (g)(4)");
		cr.setPrivacySecurityFramework("Approach 1 Approach 2"); // bad value
		cr.setSed(null);
		cr.setSuccess(true);
		cr.setTestDataUsed(null);
		cr.setTestFunctionality(null);
		cr.setTestProcedures(null);
		cr.setTestStandards(null);
		cr.setTestTasks(null);
		List<CertificationResultTestTool> crttList = new ArrayList<CertificationResultTestTool>();
		CertificationResultTestTool crtt = new CertificationResultTestTool();
		crtt.setId(2L);
		crtt.setRetired(true);
		crtt.setTestToolId(2L);
		crtt.setTestToolName("Transport Test Tool");
		crttList.add(crtt);
		cr.setTestToolsUsed(crttList);
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
		Map<String, Object> certificationEdition = new HashMap<String, Object>();
		String certEdition = "2015";
		certificationEdition.put("name", certEdition);
		updateRequest.setCertificationEdition(certificationEdition);
		updateRequest.setIcs(true); // Inherited Status = product.getIcs();
		updateRequest.setChplProductNumber("15.07.07.2642.EIC04.36.0.1.160402");
		try {
			ListingUpdateRequest listingUpdateRequest = new ListingUpdateRequest();
			listingUpdateRequest.setListing(updateRequest);
			certifiedProductController.updateCertifiedProduct(listingUpdateRequest);
		} catch (InvalidArgumentsException e) {
			e.printStackTrace();
		} 
		catch (ValidationException e) {
			assertNotNull(e);
			Boolean hasError = false;
			for(String error : e.getErrorMessages()){
				if(error.startsWith("Certification 170.314 (g)(4) contains Privacy and Security Framework")){
					hasError = true;
				}
			}
			assertTrue(hasError);
		}
	}
	
	/** 
	 * GIVEN A user edits a certified product with a privacy and security framework value of " Approach 1 ,  Approach 2 "
	 * WHEN they view their pending products
	 * THEN the security framework value is formatted to remove whitespaces
	 * (Note: Validation should be generous with case and whitespace)
	 * @throws IOException 
	 * @throws ValidationException 
	 * @throws InvalidArgumentsException 
	 * @throws JSONException 
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_updateCertifiedProductSearchDetails_privacyAndSecurityFramework_handlesWhitespaces() throws EntityRetrievalException, EntityCreationException, IOException, ValidationException, InvalidArgumentsException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		
		String formattedPrivacyAndSecurityFramework = CertificationResult.formatPrivacyAndSecurityFramework(" Approach 1 ,  Approach 2 ");
		assertEquals("Approach 1;Approach 2", formattedPrivacyAndSecurityFramework);
		
		formattedPrivacyAndSecurityFramework = CertificationResult.formatPrivacyAndSecurityFramework(" Approach 1   Approach 2 ");
		assertEquals("Approach 1   Approach 2", formattedPrivacyAndSecurityFramework);
		
		formattedPrivacyAndSecurityFramework = CertificationResult.formatPrivacyAndSecurityFramework(" Approach 1   ");
		assertEquals("Approach 1", formattedPrivacyAndSecurityFramework);
		
		formattedPrivacyAndSecurityFramework = CertificationResult.formatPrivacyAndSecurityFramework("  Approach 2   ");
		assertEquals("Approach 2", formattedPrivacyAndSecurityFramework);
		
		CertifiedProductSearchDetails updateRequest = new CertifiedProductSearchDetails();
		updateRequest.setCertificationDate(1440090840000L);
		updateRequest.setId(1L);
		Map<String, Object> certStatus = new HashMap<String, Object>();
		certStatus.put("name", "Active");
		updateRequest.setCertificationStatus(certStatus);
		List<CertificationResult> certificationResults = new ArrayList<CertificationResult>();
		CertificationResult cr = new CertificationResult();
		cr.setAdditionalSoftware(null);
		cr.setApiDocumentation(null);
		cr.setG1Success(false);
		cr.setG2Success(false);
		cr.setGap(null);
		cr.setNumber("170.314 (g)(4)");
		cr.setPrivacySecurityFramework(" Approach 1 ,  Approach 2 "); // bad value
		cr.setSed(null);
		cr.setSuccess(true);
		cr.setTestDataUsed(null);
		cr.setTestFunctionality(null);
		cr.setTestProcedures(null);
		cr.setTestStandards(null);
		cr.setTestTasks(null);
		List<CertificationResultTestTool> crttList = new ArrayList<CertificationResultTestTool>();
		CertificationResultTestTool crtt = new CertificationResultTestTool();
		crtt.setId(2L);
		crtt.setRetired(true);
		crtt.setTestToolId(2L);
		crtt.setTestToolName("Transport Test Tool");
		crttList.add(crtt);
		cr.setTestToolsUsed(crttList);
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
		Map<String, Object> certificationEdition = new HashMap<String, Object>();
		String certEdition = "2015";
		certificationEdition.put("name", certEdition);
		updateRequest.setCertificationEdition(certificationEdition);
		updateRequest.setIcs(true); // Inherited Status = product.getIcs();
		updateRequest.setChplProductNumber("15.07.07.2642.EIC04.36.0.1.160402");
		ListingUpdateRequest listingUpdateRequest = new ListingUpdateRequest();
		listingUpdateRequest.setListing(updateRequest);
		try{
			certifiedProductController.updateCertifiedProduct(listingUpdateRequest);
		}
		catch (InvalidArgumentsException e) {
			e.printStackTrace();
		} 
		catch (ValidationException e) {
			assertNotNull(e);
			Boolean hasError = false;
			for(String error : e.getErrorMessages()){
				if(error.contains("Privacy and Security Framework")){
					hasError = true;
				}
			}
			assertFalse(hasError);
		}
		
	}
	
	/** 
	 * GIVEN A user edits a certified product
	 * WHEN they view their pending products
	 * THEN they should see errors if any privacy and security framework values do not match one of "Approach 1", "Approach 2", "Approach 1;Approach 2"
	 * (Note: Validation should be generous with case and whitespace)
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_updatePendingCertifiedProductDTO_privacyAndSecurityFramework_badValueShowsError() throws EntityRetrievalException, EntityCreationException, IOException, ParseException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		PendingCertifiedProductDTO pcpDTO = new PendingCertifiedProductDTO();
		String certDateString = "11-09-2016";
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date inputDate = dateFormat.parse(certDateString);
		pcpDTO.setCertificationDate(inputDate);
		pcpDTO.setId(1L); // Certified_product_id = 1 has icsCode = true and is associated with TestTool with id=2 & id = 3 that have retired = true
		List<CertificationResultTestTool> crttList = new ArrayList<CertificationResultTestTool>();
		CertificationResultTestTool crtt = new CertificationResultTestTool();
		crtt.setId(1L);
		crtt.setRetired(true);
		crtt.setTestToolId(2L);
		crtt.setTestToolName("Transport Test Tool");
		crttList.add(crtt);
		List<PendingCertificationResultDTO> pcrDTOs = new ArrayList<PendingCertificationResultDTO>();
		PendingCertificationResultDTO pcpCertResultDTO1 = new PendingCertificationResultDTO();
		pcpCertResultDTO1.setPendingCertifiedProductId(1L);
		pcpCertResultDTO1.setId(1L);
		pcpCertResultDTO1.setAdditionalSoftware(null);
		pcpCertResultDTO1.setApiDocumentation(null);
		pcpCertResultDTO1.setG1Success(false);
		pcpCertResultDTO1.setG2Success(false);
		pcpCertResultDTO1.setGap(null);
		pcpCertResultDTO1.setNumber("170.314 (g)(4)");
		pcpCertResultDTO1.setPrivacySecurityFramework("Approach 1 Approach 2");
		pcpCertResultDTO1.setSed(null);
		pcpCertResultDTO1.setG1Success(false);
		pcpCertResultDTO1.setG2Success(false);
		pcpCertResultDTO1.setTestData(null);
		pcpCertResultDTO1.setTestFunctionality(null);
		pcpCertResultDTO1.setTestProcedures(null);
		pcpCertResultDTO1.setTestStandards(null);
		pcpCertResultDTO1.setTestTasks(null);
		pcpCertResultDTO1.setMeetsCriteria(true);
		List<PendingCertificationResultTestToolDTO> pcprttdtoList = new ArrayList<PendingCertificationResultTestToolDTO>();
		PendingCertificationResultTestToolDTO pcprttdto1 = new PendingCertificationResultTestToolDTO();
		pcprttdto1.setId(2L);
		pcprttdto1.setName("Transport Test Tool");
		pcprttdto1.setPendingCertificationResultId(1L);
		pcprttdto1.setTestToolId(2L);
		pcprttdto1.setVersion(null);
		PendingCertificationResultTestToolDTO pcprttdto2 = new PendingCertificationResultTestToolDTO();
		pcprttdto2.setId(3L);
		pcprttdto2.setName("Transport Test Tool");
		pcprttdto2.setPendingCertificationResultId(1L);
		pcprttdto2.setTestToolId(3L);
		pcprttdto2.setVersion(null);
		pcprttdtoList.add(pcprttdto1);
		pcprttdtoList.add(pcprttdto2);
		pcpCertResultDTO1.setTestTools(pcprttdtoList);
		pcrDTOs.add(pcpCertResultDTO1);
		pcpDTO.setCertificationCriterion(pcrDTOs);
		List<PendingCqmCriterionDTO> cqmCriterionDTOList = new ArrayList<PendingCqmCriterionDTO>();
		PendingCqmCriterionDTO cqm = new PendingCqmCriterionDTO();
		cqm.setVersion("v0");;
		cqm.setCmsId("CMS60");
		cqm.setCqmCriterionId(0L);
		cqm.setCqmNumber(null); 
		cqm.setDomain(null);
		cqm.setId(0L);
		cqm.setNqfNumber("0164");
		cqm.setTitle("Fibrinolytic Therapy Received Within 30 Minutes of Hospital Arrival");
		cqm.setTypeId(2L);
		cqmCriterionDTOList.add(cqm);
		pcpDTO.setCqmCriterion(cqmCriterionDTOList);
		String certEdition = "2015";
		pcpDTO.setCertificationEdition(certEdition);
		pcpDTO.setCertificationEditionId(3L); // 1 = 2011; 2 = 2014; 3 = 2015
		pcpDTO.setIcs(false); // Inherited Status = product.getIcs();
		pcpDTO.setUniqueId("15.07.07.2642.EIC04.36.0.1.160402");
		CertifiedProductValidator validator = validatorFactory.getValidator(pcpDTO);
		if(validator != null) {
			validator.validate(pcpDTO);
		}
		
		Boolean hasError = false;
		for(String error : pcpDTO.getErrorMessages()){
			if(error.startsWith("Certification 170.314 (g)(4) contains Privacy and Security Framework value 'Approach 1 Approach 2'")){
				hasError = true;
			}
		}
		assertTrue(hasError);
	}
	
	/** 
	 * GIVEN A user edits a certified product
	 * WHEN they view their pending products
	 * WHEN the user edits the privacy and security framework value to be 'Approach 1, Approach 2'
	 * THEN they should see no error for the privacy and security framework value
	 * (Note: Validation should be generous with case and whitespace)
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_updatePendingCertifiedProductDTO_privacyAndSecurityFramework_validValueShowsNoError() throws EntityRetrievalException, EntityCreationException, IOException, ParseException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		PendingCertifiedProductDTO pcpDTO = new PendingCertifiedProductDTO();
		String certDateString = "11-09-2016";
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date inputDate = dateFormat.parse(certDateString);
		pcpDTO.setCertificationDate(inputDate);
		pcpDTO.setId(1L); // Certified_product_id = 1 has icsCode = true and is associated with TestTool with id=2 & id = 3 that have retired = true
		List<CertificationResultTestTool> crttList = new ArrayList<CertificationResultTestTool>();
		CertificationResultTestTool crtt = new CertificationResultTestTool();
		crtt.setId(1L);
		crtt.setRetired(true);
		crtt.setTestToolId(2L);
		crtt.setTestToolName("Transport Test Tool");
		crttList.add(crtt);
		List<PendingCertificationResultDTO> pcrDTOs = new ArrayList<PendingCertificationResultDTO>();
		PendingCertificationResultDTO pcpCertResultDTO1 = new PendingCertificationResultDTO();
		pcpCertResultDTO1.setPendingCertifiedProductId(1L);
		pcpCertResultDTO1.setId(1L);
		pcpCertResultDTO1.setAdditionalSoftware(null);
		pcpCertResultDTO1.setApiDocumentation(null);
		pcpCertResultDTO1.setG1Success(false);
		pcpCertResultDTO1.setG2Success(false);
		pcpCertResultDTO1.setGap(null);
		pcpCertResultDTO1.setNumber("170.314 (g)(4)");
		pcpCertResultDTO1.setPrivacySecurityFramework("Approach 1, Approach 2");
		pcpCertResultDTO1.setSed(null);
		pcpCertResultDTO1.setG1Success(false);
		pcpCertResultDTO1.setG2Success(false);
		pcpCertResultDTO1.setTestData(null);
		pcpCertResultDTO1.setTestFunctionality(null);
		pcpCertResultDTO1.setTestProcedures(null);
		pcpCertResultDTO1.setTestStandards(null);
		pcpCertResultDTO1.setTestTasks(null);
		pcpCertResultDTO1.setMeetsCriteria(true);
		List<PendingCertificationResultTestToolDTO> pcprttdtoList = new ArrayList<PendingCertificationResultTestToolDTO>();
		PendingCertificationResultTestToolDTO pcprttdto1 = new PendingCertificationResultTestToolDTO();
		pcprttdto1.setId(2L);
		pcprttdto1.setName("Transport Test Tool");
		pcprttdto1.setPendingCertificationResultId(1L);
		pcprttdto1.setTestToolId(2L);
		pcprttdto1.setVersion(null);
		PendingCertificationResultTestToolDTO pcprttdto2 = new PendingCertificationResultTestToolDTO();
		pcprttdto2.setId(3L);
		pcprttdto2.setName("Transport Test Tool");
		pcprttdto2.setPendingCertificationResultId(1L);
		pcprttdto2.setTestToolId(3L);
		pcprttdto2.setVersion(null);
		pcprttdtoList.add(pcprttdto1);
		pcprttdtoList.add(pcprttdto2);
		pcpCertResultDTO1.setTestTools(pcprttdtoList);
		pcrDTOs.add(pcpCertResultDTO1);
		pcpDTO.setCertificationCriterion(pcrDTOs);
		List<PendingCqmCriterionDTO> cqmCriterionDTOList = new ArrayList<PendingCqmCriterionDTO>();
		PendingCqmCriterionDTO cqm = new PendingCqmCriterionDTO();
		cqm.setVersion("v0");;
		cqm.setCmsId("CMS60");
		cqm.setCqmCriterionId(0L);
		cqm.setCqmNumber(null); 
		cqm.setDomain(null);
		cqm.setId(0L);
		cqm.setNqfNumber("0164");
		cqm.setTitle("Fibrinolytic Therapy Received Within 30 Minutes of Hospital Arrival");
		cqm.setTypeId(2L);
		cqmCriterionDTOList.add(cqm);
		pcpDTO.setCqmCriterion(cqmCriterionDTOList);
		String certEdition = "2015";
		pcpDTO.setCertificationEdition(certEdition);
		pcpDTO.setCertificationEditionId(3L); // 1 = 2011; 2 = 2014; 3 = 2015
		pcpDTO.setIcs(false); // Inherited Status = product.getIcs();
		pcpDTO.setUniqueId("15.07.07.2642.EIC04.36.0.1.160402");
		CertifiedProductValidator validator = validatorFactory.getValidator(pcpDTO);
		if(validator != null) {
			validator.validate(pcpDTO);
		}
		
		Boolean hasError = false;
		for(String error : pcpDTO.getErrorMessages()){
			if(error.contains("Privacy and Security Framework")){
				hasError = true;
			}
		}
		assertFalse(hasError);
	}
	
	/** 
	 * GIVEN A user edits a certified product
	 * WHEN they view their pending products
	 * WHEN the user edits the privacy and security framework value to be ' Approach 1 ,  Approach 2 '
	 * THEN they should see no error for the privacy and security framework value
	 * (Note: Validation should be generous with case and whitespace)
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws JSONException 
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_updatePendingCertifiedProductDTO_privacyAndSecurityFramework_handlesWhitespace() throws EntityRetrievalException, EntityCreationException, IOException, ParseException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		PendingCertifiedProductDTO pcpDTO = new PendingCertifiedProductDTO();
		String certDateString = "11-09-2016";
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date inputDate = dateFormat.parse(certDateString);
		pcpDTO.setCertificationDate(inputDate);
		pcpDTO.setId(1L); // Certified_product_id = 1 has icsCode = true and is associated with TestTool with id=2 & id = 3 that have retired = true
		List<CertificationResultTestTool> crttList = new ArrayList<CertificationResultTestTool>();
		CertificationResultTestTool crtt = new CertificationResultTestTool();
		crtt.setId(1L);
		crtt.setRetired(true);
		crtt.setTestToolId(2L);
		crtt.setTestToolName("Transport Test Tool");
		crttList.add(crtt);
		List<PendingCertificationResultDTO> pcrDTOs = new ArrayList<PendingCertificationResultDTO>();
		PendingCertificationResultDTO pcpCertResultDTO1 = new PendingCertificationResultDTO();
		pcpCertResultDTO1.setPendingCertifiedProductId(1L);
		pcpCertResultDTO1.setId(1L);
		pcpCertResultDTO1.setAdditionalSoftware(null);
		pcpCertResultDTO1.setApiDocumentation(null);
		pcpCertResultDTO1.setG1Success(false);
		pcpCertResultDTO1.setG2Success(false);
		pcpCertResultDTO1.setGap(null);
		pcpCertResultDTO1.setNumber("170.314 (g)(4)");
		pcpCertResultDTO1.setPrivacySecurityFramework(" Approach 1 ,  Approach 2 ");
		pcpCertResultDTO1.setSed(null);
		pcpCertResultDTO1.setG1Success(false);
		pcpCertResultDTO1.setG2Success(false);
		pcpCertResultDTO1.setTestData(null);
		pcpCertResultDTO1.setTestFunctionality(null);
		pcpCertResultDTO1.setTestProcedures(null);
		pcpCertResultDTO1.setTestStandards(null);
		pcpCertResultDTO1.setTestTasks(null);
		pcpCertResultDTO1.setMeetsCriteria(true);
		List<PendingCertificationResultTestToolDTO> pcprttdtoList = new ArrayList<PendingCertificationResultTestToolDTO>();
		PendingCertificationResultTestToolDTO pcprttdto1 = new PendingCertificationResultTestToolDTO();
		pcprttdto1.setId(2L);
		pcprttdto1.setName("Transport Test Tool");
		pcprttdto1.setPendingCertificationResultId(1L);
		pcprttdto1.setTestToolId(2L);
		pcprttdto1.setVersion(null);
		PendingCertificationResultTestToolDTO pcprttdto2 = new PendingCertificationResultTestToolDTO();
		pcprttdto2.setId(3L);
		pcprttdto2.setName("Transport Test Tool");
		pcprttdto2.setPendingCertificationResultId(1L);
		pcprttdto2.setTestToolId(3L);
		pcprttdto2.setVersion(null);
		pcprttdtoList.add(pcprttdto1);
		pcprttdtoList.add(pcprttdto2);
		pcpCertResultDTO1.setTestTools(pcprttdtoList);
		pcrDTOs.add(pcpCertResultDTO1);
		pcpDTO.setCertificationCriterion(pcrDTOs);
		List<PendingCqmCriterionDTO> cqmCriterionDTOList = new ArrayList<PendingCqmCriterionDTO>();
		PendingCqmCriterionDTO cqm = new PendingCqmCriterionDTO();
		cqm.setVersion("v0");;
		cqm.setCmsId("CMS60");
		cqm.setCqmCriterionId(0L);
		cqm.setCqmNumber(null); 
		cqm.setDomain(null);
		cqm.setId(0L);
		cqm.setNqfNumber("0164");
		cqm.setTitle("Fibrinolytic Therapy Received Within 30 Minutes of Hospital Arrival");
		cqm.setTypeId(2L);
		cqmCriterionDTOList.add(cqm);
		pcpDTO.setCqmCriterion(cqmCriterionDTOList);
		String certEdition = "2015";
		pcpDTO.setCertificationEdition(certEdition);
		pcpDTO.setCertificationEditionId(3L); // 1 = 2011; 2 = 2014; 3 = 2015
		pcpDTO.setIcs(false); // Inherited Status = product.getIcs();
		pcpDTO.setUniqueId("15.07.07.2642.EIC04.36.0.1.160402");
		CertifiedProductValidator validator = validatorFactory.getValidator(pcpDTO);
		if(validator != null) {
			validator.validate(pcpDTO);
		}
		
		Boolean hasError = false;
		for(String error : pcpDTO.getErrorMessages()){
			if(error.contains("Privacy and Security Framework")){
				hasError = true;
			}
		}
		assertFalse(hasError);
	}
	
	/**
	 * GIVEN a user is on the Pending CPs page
	 * WHEN they reject a pending CP that was already deleted because it was rejected or confirmed
	 * THEN the API returns a 400 BAD REQUEST with the lastModifiedUser's Contact info
	 * @throws EntityCreationException 
	 * @throws EntityRetrievalException 
	 * @throws JsonProcessingException 
	 */
	public void test_rejectPendingCP_isAlreadyDeleted_returnsBadRequest() throws JsonProcessingException, EntityRetrievalException, EntityCreationException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		Boolean hasError = false;
		try{
			certifiedProductController.deletePendingCertifiedProduct(-1L);
		} catch (ObjectMissingValidationException e){
			for(String error : e.getErrorMessages()){
				if(error.contains("has already been confirmed/rejected")){
					hasError = true;
				}
			}
			assertTrue(e.getContact() != null);
		}
		assertTrue(hasError);
	}
	
	/** 
	 * GIVEN a user is on the Pending CPs page
	 * WHEN they confirm a pending CP that was already deleted because it was rejected or confirmed
	 * THEN the API returns a 400 BAD REQUEST with the lastModifiedUser's Contact info
	 * @throws EntityCreationException 
	 * @throws EntityRetrievalException 
	 * @throws JsonProcessingException 
	 * @throws ValidationException 
	 * @throws InvalidArgumentsException 
	 */
	@Transactional
	@Rollback(true)
	@Test
	public void test_confirmPendingCP_isAlreadyDeleted_returnsBadRequest() throws JsonProcessingException, EntityRetrievalException, EntityCreationException, InvalidArgumentsException, ValidationException {
		SecurityContextHolder.getContext().setAuthentication(adminUser);
		PendingCertifiedProductDTO pcpDTO = null;
		PendingCertifiedProductDetails pcpDetails = null;
		pcpDTO = pcpDAO.findById(-1L, true);
		pcpDetails = new PendingCertifiedProductDetails(pcpDTO);
		Boolean hasError = false;
		try{
			certifiedProductController.confirmPendingCertifiedProduct(pcpDetails);
		} catch (ObjectMissingValidationException e){
			for(String error : e.getErrorMessages()){
				if(error.contains("has already been confirmed/rejected")){
					hasError = true;
				}
			}
			assertTrue(e.getContact() != null);
		}
		assertTrue(hasError);
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
		CertifiedProductSearchDetails cpDetails = certifiedProductController.getCertifiedProductById(cpId);
		assertNotNull(cpDetails);
	}
	
}
