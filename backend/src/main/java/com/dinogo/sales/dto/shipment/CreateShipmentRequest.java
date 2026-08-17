package com.dinogo.sales.dto.shipment;

import jakarta.validation.constraints.Size;

public record CreateShipmentRequest(
        @Size(max = 100) String carrierName,
        @Size(max = 100) String trackingNo) {
}
