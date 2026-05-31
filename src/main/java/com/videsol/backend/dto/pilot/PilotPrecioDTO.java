package com.videsol.backend.dto.pilot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mapea la entidad Lista de Precio tal como la devuelve Pilot.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PilotPrecioDTO(
        String id,
        String code,
        String name,
        String price,
        @JsonProperty("commercial_condition") String commercialCondition,
        @JsonProperty("valid_date") String validDate,
        @JsonProperty("rep_authorized_discount") String repAuthorizedDiscount,
        @JsonProperty("is_saving_plan") String isSavingPlan,
        @JsonProperty("saving_plan_type") PilotSavingPlanTypeDTO savingPlanType,
        @JsonProperty("savingplan_shares") String savingPlanShares,
        @JsonProperty("savingplan_first_share_amount") String savingPlanFirstShareAmount,
        @JsonProperty("full_name") String fullName,
        String description,
        String visible,
        @JsonProperty("business_type") PilotBusinessTypeDTO businessType,
        PilotModelDTO model
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotModelDTO(
            String code,
            String name,
            PilotBrandDTO brand
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotBrandDTO(
            String code,
            String name
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotBusinessTypeDTO(
            String id,
            String code,
            String name
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PilotSavingPlanTypeDTO(
            String code,
            String name
    ) {}
}
