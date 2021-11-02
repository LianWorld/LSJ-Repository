package utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * * @author 连仕杰
 *
 * title:
 * 0: imdb_title_id      1:  title                2:  original_title             3 : year
 * 4:  date_published    5:  genre                6:  duration                   7 : country
 * 8:  language          9:  director             10: writer                     11: production_company
 * 12: actors            13: description          14: avg_vote                   15: votes
 * 16: budget            17: usa_gross_income     18: worlwide_gross_income      19: metascore
 * 20: reviews_from_users                         21: reviews_from_critics
*/
public class DataFromCsv{

    String[] title;                             Table table;
    Configuration con;                          Connection conn;
    static int split = 14;                      List<Put> list = new ArrayList<Put>();
    static String tbName = "IMDb";              Admin admin;
    static String[] columnFamily = new String[]{"Info","Data"};
    static String csvSplitBy =",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
    int count = 0;


    public void init() throws IOException {
        con = new Configuration();
        con.set("hbase.zookeeper.quorum","hadoop102,hadoop103,hadoop104");
        System.setProperty("HADOOP_USER_NAME","hadoop");
        conn = ConnectionFactory.createConnection(con);
        admin = conn.getAdmin();
    }

    public void createTable() throws IOException {
        TableDescriptorBuilder tdb = TableDescriptorBuilder.newBuilder(TableName.valueOf(tbName));
        tdb.setColumnFamily(ColumnFamilyDescriptorBuilder.of(columnFamily[0])).build();
        tdb.setColumnFamily(ColumnFamilyDescriptorBuilder.of(columnFamily[1])).build();
        admin.createTable(tdb.build());
    }

    public void destroy() throws IOException {
        table.close();
        conn.close();
        admin.close();
    }

    @Test
    public void dataFromCsvToHbase() throws IOException {

        init();
        if(!admin.tableExists(TableName.valueOf(tbName))){
            createTable();
        }
        table = conn.getTable(TableName.valueOf(tbName));
        BufferedReader reader = new BufferedReader(
                new FileReader("src\\main\\dataset\\IMDb_movies.csv"));
        String line;
        line = reader.readLine();
        title = line.split(csvSplitBy);
        while ((line = reader.readLine()) != null) {
            count++;
            String[] item = line.split(csvSplitBy);
            Put p = new Put(Bytes.toBytes(item[0]));
            for (int i = 1 ; i < split ; i++){
                if(!("".equals(item[i]))) {
                    p.addColumn(Bytes.toBytes(columnFamily[0]),
                                Bytes.toBytes(title[i]),
                                Bytes.toBytes(item[i]));
                }
            }
            for (int i = split ; i < item.length ; i++){
                if(!("".equals(item[i]))) {
                    p.addColumn(Bytes.toBytes(columnFamily[1]),
                                Bytes.toBytes(title[i]),
                                Bytes.toBytes(item[i]));
                }
            }
            list.add(p);
            if(count % 10000 == 0 ){
                System.out.println("========>["+count+"/85855]");
            }
        }
        table.put(list);
        destroy();
    }
}