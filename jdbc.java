import java.sql.*;

public class jdbc {
    public jdbc() {
    }
    public static void main(String args[]) {
	//String usage = "java jdbc";
	rebuildIndexes("indexes");
    }
    public static void rebuildIndexes(String indexPath) {
	Connection conn = null;
	Statement stmt = null;
	try {
	    conn = DbManager.getConnection(true);
	    stmt = conn.createStatement();
	    //String sql = "SELECT * from item limit 3;";
	    String sql = "SELECT count(*) as count from item;";
	    ResultSet rs = stmt.executeQuery(sql);
	    while(rs.next()){
		String count = rs.getString("count");
		System.out.println("count: " + count);
	    }
	    rs.close();
	    conn.close();
	} catch (SQLException ex) {
	    System.out.println(ex);
	}
    }
}

