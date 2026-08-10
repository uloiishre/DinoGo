package com.dinogo.port.cart;

import java.util.List;

public interface CheckoutCartPort {

    void clearCheckedOutItems(
            Integer memberId,
            List<Integer> cartItemIds,
            Integer orderId);
}
