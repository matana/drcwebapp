package de.uni_koeln.spinfo.drc.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.uni_koeln.spinfo.drc.util.PropertyReader;

@Service
public class Searcher {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	PropertyReader propertyReader;

	private int totalHits;

	/**
	 * Search a given index (index directory specified in
	 * src/main/resources/application.properties).
	 * 
	 * @param q
	 *            the query
	 * @return a list of hits, wrapped up in objects of type {@link SearchResult}.
	 * @throws IOException
	 * @throws ParseException
	 */
	public List<SearchResult> search(String q) throws IOException, ParseException {

		String indexDir = propertyReader.getIndexDir();
		Directory dir = new SimpleFSDirectory(new File(indexDir).toPath());
		DirectoryReader dirReader = DirectoryReader.open(dir);
		IndexSearcher is = new IndexSearcher(dirReader);

		StandardAnalyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("contents", analyzer);
		Query query = parser.parse(q);
		TopDocs hits = is.search(query, propertyReader.getMaxHits());
		this.setTotalHits(hits.totalHits);
		logger.info("QUERY: " + query);

		List<SearchResult> resultList = new ArrayList<SearchResult>();
		for (int i = 0; i < hits.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = hits.scoreDocs[i];
			Document doc = is.doc(scoreDoc.doc);
			SearchResult result = wrapFieldResults(doc);
			resultList.add(result);
		}

		dirReader.close();
		return resultList;
	}

	/*
	 * Wrap up all field contents of a hit.
	 * 
	 * @param doc
	 * @return a SearchResult object reflecting the given Document.
	 */
	private SearchResult wrapFieldResults(Document doc) {

		String filename = doc.get("url");
		String pageId = doc.get("pageId");
		String content = doc.get("contents");
		String language = doc.get("languages");
		String chapterId = doc.get("chapterId");
		String chapter = doc.get("chapters");
		String volumeId = doc.get("volumeId");
		String volume = doc.get("volume");

		SearchResult result = new SearchResult();
		result.setFilename(filename);
		result.setContent(content);
		result.setPageId(pageId);
		result.setLanguage(language);
		result.setChapterId(chapterId);
		result.setChapter(chapter);
		result.setVolumeId(volumeId);
		result.setVolume(volume);

		// das gleiche, etwas kompakter:
		result = new SearchResult(doc.get("url"), doc.get("pageId"), doc.get("contents"), doc.get("languages"),
				doc.get("chapterId"), doc.get("chapters"), doc.get("volumeId"), doc.get("volume"));

		return result;
	}

	/**
	 * @return totalHits in current search
	 */
	public int getTotalHits() {
		return totalHits;
	}

	/**
	 * @param totalHits
	 */
	public void setTotalHits(int totalHits) {
		this.totalHits = totalHits;
	}

}
