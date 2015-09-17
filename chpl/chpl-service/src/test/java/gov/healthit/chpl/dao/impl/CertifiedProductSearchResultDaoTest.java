package gov.healthit.chpl.dao.impl;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

import gov.healthit.chpl.auth.user.JWTAuthenticatedUser;
import gov.healthit.chpl.dao.CertifiedProductSearchResultDAO;
import gov.healthit.chpl.domain.SearchRequest;
import gov.healthit.chpl.dto.CertifiedProductDetailsDTO;
import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { gov.healthit.chpl.CHPLTestConfig.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class })
@DatabaseSetup("classpath:data/testData.xml")
public class CertifiedProductSearchResultDaoTest extends TestCase {

	@Autowired
	private CertifiedProductSearchResultDAO searchResultDAO;

	private static JWTAuthenticatedUser authUser;
	
	@Test
	@Transactional
	public void testCountSearchResults(){
		
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setVendor("Test");
		searchRequest.setVisibleOnCHPL("YES");
		Long countProducts = searchResultDAO.countMultiFilterSearchResults(searchRequest);
		assertEquals(2, countProducts.intValue());
		
		searchRequest.setVersion("1.0.0");
		Long countProductsVersionSpecific = searchResultDAO.countMultiFilterSearchResults(searchRequest);
		assertEquals(1, countProductsVersionSpecific.intValue());
		
	}
	
	@Test
	@Transactional
	public void testSearchVendor(){
		
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setVendor("Test Vendor 1");
		List<CertifiedProductDetailsDTO> products = searchResultDAO.search(searchRequest);
		assertEquals(2, products.size());
	}
	
	@Test
	@Transactional
	public void testSearchProduct(){
		
		SearchRequest searchRequest = new SearchRequest();
		
		searchRequest.setProduct("Test Product 1");
		List<CertifiedProductDetailsDTO> products = searchResultDAO.search(searchRequest);
		assertEquals(2, products.size());
		
	}
	
	@Test
	@Transactional
	public void testSearchVersion(){
		
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setVersion("1.0.1");
		List<CertifiedProductDetailsDTO> products = searchResultDAO.search(searchRequest);
		assertEquals(1, products.size());
		
	}
	
	@Test
	@Transactional
	public void testSearchCertificationEdition(){
		
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setCertificationEdition("2014");
		List<CertifiedProductDetailsDTO> products = searchResultDAO.search(searchRequest);
		assertEquals(2, products.size());
		
	}
	
	@Test
	@Transactional
	public void testSearchCertificationBody(){
		
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setCertificationBody("InfoGard");
		searchRequest.setVisibleOnCHPL("BOTH");
		List<CertifiedProductDetailsDTO> products = searchResultDAO.search(searchRequest);
		assertEquals(3, products.size());
		
	}
	
	
	@Test
	@Transactional
	public void testSearchProductClassificationType(){
		
		SearchRequest searchRequest = new SearchRequest();
		
		searchRequest.setProductClassification("Complete EHR");
		
		List<CertifiedProductDetailsDTO> products = searchResultDAO.search(searchRequest);
		assertEquals(1, products.size());
		
	}
	
	@Test
	@Transactional
	public void testSearchPracticeType(){
		
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setPracticeType("Ambulatory");
		searchRequest.setVisibleOnCHPL("BOTH");
		List<CertifiedProductDetailsDTO> products = searchResultDAO.search(searchRequest);
		assertEquals(3, products.size());
		
	}
	
	
	@Test
	@Transactional
	public void testSearchVisibleOnCHPL(){
		
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setVisibleOnCHPL("YES");
		List<CertifiedProductDetailsDTO> products = searchResultDAO.search(searchRequest);
		assertEquals(2, products.size());
		
	}
	
	@Test
	@Transactional
	public void testSearch(){
		
		SearchRequest searchRequest = new SearchRequest();
		
		searchRequest.setSearchTerm("Test");
		searchRequest.setVendor("Test Vendor");
		searchRequest.setProduct("Test");
		searchRequest.setVersion("1.0.1");
		searchRequest.setCertificationEdition("2014");
		searchRequest.setCertificationBody("InfoGard");
		searchRequest.setProductClassification("Complete EHR");
		searchRequest.setPracticeType("Ambulatory");
		searchRequest.setVisibleOnCHPL("YES");
		searchRequest.setOrderBy("product");
		searchRequest.setSortDescending(true);
		searchRequest.setPageNumber(0);
		
		
		List<CertifiedProductDetailsDTO> products = searchResultDAO.search(searchRequest);
		assertEquals(1, products.size());
		
	}
	
}