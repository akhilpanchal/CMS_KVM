

import java.io.IOException;
import java.net.InetAddress;
import java.sql.*;
import javax.sql.DataSource;

import model.Host;

import org.libvirt.*;

import play.Application;
import play.GlobalSettings;
import play.db.*;


public class Global extends GlobalSettings {
	public void onStart(Application app) {
    	initDB();
    		//get subnet 
    	new Thread(new LoadHostList("192.168.43")).start();
    	Host.loadDynamicList();
    	
    }
    
    private void initDB() {
		//create tables
		DataSource ds=DB.getDataSource();
		Connection dbConn=null;
		Statement stmt=null;
		
		try {
			System.out.println("Creating table in given database...");
			dbConn = ds.getConnection();
		    stmt= dbConn.createStatement();
		    String sql="CREATE TABLE IF NOT EXISTS Host " +
	                "(hostIP VARCHAR(255), " + 
	                " hostName VARCHAR(255) NOT NULL, " + 
	                " PRIMARY KEY ( hostIP ))"; 
	    	stmt.executeUpdate(sql);
	    	
	    	sql = "CREATE TABLE IF NOT EXISTS snapshot " +
	                "(vmuuid VARCHAR(255) NOT NULL, " +
	                " path VARCHAR(255) NOT NULL, "+
	                "PRIMARY KEY ( vmuuid ))"; 
	    	if((stmt.executeUpdate(sql))<0)
	    		System.out.println("Created snapshot table in given database...");
	    	sql = "CREATE TABLE IF NOT EXISTS activeVM " +
	                "(vmuuid VARCHAR(255) NOT NULL, " +
	                " state VARCHAR(255) NOT NULL, "+
	                " cpu DECIMAL(5,2) NOT NULL, "+
	                " memory DECIMAL(5,2) NOT NULL, "+
	                "PRIMARY KEY ( vmuuid )) WITH OIDS";  
	    	if((stmt.executeUpdate(sql))<0)
	    		System.out.println("Created vmSaveSnapshot table in given database...");
	    	stmt.close();
	    	dbConn.close();
	    			    
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class LoadHostList implements Runnable{
    	String subnet=null;
    	
    	public LoadHostList (String subnet) {
			this.subnet=subnet;
		}
    	
    	public void run() {
    		//to probe the network and load list of hodt with hyperviso in database.
    		int timeout=1000;
    		
//        ArrayList<String> hostURIList = new ArrayList<String>() ;
    		String hostIP;
    		Connect conn;
    		Connection dbConn=null;
    		PreparedStatement pstmt=null;
    		Statement stmt=null;
			ResultSet rs=null;
			String local=new String("localhost");
				try {
					stmt=DB.getConnection().createStatement();
					stmt.executeUpdate("INSERT INTO Host VALUES('"+local+"','"+local+"')");
					stmt.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    		while(true){
    			try {
    				dbConn = DB.getConnection();
    				pstmt=dbConn.prepareStatement("INSERT INTO Host VALUES(?,?)");
    				for (int i=82;i<144;i++) {
    					hostIP=subnet + "." + i;
    					    					   							
    					try {
    						if (InetAddress.getByName(hostIP).isReachable(timeout)){
    							String hostURI="qemu+tcp://"+hostIP+ "/system";
    							ConnectAuth ca= new ConnectAuthDefault();
    							conn=new Connect(hostURI,ca,0); //connecting to hypervisor		    
    						
    							if (conn.isConnected()){
    								rs=stmt.executeQuery("SELECT COUNT(*) AS total FROM Host WHERE hostIP = '"+hostIP+"'");
    								while (rs.next()) {
    									if(rs.getInt("total")==0){
    										pstmt.setString(1, hostIP);
    										pstmt.setString(2, conn.getHostName());
    										if(pstmt.executeUpdate()>0){
    											System.out.println("host "+hostIP+ " row added to table");
    										}
    										conn.close();
    									}
    								}
    							}else {
    								stmt=dbConn.createStatement();
    								rs=stmt.executeQuery("SELECT COUNT(*) AS total FROM Host WHERE hostIP = '"+hostIP+"'");
    								while (rs.next()) {
    									if(rs.getInt("total")!=0){
    										stmt.executeUpdate("DELETE FROM Host WHERE hostIP = '"+hostIP+"'");
    										System.out.println(hostIP+ "removed from table");
    									}
    								}
    							}
    						}else {
								stmt=dbConn.createStatement();
								rs=stmt.executeQuery("SELECT COUNT(*) AS total FROM Host WHERE hostIP = '"+hostIP+"'");
								while (rs.next()) {
									if(rs.getInt("total")!=0){
										stmt.executeUpdate("DELETE FROM Host WHERE hostIP = '"+hostIP+"'");
										System.out.println(hostIP+ "removed from table");
									}
								}
							}
    					} catch ( IOException | LibvirtException | SQLException e) {
    					// TODO Auto-generated catch block
    					System.err.println(e.getMessage());
    					}
    				}
    			}catch (SQLException e) {
    				// TODO Auto-generated catch block
    				
    				System.err.println(e.getMessage());
    			}
    		}
    	}
    }

	}

