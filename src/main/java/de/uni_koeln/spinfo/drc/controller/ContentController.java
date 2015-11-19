package de.uni_koeln.spinfo.drc.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_koeln.spinfo.drc.mongodb.Constans;
import de.uni_koeln.spinfo.drc.mongodb.DataBase;
import de.uni_koeln.spinfo.drc.mongodb.data.Letter;
import de.uni_koeln.spinfo.drc.mongodb.data.Version;
import de.uni_koeln.spinfo.drc.mongodb.data.document.Chapter;
import de.uni_koeln.spinfo.drc.mongodb.data.document.Page;
import de.uni_koeln.spinfo.drc.mongodb.data.document.Word;

@Controller()
@RequestMapping(value = "/drc")
public class ContentController {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private DataBase db;

	@RequestMapping(value = "/pages")
	public ModelAndView getPages(
			@RequestParam("chapterId") String chapterId,
			@RequestParam("chapterTitle") String chapterTitle,
			@RequestParam("volumeId") String volumeId,
			@RequestParam("volumeTitle") String volumeTitle) {

		List<Page> pages = db.getPageRepository().findByChapterIds(
				Arrays.asList(new String[] { chapterId }));

		ModelAndView mv = new ModelAndView(Constans.PAGES);
		
		mv.addObject("pages", pages);
		mv.addObject("chapterTitle", chapterTitle);
		mv.addObject("chapterId", chapterId);
		mv.addObject("volumeId", volumeId);
		mv.addObject("volumeTitle", volumeTitle);

		return mv;
	}

	@RequestMapping(value = "/correct")
	public ModelAndView pageDetails(@RequestParam("pageId") String pageId,
			@RequestParam("chapterId") String chapterId,
			@RequestParam("chapterTitle") String chapterTitle,
			@RequestParam("volumeId") String volumeId,
			@RequestParam("volumeTitle") String volumeTitle)
			throws JsonGenerationException, JsonMappingException, IOException {

		Page page = db.getPageRepository().findByPageId(pageId);
		List<Word> words = db.getWordRepository().findByRange(page.getStart(), page.getEnd());
		// List<Word> words = db.getWordRepository().findByPageId(page.getId());
		// List<Word> words = page.getWords();
		Collections.sort(words);
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(System.out, words);
		// String json = mapper.writeValueAsString(words);
		List<Letter> letters = new ArrayList<>();
		AtomicInteger textLength = new AtomicInteger();
		for (Word word : words) {
			Version currentVersion = word.getCurrentVersion();
			String id = word.getId();
			int index = word.getIndex();
			char[] charArray = currentVersion.getVersion().toCharArray();
			AtomicInteger wordLength = new AtomicInteger();
			for (char c : charArray) {
				letters.add(new Letter(id, index, c, wordLength
						.getAndIncrement(), textLength.getAndIncrement()));
			}
		}

		ModelAndView mv = new ModelAndView("correct");
		mv.addObject("letters", letters);
		// mv.addObject("json", json);
		// mv.addObject("words", words);
		mv.addObject("page", page);
		mv.addObject("chapterTitle", chapterTitle);
		mv.addObject("chapterId", chapterId);
		mv.addObject("chapters", getChapters(page));
		mv.addObject("volumeId", volumeId);
		mv.addObject("volumeTitle", volumeTitle);

		return mv;
	}

	public List<Chapter> getChapters(Page page) {
		
		List<String> chapterIds = page.getChapterIds();
		List<Chapter> chapters = new ArrayList<>();
		for (String chapterId : chapterIds) {
			chapters.add(db.getChapterRepository().findOne(chapterId));
		}

		return chapters;
	}

	@RequestMapping(value = "/language", method = RequestMethod.GET)
	@ResponseBody
	public String getLanguage(@RequestParam("languageId") String languageId) {
		return db.getLanguageRepository().findOne(languageId).getTitle();
	}

	@RequestMapping(value = "/random/page")
	@ResponseBody
	public String getRandomPage() throws JsonParseException,
			JsonMappingException, IOException {
		return db.getPageRepository().findOne("5622c29eef86d3c2f23fd62c");
	}

}
