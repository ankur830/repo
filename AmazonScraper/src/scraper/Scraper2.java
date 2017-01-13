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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Scraper2 {

	private final List<String> scrapedUrls = Collections.synchronizedList( new ArrayList<String>() );
	private static final int poolSize	=	10;
	AtomicInteger	count =	new AtomicInteger(0);

	private static Document connect_url(String line){
		Document doc	=	null;
		try {
			try {
				line	=	java.net.URLDecoder.decode(line, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			doc = Jsoup.connect(line).
					userAgent("sssMozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					.referrer("http://www.google.com")
					.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return doc;

	}
	public void getProducts(BlockingQueue<String> sub_categories){

		ScrapTask t[] = new ScrapTask[poolSize];

		for( int i = 0; i < poolSize; i++ ) {
			t[i] = new ScrapTask(sub_categories);
			
			t[i].start();
		}

		for( int i = 0; i < poolSize; i++ ) {
			try {
				t[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
	
	
	private BlockingQueue<String> getSubCategories(){
		BlockingQueue<String> sub_categories = new LinkedBlockingQueue<String>();

		try (BufferedReader br = new BufferedReader(new FileReader("categories.txt"))) {
			String line	=	null;

			while ((line = br.readLine()) != null) {

				line	=	decodeURL(line);
				Document doc = connect_url(line);
				if(doc.select("#search-results").first()!=null){
					sub_categories.add(line);
					
				}
				else if(doc.select("#resultsCol").first()!=null){
					sub_categories.add(line);
				}
				else{			
					doc.select(".shoppingEngineExpand").remove();		    	 
					Elements subCatList	=	doc.select(".categoryRefinementsSection li");
					System.out.println(line);
					System.out.println("size is: "+subCatList.size());
					if(subCatList.size()==1)
					subCatList.remove(0);

					for(Element subCat:subCatList){

						if(subCat.select("a[href]").first()!=null){
							sub_categories.add(subCat.select("a[href]").attr("abs:href"));    		 
						}
					}

				}

			}



		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sub_categories;
	}


	class ScrapTask extends Thread {
		BlockingQueue<String> urls;
		List<String> scrapedUrls;

		ScrapTask(BlockingQueue<String> urls) {
			this.urls = urls;
			// this.scrapedUrls = scrapedUrls;
		}

		@Override
		public void run() {
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
			// scraped Urls.add( urlstr );
			
			// Only http/https links are scraped
			/*if( !urlstr.toLowerCase().startsWith( "http" ) ) {
				return;
			}*/

			urlstr	=	decodeURL(urlstr);
			

			Document doc = connect_url(urlstr);

			Elements search_results	=	doc.select("#search-results li");
			if(search_results.size()==0){
				search_results	=	doc.select("#resultsCol li");
			}
			System.out.println("Total results: "+search_results.size());

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

				count.addAndGet(1);

				System.out.println(count+" : "+Thread.currentThread().getName()+" : "+ asin+" : "+title);
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

	public static void main(String[] args) {
		Scraper2 scraper	=	new Scraper2();
		BlockingQueue<String> subCategories = new LinkedBlockingQueue<String>();

		String host = "127.0.0.1";
		String port = "80";
		System.setProperty("http.proxyHost", host);
		System.setProperty("http.proxyPort", port);

		subCategories	=	scraper.getSubCategories();
		//Get all the categories from text file;

		scraper.getProducts(subCategories);

	}
}
