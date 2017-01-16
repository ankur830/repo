package scraper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import scraper.Utils;
public class Scraper2 implements Runnable{

	private Set<String>  set	=	new HashSet<String>();
	//private static final int poolSize	=	100;
	//AtomicInteger	count =	new AtomicInteger(0);

	BlockingQueue<String> urls;
	//List<String> scrapedUrls;

	Scraper2(BlockingQueue<String> urls, Set<String>  set) {
		this.urls = urls;
		this.set	=	set;

	}




	@Override
	public void run() {
		Connection con	=	Utils.getConnection();
		PreparedStatement ps	=	null;
		try {
			ps = con.prepareStatement("INSERT INTO product(asin, title, price, reviews,ratings) VALUES (?, ?, ?, ?, ?)");
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			//e1.printStackTrace();
		}


		while(true) {
			//System.out.println(urls.size());
			try {
				String url = urls.take();

				scrap( url, con , ps);

			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			} catch (FileNotFoundException e) {
				//logger.error( "URL: " + e.getMessage() + " not found" );
			} catch (Exception e) {
				e.printStackTrace();
			}
			finally{
				try {
					con.close();
					ps.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}

	private void scrap(String urlstr, Connection con, PreparedStatement ps) throws Exception {

		urlstr	=	Utils.decodeURL(urlstr);


		Document doc = Utils.connect_url(urlstr);

		Elements search_results	=	doc.select("#search-results li");
		if(search_results.size()==0){
			search_results	=	doc.select("#resultsCol li");
		}
		//System.out.println(urlstr);
		//System.out.println("Total results: "+search_results.size());


		//If no products found, navigate to sub categories from the side panel	

		if(search_results.first()==null)
		{			
			doc.select(".shoppingEngineExpand").remove();		    	 
			Elements subCatList	=	doc.select(".categoryRefinementsSection li");

			if(subCatList.size()==1)
				subCatList.remove(0);

			for(Element subCat:subCatList){

				if(subCat.select("a[href]").first()!=null){
					urls.add(subCat.select("a[href]").attr("abs:href"));    		 
				}
			}

		}
		// If products exist on this page, get every product's details
		else{
			for(Element li:search_results){
				String reviews	=	"";
				String ratings	=	"";
				String asin	=	li.attr("data-asin");
				if(asin.equals("")||set.contains(asin))continue;
				set.add(asin);
				String title	=	li.select("a").attr("title");
				String price	=	li.select("a[class=a-link-normal a-text-normal]").text().split(" ")[0];
				if(li.select("span[name="+asin+"]").first()!=null)
					ratings	=	li.select("span[name="+asin+"]").text();
				if(li.select("span[name="+asin+"]").first()!=null)
					reviews	=	li.select("span[name="+asin+"]").first().nextElementSibling().text();


				//count.addAndGet(1);

				//System.out.println(" : "+Thread.currentThread().getName()+" : "+ asin+" : "+title);
				/*System.out.println("ASIN: "+asin);
				System.out.println("Title : "+title);
				System.out.println("Price: "+price);
				System.out.println("Ratings: "+ratings);
				System.out.println("Reviews: "+reviews);*/

				ps.setString(1, asin);
				ps.setString(2,title);
				ps.setString(3,price);
				ps.setString(4, reviews);
				ps.setString(5,ratings);
				ps.addBatch();
			}
			ps.executeBatch();


			//Enqueue the next link to the queue
			Element nextPageLink	=	doc.select("a#pagnNextLink").first();
			if(nextPageLink!=null)
				urls.put(nextPageLink.attr("abs:href"));

			//System.out.println(Thread.currentThread().getName()+" : scraped "+urlstr);
		}


	}



	



	public static void main(String[] args) {
		String host = "127.0.0.1";
		String port = "8";
		System.setProperty("http.proxyHost", host);
		System.setProperty("http.proxyPort", port);
		String allDirectoryUrl	=	"http://www.amazon.in/gp/site-directory/ref=nav_shopall_btn";
		ExecutorService executor	=	Executors.newFixedThreadPool(30);
		BlockingQueue<String> Q	=	new LinkedBlockingQueue<String>();
		Set<String> unsync_set	=	new HashSet<String>();
		Set<String> productSet = Collections.synchronizedSet(unsync_set);


		try (BufferedReader br = new BufferedReader(new FileReader("categories.txt"))) {
			String line	=	null;

			while ((line = br.readLine()) != null) {
				//Q.add(line);						
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Document doc	=	Utils.connect_url(allDirectoryUrl);

	      Elements categories = doc.select("table#shopAllLinks").first().select("div.popover-grouping li");
	      
	      for (Element li : categories) {
	    	  if(li.select("a[href]").first().text().contains("All")){
	    		  String link	=	li.select("a[href]").first().attr("abs:href"); 
		    	  Q.add(link); 
	    	  }
	      	 	  
	      }
	      
	      
		
		for(int i=0;i<30;i++) {
			executor.execute(new Scraper2(Q,productSet));

		}


	}


}
