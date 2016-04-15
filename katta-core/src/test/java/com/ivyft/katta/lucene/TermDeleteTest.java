package com.ivyft.katta.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * <pre>
 *
 * Created by IntelliJ IDEA.
 * User: zhenqin
 * Date: 15/11/26
 * Time: 17:28
 * To change this template use File | Settings | File Templates.
 *
 * </pre>
 *
 * @author zhenqin
 */
public class TermDeleteTest {


    protected String indexPath = "../data/test/1OgAEMwPu0snaXcdOhe";


    protected IndexWriter indexWriter;


    IndexReader indexReader;


    IndexSearcher indexSearcher;

    public TermDeleteTest() {
    }


    @Before
    public void setUp() throws Exception {
        FSDirectory directory = FSDirectory.open(new File(indexPath));
        indexWriter = new IndexWriter(directory, new IndexWriterConfig(Version.LUCENE_46, new StandardAnalyzer(Version.LUCENE_46)));

        indexReader = DirectoryReader.open(directory);

        indexSearcher = new IndexSearcher(indexReader);
    }


    @After
    public void tearDown() throws Exception {
        indexWriter.close();
        indexReader.close();
    }


    @Test
    public void testAddDocs() throws Exception {

        Document document = new Document();
        document.add(new StringField("ID", "123456", Field.Store.YES));
        document.add(new StringField("NAME", "ZhenQin", Field.Store.YES));

        indexWriter.addDocument(document);

        document = new Document();
        document.add(new StringField("ID", "654321", Field.Store.YES));
        document.add(new StringField("NAME", "YY", Field.Store.YES));

        indexWriter.addDocument(document);
        indexWriter.commit();


        testSelfLoadSearch();
    }

    @Test
    public void testDelete() throws Exception {
        System.out.println(indexWriter.numDocs());
        indexWriter.deleteDocuments(new Term("ID", "654321"));
        indexWriter.commit();
        indexWriter.forceMergeDeletes(true);
        System.out.println(indexWriter.numDocs());


        testSelfLoadSearch();
    }

    @Test
    public void testReloadSearch() throws Exception {
        indexReader = DirectoryReader.open(indexWriter, true);
        indexSearcher = new IndexSearcher(indexReader);

        System.out.println(indexSearcher.getIndexReader().numDocs());
        System.out.println(indexSearcher.getIndexReader().maxDoc());


        TopDocs word = indexSearcher.search(new MatchAllDocsQuery(), 10);
        System.out.println(word.totalHits);

        ScoreDoc[] scoreDocs = word.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println(indexSearcher.doc(scoreDoc.doc));
        }
    }




    @Test
    public void testSelfLoadSearch() throws Exception {
        DirectoryReader reader = DirectoryReader.openIfChanged((DirectoryReader)indexSearcher.getIndexReader());
        if(reader != null) {
            System.out.println("self load");
            indexSearcher = new IndexSearcher(reader);
        }
        System.out.println(indexSearcher.getIndexReader().numDocs());
        System.out.println(indexSearcher.getIndexReader().maxDoc());


        TopDocs word = indexSearcher.search(new MatchAllDocsQuery(), 10);
        System.out.println(word.totalHits);

        ScoreDoc[] scoreDocs = word.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println(indexSearcher.doc(scoreDoc.doc));
        }
    }

    @Test
    public void testSearch() throws Exception {
        testSelfLoadSearch();
        System.out.println(indexSearcher.getIndexReader().numDocs());
        System.out.println(indexSearcher.getIndexReader().maxDoc());


        TopDocs word = indexSearcher.search(new MatchAllDocsQuery(), 10);
        System.out.println(word.totalHits);

        ScoreDoc[] scoreDocs = word.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println(indexSearcher.doc(scoreDoc.doc));
        }
    }
}
