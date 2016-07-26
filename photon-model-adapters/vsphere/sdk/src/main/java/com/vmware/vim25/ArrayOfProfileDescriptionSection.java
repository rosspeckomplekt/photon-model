
package com.vmware.vim25;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfProfileDescriptionSection complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfProfileDescriptionSection"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ProfileDescriptionSection" type="{urn:vim25}ProfileDescriptionSection" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfProfileDescriptionSection", propOrder = {
    "profileDescriptionSection"
})
public class ArrayOfProfileDescriptionSection {

    @XmlElement(name = "ProfileDescriptionSection")
    protected List<ProfileDescriptionSection> profileDescriptionSection;

    /**
     * Gets the value of the profileDescriptionSection property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the profileDescriptionSection property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProfileDescriptionSection().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ProfileDescriptionSection }
     * 
     * 
     */
    public List<ProfileDescriptionSection> getProfileDescriptionSection() {
        if (profileDescriptionSection == null) {
            profileDescriptionSection = new ArrayList<ProfileDescriptionSection>();
        }
        return this.profileDescriptionSection;
    }

}