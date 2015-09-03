package gov.healthit.chpl.entity;


import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.proxy.HibernateProxy;


/** 
 * Object mapping for hibernate-handled table: certified_product.
 * A product that has been Certified
 *
 * @author autogenerated / cwatson
 */

@Entity
@Table(name = "certified_product")
public class CertifiedProductEntity {

	/** Serial Version UID. */
	private static final long serialVersionUID = -2928065796550377879L;
	
    @Id 
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "certifiedProductCertified_product_idGenerator")
	@Basic( optional = false )
	@Column( name = "certified_product_id", nullable = false  )
	@SequenceGenerator(name = "certifiedProductCertified_product_idGenerator", sequenceName = "openchpl.openchpl.certified_product_certified_product_id_seq")
	private Long id;
    
	@Basic( optional = true )
	@Column( name = "acb_certification_id", length = 250  )
	private String acbCertificationId;
	
	@Basic( optional = false )
	@Column(name = "certification_body_id", nullable = false )
	private Long certificationBodyId;
	
	@Basic( optional = false )
	@Column(name = "certification_edition_id", nullable = false )
	private Long certificationEditionId;
	
	@Basic( optional = true )
	@Column( name = "chpl_product_number", length = 250  )
	private String chplProductNumber;
	
	@Basic( optional = false )
	@Column( name = "creation_date", nullable = false  )
	private Date creationDate;
	
	@Basic( optional = false )
	@Column( name = "deleted", nullable = false  )
	private Boolean deleted;
	
	@Basic( optional = false )
	@Column( name = "last_modified_date", nullable = false)
	private Date lastModifiedDate;
	
	@Basic( optional = false )
	@Column( name = "last_modified_user", nullable = false  )
	private Long lastModifiedUser;
	
	@Basic( optional = true )
	@Column(name = "practice_type_id", nullable = true )
	private Long practiceTypeId;
	
	@Basic( optional = true )
	@Column(name = "product_classification_type_id", nullable = true )
	private Long productClassificationTypeId;
	
	@Basic( optional = false )
	@Column(name = "product_version_id", nullable = false )
	private Long productVersionId;
	
	@Basic( optional = true )
	@Column( name = "quality_management_system_att", length = 2147483647  )
	private String qualityManagementSystemAtt;
	
	@Basic( optional = true )
	@Column( name = "report_file_location", length = 255  )
	private String reportFileLocation;
	
	@Basic( optional = true )
	@Column(name = "testing_lab_id", nullable = true )
	private Long testingLabId;
	
	/**
	 * Default constructor, mainly for hibernate use.
	 */
	public CertifiedProductEntity() {
		// Default constructor
	} 

	/** Constructor taking a given ID.
	 * @param id to set
	 */
	public CertifiedProductEntity(Long id) {
		this.id = id;
	}
	
	/** Return the type of this class. Useful for when dealing with proxies.
	* @return Defining class.
	*/
	@Transient
	public Class<?> getClassType() {
		return CertifiedProductEntity.class;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAcbCertificationId() {
		return acbCertificationId;
	}

	public void setAcbCertificationId(String acbCertificationId) {
		this.acbCertificationId = acbCertificationId;
	}

	public Long getCertificationBodyId() {
		return certificationBodyId;
	}

	public void setCertificationBodyId(Long certificationBodyId) {
		this.certificationBodyId = certificationBodyId;
	}

	public Long getCertificationEditionId() {
		return certificationEditionId;
	}

	public void setCertificationEditionId(Long certificationEditionId) {
		this.certificationEditionId = certificationEditionId;
	}

	public String getChplProductNumber() {
		return chplProductNumber;
	}

	public void setChplProductNumber(String chplProductNumber) {
		this.chplProductNumber = chplProductNumber;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public Long getLastModifiedUser() {
		return lastModifiedUser;
	}

	public void setLastModifiedUser(Long lastModifiedUser) {
		this.lastModifiedUser = lastModifiedUser;
	}

	public Long getPracticeTypeId() {
		return practiceTypeId;
	}

	public void setPracticeTypeId(Long practiceTypeId) {
		this.practiceTypeId = practiceTypeId;
	}

	public Long getProductClassificationTypeId() {
		return productClassificationTypeId;
	}

	public void setProductClassificationTypeId(Long productClassificationTypeId) {
		this.productClassificationTypeId = productClassificationTypeId;
	}

	public Long getProductVersionId() {
		return productVersionId;
	}

	public void setProductVersionId(Long productVersionId) {
		this.productVersionId = productVersionId;
	}

	public String getQualityManagementSystemAtt() {
		return qualityManagementSystemAtt;
	}

	public void setQualityManagementSystemAtt(String qualityManagementSystemAtt) {
		this.qualityManagementSystemAtt = qualityManagementSystemAtt;
	}

	public String getReportFileLocation() {
		return reportFileLocation;
	}

	public void setReportFileLocation(String reportFileLocation) {
		this.reportFileLocation = reportFileLocation;
	}

	public Long getTestingLabId() {
		return testingLabId;
	}

	public void setTestingLabId(Long testingLabId) {
		this.testingLabId = testingLabId;
	}

}
