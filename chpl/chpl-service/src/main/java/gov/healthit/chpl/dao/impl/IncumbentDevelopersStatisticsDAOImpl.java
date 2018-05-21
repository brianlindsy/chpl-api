package gov.healthit.chpl.dao.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import gov.healthit.chpl.auth.Util;
import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.dao.IncumbentDevelopersStatisticsDAO;
import gov.healthit.chpl.dto.IncumbentDevelopersStatisticsDTO;
import gov.healthit.chpl.entity.IncumbentDevelopersStatisticsEntity;

/**
 * The implementation for IncumbentDevelopersStatisticsDAO.
 * @author alarned
 *
 */
@Repository("incumbentDevelopersStatisticsDAO")
public class IncumbentDevelopersStatisticsDAOImpl extends BaseDAOImpl implements IncumbentDevelopersStatisticsDAO {
    private static final long MODIFIED_USER_ID = -3L;

    @Override
    public List<IncumbentDevelopersStatisticsDTO> findAll() {
        List<IncumbentDevelopersStatisticsEntity> result = this.findAllEntities();
        List<IncumbentDevelopersStatisticsDTO> dtos = new ArrayList<IncumbentDevelopersStatisticsDTO>(result.size());
        for (IncumbentDevelopersStatisticsEntity entity : result) {
            dtos.add(new IncumbentDevelopersStatisticsDTO(entity));
        }
        return dtos;
    }

    @Override
    public void delete(final Long id) throws EntityRetrievalException {
        IncumbentDevelopersStatisticsEntity toDelete = getEntityById(id);

        if (toDelete != null) {
            toDelete.setDeleted(true);
            toDelete.setLastModifiedUser(getUserId());
            entityManager.merge(toDelete);
        }
    }

    @Override
    public IncumbentDevelopersStatisticsEntity create(final IncumbentDevelopersStatisticsDTO dto)
            throws EntityCreationException, EntityRetrievalException {
        IncumbentDevelopersStatisticsEntity entity = new IncumbentDevelopersStatisticsEntity();
        entity.setNewCount(dto.getNewCount());
        entity.setIncumbentCount(dto.getIncumbentCount());
        entity.setOldCertificationEditionId(dto.getOldCertificationEditionId());
        entity.setNewCertificationEditionId(dto.getNewCertificationEditionId());

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

    private List<IncumbentDevelopersStatisticsEntity> findAllEntities() {
        Query query = entityManager
                .createQuery("SELECT a from IncumbentDevelopersStatisticsEntity a where (NOT a.deleted = true)");
        return query.getResultList();
    }

    private IncumbentDevelopersStatisticsEntity getEntityById(final Long id) throws EntityRetrievalException {
        IncumbentDevelopersStatisticsEntity entity = null;

        Query query = entityManager.createQuery(
                "from IncumbentDevelopersStatisticsEntity a where (NOT deleted = true) AND (id = :entityid) ",
                IncumbentDevelopersStatisticsEntity.class);
        query.setParameter("entityid", id);
        List<IncumbentDevelopersStatisticsEntity> result = query.getResultList();

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
