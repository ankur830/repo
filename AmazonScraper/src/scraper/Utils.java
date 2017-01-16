package scraper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Utils {

	public static Connection getConnection(){
		Connection con	=	null;

		try {
			Class.forName("com.mysql.jdbc.Driver");
			con=DriverManager.getConnection(  
					"jdbc:mysql://localhost:3306/scraper","root","123");

		} catch (ClassNotFoundException|SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  

		return con;
	}
	
	public static String decodeURL(String url){
		String decodedURL	=	null;
		try {
			decodedURL	=	java.net.URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return decodedURL;

	}
	public static Document connect_url(String line){
		Document doc	=	null;
		try {

			line	=	decodeURL(line);
			line=line.replaceAll("ref=[(a-z)(A-Z)(0-9)_]*", "");
			int randomNum = ThreadLocalRandom.current().nextInt(0, 1001);
			
			doc = Jsoup.connect(line).
					userAgent(randomNum+"xxMozddilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					.ignoreHttpErrors(true)
					.referrer("http://www.google.com")
					.get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return doc;
	}
}
