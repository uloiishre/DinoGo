package com.dinogo.sysmsg.service;

/** OA Send 提交後用於觸發廣播的不可變事件資料。 */
public record OaBroadcastRequested(Integer sendId) {
}
