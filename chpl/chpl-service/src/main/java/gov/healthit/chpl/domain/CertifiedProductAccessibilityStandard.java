package gov.healthit.chpl.domain;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import gov.healthit.chpl.dto.CertifiedProductAccessibilityStandardDTO;

@XmlType(namespace = "http://chpl.healthit.gov/listings")
@XmlAccessorType(XmlAccessType.FIELD)
public class CertifiedProductAccessibilityStandard implements Serializable {
	private static final long serialVersionUID = -676179466407109456L;
	
	@XmlElement(required = true)
	private Long id;
	
	@XmlElement(required = true)
	private Long accessibilityStandardId;
	
	@XmlElement(required = true)
	private String accessibilityStandardName;

	public CertifiedProductAccessibilityStandard() {
		super();
	}
	
	public CertifiedProductAccessibilityStandard(CertifiedProductAccessibilityStandardDTO dto) {
		this.id = dto.getId();
		this.accessibilityStandardId = dto.getAccessibilityStandardId();
		this.accessibilityStandardName = dto.getAccessibilityStandardName();
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAccessibilityStandardId() {
		return accessibilityStandardId;
	}

	public void setAccessibilityStandardId(Long accessibilityStandardId) {
		this.accessibilityStandardId = accessibilityStandardId;
	}

	public String getAccessibilityStandardName() {
		return accessibilityStandardName;
	}

	public void setAccessibilityStandardName(String accessibilityStandardName) {
		this.accessibilityStandardName = accessibilityStandardName;
	}

}
