from flask import Flask, request, jsonify
from flask_cors import CORS
import requests
from bs4 import BeautifulSoup
import json
import time
import random
from urllib.parse import quote_plus
import logging

# 設定日誌
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)  # 允許跨域請求

class GoogleScholarCrawler:
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'en-US,en;q=0.5',
            'Accept-Encoding': 'gzip, deflate',
            'Connection': 'keep-alive',
            'Upgrade-Insecure-Requests': '1',
        })
    
    def crawl_papers(self, keyword, max_results=10):
        """爬取 Google Scholar 論文"""
        papers = []
        try:
            # 建立搜尋 URL
            search_url = self._build_search_url(keyword)
            logger.info(f"開始爬取: {search_url}")
            
            # 發送請求
            response = self.session.get(search_url, timeout=30)
            response.raise_for_status()
            
            # 解析 HTML
            soup = BeautifulSoup(response.content, 'html.parser')
            
            # 提取論文資訊
            paper_elements = soup.select('.gs_r')
            
            for element in paper_elements:
                if len(papers) >= max_results:
                    break
                
                paper = self._extract_paper_info(element)
                if paper and paper.get('title'):
                    papers.append(paper)
            
            logger.info(f"成功爬取 {len(papers)} 篇論文")
            
        except Exception as e:
            logger.error(f"爬取失敗: {str(e)}")
            raise
        
        return papers
    
    def _build_search_url(self, keyword):
        """建立 Google Scholar 搜尋 URL"""
        encoded_keyword = quote_plus(keyword)
        return f"https://scholar.google.com/scholar?q={encoded_keyword}&hl=en&as_sdt=0,5"
    
    def _extract_paper_info(self, element):
        """從 HTML 元素中提取論文資訊"""
        try:
            # 提取標題
            title_element = element.select_one('.gs_rt a')
            title = title_element.get_text(strip=True) if title_element else ""
            
            # 提取作者、期刊、年份
            author_element = element.select_one('.gs_a')
            author = ""
            journal = ""
            year = None
            
            if author_element:
                author_text = author_element.get_text(strip=True)
                # 解析作者、期刊、年份
                author, journal, year = self._parse_author_info(author_text)
            
            # 提取摘要
            abstract_element = element.select_one('.gs_rs')
            abstract_text = abstract_element.get_text(strip=True) if abstract_element else ""
            
            # 提取引用數
            citation_element = element.select_one('.gs_fl a')
            citations = 0
            if citation_element:
                citation_text = citation_element.get_text()
                # 提取數字
                import re
                citation_match = re.search(r'(\d+)', citation_text)
                if citation_match:
                    citations = int(citation_match.group(1))
            
            # 提取 PDF 連結
            pdf_element = element.select_one('.gs_or_ggsm a')
            pdf_url = pdf_element.get('href') if pdf_element else ""
            
            return {
                'title': title,
                'author': author,
                'journal': journal,
                'year': year,
                'abstractText': abstract_text,
                'citations': citations,
                'pdfUrl': pdf_url
            }
            
        except Exception as e:
            logger.error(f"提取論文資訊失敗: {str(e)}")
            return None
    
    def _parse_author_info(self, author_text):
        """解析作者資訊字串"""
        author = ""
        journal = ""
        year = None
        
        try:
            # 通常格式: "作者, 作者 - 期刊, 年份" 或 "作者 - 期刊, 年份"
            parts = author_text.split(" - ")
            
            if len(parts) > 0:
                author = parts[0].strip()
            
            if len(parts) > 1:
                journal_year = parts[1].strip()
                # 分離期刊和年份
                if "," in journal_year:
                    journal_part, year_part = journal_year.rsplit(",", 1)
                    journal = journal_part.strip()
                    
                    # 提取年份
                    import re
                    year_match = re.search(r'(\d{4})', year_part)
                    if year_match:
                        year = int(year_match.group(1))
                else:
                    journal = journal_year
            
            # 如果沒有找到年份，嘗試從整個字串中尋找
            if year is None:
                import re
                year_match = re.search(r'(\d{4})', author_text)
                if year_match:
                    potential_year = int(year_match.group(1))
                    if 1900 <= potential_year <= 2030:
                        year = potential_year
                        
        except Exception as e:
            logger.error(f"解析作者資訊失敗: {str(e)}")
        
        return author, journal, year

# 建立爬蟲實例
crawler = GoogleScholarCrawler()

@app.route('/health', methods=['GET'])
def health_check():
    """健康檢查端點"""
    return jsonify({
        'status': 'healthy',
        'service': 'python-crawler',
        'timestamp': time.time()
    })

@app.route('/crawl', methods=['POST'])
def crawl_papers():
    """爬取論文端點"""
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({'error': '請求資料為空'}), 400
        
        keyword = data.get('keyword')
        max_results = data.get('maxResults', 10)
        year_from = data.get('yearFrom')
        year_to = data.get('yearTo')
        
        if not keyword:
            return jsonify({'error': '關鍵字不能為空'}), 400
        
        # 爬取論文
        papers = crawler.crawl_papers(keyword, max_results)
        
        # 根據年份過濾
        if year_from or year_to:
            papers = filter_papers_by_year(papers, year_from, year_to)
        
        return jsonify({
            'success': True,
            'papers': papers,
            'count': len(papers),
            'keyword': keyword
        })
        
    except Exception as e:
        logger.error(f"爬取請求失敗: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

def filter_papers_by_year(papers, year_from, year_to):
    """根據年份範圍過濾論文"""
    filtered_papers = []
    
    for paper in papers:
        year = paper.get('year')
        
        if year is None:
            # 如果沒有年份資訊，預設包含
            filtered_papers.append(paper)
            continue
        
        # 檢查年份範圍
        if year_from and year < year_from:
            continue
        if year_to and year > year_to:
            continue
        
        filtered_papers.append(paper)
    
    return filtered_papers

@app.route('/crawl/batch', methods=['POST'])
def crawl_batch():
    """批量爬取多個關鍵字"""
    try:
        data = request.get_json()
        
        if not data or 'keywords' not in data:
            return jsonify({'error': '請提供關鍵字列表'}), 400
        
        keywords = data['keywords']
        max_results_per_keyword = data.get('maxResultsPerKeyword', 5)
        
        all_papers = []
        
        for keyword in keywords:
            try:
                papers = crawler.crawl_papers(keyword, max_results_per_keyword)
                all_papers.extend(papers)
                
                # 避免請求過於頻繁
                time.sleep(random.uniform(1, 3))
                
            except Exception as e:
                logger.error(f"爬取關鍵字 '{keyword}' 失敗: {str(e)}")
                continue
        
        return jsonify({
            'success': True,
            'papers': all_papers,
            'count': len(all_papers),
            'keywords': keywords
        })
        
    except Exception as e:
        logger.error(f"批量爬取失敗: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False) 