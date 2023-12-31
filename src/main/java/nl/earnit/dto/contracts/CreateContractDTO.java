package nl.earnit.dto.contracts;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The type Create contract.
 */
@XmlRootElement
public class CreateContractDTO {

    private String role;
    private String description;

    /**
     * Instantiates a new Create contract.
     *
     * @param role        the role
     * @param description the description
     */
    public CreateContractDTO(String role, String description) {
        this.role = role;
        this.description = description;
    }

    /**
     * Instantiates a new Create contract.
     */
    public CreateContractDTO() {
    }

    /**
     * Gets role.
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets role.
     *
     * @param role the role
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets description.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
