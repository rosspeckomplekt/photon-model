
package com.vmware.vim25;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for OrAlarmExpression complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OrAlarmExpression"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:vim25}AlarmExpression"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="expression" type="{urn:vim25}AlarmExpression" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OrAlarmExpression", propOrder = {
    "expression"
})
public class OrAlarmExpression
    extends AlarmExpression
{

    @XmlElement(required = true)
    protected List<AlarmExpression> expression;

    /**
     * Gets the value of the expression property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the expression property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getExpression().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AlarmExpression }
     * 
     * 
     */
    public List<AlarmExpression> getExpression() {
        if (expression == null) {
            expression = new ArrayList<AlarmExpression>();
        }
        return this.expression;
    }

}
