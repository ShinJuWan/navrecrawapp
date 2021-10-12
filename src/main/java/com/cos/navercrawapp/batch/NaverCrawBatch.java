package com.cos.navercrawapp.batch;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cos.navercrawapp.domain.NaverNews;
import com.cos.navercrawapp.domain.NaverNewsRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

// 동기적 배치프로그램 (약속, 어음을 응답받을 수 없다)ㄴ
@RequiredArgsConstructor
@Component
public class NaverCrawBatch {
	
	private long aid = 278000;
	private final NaverNewsRepository naverNewsRepository;
	
	// 초 분 시 일 월 주
	//@Scheduled(cron = "* * 1 * * *", zone = "Asia/Seoul")
	@Scheduled(cron = "0 27 14 * * *", zone = "Asia/Seoul")
	public void 네이버뉴스크롤링() {
		List<NaverNews> naverNewsList = new ArrayList<>();
		while(true) {
			String aidStr = String.format("%010d", aid);
			System.out.println("aidStr : "+aidStr);
			String url = "https://news.naver.com/main/read.naver?mode=LSD&mid=shm&sid1=103&oid=437&aid="+aidStr;
			
			try {
				Document doc =  Jsoup.connect(url).get();

				String title = doc.selectFirst("#articleTitle").text();
				String company = doc.selectFirst(".press_logo img").attr("alt");
				String createdAt = doc.selectFirst(".t11").text();
				
				LocalDate today = LocalDate.now();
				LocalDate yesterday = today.minusDays(1);
				
				createdAt = createdAt.substring(0, 10);
				createdAt = createdAt.replace(".", "-");
				
				if(today.toString().equals(createdAt)) {
					break; 
				}
				
				if(yesterday.toString().equals(createdAt)) { 
					
					naverNewsList.add(NaverNews.builder()
							.title(title)
							.company(company)
							.createdAt(Timestamp.valueOf(LocalDateTime.now().minusDays(1).plusHours(9)))
							.build()
					);
					
				}
				
			} catch (Exception e) {
			} 
			aid++;
		}	
		
		Flux.fromIterable(naverNewsList)
			.flatMap(naverNewsRepository::save)
			.subscribe(); 			
	}
}