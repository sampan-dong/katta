package com.ivyft.katta.node;

import com.ivyft.katta.Katta;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * <pre>
 *
 * Created by IntelliJ IDEA.
 * User: zhenqin
 * Date: 15/12/7
 * Time: 10:43
 * To change this template use File | Settings | File Templates.
 *
 * </pre>
 *
 * @author zhenqin
 */
public class KattaTest {


    public KattaTest() {
    }


    @Test
    public void testTimeUnit() throws Exception {
        System.out.println(System.currentTimeMillis());
        System.out.println(TimeUnit.SECONDS.toMillis(2));

    }

    @Test
    public void testCreateIndex() throws Exception {
        Katta.main(new String[]{"createIndex", "ts", "5", "10", "-s"});

    }
}
