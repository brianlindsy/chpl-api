package gov.healthit.chpl.dto;

import java.io.Serializable;

import gov.healthit.chpl.entity.CorrectiveActionPlanCertificationEntity;

public class CorrectiveActionPlanCertificationResultDTO implements Serializable {
    private static final long serialVersionUID = -5246550225480177796L;
    private Long id;
    private Long correctiveActionPlanId;
    private CertificationCriterionDTO certCriterion;
    private String summary;
    private String developerExplanation;
    private String resolution;
    private Integer numSitesPassed;
    private Integer numSitesTotal;

    public CorrectiveActionPlanCertificationResultDTO() {

    }

    public CorrectiveActionPlanCertificationResultDTO(CorrectiveActionPlanCertificationEntity entity) {
        this.id = entity.getId();
        if (entity.getCorrectiveActionPlan() != null) {
            setCorrectiveActionPlanId(entity.getCorrectiveActionPlan().getId());
        }
        if (entity.getCertificationCriterion() != null) {
            this.certCriterion = new CertificationCriterionDTO(entity.getCertificationCriterion());
        }
        this.summary = entity.getSummary();
        this.developerExplanation = entity.getDeveloperExplanation();
        this.resolution = entity.getResolution();
        this.numSitesPassed = entity.getNumSitesPassed();
        this.numSitesTotal = entity.getNumSitesTotal();
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public CertificationCriterionDTO getCertCriterion() {
        return certCriterion;
    }

    public void setCertCriterion(final CertificationCriterionDTO certCriterion) {
        this.certCriterion = certCriterion;
    }

    public Long getCorrectiveActionPlanId() {
        return correctiveActionPlanId;
    }

    public void setCorrectiveActionPlanId(final Long correctiveActionPlanId) {
        this.correctiveActionPlanId = correctiveActionPlanId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(final String summary) {
        this.summary = summary;
    }

    public String getDeveloperExplanation() {
        return developerExplanation;
    }

    public void setDeveloperExplanation(final String developerExplanation) {
        this.developerExplanation = developerExplanation;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(final String resolution) {
        this.resolution = resolution;
    }

    public Integer getNumSitesPassed() {
        return numSitesPassed;
    }

    public void setNumSitesPassed(final Integer numSitesPassed) {
        this.numSitesPassed = numSitesPassed;
    }

    public Integer getNumSitesTotal() {
        return numSitesTotal;
    }

    public void setNumSitesTotal(final Integer numSitesTotal) {
        this.numSitesTotal = numSitesTotal;
    }
}
