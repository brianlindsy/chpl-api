package gov.healthit.chpl.validation.listing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.validation.listing.reviewer.CertificationDateReviewer;
import gov.healthit.chpl.validation.listing.reviewer.CertificationStatusReviewer;
import gov.healthit.chpl.validation.listing.reviewer.ChplNumberReviewer;
import gov.healthit.chpl.validation.listing.reviewer.DeveloperStatusReviewer;
import gov.healthit.chpl.validation.listing.reviewer.FieldLengthReviewer;
import gov.healthit.chpl.validation.listing.reviewer.InheritedCertificationStatusReviewer;
import gov.healthit.chpl.validation.listing.reviewer.Reviewer;
import gov.healthit.chpl.validation.listing.reviewer.SedG3Reviewer;
import gov.healthit.chpl.validation.listing.reviewer.TestToolReviewer;
import gov.healthit.chpl.validation.listing.reviewer.UnattestedCriteriaWithDataReviewer;
import gov.healthit.chpl.validation.listing.reviewer.UnsupportedCharacterReviewer;
import gov.healthit.chpl.validation.listing.reviewer.ValidDataReviewer;
import gov.healthit.chpl.validation.listing.reviewer.edition2015.RequiredData2015Reviewer;

/**
 * Validation interface for any listing that is already uploaded and confirmed on the CHPL.
 * @author kekey
 *
 */
@Component
public class Edition2015ListingValidator extends Validator {
    @Autowired
    @Qualifier("chplNumberReviewer")
    private ChplNumberReviewer chplNumberReviewer;

    @Autowired 
    @Qualifier("developerStatusReviewer")
    private DeveloperStatusReviewer devStatusReviewer;

    @Autowired 
    @Qualifier("unsupportedCharacterReviewer")
    private UnsupportedCharacterReviewer unsupportedCharacterReviewer;

    @Autowired 
    @Qualifier("fieldLengthReviewer")
    private FieldLengthReviewer fieldLengthReviewer;

    @Autowired 
    @Qualifier("requiredData2015Reviewer")
    private RequiredData2015Reviewer requiredDataReviewer;

    @Autowired
    @Qualifier("validDataReviewer")
    private ValidDataReviewer validDataReviewer;

    @Autowired 
    @Qualifier("sedG3Reviewer")
    private SedG3Reviewer sedG3Reviewer;

    @Autowired 
    @Qualifier("certificationStatusReviewer")
    private CertificationStatusReviewer certStatusReviewer;

    @Autowired 
    @Qualifier("certificationDateReviewer")
    private CertificationDateReviewer certDateReviewer;

    @Autowired 
    @Qualifier("unattestedCriteriaWithDataReviewer")
    private UnattestedCriteriaWithDataReviewer unattestedCriteriaWithDataReviewer;

    @Autowired 
    @Qualifier("testToolReviewer")
    private TestToolReviewer ttReviewer;

    @Autowired 
    @Qualifier("icsReviewer")
    private InheritedCertificationStatusReviewer icsReviewer;

    private List<Reviewer> reviewers;

    public List<Reviewer> getReviewers() {
        if(reviewers == null) {
            reviewers = new ArrayList<Reviewer>();
            reviewers.add(chplNumberReviewer);
            reviewers.add(devStatusReviewer);
            reviewers.add(unsupportedCharacterReviewer);
            reviewers.add(fieldLengthReviewer);
            reviewers.add(requiredDataReviewer);
            reviewers.add(validDataReviewer);
            reviewers.add(sedG3Reviewer);
            reviewers.add(certStatusReviewer);
            reviewers.add(certDateReviewer);
            reviewers.add(unattestedCriteriaWithDataReviewer);
            reviewers.add(icsReviewer);
            reviewers.add(ttReviewer);
        }
        return reviewers;
    }
}