
package com.vmware.vim25;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfClusterNotAttemptedVmInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfClusterNotAttemptedVmInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ClusterNotAttemptedVmInfo" type="{urn:vim25}ClusterNotAttemptedVmInfo" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfClusterNotAttemptedVmInfo", propOrder = {
    "clusterNotAttemptedVmInfo"
})
public class ArrayOfClusterNotAttemptedVmInfo {

    @XmlElement(name = "ClusterNotAttemptedVmInfo")
    protected List<ClusterNotAttemptedVmInfo> clusterNotAttemptedVmInfo;

    /**
     * Gets the value of the clusterNotAttemptedVmInfo property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the clusterNotAttemptedVmInfo property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getClusterNotAttemptedVmInfo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ClusterNotAttemptedVmInfo }
     * 
     * 
     */
    public List<ClusterNotAttemptedVmInfo> getClusterNotAttemptedVmInfo() {
        if (clusterNotAttemptedVmInfo == null) {
            clusterNotAttemptedVmInfo = new ArrayList<ClusterNotAttemptedVmInfo>();
        }
        return this.clusterNotAttemptedVmInfo;
    }

}