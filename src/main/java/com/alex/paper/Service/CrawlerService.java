package com.alex.paper.Service;

import com.alex.paper.Crawler.GoogleScholarCrawler;
import com.alex.paper.DTO.CrawlerRequest;
import com.alex.paper.Model.Paper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);

    @Autowired
    private GoogleScholarCrawler googleScholarCrawler;

    @Autowired
    private PythonCrawlerService pythonCrawlerService;

    @Autowired
    private PaperService paperService;

    public List<Paper> crawlGoogleScholar(CrawlerRequest request) {
        List<Paper> crawledPapers = new ArrayList<>();
        
        try {
            // 優先使用 Python BeautifulSoup 爬蟲
            logger.info("嘗試使用 Python BeautifulSoup 爬蟲服務");
            crawledPapers = pythonCrawlerService.crawlWithPython(request);
            logger.info("Python 爬蟲服務成功爬取 {} 篇論文", crawledPapers.size());
            
        } catch (Exception e) {
            logger.warn("Python 爬蟲服務失敗，切換到 jsoup 備用方案: {}", e.getMessage());
            
            try {
                // 備用方案：使用 jsoup 爬蟲
                crawledPapers = googleScholarCrawler.crawlPapers(
                    request.getKeyword(), 
                    request.getMaxResults()
                );
                logger.info("jsoup 備用方案成功爬取 {} 篇論文", crawledPapers.size());
                
            } catch (Exception jsoupException) {
                logger.error("jsoup 備用方案也失敗: {}", jsoupException.getMessage());
                throw new RuntimeException("所有爬蟲方案都失敗", jsoupException);
            }
        }

        // 2. 過濾重複論文
        List<Paper> filteredPapers = filterDuplicatePapers(crawledPapers);

        // 3. 根據年份過濾（如果指定了年份範圍）
        if (request.getYearFrom() != null || request.getYearTo() != null) {
            filteredPapers = filterByYear(filteredPapers, request.getYearFrom(), request.getYearTo());
        }

        return filteredPapers;
    }

    /**
     * 批量爬取多個關鍵字
     */
    public List<Paper> crawlBatchGoogleScholar(List<String> keywords, int maxResultsPerKeyword) {
        List<Paper> allPapers = new ArrayList<>();
        
        try {
            // 優先使用 Python 批量爬蟲
            logger.info("嘗試使用 Python 批量爬蟲服務");
            allPapers = pythonCrawlerService.crawlBatchWithPython(keywords, maxResultsPerKeyword);
            logger.info("Python 批量爬蟲服務成功爬取 {} 篇論文", allPapers.size());
            
        } catch (Exception e) {
            logger.warn("Python 批量爬蟲服務失敗，使用單個關鍵字爬取: {}", e.getMessage());
            
            // 備用方案：逐個關鍵字使用 jsoup 爬取
            for (String keyword : keywords) {
                try {
                    CrawlerRequest request = new CrawlerRequest();
                    request.setKeyword(keyword);
                    request.setMaxResults(maxResultsPerKeyword);
                    
                    List<Paper> papers = googleScholarCrawler.crawlPapers(keyword, maxResultsPerKeyword);
                    allPapers.addAll(papers);
                    
                    // 避免請求過於頻繁
                    Thread.sleep(2000);
                    
                } catch (Exception keywordException) {
                    logger.error("爬取關鍵字 '{}' 失敗: {}", keyword, keywordException.getMessage());
                }
            }
        }

        // 過濾重複論文
        return filterDuplicatePapers(allPapers);
    }

    public List<Paper> crawlAndSave(CrawlerRequest request) {
        // 1. 爬取論文
        List<Paper> papers = crawlGoogleScholar(request);

        // 2. 儲存到資料庫
        List<Paper> savedPapers = new ArrayList<>();
        for (Paper paper : papers) {
            if (paperService.createPaper(paper)) {
                savedPapers.add(paper);
            }
        }

        return savedPapers;
    }

    /**
     * 批量爬取並儲存
     */
    public List<Paper> crawlBatchAndSave(List<String> keywords, int maxResultsPerKeyword) {
        // 1. 批量爬取論文
        List<Paper> papers = crawlBatchGoogleScholar(keywords, maxResultsPerKeyword);

        // 2. 儲存到資料庫
        List<Paper> savedPapers = new ArrayList<>();
        for (Paper paper : papers) {
            if (paperService.createPaper(paper)) {
                savedPapers.add(paper);
            }
        }

        return savedPapers;
    }

    // 批量儲存指定的論文
    public List<Paper> saveSelectedPapers(List<Paper> papers) {
        List<Paper> savedPapers = new ArrayList<>();
        
        for (Paper paper : papers) {
            // 檢查論文是否已存在
            if (!isPaperExists(paper)) {
                if (paperService.createPaper(paper)) {
                    savedPapers.add(paper);
                }
            }
        }
        
        return savedPapers;
    }

    /**
     * 檢查爬蟲服務健康狀態
     */
    public boolean isPythonCrawlerHealthy() {
        return pythonCrawlerService.isHealthy();
    }

    private List<Paper> filterDuplicatePapers(List<Paper> papers) {
        // 簡單的去重邏輯：根據標題和作者
        return papers.stream()
            .filter(paper -> !isPaperExists(paper))
            .collect(Collectors.toList());
    }

    private boolean isPaperExists(Paper paper) {
        // 檢查論文是否已存在於資料庫中
        List<Paper> existingPapers = paperService.getAllPapers();
        return existingPapers.stream()
            .anyMatch(existing -> 
                existing.getTitle().equalsIgnoreCase(paper.getTitle()) &&
                existing.getAuthor().equalsIgnoreCase(paper.getAuthor())
            );
    }

    private List<Paper> filterByYear(List<Paper> papers, Integer yearFrom, Integer yearTo) {
        return papers.stream()
            .filter(paper -> {
                // 使用 Paper 物件的 year 欄位
                Integer year = paper.getYear();
                if (year != null) {
                    if (yearFrom != null && year < yearFrom) return false;
                    if (yearTo != null && year > yearTo) return false;
                    return true;
                }
                // 如果沒有年份資訊，預設包含
                return true;
            })
            .collect(Collectors.toList());
    }
} 