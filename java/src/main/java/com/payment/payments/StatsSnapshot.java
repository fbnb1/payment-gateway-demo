package com.payment.payments;

/**
 * @param realCreateCalls  create() thực sự chạy bao nhiêu lần
 * @param proxyIntercepted proxy chặn được bao nhiêu lần
 * @param bypassedProxy    số lần LỌT QUA proxy — đây là con số cần nhìn
 */
public record StatsSnapshot(int realCreateCalls, int proxyIntercepted, int bypassedProxy) {}
