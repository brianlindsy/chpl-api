package gov.healthit.chpl.scheduler.job.chartdata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import gov.healthit.chpl.dao.search.CertifiedProductSearchDAO;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.search.CertifiedProductFlatSearchResult;
import gov.healthit.chpl.dto.IncumbentDevelopersStatisticsDTO;
import gov.healthit.chpl.dto.ListingCountStatisticsDTO;
import gov.healthit.chpl.dto.NonconformityTypeStatisticsDTO;
import gov.healthit.chpl.scheduler.job.BrokenSurveillanceRulesCreatorJob;
import gov.healthit.chpl.scheduler.job.QuartzJob;

/**
 * This is the starting point for populating statistics tables that will be used
 * for the charts. As new tables need to be populated, they will be added here.
 *
 * @author TYoung
 *
 */
@DisallowConcurrentExecution
public final class ChartDataCreatorJob extends QuartzJob {
    private static final Logger LOGGER = LogManager.getLogger(ChartDataCreatorJob.class);
    private static final String DEFAULT_PROPERTIES_FILE = "environment.properties";
    private Properties props;

    @Autowired
    private CertifiedProductSearchDAO certifiedProductSearchDAO;

    /**
     * Constructor to initialize InheritanceErrorsReportCreatorJob object.
     *
     * @throws Exception
     *             is thrown
     */
    public ChartDataCreatorJob() throws Exception {
        super();
        loadProperties();
    }

    private Properties loadProperties() throws IOException {
        InputStream in = BrokenSurveillanceRulesCreatorJob.class.getClassLoader().getResourceAsStream(
                DEFAULT_PROPERTIES_FILE);
        if (in == null) {
            props = null;
            throw new FileNotFoundException("Environment Properties File not found in class path.");
        } else {
            props = new Properties();
            props.load(in);
            in.close();
        }
        return props;
    }

    @Override
    @Transactional
    public void execute(final JobExecutionContext arg0) throws JobExecutionException {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        List<CertifiedProductFlatSearchResult> certifiedProducts = certifiedProductSearchDAO.getAllCertifiedProducts();
        LOGGER.info("Certified Product Count: " + certifiedProducts.size());

        analyzeSed(certifiedProducts);
        analyzeProducts(certifiedProducts);
        analyzeDevelopers(certifiedProducts);
        analyzeListingCounts(certifiedProducts);
        analyzeNonconformity();
    }

    private static void analyzeDevelopers(final List<CertifiedProductFlatSearchResult> listings) {
        IncumbentDevelopersStatisticsCalculator incumbentDevelopersStatisticsCalculator =
                new IncumbentDevelopersStatisticsCalculator();
        List<IncumbentDevelopersStatisticsDTO> dtos = incumbentDevelopersStatisticsCalculator.getCounts(listings);
        incumbentDevelopersStatisticsCalculator.logCounts(dtos);
        incumbentDevelopersStatisticsCalculator.save(dtos);
    }

    private static void analyzeListingCounts(final List<CertifiedProductFlatSearchResult> listings) {
        ListingCountDataFilter listingCountDataFilter = new ListingCountDataFilter();
        List<CertifiedProductFlatSearchResult> filteredListings = listingCountDataFilter.filterData(listings);
        ListingCountStatisticsCalculator listingCountStatisticsCalculator = new ListingCountStatisticsCalculator();
        List<ListingCountStatisticsDTO> dtos = listingCountStatisticsCalculator.getCounts(filteredListings);
        listingCountStatisticsCalculator.logCounts(dtos);
        listingCountStatisticsCalculator.save(dtos);
    }

    private static void analyzeNonconformity() {
        NonconformityTypeChartCalculator nonconformityStatisticsCalculator = new NonconformityTypeChartCalculator();
        List<NonconformityTypeStatisticsDTO> dtos = nonconformityStatisticsCalculator.getCounts();
        nonconformityStatisticsCalculator.logCounts(dtos);
        nonconformityStatisticsCalculator.saveCounts(dtos);
    }

    private static void analyzeProducts(final List<CertifiedProductFlatSearchResult> listings) {
        CriterionProductDataFilter criterionProductDataFilter = new CriterionProductDataFilter();
        List<CertifiedProductFlatSearchResult> filteredListings = criterionProductDataFilter.filterData(listings);
        CriterionProductStatisticsCalculator criterionProductStatisticsCalculator =
                new CriterionProductStatisticsCalculator();
        Map<String, Long> productCounts = criterionProductStatisticsCalculator.getCounts(filteredListings);
        criterionProductStatisticsCalculator.logCounts(productCounts);
        criterionProductStatisticsCalculator.save(productCounts);

    }

    private static void analyzeSed(final List<CertifiedProductFlatSearchResult> listings) {
        // Get Certified Products
        SedDataCollector sedDataCollector = new SedDataCollector();
        List<CertifiedProductSearchDetails> seds = sedDataCollector.retreiveData(listings);

        LOGGER.info("Collected SED Data");

        // Extract SED Statistics
        SedParticipantsStatisticCountCalculator sedParticipantsStatisticCountCalculator =
                new SedParticipantsStatisticCountCalculator();
        sedParticipantsStatisticCountCalculator.run(seds);

        ParticipantGenderStatisticsCalculator participantGenderStatisticsCalculator =
                new ParticipantGenderStatisticsCalculator();
        participantGenderStatisticsCalculator.run(seds);

        ParticipantAgeStatisticsCalculator participantAgeStatisticsCalculator =
                new ParticipantAgeStatisticsCalculator();
        participantAgeStatisticsCalculator.run(seds);

        ParticipantEducationStatisticsCalculator participantEducationStatisticsCalculator =
                new ParticipantEducationStatisticsCalculator();
        participantEducationStatisticsCalculator.run(seds);

        ParticipantExperienceStatisticsCalculator participantProfExperienceStatisticsCalculator =
                new ParticipantExperienceStatisticsCalculator();

        participantProfExperienceStatisticsCalculator.run(seds, ExperienceType.COMPUTER_EXPERIENCE);
        participantProfExperienceStatisticsCalculator.run(seds, ExperienceType.PRODUCT_EXPERIENCE);
        participantProfExperienceStatisticsCalculator.run(seds, ExperienceType.PROFESSIONAL_EXPERIENCE);
    }

}
