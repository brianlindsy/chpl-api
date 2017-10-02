package gov.healthit.chpl.entity.listing;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import gov.healthit.chpl.entity.QmsStandardEntity;

@Entity
@Table(name = "certified_product_qms_standard")
public class CertifiedProductQmsStandardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "certified_product_qms_standard_id")
    private Long id;

    @Basic(optional = false)
    @Column(name = "certified_product_id", nullable = false)
    private Long certifiedProductId;

    @Basic(optional = false)
    @Column(name = "qms_standard_id", nullable = false)
    private Long qmsStandardId;

    @Basic(optional = true)
    @OneToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "qms_standard_id", unique = true, nullable = true, insertable = false, updatable = false)
    private QmsStandardEntity qmsStandard;

    @Basic(optional = false)
    @Column(name = "modification", nullable = false)
    private String modification;

    @Basic(optional = false)
    @Column(name = "applicable_criteria", nullable = false)
    private String applicableCriteria;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCertifiedProductId() {
        return certifiedProductId;
    }

    public void setCertifiedProductId(Long certifiedProductId) {
        this.certifiedProductId = certifiedProductId;
    }

    public Long getQmsStandardId() {
        return qmsStandardId;
    }

    public void setQmsStandardId(Long qmsStandardId) {
        this.qmsStandardId = qmsStandardId;
    }

    public QmsStandardEntity getQmsStandard() {
        return qmsStandard;
    }

    public void setQmsStandard(QmsStandardEntity qmsStandard) {
        this.qmsStandard = qmsStandard;
    }

    public String getModification() {
        return modification;
    }

    public void setModification(String modification) {
        this.modification = modification;
    }

    public String getApplicableCriteria() {
        return applicableCriteria;
    }

    public void setApplicableCriteria(String applicableCriteria) {
        this.applicableCriteria = applicableCriteria;
    }

    @Basic(optional = false)
    @Column(name = "creation_date", nullable = false)
    protected Date creationDate;

    @Basic(optional = false)
    @Column(nullable = false)
    protected Boolean deleted;

    @Basic(optional = false)
    @Column(name = "last_modified_date", nullable = false)
    protected Date lastModifiedDate;

    @Basic(optional = false)
    @Column(name = "last_modified_user", nullable = false)
    protected Long lastModifiedUser;

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Boolean getDeleted() {
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
}
