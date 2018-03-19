package gov.healthit.chpl.dao.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import gov.healthit.chpl.auth.Util;
import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.dao.ParticipantGenderStatisticsDAO;
import gov.healthit.chpl.dto.ParticipantGenderStatisticsDTO;
import gov.healthit.chpl.entity.ParticipantGenderStatisticsEntity;

/**
 * The implementation for ParticipantGenderStatisticsDAO.
 * @author TYoung
 *
 */
@Repository("participantGenderStatisticsDAO")
public class ParticipantGenderStatisticsDAOImpl extends BaseDAOImpl implements ParticipantGenderStatisticsDAO {
    private static final long MODIFIED_USER_ID = -3L;

    @Override
    public List<ParticipantGenderStatisticsDTO> findAll() {
        List<ParticipantGenderStatisticsEntity> result = this.findAllEntities();
        List<ParticipantGenderStatisticsDTO> dtos = new ArrayList<ParticipantGenderStatisticsDTO>(result.size());
        for (ParticipantGenderStatisticsEntity entity : result) {
            dtos.add(new ParticipantGenderStatisticsDTO(entity));
        }
        return dtos;
    }

    @Override
    public void delete(final Long id) throws EntityRetrievalException {
        ParticipantGenderStatisticsEntity toDelete = getEntityById(id);

        if (toDelete != null) {
            toDelete.setDeleted(true);
            toDelete.setLastModifiedUser(getUserId());
            entityManager.merge(toDelete);
        }
    }

    @Override
    public ParticipantGenderStatisticsEntity create(final ParticipantGenderStatisticsDTO dto)
            throws EntityCreationException, EntityRetrievalException {
        ParticipantGenderStatisticsEntity entity = new ParticipantGenderStatisticsEntity();
        entity.setFemaleCount(dto.getFemaleCount());
        entity.setMaleCount(dto.getMaleCount());

        if (dto.getDeleted() != null) {
            entity.setDeleted(dto.getDeleted());
        } else {
            entity.setDeleted(false);
        }

        if (dto.getLastModifiedUser() != null) {
            entity.setLastModifiedUser(dto.getLastModifiedUser());
        } else {
            entity.setLastModifiedUser(getUserId());
        }
        if (dto.getLastModifiedDate() != null) {
            entity.setLastModifiedDate(dto.getLastModifiedDate());
        } else {
            entity.setLastModifiedDate(new Date());
        }
        if (dto.getCreationDate() != null) {
            entity.setCreationDate(dto.getCreationDate());
        } else {
            entity.setCreationDate(new Date());
        }

        entityManager.persist(entity);
        entityManager.flush();
        return entity;

    }

    private List<ParticipantGenderStatisticsEntity> findAllEntities() {
        Query query = entityManager
                .createQuery("SELECT a from ParticipantGenderStatisticsEntity a where (NOT a.deleted = true)");
        return query.getResultList();
    }

    private ParticipantGenderStatisticsEntity getEntityById(final Long id) throws EntityRetrievalException {
        ParticipantGenderStatisticsEntity entity = null;

        Query query = entityManager.createQuery(
                "from ParticipantGenderStatisticsEntity a where (NOT deleted = true) AND (id = :entityid) ",
                ParticipantGenderStatisticsEntity.class);
        query.setParameter("entityid", id);
        List<ParticipantGenderStatisticsEntity> result = query.getResultList();

        if (result.size() > 1) {
            throw new EntityRetrievalException("Data error. Duplicate address id in database.");
        } else if (result.size() == 1) {
            entity = result.get(0);
        }

        return entity;
    }

    private Long getUserId() {
        // If there is no user the current context, assume this is a system
        // process
        if (Util.getCurrentUser() == null || Util.getCurrentUser().getId() == null) {
            return MODIFIED_USER_ID;
        } else {
            return Util.getCurrentUser().getId();
        }
    }
}
