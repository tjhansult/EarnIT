package nl.earnit.models.db;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.math.BigInteger;

@XmlRootElement
public class UserContract {
    private String id;
    private String contractId;
    private Float hourlyWage;
    private boolean active;

    public UserContract() {}

    public UserContract(String id, String contractId, Float hourlyWage, boolean active) {
        this.id = id;
        this.contractId = contractId;
        this.hourlyWage = hourlyWage;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public Float getHourlyWage() {
        return hourlyWage;
    }

    public void setHourlyWage(Float hourlyWage) {
        this.hourlyWage = hourlyWage;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
