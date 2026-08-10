package com.dinogo.port.inventory;

import java.util.List;
import java.util.Map;

public interface ProductInventoryPort {
    List<OrderSkuSnapshot> validateAndDeduct(Map<Integer, Integer> quantitiesBySku);
    void restore(Map<Integer, Integer> quantitiesBySku);
}
