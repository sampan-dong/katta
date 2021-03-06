package com.ivyft.katta.hadoop;


import java.io.IOException;

import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.lucene.document.Document;

/**
 * <pre>
 *
 * Created by IntelliJ IDEA.
 * User: zhenqin
 * Date: 13-10-31
 * Time: 上午8:36
 * To change this template use File | Settings | File Templates.
 *
 * </pre>
 *
 * @author zhenqin
 */
public class LuceneDocumentOutputFormat<K> extends FileOutputFormat<K, Document> {



    public final static String LUCENE_INDEXWRITER_ANALYZER = "lucene.index.writer.analyzer.class";



    public final static String LUCENE_INDEX_TEMP_DIR = "lucene.index.tmp.dir";



    public final static String LUCENE_MAX_BUFFERED_DOCS = "lucene.max.buffered.docs";



    public final static String LUCENE_MAX_BUFFERED_SIZE_MB = "lucene.max.buffered.size.mb";


    public final static String LUCENE_COMMIT_COUNTER = "lucene.commit.counter";


    public LuceneDocumentOutputFormat() {

    }

    @Override
    public RecordWriter<K, Document> getRecordWriter(TaskAttemptContext job)
            throws IOException, InterruptedException {
        return new LuceneDocumentWriter<K>(job);
    }




    public static class LuceneDocumentWriter<K> extends RecordWriter<K, Document>{
        // LuceneWriter是包含Lucene的IndexWriter对象的类
        final LuceneOutputWriter lw = new LuceneOutputWriter();

        public LuceneDocumentWriter(JobContext job) {
            // 完成索引前的配置工作
            try {
                // 修改，更具有通用性
                lw.open(job.getConfiguration());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }


        @Override
        public void write(K key, Document value) throws IOException, InterruptedException {
            // 建立索引
            lw.write(value);
        }

        @Override
        public void close(TaskAttemptContext context) throws IOException, InterruptedException {
            // 完成索引优化，关闭IndexWriter的对象
            lw.close();
        }
    }
}