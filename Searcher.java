import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.*;
import java.io.IOException;
import java.nio.file.Path;

public class Searcher {

	static Double longitude = 0.0;
	static Double latitude = 0.0;
	static Double width = 0.0;
	static IndexDoc doc;
	static protected ArrayList<IndexDoc> indexDocs = new ArrayList<IndexDoc>();

	public Searcher() {
	}

	public static void main(String[] args) throws Exception {
		// String usage = "java Searcher";

		if (args.length == 1) {
			System.out.println("Coordinates and width were not specified.");
			basicSearch(args[0], "content");
		} else if (args.length == 7) {
			System.out.println("Coordinates and width were specified.");
			longitude = Double.parseDouble(args[2]);
			latitude = Double.parseDouble(args[4]);
			width = Double.parseDouble(args[6]);
			spatialSearch(args[0], "content", longitude, latitude, width);
		} else {
			System.out.println("Error in Searcher arguments");
			System.out.println("For basic search use   ./runLoad.sh " +'"'+"SearchText"+'"');
			System.out.println("For spatial search use ./runLoad.sh " +'"'+"SearchText"+'"'+" -x longititude -y latitude -w width");
		}

	}

	/**
	 * This method performs a simple search in the index. If the Searcher
	 * program is called only with a phrase and without any other parameter,
	 * then the ranked list of item-IDs, names, and scores are ordered by
	 * decreasing Lucene-score for items with equal Lucene score and ordered by
	 * increasing price (i.e., lowest price first). Price represents the current
	 * price of the item.
	 * 
	 * @param searchText
	 *            is the phrase to search in the index
	 * @param q
	 *            is the query for the index
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	private static void basicSearch(String searchText, String q) throws IOException, ParseException {

		System.out.println("Running basic search on (" + searchText + ")");
		Path path = Paths.get("indexes");
		Directory directory = FSDirectory.open(path);
		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		QueryParser queryParser = new QueryParser(q, new SimpleAnalyzer());
		Query query = queryParser.parse(searchText);
		TopDocs topDocs = indexSearcher.search(query, 19532);

		System.out.println("Number of Hits: " + topDocs.totalHits);
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document document = indexSearcher.doc(scoreDoc.doc);
			IndexDoc doc = new IndexDoc();
			doc.setId(document.get("item_id"));
			doc.setName(document.get("item_name"));
			doc.setPrice(document.get("item_curr_price"));
			doc.setScore(scoreDoc.score);
			indexDocs.add(doc); // add to List
			
			doc = null; // reset object
		}

//		 /* sort by Price ascending */
//		 Collections.sort(indexDocs, new Comparator<IndexDoc>() {
//		
//		 @Override
//		 public int compare(IndexDoc o1, IndexDoc o2) {
//		
//		 return o2.getPrice().compareTo(o1.getPrice());
//		 }
//		
//		 });
//		
//		 /* sort by Score ascending */
//		
//		 Collections.sort(indexDocs, new Comparator<IndexDoc>() {
//		
//		 @Override
//		 public int compare(IndexDoc o1, IndexDoc o2) {
//		
//		 return Float.compare(o2.getScore(), o1.getScore());
//		 }
//		
//		 });

		Collections.sort(indexDocs, new Comparator<IndexDoc>() {

			@Override
			public int compare(IndexDoc o1, IndexDoc o2) {
				if (o1.getScore() == o2.getScore()) {
					return o1.getPrice().compareTo(o2.getPrice());
				} else if (o1.getScore() > o2.getScore())
					return 1;
				else
					return -1;
			}

		});

		// print the sorted List
		for (IndexDoc element : indexDocs) {
			System.out.println(element.getId() + ", " + element.getName() + ", score:" + element.getScore());
		}
		indexDocs.clear();
	}

	/**
	 * This method is used to search for a phrase in the index and then if three
	 * parameters are given, then it restricts the results of the keyword search
	 * by only returning items that have longitude and latitude information, and
	 * for which these numbers fall into the "square" that is centered at the
	 * given longitude and latitude numbers, and that has width given by the
	 * width number.
	 * 
	 * @param searchText
	 *            is the phrase to search in the index
	 * @param q
	 *            is the query for the index
	 * @param longitude
	 * @param latitude
	 * @param width
	 * @throws IOException
	 * @throws ParseException
	 * @throws SQLException
	 */
	private static void spatialSearch(String searchText, String q, double longitude, double latitude, double width)
			throws IOException, ParseException, SQLException {

		Connection conn = null;
		double dist;
		int counter = 0;

		try {
			conn = DbManager.getConnection(true);
		} catch (SQLException ex) {
			System.out.println(ex);
		}
		Statement stmt = conn.createStatement();

		System.out.println("Running spatial search on (" + searchText + ") with longitude: " + longitude + " latitude: "
				+ latitude + " and width: " + width);

		/* calculate the lat long values with the given width */
		double longitudeD = (Math.asin((width / 2) / (6371.04 * Math.cos(Math.PI * latitude / 180)))) * 180 / Math.PI;
		double latitudeD = (Math.asin((double) (width / 2) / (double) 6371.04)) * 180 / Math.PI;
		// store the 4 edges of the rectangle polygon
		double latitudeMax = latitude + (latitudeD);
		double latitudeMin = latitude - (latitudeD);
		double longitudeMax = longitude + (longitudeD);
		double longitudeMin = longitude - (longitudeD);

		String topRight = latitudeMax + " " + longitudeMax;
		String topLeft = latitudeMin + " " + longitudeMax;
		String botRight = latitudeMax + " " + longitudeMin;
		String botLeft = latitudeMin + " " + longitudeMin;

//		System.out.println(topRight + " " + topLeft + " " + botRight + " " + botLeft);

		Path path = Paths.get("indexes");
		Directory directory = FSDirectory.open(path);
		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		QueryParser queryParser = new QueryParser(q, new SimpleAnalyzer());
		Query query = queryParser.parse(searchText);
		TopDocs topDocs = indexSearcher.search(query, 19532);
		
		System.out.println("Number of Hits: " + topDocs.totalHits);
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document document = indexSearcher.doc(scoreDoc.doc);

			// get only the items which have long/lat
			String sqlId = "SELECT ItemID, AsText(Location) AS l, X(Location) AS x, Y(Location) AS y FROM ItemLocation AS iloc WHERE iloc.ItemID="
					+ document.get("item_id") + ";";
			ResultSet rsID = stmt.executeQuery(sqlId);

			if (rsID.next()) {
				// calculate the distance between the center and the current
				// geo-point
				dist = Haversine.haversine(latitude, longitude, Double.valueOf(rsID.getString("x")),
						Double.valueOf(rsID.getString("y")));

				// create query to check if the current geo-point is contained
				// in the created bounding box
				String sqlp = "SELECT MBRContains(GeomFromText('Polygon((" + botLeft + "," + topLeft + "," + topRight
						+ "," + botRight + "," + botLeft + "))'), GeomFromText('" + rsID.getString("l") + "')) AS p;";
				ResultSet rs = stmt.executeQuery(sqlp); // execute query

				if (rs.next()) {
					if (rs.getString("p").equals("1")) { // if current POINT is
															// contained in
															// bounding box
						IndexDoc doc = new IndexDoc();
						// populate IndexDoc object
						doc.setId(document.get("item_id"));
						doc.setName(document.get("item_name"));
						doc.setPrice(document.get("item_curr_price"));
						doc.setScore(scoreDoc.score);
						doc.setDistance(dist);
						// add to List
						indexDocs.add(doc);
						counter++;
						doc = null; // reset object
					}
				}
			}
		}
		System.out.println("Number of Hits inside Bounding Box: " + counter);

		/**
		 * ordered by decreasing Lucene-score for items with equal Lucene score,
		 * order them by decreasing distance from the geo-location given by the
		 * latitude and longitude numbers for items with equal Lucene score and
		 * equal distance, order them by decreasing price, where price is
		 * defined as before.
		 */
		Collections.sort(indexDocs, new Comparator<IndexDoc>() {

			@Override
			public int compare(IndexDoc o1, IndexDoc o2) {

				if (o1.score == o2.score) {
					if (o1.getDistance() == o2.getDistance())
						return o1.getPrice().compareTo(o2.getPrice());
					else
						return Double.compare(o1.getDistance(), o2.getDistance());
				} else if (o1.score < o2.score)
					return 1;
				else
					return -1;
			}
		});

		/* print the sorted ArrayList */
		for (IndexDoc element : indexDocs) {
			System.out.println(element.getId() + ", " + element.getName() + ", score:" + element.getScore() + ", "
					+ element.getDistance() + ", " + element.getPrice());
		}
		
		indexDocs.clear();
	}

	/*
	 * Calculate the distance between two geo-Points
	 * https://rosettacode.org/wiki/Haversine_formula#Java
	 */
	public static class Haversine {
		public static final double R = 6371.0; // In kilometers

		public static double haversine(double lat1, double lon1, double lat2, double lon2) {
			double dLat = Math.toRadians(lat2 - lat1);
			double dLon = Math.toRadians(lon2 - lon1);
			lat1 = Math.toRadians(lat1);
			lat2 = Math.toRadians(lat2);

			double a = Math.pow(Math.sin(dLat / 2), 2)
					+ Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
			double c = 2 * Math.asin(Math.sqrt(a));
			return R * c;
		}

	}

	/**
	 * This class is used to store each row returned from the index.
	 *
	 */
	public static class IndexDoc {
		private String id;
		private String name;
		private float score;
		private String price;
		private double distance;

		public IndexDoc() {
		};

		public IndexDoc(String id, String name, float score, String price, double distance) {
			super();
			this.id = id;
			this.name = name;
			this.score = score;
			this.price = price;
			this.distance = distance;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public float getScore() {
			return score;
		}

		public void setScore(float score) {
			this.score = score;
		}

		public String getPrice() {
			return price;
		}

		public void setPrice(String price) {
			this.price = price;
		}

		public double getDistance() {
			return distance;
		}

		public void setDistance(double distance) {
			this.distance = distance;
		}

	}

}
