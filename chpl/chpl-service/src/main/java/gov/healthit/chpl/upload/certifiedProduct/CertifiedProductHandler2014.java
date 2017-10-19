package gov.healthit.chpl.upload.certifiedProduct;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.dto.CertifiedProductDTO;
import gov.healthit.chpl.dto.CertifiedProductDetailsDTO;
import gov.healthit.chpl.dto.QmsStandardDTO;
import gov.healthit.chpl.dto.TestFunctionalityDTO;
import gov.healthit.chpl.dto.TestStandardDTO;
import gov.healthit.chpl.dto.TestToolDTO;
import gov.healthit.chpl.dto.UcdProcessDTO;
import gov.healthit.chpl.entity.CQMCriterionEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCertificationResultAdditionalSoftwareEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCertificationResultEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCertificationResultTestDataEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCertificationResultTestFunctionalityEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCertificationResultTestProcedureEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCertificationResultTestStandardEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCertificationResultTestToolEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCertificationResultUcdProcessEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCertifiedProductEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCertifiedProductQmsStandardEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCqmCriterionEntity;
import gov.healthit.chpl.upload.certifiedProduct.template.TemplateColumnIndexMap;
import gov.healthit.chpl.upload.certifiedProduct.template.TemplateColumnIndexMap2014;
import gov.healthit.chpl.web.controller.InvalidArgumentsException;

@Component("certifiedProductHandler2014")
public class CertifiedProductHandler2014 extends CertifiedProductHandler {

    private static final Logger LOGGER = LogManager.getLogger(CertifiedProductHandler2014.class);
    private TemplateColumnIndexMap templateColumnIndexMap;
    private String[] criteriaNames = {
            "170.314 (a)(1)", "170.314 (a)(2)", "170.314 (a)(3)", "170.314 (a)(4)", "170.314 (a)(5)",
            "170.314 (a)(6)", "170.314 (a)(7)", "170.314 (a)(8)", "170.314 (a)(9)", "170.314 (a)(10)",
            "170.314 (a)(11)", "170.314 (a)(12)", "170.314 (a)(13)", "170.314 (a)(14)", "170.314 (a)(15)",
            "170.314 (a)(16)", "170.314 (a)(17)", "170.314 (a)(18)", "170.314 (a)(19)", "170.314 (a)(20)",
            "170.314 (b)(1)", "170.314 (b)(2)", "170.314 (b)(3)", "170.314 (b)(4)", "170.314 (b)(5)(A)",
            "170.314 (b)(5)(B)", "170.314 (b)(6)", "170.314 (b)(7)", "170.314 (b)(8)", "170.314 (b)(9)",
            "170.314 (c)(1)", "170.314 (c)(2)", "170.314 (c)(3)", "170.314 (d)(1)", "170.314 (d)(2)",
            "170.314 (d)(3)", "170.314 (d)(4)", "170.314 (d)(5)", "170.314 (d)(6)", "170.314 (d)(7)",
            "170.314 (d)(8)", "170.314 (d)(9)", "170.314 (e)(1)", "170.314 (e)(2)", "170.314 (e)(3)",
            "170.314 (f)(1)", "170.314 (f)(2)", "170.314 (f)(3)", "170.314 (f)(4)", "170.314 (f)(5)",
            "170.314 (f)(6)", "170.314 (f)(7)", "170.314 (g)(1)", "170.314 (g)(2)", "170.314 (g)(3)",
            "170.314 (g)(4)", "170.314 (h)(1)",  "170.314 (h)(2)", "170.314 (h)(3)"
    };
    
    public CertifiedProductHandler2014() {
       templateColumnIndexMap = new TemplateColumnIndexMap2014();
    }
    
    public TemplateColumnIndexMap getColumnIndexMap() {
         return templateColumnIndexMap;
    }
    
    public String[] getCriteriaNames() {
        return this.criteriaNames;
    }
    
    public PendingCertifiedProductEntity handle() {
        PendingCertifiedProductEntity pendingCertifiedProduct = new PendingCertifiedProductEntity();
        pendingCertifiedProduct.setStatus(getDefaultStatusId());

        // get the first row of the certified product
        for (CSVRecord record : getRecord()) {
            String statusStr = record.get(1);
            if (!StringUtils.isEmpty(statusStr) && FIRST_ROW_INDICATOR.equalsIgnoreCase(statusStr)) {
                parseCertifiedProductDetails(record, pendingCertifiedProduct);
            }
        }

        // get the QMS's for the certified product
        for (CSVRecord record : getRecord()) {
            String statusStr = record.get(1);
            if (!StringUtils.isEmpty(statusStr) && (FIRST_ROW_INDICATOR.equalsIgnoreCase(statusStr)
                    || SUBSEQUENT_ROW_INDICATOR.equalsIgnoreCase(statusStr))) {
                parseQms(record, pendingCertifiedProduct);
            }
        }
        if (!pendingCertifiedProduct.isHasQms() && pendingCertifiedProduct.getQmsStandards().size() > 0) {
            pendingCertifiedProduct.getErrorMessages()
                    .add(pendingCertifiedProduct.getUniqueId() + " has 'false' in the QMS column but a QMS was found.");
        } else if (pendingCertifiedProduct.isHasQms() && pendingCertifiedProduct.getQmsStandards().size() == 0) {
            pendingCertifiedProduct.getErrorMessages()
                    .add(pendingCertifiedProduct.getUniqueId() + " has 'true' in the QMS column but no QMS was found.");
        }

        // parse CQMs
        for (CSVRecord record : getRecord()) {
            String statusStr = record.get(1);
            if (!StringUtils.isEmpty(statusStr) && (FIRST_ROW_INDICATOR.equalsIgnoreCase(statusStr)
                    || SUBSEQUENT_ROW_INDICATOR.equalsIgnoreCase(statusStr))) {
                parseCqms(record, pendingCertifiedProduct);
            }
        }

        // parse criteria
        CSVRecord firstRow = null;
        for (int i = 0; i < getRecord().size() && firstRow == null; i++) {
            CSVRecord currRecord = getRecord().get(i);
            String statusStr = currRecord.get(1);
            if (!StringUtils.isEmpty(statusStr) && FIRST_ROW_INDICATOR.equalsIgnoreCase(statusStr)) {
                firstRow = currRecord;
            }
        }
        if (firstRow != null) {
            int criteriaBeginIndex = getColumnIndexMap().getCriteriaStartIndex();
            for(int i = 0; i < getCriteriaNames().length; i++) {
                String criteriaName = getCriteriaNames()[i];
                int criteriaEndIndex = getColumnIndexMap().getLastIndexForCriteria(getHeading(), criteriaBeginIndex);
                pendingCertifiedProduct.getCertificationCriterion().add(parseCriteria(pendingCertifiedProduct,
                        criteriaName, firstRow, criteriaBeginIndex, criteriaEndIndex));
                criteriaBeginIndex = criteriaEndIndex + 1;
            }
        }

        return pendingCertifiedProduct;
    }

    private void parseCertifiedProductDetails(CSVRecord record, PendingCertifiedProductEntity pendingCertifiedProduct) {
        parseUniqueId(pendingCertifiedProduct, record);
        parseRecordStatus(pendingCertifiedProduct, record);
        parsePracticeType(pendingCertifiedProduct, record);
        parseDeveloperProductVersion(pendingCertifiedProduct, record);
        parseDeveloperAddress(pendingCertifiedProduct, record);
        parseEdition("2014", pendingCertifiedProduct, record);
        parseAcbCertificationId(pendingCertifiedProduct, record);
        parseAcb(pendingCertifiedProduct, record);
        parseAtl(pendingCertifiedProduct, record);
        parseProductClassification(pendingCertifiedProduct, record);
        parseCertificationDate(pendingCertifiedProduct, record);        
        parseSed(pendingCertifiedProduct, record);
        parseHasQms(pendingCertifiedProduct, record);
        parseHasIcs(pendingCertifiedProduct, record);
        parseTransparencyAttestation(pendingCertifiedProduct, record);
    }

    protected void parseSed(PendingCertifiedProductEntity pendingCertifiedProduct, CSVRecord record) {
        int sedIndex = getColumnIndexMap().getSedStartIndex();
        // report file location
        pendingCertifiedProduct.setReportFileLocation(record.get(sedIndex++).trim());
        // sed report link
        pendingCertifiedProduct.setSedReportFileLocation(record.get(sedIndex).trim());
    }
    
    private void parseQms(CSVRecord record, PendingCertifiedProductEntity pendingCertifiedProduct) {
        int colIndex = getColumnIndexMap().getQmsStartIndex()+1;
        if (!StringUtils.isEmpty(record.get(colIndex))) {
            String qmsStandardName = record.get(colIndex++).trim();
            QmsStandardDTO qmsStandard = qmsDao.getByName(qmsStandardName);
            String qmsMods = record.get(colIndex).trim();

            PendingCertifiedProductQmsStandardEntity qmsEntity = new PendingCertifiedProductQmsStandardEntity();
            qmsEntity.setMappedProduct(pendingCertifiedProduct);
            qmsEntity.setModification(qmsMods);
            qmsEntity.setName(qmsStandardName);
            if (qmsStandard != null) {
                qmsEntity.setQmsStandardId(qmsStandard.getId());
            }
            pendingCertifiedProduct.getQmsStandards().add(qmsEntity);
        }
    }

    private void parseCqms(CSVRecord record, PendingCertifiedProductEntity pendingCertifiedProduct) {
        int cqmNameIndex = getColumnIndexMap().getCqmStartIndex();
        int cqmVersionIndex = cqmNameIndex+1;

        String cqmName = record.get(cqmNameIndex).trim();
        String cqmVersions = record.get(cqmVersionIndex).trim();

        List<PendingCqmCriterionEntity> criterion = handleCqmCmsCriterion(pendingCertifiedProduct, cqmName,
                cqmVersions);
        for (PendingCqmCriterionEntity entity : criterion) {
            if (entity != null && entity.getMappedCriterion() != null && entity.getMeetsCriteria() == Boolean.TRUE) {
                pendingCertifiedProduct.getCqmCriterion().add(entity);
            }
        }
    }

    private PendingCertificationResultEntity parseCriteria(PendingCertifiedProductEntity pendingCertifiedProduct,
            String criteriaNumber, CSVRecord firstRow, int beginIndex, int endIndex) {
        int currIndex = beginIndex;
        PendingCertificationResultEntity cert = null;
        try {
            cert = getCertificationResult(criteriaNumber, firstRow.get(currIndex++).trim());

            while (currIndex <= endIndex) {
                String colTitle = getHeading().get(currIndex);
                if (!StringUtils.isEmpty(colTitle)) {
                    colTitle = colTitle.trim().toUpperCase();
                    switch (colTitle) {
                    case "GAP":
                        cert.setGap(asBoolean(firstRow.get(currIndex++).trim()));
                        break;
                    case "STANDARD TESTED AGAINST":
                        parseTestStandards(pendingCertifiedProduct, cert, currIndex);
                        currIndex++;
                        break;
                    case "FUNCTIONALITY TESTED":
                        parseTestFunctionality(pendingCertifiedProduct, cert, currIndex);
                        currIndex++;
                        break;
                    case "MEASURE SUCCESSFULLY TESTED FOR G1":
                        cert.setG1Success(asBoolean(firstRow.get(currIndex++).trim()));
                        break;
                    case "MEASURE SUCCESSFULLY TESTED FOR G2":
                        cert.setG2Success(asBoolean(firstRow.get(currIndex++).trim()));
                        break;
                    case "ADDITIONAL SOFTWARE":
                        Boolean hasAdditionalSoftware = asBoolean(firstRow.get(currIndex).trim());
                        cert.setHasAdditionalSoftware(hasAdditionalSoftware);
                        parseAdditionalSoftware(pendingCertifiedProduct, cert, currIndex);
                        currIndex += 4;
                        break;
                    case "TEST TOOL NAME":
                        parseTestTools(pendingCertifiedProduct, cert, currIndex);
                        currIndex += 2;
                    case "TEST PROCEDURE VERSION":
                        parseTestProcedures(cert, currIndex);
                        currIndex++;
                        break;
                    case "TEST DATA VERSION":
                        parseTestData(pendingCertifiedProduct, cert, currIndex);
                        currIndex += 3;
                        break;
                    case "SED":
                        cert.setSed(asBoolean(firstRow.get(currIndex++).trim()));
                        String ucdProcessName = firstRow.get(currIndex++).trim();
                        String ucdProcessDetails = firstRow.get(currIndex++).trim();
                        if (!StringUtils.isEmpty(ucdProcessName)) {
                            PendingCertificationResultUcdProcessEntity ucd = new PendingCertificationResultUcdProcessEntity();
                            ucd.setUcdProcessName(ucdProcessName);
                            ucd.setUcdProcessDetails(ucdProcessDetails);
                            UcdProcessDTO dto = ucdDao.getByName(ucd.getUcdProcessName());
                            if (dto != null) {
                                ucd.setUcdProcessId(dto.getId());
                            }
                            cert.getUcdProcesses().add(ucd);
                        }
                        break;
                    default:
                        pendingCertifiedProduct.getErrorMessages()
                                .add("Invalid column title " + colTitle + " at index " + currIndex);
                        LOGGER.error("Could not handle column " + colTitle + " at index " + currIndex + ".");
                        currIndex++;
                    }
                }
            }
        } catch (final InvalidArgumentsException ex) {
            LOGGER.error(ex.getMessage());
        }
        return cert;
    }

    private void parseTestStandards(PendingCertifiedProductEntity listing, PendingCertificationResultEntity cert,
            int tsColumn) {
        for (CSVRecord row : getRecord()) {
            String tsValue = row.get(tsColumn).trim();
            if (!StringUtils.isEmpty(tsValue)) {
                PendingCertificationResultTestStandardEntity tsEntity = new PendingCertificationResultTestStandardEntity();
                tsEntity.setTestStandardName(tsValue);
                TestStandardDTO ts = testStandardDao.getByNumberAndEdition(tsValue,
                        listing.getCertificationEditionId());
                if (ts != null) {
                    tsEntity.setTestStandardId(ts.getId());
                }
                cert.getTestStandards().add(tsEntity);
            }
        }
    }

    private void parseTestFunctionality(PendingCertifiedProductEntity listing, PendingCertificationResultEntity cert,
            int tfColumn) {
        for (CSVRecord row : getRecord()) {
            String tfValue = row.get(tfColumn).trim();
            if (!StringUtils.isEmpty(tfValue)) {
                PendingCertificationResultTestFunctionalityEntity tfEntity = new PendingCertificationResultTestFunctionalityEntity();
                tfEntity.setTestFunctionalityNumber(tfValue);
                TestFunctionalityDTO tf = testFunctionalityDao.getByNumberAndEdition(tfValue,
                        listing.getCertificationEditionId());
                if (tf != null) {
                    tfEntity.setTestFunctionalityId(tf.getId());
                }
                cert.getTestFunctionality().add(tfEntity);
            }
        }
    }

    private void parseAdditionalSoftware(PendingCertifiedProductEntity product, PendingCertificationResultEntity cert,
            int asColumnBegin) {
        int cpSourceColumn = asColumnBegin + 1;
        int nonCpSourceColumn = asColumnBegin + 2;

        for (CSVRecord row : getRecord()) {
            String cpSourceValue = row.get(cpSourceColumn).trim();
            if (!StringUtils.isEmpty(cpSourceValue)) {
                PendingCertificationResultAdditionalSoftwareEntity asEntity = new PendingCertificationResultAdditionalSoftwareEntity();
                asEntity.setChplId(cpSourceValue);
                if (cpSourceValue.startsWith("CHP-")) {
                    CertifiedProductDTO cp = certifiedProductDao.getByChplNumber(cpSourceValue);
                    if (cp != null) {
                        asEntity.setCertifiedProductId(cp.getId());
                    }
                } else {
                    try {
                        CertifiedProductDetailsDTO cpd = certifiedProductDao.getByChplUniqueId(cpSourceValue);
                        if (cpd != null) {
                            asEntity.setCertifiedProductId(cpd.getId());
                        }
                    } catch (final EntityRetrievalException ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                }
                cert.getAdditionalSoftware().add(asEntity);
            }
            String nonCpSourceValue = row.get(nonCpSourceColumn).trim();
            if (!StringUtils.isEmpty(nonCpSourceValue)) {
                PendingCertificationResultAdditionalSoftwareEntity asEntity = new PendingCertificationResultAdditionalSoftwareEntity();
                asEntity.setSoftwareName(nonCpSourceValue);
                asEntity.setSoftwareVersion(row.get(nonCpSourceColumn + 1).trim());
                cert.getAdditionalSoftware().add(asEntity);
            }
        }

        if (cert.getHasAdditionalSoftware() != null && cert.getHasAdditionalSoftware().booleanValue() == true
                && cert.getAdditionalSoftware().size() == 0) {
            product.getErrorMessages().add("Certification " + cert.getMappedCriterion().getNumber() + " for product "
                    + product.getUniqueId() + " indicates additional software should be present but none was found.");
        } else if ((cert.getHasAdditionalSoftware() == null || cert.getHasAdditionalSoftware().booleanValue() == false)
                && cert.getAdditionalSoftware().size() > 0) {
            product.getErrorMessages()
                    .add("Certification " + cert.getMappedCriterion().getNumber() + " for product "
                            + product.getUniqueId()
                            + " indicates additional software should not be present but some was found.");
        }
    }

    private void parseTestProcedures(PendingCertificationResultEntity cert, int tpColumn) {
        for (CSVRecord row : getRecord()) {
            String tpValue = row.get(tpColumn).trim();
            if (!StringUtils.isEmpty(tpValue)) {
                PendingCertificationResultTestProcedureEntity tpEntity = new PendingCertificationResultTestProcedureEntity();
                tpEntity.setTestProcedureVersion(tpValue);
                // don't look up by name because we don't want these to be
                // shared
                // among certifications. they are user-entered, could be
                // anything, and if
                // they are shared then updating in one place could affect other
                // places
                // when that is not the intended behavior
                cert.getTestProcedures().add(tpEntity);
            }
        }
    }

    private void parseTestData(PendingCertifiedProductEntity product, PendingCertificationResultEntity cert,
            int tdColumnBegin) {
        for (CSVRecord row : getRecord()) {
            String tdVersionValue = row.get(tdColumnBegin).trim();
            if (!StringUtils.isEmpty(tdVersionValue)) {
                PendingCertificationResultTestDataEntity tdEntity = new PendingCertificationResultTestDataEntity();
                tdEntity.setVersion(tdVersionValue);
                Boolean hasAlteration = asBoolean(row.get(tdColumnBegin + 1).trim());
                tdEntity.setHasAlteration(hasAlteration);
                String alterationStr = row.get(tdColumnBegin + 2).trim();
                if (tdEntity.isHasAlteration() && StringUtils.isEmpty(alterationStr)) {
                    product.getErrorMessages()
                            .add("Certification " + cert.getMappedCriterion().getNumber() + " for product "
                                    + product.getUniqueId()
                                    + " indicates test data was altered however no test data alteration was found.");
                } else if (!tdEntity.isHasAlteration() && !StringUtils.isEmpty(alterationStr)) {
                    product.getErrorMessages()
                            .add("Certification " + cert.getMappedCriterion().getNumber() + " for product "
                                    + product.getUniqueId()
                                    + " indicates test data was not altered however a test data alteration was found.");
                }
                tdEntity.setAlteration(row.get(tdColumnBegin + 2).trim());
                cert.getTestData().add(tdEntity);
            }
        }
    }

    private void parseTestTools(PendingCertifiedProductEntity product, PendingCertificationResultEntity cert,
            int toolColumnBegin) {
        for (CSVRecord row : getRecord()) {
            String testToolName = row.get(toolColumnBegin).trim();
            String testToolVersion = row.get(toolColumnBegin + 1).trim();
            if (!StringUtils.isEmpty(testToolName)) {
                PendingCertificationResultTestToolEntity ttEntity = new PendingCertificationResultTestToolEntity();
                ttEntity.setTestToolName(testToolName);
                ttEntity.setTestToolVersion(testToolVersion);
                TestToolDTO testTool = testToolDao.getByName(testToolName);
                if (testTool != null) {
                    ttEntity.setTestToolId(testTool.getId());
                }
                cert.getTestTools().add(ttEntity);
            }
        }
    }
    
    /**
     * look up a CQM CMS criteria by number and version. throw an error if we
     * can't find it
     * 
     * @param criterionNum
     * @param column
     * @return
     * @throws InvalidArgumentsException
     */
    protected List<PendingCqmCriterionEntity> handleCqmCmsCriterion(PendingCertifiedProductEntity product,
            String criterionNum, String version) {
        if (!StringUtils.isEmpty(version)) {
            version = version.trim();
        }

        List<PendingCqmCriterionEntity> result = new ArrayList<PendingCqmCriterionEntity>();

        if (!StringUtils.isEmpty(version) && !"0".equals(version)) {
            // split on ;
            String[] versionList = version.split(";");
            if (versionList.length == 1) {
                // also try splitting on ,
                versionList = version.split(",");
            }

            for (int i = 0; i < versionList.length; i++) {
                String currVersion = versionList[i].trim();
                if (!criterionNum.startsWith("CMS")) {
                    criterionNum = "CMS" + criterionNum;
                }
                CQMCriterionEntity cqmEntity = cqmDao.getCMSEntityByNumberAndVersion(criterionNum, currVersion);
                if (cqmEntity == null) {
                    product.getErrorMessages().add("Could not find a CQM CMS criterion matching " + criterionNum
                            + " and version " + currVersion);
                }

                PendingCqmCriterionEntity currResult = new PendingCqmCriterionEntity();
                currResult.setMappedCriterion(cqmEntity);
                currResult.setMeetsCriteria(true);
                result.add(currResult);
            }
        }

        return result;
    }
}
