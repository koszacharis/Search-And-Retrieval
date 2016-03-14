import java.sql.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Indexer {
	public Indexer() {
	}
	
	public static IndexWriter indexWriter;

	public static void main(String args[]) {
		// String usage = "java Indexer";
		rebuildIndexes("indexes");
	}

	/**
	 * Create index
	 * @param indexWriter
	 * @param item_id
	 * @param item_name
	 * @param item_category
	 * @param item_desc
	 * @param item_curr_price
	 */
	public static void insertDoc(IndexWriter indexWriter, String item_id, String item_name, String item_category,
			String item_desc, String item_curr_price) {
		Document doc = new Document();
		doc.add(new StringField("item_id", item_id, Field.Store.YES));
		doc.add(new StringField("item_name", item_name, Field.Store.YES));
		doc.add(new TextField("item_category", item_category, Field.Store.NO));
		doc.add(new TextField("item_desc", item_desc, Field.Store.NO));
		doc.add(new TextField("item_curr_price", item_curr_price, Field.Store.YES));
		String fullSearchableText = String.valueOf(item_id) + " " + item_name + " " + item_category + " " + item_desc
				+ " " + item_curr_price;
		doc.add(new TextField("content", fullSearchableText, Field.Store.NO));
		try {
			indexWriter.addDocument(doc);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void rebuildIndexes(String indexPath) {
		Connection conn = null;
		int progBar=0;
		try {

			// create a connection to the database to retrieve Items from MySQL
			try {
				conn = DbManager.getConnection(true);
			} catch (SQLException ex) {
				System.out.println(ex);
			}
			Path path = Paths.get(indexPath);
			System.out.println("Indexing to directory '" + indexPath + "'...\n");
			Directory directory = FSDirectory.open(path);
			IndexWriterConfig config = new IndexWriterConfig(new SimpleAnalyzer());
			// IndexWriterConfig config = new IndexWriterConfig(new
			// StandardAnalyzer());
			// IndexWriterConfig config = new IndexWriterConfig(new
			// EnglishAnalyzer());
			IndexWriter indexWriter = new IndexWriter(directory, config);
			indexWriter.deleteAll();
			Statement stmt = conn.createStatement();

			/*
			 * store also the current_price from auction table in order to use
			 * secondary sorting on item's current price
			 */
			String sql = "SELECT i.item_id, i.item_name, C.Categories, description, current_price FROM (SELECT item_id, group_concat(has_category.category_name SEPARATOR ' ') AS Categories FROM has_category GROUP BY item_id) as C  INNER JOIN item AS i ON i.item_id = C.item_id INNER JOIN auction AS a ON C.item_id=a.item_id ORDER BY C.item_id;";

			// Fetch all the items
			ResultSet items = stmt.executeQuery(sql);
			System.out.println("Building Lucene index ...");
			// Add an index on each item
			while (items.next()) {
				progBar++;
				if (progBar%195 == 0)
					printProgBar(progBar/195);
				insertDoc(indexWriter, items.getString("item_id"), items.getString("item_name"), items.getString("Categories"),
						items.getString("description"), items.getString("current_price"));
				if (progBar==19532){
					System.out.println("\n");
					System.out.println("Done.\n");
				}
			}

			stmt.close();
			// close the database connection
			try {
				conn.close();
			} catch (SQLException ex) {
				System.out.println(ex);
			}

			indexWriter.close();
			directory.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printProgBar(int percent){
	    StringBuilder bar = new StringBuilder("[");

	    for(int i = 0; i < 50; i++){
	        if( i < (percent/2)){
	            bar.append("=");
	        }else if( i == (percent/2)){
	            bar.append(">");
	        }else{
	            bar.append(" ");
	        }
	    }

	    bar.append("]   " + percent + "%     ");
	    System.out.print("\r" + bar.toString());
	}
}
