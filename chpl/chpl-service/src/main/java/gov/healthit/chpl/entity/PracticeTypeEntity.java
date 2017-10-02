package gov.healthit.chpl.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Object mapping for hibernate-handled table: practice_type.
 *
 *
 * @author autogenerated / cwatson
 */

@Entity
@Table(name = "practice_type", schema = "openchpl")
public class PracticeTypeEntity implements Serializable {

    /** Serial Version UID. */
    private static final long serialVersionUID = -512191905822294896L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "practice_type_id", nullable = false)
    private Long id;

    @Basic(optional = false)
    @Column(name = "creation_date", nullable = false)
    private Date creationDate;

    @Basic(optional = false)
    @Column(name = "deleted", nullable = false)
    private Boolean deleted;

    @Basic(optional = false)
    @Column(nullable = false, length = 250)
    private String description;

    @Basic(optional = false)
    @Column(name = "last_modified_date", nullable = false)
    private Date lastModifiedDate;

    @Basic(optional = false)
    @Column(name = "last_modified_user", nullable = false)
    private Long lastModifiedUser;

    @Basic(optional = false)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * Default constructor, mainly for hibernate use.
     */
    public PracticeTypeEntity() {
        // Default constructor
    }

    /**
     * Constructor taking a given ID.
     * 
     * @param id
     *            to set
     */
    public PracticeTypeEntity(Long id) {
        this.id = id;
    }

    /**
     * Return the type of this class. Useful for when dealing with proxies.
     * 
     * @return Defining class.
     */
    @Transient
    public Class<?> getClassType() {
        return PracticeTypeEntity.class;
    }

    /**
     * Return the value associated with the column: creationDate.
     * 
     * @return A Date object (this.creationDate)
     */
    public Date getCreationDate() {
        return this.creationDate;

    }

    /**
     * Set the value related to the column: creationDate.
     * 
     * @param creationDate
     *            the creationDate value you wish to set
     */
    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Return the value associated with the column: deleted.
     * 
     * @return A Boolean object (this.deleted)
     */
    public Boolean isDeleted() {
        return this.deleted;

    }

    /**
     * Set the value related to the column: deleted.
     * 
     * @param deleted
     *            the deleted value you wish to set
     */
    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Return the value associated with the column: description.
     * 
     * @return A String object (this.description)
     */
    public String getDescription() {
        return this.description;

    }

    /**
     * Set the value related to the column: description.
     * 
     * @param description
     *            the description value you wish to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Return the value associated with the column: id.
     * 
     * @return A Long object (this.id)
     */
    public Long getId() {
        return this.id;

    }

    /**
     * Set the value related to the column: id.
     * 
     * @param id
     *            the id value you wish to set
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Return the value associated with the column: lastModifiedDate.
     * 
     * @return A Date object (this.lastModifiedDate)
     */
    public Date getLastModifiedDate() {
        return this.lastModifiedDate;

    }

    /**
     * Set the value related to the column: lastModifiedDate.
     * 
     * @param lastModifiedDate
     *            the lastModifiedDate value you wish to set
     */
    public void setLastModifiedDate(final Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Return the value associated with the column: lastModifiedUser.
     * 
     * @return A Long object (this.lastModifiedUser)
     */
    public Long getLastModifiedUser() {
        return this.lastModifiedUser;

    }

    /**
     * Set the value related to the column: lastModifiedUser.
     * 
     * @param lastModifiedUser
     *            the lastModifiedUser value you wish to set
     */
    public void setLastModifiedUser(final Long lastModifiedUser) {
        this.lastModifiedUser = lastModifiedUser;
    }

    /**
     * Return the value associated with the column: name.
     * 
     * @return A String object (this.name)
     */
    public String getName() {
        return this.name;

    }

    /**
     * Set the value related to the column: name.
     * 
     * @param name
     *            the name value you wish to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Provides toString implementation.
     * 
     * @see java.lang.Object#toString()
     * @return String representation of this class.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("creationDate: " + this.getCreationDate() + ", ");
        sb.append("deleted: " + this.isDeleted() + ", ");
        sb.append("description: " + this.getDescription() + ", ");
        sb.append("id: " + this.getId() + ", ");
        sb.append("lastModifiedDate: " + this.getLastModifiedDate() + ", ");
        sb.append("lastModifiedUser: " + this.getLastModifiedUser() + ", ");
        sb.append("name: " + this.getName());
        return sb.toString();
    }

}
