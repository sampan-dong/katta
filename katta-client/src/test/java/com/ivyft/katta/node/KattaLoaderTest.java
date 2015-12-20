package com.ivyft.katta.node;

import com.ivyft.katta.client.KattaClient;
import com.ivyft.katta.client.KattaLoader;
import com.ivyft.katta.client.LuceneClient;
import com.ivyft.katta.util.HadoopUtil;
import com.ivyft.katta.util.ZkConfiguration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.junit.Test;

import java.util.Random;

/**
 * <pre>
 *
 * Created by IntelliJ IDEA.
 * User: zhenqin
 * Date: 15/11/27
 * Time: 20:54
 * To change this template use File | Settings | File Templates.
 *
 * </pre>
 *
 * @author zhenqin
 */
public class KattaLoaderTest {

    public KattaLoaderTest() {

    }



    @Test
    public void testCreatedLoaderByMaster() throws Exception {
        LuceneClient client = new LuceneClient(new ZkConfiguration());
        KattaLoader<Object> test = client.getKattaLoader("hello");

        for (int i = 0; i < 10000; i++) {
            System.out.println(test.addBean("java" + i, "hello" + new Random().nextInt()));
        }
        test.commit();

        for (int i = 0; i < 10000; i++) {
            System.out.println(test.addBean("java" + i, "hello" + new Random().nextInt()));
        }

        test.close();
    }


    @Test
    public void testSend() throws Exception {
        KattaLoader<String> loader = new KattaClient<String>("zhenqin-pro102", 8440, "hello");
        for (int i = 0; i < 10000; i++) {
            System.out.println(loader.addBean("java" + i, "hello" + new Random().nextInt()));
        }
        loader.commit();
    }


    @Test
    public void testDelete() throws Exception {
        FileSystem fs = HadoopUtil.getFileSystem();
        FileStatus[] fileStatuses = fs.listStatus(new Path("/user/katta/data/hello"));
        for (FileStatus status : fileStatuses) {
            FileStatus[] commits = fs.listStatus(status.getPath(), new PathFilter() {
                @Override
                public boolean accept(Path path) {
                    return path.getName().startsWith("commit-");
                }
            });

            if(commits != null) {
                for (FileStatus commit : commits) {
                    System.out.println(commit.getPath().toString());
                    fs.delete(commit.getPath(), true);
                }
            }
        }

    }
}
