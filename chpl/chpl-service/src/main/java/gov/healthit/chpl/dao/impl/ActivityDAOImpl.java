package gov.healthit.chpl.dao.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import gov.healthit.chpl.dao.ActivityDAO;
import gov.healthit.chpl.domain.concept.ActivityConcept;
import gov.healthit.chpl.dto.ActivityDTO;
import gov.healthit.chpl.dto.CertificationBodyDTO;
import gov.healthit.chpl.dto.TestingLabDTO;
import gov.healthit.chpl.entity.ActivityEntity;
import gov.healthit.chpl.exception.EntityCreationException;
import gov.healthit.chpl.exception.EntityRetrievalException;

/**
 * Data access for Activity. Generally activity should only
 * be created and retrieved and not changed in any way once inserted.
 * @author kekey
 *
 */
@Repository("activityDAO")
public class ActivityDAOImpl extends BaseDAOImpl implements ActivityDAO {

    @Override
    public ActivityDTO create(final ActivityDTO dto) throws EntityCreationException, EntityRetrievalException {

        ActivityEntity entity = null;
        try {
            if (dto.getId() != null) {
                entity = this.getEntityById(dto.getId());
            }
        } catch (final EntityRetrievalException e) {
            throw new EntityCreationException(e);
        }

        if (entity != null) {
            throw new EntityCreationException("An entity with this ID already exists.");
        } else {

            entity = new ActivityEntity();
            entity.setId(dto.getId());
            entity.setDescription(dto.getDescription());
            entity.setOriginalData(dto.getOriginalData());
            entity.setNewData(dto.getNewData());
            entity.setActivityDate(dto.getActivityDate());
            entity.setConcept(dto.getConcept());
            entity.setActivityObjectId(dto.getActivityObjectId());
            entity.setCreationDate(new Date());
            entity.setLastModifiedDate(new Date());
            // user may be null because when they get an API Key they do not
            // have to be logged in
            entity.setLastModifiedUser(dto.getLastModifiedUser());
            entity.setDeleted(false);
            entityManager.persist(entity);
            entityManager.flush();
        }
        ActivityDTO result = null;
        if (entity != null) {
            result = new ActivityDTO(entity);
        }
        return result;
    }

    @Override
    public ActivityDTO getById(final Long id) throws EntityRetrievalException {

        ActivityEntity entity = getEntityById(id);
        ActivityDTO dto = null;
        if (entity != null) {
            dto = new ActivityDTO(entity);
        }
        return dto;
    }

    @Override
    public List<ActivityDTO> findByObjectId(final Long objectId, final ActivityConcept concept,
            final Date startDate, final Date endDate) {

        List<ActivityEntity> entities = this.getEntitiesByObjectId(objectId, concept, startDate, endDate);
        List<ActivityDTO> activities = new ArrayList<>();

        for (ActivityEntity entity : entities) {
            ActivityDTO result = new ActivityDTO(entity);
            activities.add(result);
        }
        return activities;
    }

    @Override
    public List<ActivityDTO> findByConcept(final ActivityConcept concept, final Date startDate, final Date endDate) {

        List<ActivityEntity> entities = this.getEntitiesByConcept(concept, startDate, endDate);
        List<ActivityDTO> activities = new ArrayList<>();

        for (ActivityEntity entity : entities) {
            ActivityDTO result = new ActivityDTO(entity);
            activities.add(result);
        }
        return activities;
    }

    @Override
    public List<ActivityDTO> findPublicAnnouncementActivity(final Date startDate, final Date endDate) {
        Query query = entityManager.createNamedQuery("getPublicAnnouncementActivityByDate",
                ActivityEntity.class);
        query.setParameter("conceptId", ActivityConcept.ACTIVITY_CONCEPT_ANNOUNCEMENT.getId());
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        List<ActivityDTO> results = new ArrayList<ActivityDTO>();
        List<ActivityEntity> entities = query.getResultList();
        for (ActivityEntity entity : entities) {
            ActivityDTO result = new ActivityDTO(entity);
            results.add(result);
        }
        return results;
    }

    @Override
    public List<ActivityDTO> findPublicAnnouncementActivityById(final Long announcementId,
            final Date startDate, final Date endDate) {
        Query query = entityManager.createNamedQuery("getPublicAnnouncementActivityByIdAndDate",
                ActivityEntity.class);
        query.setParameter("announcementId", announcementId);
        query.setParameter("conceptId", ActivityConcept.ACTIVITY_CONCEPT_ANNOUNCEMENT.getId());
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        List<ActivityDTO> results = new ArrayList<ActivityDTO>();
        List<ActivityEntity> entities = query.getResultList();
        for (ActivityEntity entity : entities) {
            ActivityDTO result = new ActivityDTO(entity);
            results.add(result);
        }
        return results;
    }

    @Override
    public List<ActivityDTO> findAcbActivity(final List<CertificationBodyDTO> acbs,
            final Date startDate, final Date endDate) {
        List<Long> acbIds = new ArrayList<Long>();
        for (CertificationBodyDTO acb : acbs) {
            acbIds.add(acb.getId());
        }

        List<ActivityEntity> entities = getEntitiesByObjectIds(acbIds,
                ActivityConcept.ACTIVITY_CONCEPT_CERTIFICATION_BODY, startDate, endDate);

        List<ActivityDTO> results = new ArrayList<ActivityDTO>();
        for (ActivityEntity entity : entities) {
            ActivityDTO result = new ActivityDTO(entity);
            results.add(result);
        }
        return results;
    }

    @Override
    public List<ActivityDTO> findAtlActivity(final List<TestingLabDTO> atls, final Date startDate,
            final Date endDate) {
        List<Long> atlIds = new ArrayList<Long>();
        for (TestingLabDTO atl : atls) {
            atlIds.add(atl.getId());
        }

        List<ActivityEntity> entities = getEntitiesByObjectIds(atlIds,
                ActivityConcept.ACTIVITY_CONCEPT_ATL, startDate, endDate);

        List<ActivityDTO> results = new ArrayList<ActivityDTO>();
        for (ActivityEntity entity : entities) {
            ActivityDTO result = new ActivityDTO(entity);
            results.add(result);
        }
        return results;
    }

    @Override
    public List<ActivityDTO> findPendingListingActivity(final List<CertificationBodyDTO> pendingListingAcbs,
            final Date startDate, final Date endDate) {
        Query query = entityManager.createNamedQuery("getPendingListingActivityByAcbIdsAndDate",
                ActivityEntity.class);
        query.setParameter("conceptId", ActivityConcept.ACTIVITY_CONCEPT_PENDING_CERTIFIED_PRODUCT.getId());
        //parameters need to be strings
        List<String> acbIdParams = new ArrayList<String>();
        for (CertificationBodyDTO acb : pendingListingAcbs) {
            acbIdParams.add(acb.getId().toString());
        }
        query.setParameter("acbIds", acbIdParams);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        List<ActivityDTO> results = new ArrayList<ActivityDTO>();
        List<ActivityEntity> entities = query.getResultList();
        for (ActivityEntity entity : entities) {
            ActivityDTO result = new ActivityDTO(entity);
            results.add(result);
        }
        return results;
    }

    @Override
    public List<ActivityDTO> findPendingListingActivity(final Long pendingListingId,
            final Date startDate, final Date endDate) {
        List<ActivityEntity> entities = getEntitiesByObjectId(pendingListingId,
                ActivityConcept.ACTIVITY_CONCEPT_PENDING_CERTIFIED_PRODUCT, startDate, endDate);

        List<ActivityDTO> results = new ArrayList<ActivityDTO>();
        for (ActivityEntity entity : entities) {
            ActivityDTO result = new ActivityDTO(entity);
            results.add(result);
        }
        return results;
    }

    @Override
    public List<ActivityDTO> findUserActivity(final List<Long> userIds, final Date startDate, final Date endDate) {
        List<ActivityEntity> entities = getEntitiesByObjectIds(userIds,
                ActivityConcept.ACTIVITY_CONCEPT_USER, startDate, endDate);

        List<ActivityDTO> results = new ArrayList<ActivityDTO>();
        for (ActivityEntity entity : entities) {
            ActivityDTO result = new ActivityDTO(entity);
            results.add(result);
        }
        return results;
    }

    @Override
    public List<ActivityDTO> findByUserId(final Long userId, final Date startDate, final Date endDate) {

        List<ActivityEntity> entities = this.getEntitiesByUserId(userId, startDate, endDate);
        List<ActivityDTO> activities = new ArrayList<>();

        for (ActivityEntity entity : entities) {
            ActivityDTO result = new ActivityDTO(entity);
            activities.add(result);
        }
        return activities;
    }

    @Override
    public Map<Long, List<ActivityDTO>> findAllByUserInDateRange(final Date startDate, final Date endDate) {

        Map<Long, List<ActivityDTO>> activityByUser = new HashMap<Long, List<ActivityDTO>>();

        List<ActivityEntity> entities = this.getAllEntitiesInDateRange(startDate, endDate);

        for (ActivityEntity entity : entities) {

            ActivityDTO result = new ActivityDTO(entity);
            Long userId = result.getLastModifiedUser();
            if (userId != null) {
                if (activityByUser.containsKey(userId)) {
                    activityByUser.get(userId).add(result);
                } else {
                    List<ActivityDTO> activity = new ArrayList<ActivityDTO>();
                    activity.add(result);
                    activityByUser.put(userId, activity);
                }
            }
        }
        return activityByUser;
    }

    private ActivityEntity getEntityById(final Long id) throws EntityRetrievalException {

        ActivityEntity entity = null;
        String queryStr = "from ActivityEntity ae " + "LEFT OUTER JOIN FETCH ae.user " + "where (ae.id = :entityid) ";
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("entityid", id);
        List<ActivityEntity> result = query.getResultList();

        if (result.size() > 1) {
            throw new EntityRetrievalException("Data error. Duplicate criterion id in database.");
        }

        if (result.size() > 0) {
            entity = result.get(0);
        }
        return entity;
    }

    private List<ActivityEntity> getEntitiesByObjectIds(final List<Long> objectIds,
            final ActivityConcept concept, final Date startDate, final Date endDate) {
        String queryStr = "SELECT ae "
            + "FROM ActivityEntity ae "
            + "LEFT OUTER JOIN FETCH ae.user "
            + "WHERE ae.activityObjectId IN (:objectIds) "
            + "AND ae.activityObjectConceptId = :conceptId ";
        if (startDate != null) {
            queryStr += "AND (ae.activityDate >= :startDate) ";
        }
        if (endDate != null) {
            queryStr += "AND (ae.activityDate <= :endDate) ";
        }
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("objectIds", objectIds);
        query.setParameter("conceptId", concept.getId());
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }

        List<ActivityEntity> result = query.getResultList();
        return result;
    }

    private List<ActivityEntity> getEntitiesByObjectId(final Long objectId, final ActivityConcept concept,
            final Date startDate, final Date endDate) {
        String queryStr = "from ActivityEntity ae " + "LEFT OUTER JOIN FETCH ae.user "
                + "WHERE (ae.activityObjectId = :objectid)  " + "AND (ae.activityObjectConceptId = :conceptid) ";
        if (startDate != null) {
            queryStr += "AND (ae.activityDate >= :startDate) ";
        }
        if (endDate != null) {
            queryStr += "AND (ae.activityDate <= :endDate) ";
        }
        Query query = entityManager.createQuery(queryStr, ActivityEntity.class);
        query.setParameter("objectid", objectId);
        query.setParameter("conceptid", concept.getId());
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }
        List<ActivityEntity> result = query.getResultList();
        return result;
    }

    private List<ActivityEntity> getEntitiesByConcept(final ActivityConcept concept, final Date startDate,
            final Date endDate) {
        String queryStr = "from ActivityEntity ae " + "LEFT OUTER JOIN FETCH ae.user "
                + "WHERE (ae.activityObjectConceptId = :conceptid) ";
        if (startDate != null) {
            queryStr += "AND (ae.activityDate >= :startDate) ";
        }
        if (endDate != null) {
            queryStr += "AND (ae.activityDate <= :endDate) ";
        }
        Query query = entityManager.createQuery(queryStr, ActivityEntity.class);
        query.setParameter("conceptid", concept.getId());
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }
        List<ActivityEntity> result = query.getResultList();
        return result;
    }

    private List<ActivityEntity> getAllEntitiesInDateRange(final Date startDate, final Date endDate) {
        String queryStr = "FROM ActivityEntity ae " + "LEFT OUTER JOIN FETCH ae.user " + "WHERE ";
        if (startDate != null) {
            if (!queryStr.endsWith("WHERE ")) {
                queryStr += "AND ";
            }
            queryStr += "(ae.activityDate >= :startDate) ";
        }
        if (endDate != null) {
            if (!queryStr.endsWith("WHERE ")) {
                queryStr += "AND ";
            }
            queryStr += "(ae.activityDate <= :endDate)";
        }

        Query query = entityManager.createQuery(queryStr, ActivityEntity.class);
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }
        List<ActivityEntity> result = query.getResultList();
        return result;
    }

    private List<ActivityEntity> getEntitiesByUserId(final Long userId, final Date startDate, final Date endDate) {
        String queryStr = "from ActivityEntity ae " + "LEFT OUTER JOIN FETCH ae.user "
                + "WHERE (ae.lastModifiedUser = :userid) ";
        if (startDate != null) {
            queryStr += "AND (ae.activityDate >= :startDate) ";
        }
        if (endDate != null) {
            queryStr += "AND (ae.activityDate <= :endDate) ";
        }

        Query query = entityManager.createQuery(queryStr, ActivityEntity.class);
        query.setParameter("userid", userId);
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }

        List<ActivityEntity> result = query.getResultList();
        return result;
    }

}
