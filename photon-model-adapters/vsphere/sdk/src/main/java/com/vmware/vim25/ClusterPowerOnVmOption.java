
package com.vmware.vim25;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ClusterPowerOnVmOption.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ClusterPowerOnVmOption"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="OverrideAutomationLevel"/&gt;
 *     &lt;enumeration value="ReserveResources"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ClusterPowerOnVmOption")
@XmlEnum
public enum ClusterPowerOnVmOption {

    @XmlEnumValue("OverrideAutomationLevel")
    OVERRIDE_AUTOMATION_LEVEL("OverrideAutomationLevel"),
    @XmlEnumValue("ReserveResources")
    RESERVE_RESOURCES("ReserveResources");
    private final String value;

    ClusterPowerOnVmOption(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ClusterPowerOnVmOption fromValue(String v) {
        for (ClusterPowerOnVmOption c: ClusterPowerOnVmOption.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
