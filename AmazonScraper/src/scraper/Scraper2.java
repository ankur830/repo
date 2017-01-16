package scraper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Scraper2 implements Runnable{

	private final List<String> scrapedUrls = Collections.synchronizedList( new ArrayList<String>() );
	private Set<String>  set	=	new HashSet<String>();
	//private static final int poolSize	=	100;
	//AtomicInteger	count =	new AtomicInteger(0);

	BlockingQueue<String> urls;
	//List<String> scrapedUrls;

	Scraper2(BlockingQueue<String> urls, Set<String>  set) {
		this.urls = urls;
		this.set	=	set;
		// this.scrapedUrls = scrapedUrls;
	}
	
	
	private Document connect_url(String line){
		Document doc	=	null;
		try {
			try {
				line	=	java.net.URLDecoder.decode(line, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			doc = Jsoup.connect(line).
					userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					.ignoreHttpErrors(true)
					.referrer("http://www.google.com")
					.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return doc;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true) {
			try {
				String url = urls.take();

				scrap( url );

			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			} catch (FileNotFoundException e) {
				//logger.error( "URL: " + e.getMessage() + " not found" );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void scrap(String urlstr) throws Exception {
		
		urlstr	=	decodeURL(urlstr);


		Document doc = connect_url(urlstr);

		Elements search_results	=	doc.select("#search-results li");
		if(search_results.size()==0){
			search_results	=	doc.select("#resultsCol li");
		}
		System.out.println(urlstr);
		System.out.println("Total results: "+search_results.size());
		
		///////
		if(search_results.first()==null)
		{			
			doc.select(".shoppingEngineExpand").remove();		    	 
			Elements subCatList	=	doc.select(".categoryRefinementsSection li");
			
			//System.out.println("size is: "+subCatList.size());
				
			if(subCatList.size()==1)
				subCatList.remove(0);

			for(Element subCat:subCatList){

				if(subCat.select("a[href]").first()!=null){
					urls.add(subCat.select("a[href]").attr("abs:href"));    		 
				}
			}

		}
		
		else{
			for(Element li:search_results){
				String reviews	=	"";
				String ratings	=	"";
				String asin	=	li.attr("data-asin");
				if(asin.equals(""))continue;
				String title	=	li.select("a").attr("title");
				String price	=	li.select("a[class=a-link-normal a-text-normal]").text().split(" ")[0];
				if(li.select("span[name="+asin+"]").first()!=null)
					ratings	=	li.select("span[name="+asin+"]").text();
				if(li.select("span[name="+asin+"]").first()!=null)
					reviews	=	li.select("span[name="+asin+"]").first().nextElementSibling().text();

				//count.addAndGet(1);

				System.out.println(" : "+Thread.currentThread().getName()+" : "+ asin+" : "+title);
				/*System.out.println("ASIN: "+asin);
				System.out.println("Title : "+title);
				System.out.println("Price: "+price);
				System.out.println("Ratings: "+ratings);
				System.out.println("Reviews: "+reviews);*/
				
				
			}
			Element nextPageLink	=	doc.select("a#pagnNextLink").first();
			if(nextPageLink!=null)
				urls.put(nextPageLink.attr("abs:href"));

			System.out.println(Thread.currentThread().getName()+" : scraped "+urlstr);
		}
		

	}
	
	

	String decodeURL(String url){
		String decodedURL	=	null;
		try {
			decodedURL	=	java.net.URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return decodedURL;

	}



	public static void main(String[] args) {
		String host = "127.0.0.1";
        String port = "8";
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", port);
        
		ExecutorService executor	=	Executors.newFixedThreadPool(30);
		BlockingQueue<String> Q	=	new LinkedBlockingQueue<String>();
		Set<String> unsync_set	=	new HashSet<String>();
		Set<String> productSet = Collections.synchronizedSet(unsync_set);

		
		try (BufferedReader br = new BufferedReader(new FileReader("categories.txt"))) {
			String line	=	null;

			while ((line = br.readLine()) != null) {
					Q.add(line);						
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		for(int i=0;i<30;i++) 
			executor.execute(new Scraper2(Q,productSet));
		

	}

	
}
