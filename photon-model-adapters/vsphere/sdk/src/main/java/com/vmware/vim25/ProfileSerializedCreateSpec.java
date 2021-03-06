
package com.vmware.vim25;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ProfileSerializedCreateSpec complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ProfileSerializedCreateSpec"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:vim25}ProfileCreateSpec"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="profileConfigString" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProfileSerializedCreateSpec", propOrder = {
    "profileConfigString"
})
@XmlSeeAlso({
    HostProfileSerializedHostProfileSpec.class
})
public class ProfileSerializedCreateSpec
    extends ProfileCreateSpec
{

    @XmlElement(required = true)
    protected String profileConfigString;

    /**
     * Gets the value of the profileConfigString property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProfileConfigString() {
        return profileConfigString;
    }

    /**
     * Sets the value of the profileConfigString property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProfileConfigString(String value) {
        this.profileConfigString = value;
    }

}
