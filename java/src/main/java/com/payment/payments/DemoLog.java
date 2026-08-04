package com.payment.payments;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Ghi nhat ky tung buoc cua mot lan chay demo.
 *
 * Dung ThreadLocal — moi thread mot cuon so rieng.
 * Day CHINH LA co che Spring dung de gan Connection vao transaction:
 * proxy bo connection vao "hop rieng cua thread", moi DAO tren thread do lay ra dung cai ay.
 * Xem lai phan giai thich ve ThreadLocal.
 */
@Component
public class DemoLog {

    private static final ThreadLocal<List<String>> CUON_SO = ThreadLocal.withInitial(ArrayList::new);

    public void batDau() {
        CUON_SO.get().clear();
    }

    public void ghi(String dong) {
        List<String> so = CUON_SO.get();
        so.add(String.format("[%02d] %s", so.size(), dong));
    }

    public List<String> doc() {
        return List.copyOf(CUON_SO.get());
    }

    public void don() {
        CUON_SO.remove();
    }
}
