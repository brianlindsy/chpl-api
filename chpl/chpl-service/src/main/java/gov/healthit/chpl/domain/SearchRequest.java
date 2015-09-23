package gov.healthit.chpl.domain;

import java.util.ArrayList;
import java.util.List;

public class SearchRequest {
	
	
	String searchTerm = null;
	String vendor = null;
	String product = null;
	String version = null;
	List<String> certificationCriteria = new ArrayList<String>();
	List<String> cqms = new ArrayList<String>();
	String certificationEdition = null;
	String certificationBody = null;
	String productClassification = null;
	String practiceType = null;
	String visibleOnCHPL = "Both";
	String orderBy = "product";
	Boolean sortDescending = false;
	Integer pageNumber = 0;
	Integer pageSize = 20;
	
	
	public String getSearchTerm() {
		return searchTerm;
	}
	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}
	public String getVendor() {
		return vendor;
	}
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}
	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public List<String> getCertificationCriteria() {
		return certificationCriteria;
	}
	public void setCertificationCriteria(List<String> certificationCriteria) {
		this.certificationCriteria = certificationCriteria;
	}
	public void addCertificationCriteria(String certificationCriteria) {
		this.certificationCriteria.add(certificationCriteria);
	}
	public List<String> getCqms() {
		return cqms;
	}
	public void setCqms(List<String> cqms) {
		this.cqms = cqms;
	}
	public String getCertificationEdition() {
		return certificationEdition;
	}
	public void setCertificationEdition(String certificationEdition) {
		this.certificationEdition = certificationEdition;
	}
	public String getProductClassification() {
		return productClassification;
	}
	public void setProductClassification(String productClassification) {
		this.productClassification = productClassification;
	}
	public String getPracticeType() {
		return practiceType;
	}
	public void setPracticeType(String practiceType) {
		this.practiceType = practiceType;
	}
	public String getOrderBy() {
		return orderBy;
	}
	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}
	public String getCertificationBody() {
		return certificationBody;
	}
	public void setCertificationBody(String certifyingBody) {
		this.certificationBody = certifyingBody;
	}
	public Boolean getSortDescending() {
		return sortDescending;
	}
	public void setSortDescending(Boolean sortDescending) {
		this.sortDescending = sortDescending;
	}
	public String getVisibleOnCHPL() {
		return visibleOnCHPL;
	}
	public void setVisibleOnCHPL(String visibleOnCHPL) {
		this.visibleOnCHPL = visibleOnCHPL;
	}
	public Integer getPageNumber() {
		return pageNumber;
	}
	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}
	public Integer getPageSize() {
		return pageSize;
	}
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}
	
}