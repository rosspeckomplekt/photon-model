
package com.vmware.pbm;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfPbmDatastoreSpaceStatistics complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfPbmDatastoreSpaceStatistics"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="PbmDatastoreSpaceStatistics" type="{urn:pbm}PbmDatastoreSpaceStatistics" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfPbmDatastoreSpaceStatistics", propOrder = {
    "pbmDatastoreSpaceStatistics"
})
public class ArrayOfPbmDatastoreSpaceStatistics {

    @XmlElement(name = "PbmDatastoreSpaceStatistics")
    protected List<PbmDatastoreSpaceStatistics> pbmDatastoreSpaceStatistics;

    /**
     * Gets the value of the pbmDatastoreSpaceStatistics property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the pbmDatastoreSpaceStatistics property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPbmDatastoreSpaceStatistics().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PbmDatastoreSpaceStatistics }
     * 
     * 
     */
    public List<PbmDatastoreSpaceStatistics> getPbmDatastoreSpaceStatistics() {
        if (pbmDatastoreSpaceStatistics == null) {
            pbmDatastoreSpaceStatistics = new ArrayList<PbmDatastoreSpaceStatistics>();
        }
        return this.pbmDatastoreSpaceStatistics;
    }

}
